rootProject.name = "4_agent_memory"

// Include shared infrastructure module
includeBuild("../workshop-infrastructure") {
    dependencySubstitution {
        substitute(module("com.redis.workshop:workshop-infrastructure")).using(project(":"))
    }
}

includeBuild("../buildSrc")

