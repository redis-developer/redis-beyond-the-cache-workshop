rootProject.name = "redis-springboot-workshop"

// Shared infrastructure module (used by all workshops)
include("workshop-infrastructure")

// Platform control plane
include("platform_control_plane")

// Platform execution plane and shared internal contracts
include("platform_contracts")
include("platform_execution_plane")

// Individual workshop modules
include("1_session_management")
include("1_session_management_frontend")
include("2_full_text_search")
include("2_full_text_search_frontend")
include("3_distributed_locks")
include("3_distributed_locks_frontend")
include("4_agent_memory")
include("4_agent_memory_frontend")

// Spring AI Multi Agent course modules
include("1_spring_ai_fundamentals")
project(":1_spring_ai_fundamentals").projectDir = file("spring_ai_multi_agent_course/1_spring_ai_fundamentals")
include("1_spring_ai_fundamentals_frontend")
project(":1_spring_ai_fundamentals_frontend").projectDir = file("spring_ai_multi_agent_course/1_spring_ai_fundamentals_frontend")
