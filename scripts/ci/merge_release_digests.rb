#!/usr/bin/env ruby

require "json"
require "pathname"
require "time"

artifacts_dir = ARGV[0]
source_revision = ARGV[1]
abort("usage: ruby scripts/ci/merge_release_digests.rb <artifact-dir> <source-revision>") if artifacts_dir.nil? || source_revision.nil?

artifact_root = Pathname.new(artifacts_dir)
entries = artifact_root.glob("*.json").sort.map do |path|
  JSON.parse(path.read)
end

puts JSON.pretty_generate(
  {
    "version" => 1,
    "source_revision" => source_revision,
    "generated_at" => Time.now.utc.iso8601,
    "entries" => entries
  }
)
