#!/usr/bin/env ruby
# frozen_string_literal: true

require "json"
require "optparse"
require "pathname"
require "time"
require "yaml"

ROOT = Pathname.new(__dir__).join("..", "..").expand_path
DIGEST_REFERENCE_PATTERN = /\A(?<repository>[^@\s]+)@(?<digest>sha256:[0-9a-f]{64})\z/
SUPPORTED_IMAGE_ROLES = %w[frontend backend combined init].freeze

def fail!(message)
  warn message
  exit 1
end

def normalize_release_source(payload, file_path)
  if payload["workshops"].is_a?(Array)
    releases = payload.fetch("workshops").flat_map do |workshop|
      fail!("Workshop registry entries must be mappings: #{file_path}") unless workshop.is_a?(Hash)

      workshop_id = workshop["id"].to_s.strip
      fail!("Workshop registry entry is missing id: #{file_path}") if workshop_id.empty?

      workshop.fetch("releases", []).map do |release|
        fail!("Workshop #{workshop_id} release entry must be a mapping") unless release.is_a?(Hash)

        release.merge("workshopId" => workshop_id)
      end
    end

    return {
      "version" => payload["version"],
      "releases" => releases,
      "path" => file_path
    }
  end

  {
    "version" => payload["version"],
    "releases" => payload["releases"],
    "path" => file_path
  }
end

def read_catalog(path)
  file_path = Pathname.new(path).expand_path
  fail!("Release source not found: #{file_path}") unless file_path.file?

  payload = YAML.safe_load(file_path.read, permitted_classes: [], aliases: false)
  fail!("Release source root must be a mapping: #{file_path}") unless payload.is_a?(Hash)

  catalog = normalize_release_source(payload, file_path)
  version = catalog["version"]
  releases = catalog["releases"]

  fail!("Release source version must be a positive integer: #{file_path}") unless version.is_a?(Integer) && version.positive?
  fail!("Release source releases must be a non empty list: #{file_path}") unless releases.is_a?(Array) && !releases.empty?

  catalog
end

def non_blank_string!(value, field_name, release_id)
  value = value.to_s.strip
  fail!("Release #{release_id} is missing #{field_name}") if value.empty?

  value
end

def registry_for(repository, release_id, image_role)
  registry = repository.split("/", 2).first
  unless registry.include?(".") || registry.include?(":") || registry == "localhost"
    fail!("Release #{release_id} image role #{image_role} must use an explicit registry host: #{repository}")
  end

  registry
end

def resolve_target(workshop_id, image_role, release_id)
  module_path =
    case image_role
    when "frontend"
      ROOT.join("java-springboot", "#{workshop_id}_frontend")
    when "backend", "combined", "init"
      ROOT.join("java-springboot", workshop_id)
    else
      fail!("Release #{release_id} uses unsupported image role #{image_role}")
    end

  dockerfile =
    case image_role
    when "init"
      module_path.join("Dockerfile.init")
    else
      module_path.join("Dockerfile")
    end

  fail!("Release #{release_id} image role #{image_role} maps to a missing module path: #{module_path.relative_path_from(ROOT)}") unless module_path.directory?
  fail!("Release #{release_id} image role #{image_role} maps to a missing Dockerfile: #{dockerfile.relative_path_from(ROOT)}") unless dockerfile.file?

  {
    "build_context" => ".",
    "dockerfile" => dockerfile.relative_path_from(ROOT).to_s
  }
end

def release_targets(catalog)
  release_ids = {}
  release_versions = {}
  workshop_defaults = Hash.new(0)
  targets = []

  catalog.fetch("releases").each do |release|
    fail!("Each release source entry must be a mapping") unless release.is_a?(Hash)

    release_id = non_blank_string!(release["releaseId"], "releaseId", "<unknown>")
    workshop_id = non_blank_string!(release["workshopId"], "workshopId", release_id)
    release_version = non_blank_string!(release["releaseVersion"], "releaseVersion", release_id)
    non_blank_string!(release["mode"], "mode", release_id)
    non_blank_string!(release["resourceClass"], "resourceClass", release_id)

    session_ttl_minutes = release["sessionTtlMinutes"]
    unless session_ttl_minutes.is_a?(Integer) && session_ttl_minutes.positive?
      fail!("Release #{release_id} must define a positive sessionTtlMinutes")
    end

    if release_ids.key?(release_id)
      fail!("Duplicate releaseId detected in release source: #{release_id}")
    end
    release_ids[release_id] = true

    workshop_release_key = "#{workshop_id}:#{release_version}"
    if release_versions.key?(workshop_release_key)
      fail!("Duplicate workshopId and releaseVersion detected in release source: #{workshop_release_key}")
    end
    release_versions[workshop_release_key] = true

    if release["defaultForWorkshop"]
      workshop_defaults[workshop_id] += 1
      if workshop_defaults[workshop_id] > 1
        fail!("Workshop #{workshop_id} has more than one default release")
      end
    end

    images = release["images"]
    fail!("Release #{release_id} must define at least one image reference") unless images.is_a?(Hash) && !images.empty?

    images.each do |image_role, source_reference|
      role = image_role.to_s
      fail!("Release #{release_id} uses unsupported image role #{role}") unless SUPPORTED_IMAGE_ROLES.include?(role)

      reference = non_blank_string!(source_reference, "images.#{role}", release_id)
      match = DIGEST_REFERENCE_PATTERN.match(reference)
      fail!("Release #{release_id} image role #{role} must be digest pinned: #{reference}") unless match

      target = resolve_target(workshop_id, role, release_id)
      repository = match[:repository]

      targets << {
        "release_id" => release_id,
        "workshop_id" => workshop_id,
        "release_version" => release_version,
        "image_role" => role,
        "image_reference" => reference,
        "repository" => repository,
        "registry" => registry_for(repository, release_id, role),
        "dockerfile" => target.fetch("dockerfile"),
        "build_context" => target.fetch("build_context")
      }
    end
  end

  fail!("Release source did not produce any build targets") if targets.empty?

  targets.sort_by { |target| [target.fetch("release_id"), target.fetch("image_role")] }
end

def parse_bool(value, option_name)
  normalized = value.to_s.strip.downcase
  return true if normalized == "true"
  return false if normalized == "false"

  fail!("Expected #{option_name} to be true or false, got #{value.inspect}")
end

def write_json(path, payload)
  output_path = Pathname.new(path).expand_path
  output_path.dirname.mkpath
  output_path.write(JSON.pretty_generate(payload) + "\n")
end

def matrix_command(argv)
  options = {
    catalog: ROOT.join("workshops.yaml").to_s
  }

  parser = OptionParser.new do |opts|
    opts.on("--catalog PATH", String) { |value| options[:catalog] = value }
  end
  parser.parse!(argv)

  catalog = read_catalog(options.fetch(:catalog))
  puts JSON.generate({ "include" => release_targets(catalog) })
end

def result_command(argv)
  options = {
    tags: []
  }

  parser = OptionParser.new do |opts|
    opts.on("--release-id VALUE", String) { |value| options[:release_id] = value }
    opts.on("--workshop-id VALUE", String) { |value| options[:workshop_id] = value }
    opts.on("--release-version VALUE", String) { |value| options[:release_version] = value }
    opts.on("--image-role VALUE", String) { |value| options[:image_role] = value }
    opts.on("--registry VALUE", String) { |value| options[:registry] = value }
    opts.on("--repository VALUE", String) { |value| options[:repository] = value }
    opts.on("--source-reference VALUE", String) { |value| options[:source_reference] = value }
    opts.on("--source-revision VALUE", String) { |value| options[:source_revision] = value }
    opts.on("--published VALUE", String) { |value| options[:published] = parse_bool(value, "--published") }
    opts.on("--digest VALUE", String) { |value| options[:digest] = value }
    opts.on("--tag VALUE", String) { |value| options[:tags] << value }
    opts.on("--output PATH", String) { |value| options[:output] = value }
  end
  parser.parse!(argv)

  %i[
    release_id
    workshop_id
    release_version
    image_role
    registry
    repository
    source_reference
    source_revision
    published
    output
  ].each do |required_key|
    fail!("Missing required option #{required_key}") unless options.key?(required_key)
  end

  digest = options[:digest]
  if options[:published]
    fail!("Published result is missing a digest for #{options[:release_id]} #{options[:image_role]}") if digest.to_s.strip.empty?
    unless digest.match?(/\Asha256:[0-9a-f]{64}\z/)
      fail!("Published digest must be sha256 pinned for #{options[:release_id]} #{options[:image_role]}: #{digest}")
    end
  else
    digest = nil
  end

  payload = {
    "schemaVersion" => 1,
    "releaseId" => options[:release_id],
    "workshopId" => options[:workshop_id],
    "releaseVersion" => options[:release_version],
    "imageRole" => options[:image_role],
    "registry" => options[:registry],
    "repository" => options[:repository],
    "sourceReference" => options[:source_reference],
    "publishedReference" => digest ? "#{options[:repository]}@#{digest}" : nil,
    "digest" => digest,
    "sourceRevision" => options[:source_revision],
    "published" => options[:published],
    "tags" => options[:tags]
  }

  write_json(options[:output], payload)
end

def aggregate_command(argv)
  options = {
    catalog: ROOT.join("workshops.yaml").to_s
  }

  parser = OptionParser.new do |opts|
    opts.on("--catalog PATH", String) { |value| options[:catalog] = value }
    opts.on("--input-dir PATH", String) { |value| options[:input_dir] = value }
    opts.on("--output PATH", String) { |value| options[:output] = value }
    opts.on("--publish VALUE", String) { |value| options[:publish] = parse_bool(value, "--publish") }
    opts.on("--source-revision VALUE", String) { |value| options[:source_revision] = value }
  end
  parser.parse!(argv)

  %i[input_dir output publish source_revision].each do |required_key|
    fail!("Missing required option #{required_key}") unless options.key?(required_key)
  end

  catalog = read_catalog(options.fetch(:catalog))
  input_dir = Pathname.new(options.fetch(:input_dir)).expand_path
  fail!("Aggregate input directory not found: #{input_dir}") unless input_dir.directory?

  result_files = input_dir.glob("*.json").sort
  fail!("Aggregate input directory did not contain any JSON result files: #{input_dir}") if result_files.empty?

  releases = {}

  result_files.each do |result_file|
    payload = JSON.parse(result_file.read)
    release_id = payload.fetch("releaseId")
    workshop_id = payload.fetch("workshopId")
    release_version = payload.fetch("releaseVersion")
    image_role = payload.fetch("imageRole")

    releases[release_id] ||= {
      "releaseId" => release_id,
      "workshopId" => workshop_id,
      "releaseVersion" => release_version,
      "images" => {}
    }

    current_release = releases.fetch(release_id)
    if current_release.fetch("workshopId") != workshop_id || current_release.fetch("releaseVersion") != release_version
      fail!("Inconsistent aggregate data for release #{release_id}")
    end

    current_release.fetch("images")[image_role] = {
      "role" => image_role,
      "registry" => payload.fetch("registry"),
      "repository" => payload.fetch("repository"),
      "sourceReference" => payload.fetch("sourceReference"),
      "publishedReference" => payload["publishedReference"],
      "digest" => payload["digest"],
      "sourceRevision" => payload.fetch("sourceRevision"),
      "published" => payload.fetch("published"),
      "tags" => payload.fetch("tags")
    }
  end

  artifact = {
    "schemaVersion" => 1,
    "catalogVersion" => catalog.fetch("version"),
    "catalogPath" => catalog.fetch("path").relative_path_from(ROOT).to_s,
    "publish" => options.fetch(:publish),
    "sourceRevision" => options.fetch(:source_revision),
    "generatedAt" => Time.now.utc.iso8601,
    "releases" => releases.sort.to_h
  }

  write_json(options[:output], artifact)
end

command = ARGV.shift

case command
when "matrix"
  matrix_command(ARGV)
when "result"
  result_command(ARGV)
when "aggregate"
  aggregate_command(ARGV)
else
  fail!("Usage: #{File.basename($PROGRAM_NAME)} <matrix|result|aggregate> [options]")
end
