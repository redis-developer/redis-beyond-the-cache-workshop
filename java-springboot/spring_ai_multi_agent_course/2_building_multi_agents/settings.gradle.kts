pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven { url = uri("https://repo.spring.io/milestone") }
        maven { url = uri("https://repo.spring.io/snapshot") }
    }
    plugins {
        id("org.springframework.boot") version "3.5.10"
        id("io.spring.dependency-management") version "1.1.7"
    }
}

rootProject.name = "2_building_multi_agents"

include("workshop-infrastructure")
project(":workshop-infrastructure").projectDir = file("../../workshop-infrastructure")
