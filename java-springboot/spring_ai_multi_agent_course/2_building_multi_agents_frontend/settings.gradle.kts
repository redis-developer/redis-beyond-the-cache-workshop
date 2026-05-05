rootProject.name = "redis-springboot-workshop"

include("workshop-infrastructure")
project(":workshop-infrastructure").projectDir = file("../../workshop-infrastructure")

include("2_building_multi_agents_frontend")
project(":2_building_multi_agents_frontend").projectDir = file(".")
