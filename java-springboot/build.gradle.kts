import org.gradle.internal.os.OperatingSystem
import org.gradle.api.tasks.StopExecutionException
plugins {
    java
    id("org.springframework.boot") version "3.5.10" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

allprojects {
    group = "com.redis.workshop"
    version = "1.0.0"

    repositories {
        mavenCentral()
    }
}

val skipFrontendBuild = (findProperty("skipFrontendBuild") as String?)?.toBoolean() == true

fun findExecutableOnPath(executableName: String): String? {
    val pathEntries = (System.getenv("PATH") ?: "")
        .split(File.pathSeparatorChar)
        .filter { it.isNotBlank() }
    val extensions = if (OperatingSystem.current().isWindows) {
        (System.getenv("PATHEXT") ?: ".EXE;.BAT;.CMD")
            .split(';')
            .filter { it.isNotBlank() }
    } else {
        listOf("")
    }
    for (entry in pathEntries) {
        for (ext in extensions) {
            val candidate = File(entry, executableName + ext)
            if (candidate.isFile && candidate.canExecute()) {
                return candidate.absolutePath
            }
        }
    }
    return null
}

fun registerFrontendBuildTask(
    project: Project,
    frontendDir: File,
    outputDir: File
): TaskProvider<Exec> {
    return project.tasks.register<Exec>("buildFrontend") {
        val npmCmd = if (OperatingSystem.current().isWindows) "npm.cmd" else "npm"
        val npmExecutable = findExecutableOnPath(npmCmd)
        workingDir = frontendDir
        onlyIf { !skipFrontendBuild }
        commandLine(npmExecutable ?: npmCmd, "run", "build")
        inputs.dir(frontendDir.resolve("src"))
        inputs.dir(frontendDir.resolve("public"))
        inputs.files(
            frontendDir.resolve("package.json"),
            frontendDir.resolve("package-lock.json"),
            frontendDir.resolve("vue.config.js")
        )
        outputs.dir(outputDir)
        doFirst {
            if (npmExecutable == null) {
                if (outputDir.exists()) {
                    logger.lifecycle("npm not found; skipping frontend build and using existing assets.")
                    throw StopExecutionException("npm not found")
                }
                throw GradleException("npm not found. Install Node.js/npm or run with -PskipFrontendBuild=true after building the frontend once.")
            }
        }
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")

    java {
        sourceCompatibility = JavaVersion.VERSION_21
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    val frontendDir = if (name == "workshop-hub") {
        rootProject.file("../frontend")
    } else {
        projectDir.resolve("frontend")
    }
    if (frontendDir.exists()) {
        val outputDir = if (name == "workshop-hub") {
            frontendDir.resolve("dist")
        } else {
            projectDir.resolve("src/main/resources/static")
        }
        val buildFrontend = registerFrontendBuildTask(this, frontendDir, outputDir)
        tasks.matching { it.name == "bootRun" }.configureEach {
            dependsOn(buildFrontend)
        }
    }
}
