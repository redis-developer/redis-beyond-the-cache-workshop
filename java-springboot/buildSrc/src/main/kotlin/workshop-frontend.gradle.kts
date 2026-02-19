/**
 * Gradle Convention Plugin for Redis Workshop Frontend Builds
 * 
 * This plugin automates the Vue.js frontend build process for workshop modules.
 * It handles npm installation and building, copying the output to Spring Boot's static resources.
 * 
 * Usage in workshop build.gradle.kts:
 *   plugins {
 *       id("workshop-frontend")
 *   }
 * 
 * The plugin expects:
 * - A 'frontend' directory in the workshop module root
 * - package.json with a 'build' script
 * - Vue.js build output configured to go to '../src/main/resources/static'
 */

// Configuration for frontend directory location
val frontendDir = file("frontend")
val staticDir = file("src/main/resources/static")

// Detect OS for npm command (Windows uses npm.cmd)
val isWindows = System.getProperty("os.name").lowercase().contains("windows")

/**
 * Task: Install npm dependencies
 * Runs 'npm install' in the frontend directory
 */
tasks.register<Exec>("npmInstall") {
    group = "workshop"
    description = "Install npm dependencies for the workshop frontend"
    
    workingDir = frontendDir
    
    if (isWindows) {
        commandLine("cmd", "/c", "npm", "install")
    } else {
        commandLine("sh", "-c", "npm install")
    }
    
    // Incremental build support
    inputs.file(file("$frontendDir/package.json"))
    inputs.file(file("$frontendDir/package-lock.json")).optional()
    outputs.dir(file("$frontendDir/node_modules"))
}

/**
 * Task: Build Vue.js frontend
 * Runs 'npm run build' to compile the Vue.js application
 */
tasks.register<Exec>("npmBuild") {
    group = "workshop"
    description = "Build the Vue.js frontend for the workshop"
    
    dependsOn("npmInstall")
    workingDir = frontendDir
    
    if (isWindows) {
        commandLine("cmd", "/c", "npm", "run", "build")
    } else {
        commandLine("sh", "-c", "npm run build")
    }
    
    // Incremental build support
    inputs.dir(frontendDir)
    inputs.files(fileTree(frontendDir) {
        include("src/**/*")
        include("public/**/*")
        include("*.js")
        include("*.json")
    })
    outputs.dir(staticDir)
}

/**
 * Hook into Spring Boot's processResources task
 * Ensures frontend is built before resources are processed
 */
tasks.named("processResources") {
    dependsOn("npmBuild")
}

/**
 * Optional: Clean task for frontend
 * Removes node_modules and build output
 */
tasks.register<Delete>("cleanFrontend") {
    group = "workshop"
    description = "Clean frontend build artifacts"
    
    delete(file("$frontendDir/node_modules"))
    delete(file("$frontendDir/dist"))
    delete(staticDir)
}

// Hook cleanFrontend into the main clean task
tasks.named("clean") {
    dependsOn("cleanFrontend")
}

