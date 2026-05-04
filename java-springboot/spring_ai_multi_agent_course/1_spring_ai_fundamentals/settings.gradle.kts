rootProject.name = "redis-springboot-workshop"

include("workshop-infrastructure")
project(":workshop-infrastructure").projectDir = file("../../workshop-infrastructure")

include("1_spring_ai_fundamentals")
project(":1_spring_ai_fundamentals").projectDir = file(".")
