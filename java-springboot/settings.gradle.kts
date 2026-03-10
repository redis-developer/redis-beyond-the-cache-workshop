rootProject.name = "redis-springboot-workshop"

// Workshop hub (main landing page)
include("workshop-hub")

// Shared infrastructure module (used by all workshops)
include("workshop-infrastructure")

// Individual workshop modules
include("1_session_management")
include("1_session_management_frontend")
include("2_full_text_search")
include("2_full_text_search_frontend")
include("3_distributed_locks")
include("3_distributed_locks_frontend")
include("4_agent_memory")
include("4_agent_memory_frontend")
