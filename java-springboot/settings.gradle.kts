rootProject.name = "redis-springboot-workshop"

// Workshop hub (main landing page)
include("workshop-hub")

// Shared infrastructure module (used by all workshops)
include("workshop-infrastructure")

// Individual workshop modules
include("1_session_management")
include("2_full_text_search")

include("3_distributed_locks")
