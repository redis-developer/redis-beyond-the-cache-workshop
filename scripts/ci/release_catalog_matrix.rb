#!/usr/bin/env ruby

require "json"
require "yaml"
require "pathname"

catalog_path = ARGV[0]

repo_root = Pathname.new(__dir__).join("..", "..").expand_path
source_path = catalog_path.nil? || catalog_path.empty? ? "workshops.yaml" : catalog_path
catalog = YAML.load_file(repo_root.join(source_path))

releases =
  if catalog["workshops"].is_a?(Array)
    catalog.fetch("workshops").flat_map do |workshop|
      workshop.fetch("releases", []).map do |release|
        release.merge("workshopId" => workshop.fetch("id"))
      end
    end
  else
    catalog.fetch("releases", [])
  end

def image_repository(reference)
  reference.split("@", 2).first
end

def docker_target(repo_root, workshop_id, role, images)
  backend_dockerfile = repo_root.join("java-springboot", workshop_id, "Dockerfile")
  frontend_dockerfile = repo_root.join("java-springboot", "#{workshop_id}_frontend", "Dockerfile")
  case role
  when "backend"
    [backend_dockerfile, [ "SKIP_FRONTEND_BUILD=true" ]]
  when "frontend"
    [frontend_dockerfile, [ "VUE_APP_BASE_PATH=/", "SKIP_FRONTEND_BUILD=false" ]]
  when "combined"
    [backend_dockerfile, [ "SKIP_FRONTEND_BUILD=true" ]]
  when "init"
    init_dockerfile = repo_root.join("java-springboot", workshop_id, "Dockerfile.init")
    unless init_dockerfile.file?
      abort("release source entry for #{workshop_id} declares init image but no java-springboot/#{workshop_id}/Dockerfile.init exists")
    end
    [init_dockerfile, []]
  else
    abort("unsupported image role #{role}")
  end
end

matrix = releases.flat_map do |release|
  images = release.fetch("images", {})
  roles = []
  roles << ["frontend", images["frontend"]] if images["frontend"]
  roles << ["backend", images["backend"]] if images["backend"]
  roles << ["combined", images["combined"]] if images["combined"]
  roles << ["init", images["init"]] if images["init"]
  if roles.empty?
    abort("release #{release.fetch("releaseId")} does not declare any image targets")
  end

  roles.map do |role, image_ref|
    dockerfile, build_args = docker_target(repo_root, release.fetch("workshopId"), role, images)
    unless dockerfile.file?
      abort("expected Dockerfile #{dockerfile.relative_path_from(repo_root)} for release #{release.fetch("releaseId")} role #{role}")
    end

    {
      "release_id" => release.fetch("releaseId"),
      "workshop_id" => release.fetch("workshopId"),
      "release_version" => release.fetch("releaseVersion"),
      "image_role" => role,
      "image_repository" => image_repository(image_ref),
      "dockerfile" => dockerfile.relative_path_from(repo_root).to_s,
      "build_context" => ".",
      "build_args" => build_args.join("\n")
    }
  end
end

puts JSON.generate({ "include" => matrix })
