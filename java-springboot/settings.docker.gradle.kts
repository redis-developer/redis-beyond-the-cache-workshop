rootProject.name = "redis-springboot-workshop"

// Workshop hub (main landing page)
include("workshop-hub")

// Shared infrastructure module (used by all workshops)
include("workshop-infrastructure")

// Note: Individual workshop modules are not included in Docker build
// They are accessed via volume mount at runtime

