rootProject.name = "redis-springboot-workshop"

include("workshop-infrastructure")
project(":workshop-infrastructure").projectDir = file("../../workshop-infrastructure")

include("3_multi_agent_patterns_frontend")
project(":3_multi_agent_patterns_frontend").projectDir = file(".")
