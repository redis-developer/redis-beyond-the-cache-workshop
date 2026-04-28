rootProject.name = "redis-springboot-workshop"

// Shared infrastructure module (used by all workshops)
include("workshop-infrastructure")

// Note: Individual workshop modules are not included in Docker build
// They are accessed via volume mount at runtime
