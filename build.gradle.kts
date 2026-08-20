tasks.register<Sync>("installDist") {
    description = "Builds mailclient and mailserver for distribution"
    group = "distribution"
    dependsOn(":frontend:installDist")
    dependsOn(":backend:installDist")

    into(layout.buildDirectory.dir("install"))
    from(project(":backend").layout.buildDirectory.dir("install"))
    from(project(":frontend").layout.buildDirectory.dir("install/browser")) {
        into("frontend")
    }
}

tasks.register<Delete>("clean") {
    description = "Cleans generated artifacts"
    group = "build"
    dependsOn(":frontend:clean")
    dependsOn(":backend:clean")
    delete(layout.buildDirectory)
}

tasks.register("lint") {
    description = "Runs static code analysis for the client and server"
    group = "verification"
    dependsOn(":frontend:lint", ":backend:lint")
}

tasks.register("check") {
    description = "Runs all tests, linters and the frontend production build"
    group = "verification"
    dependsOn("lint", ":backend:test", ":frontend:test", ":frontend:build")
}

tasks.register<Exec>("composeUp") {
    description = "Builds, verifies and starts the complete application with Docker Compose"
    group = "application"
    dependsOn("check")
    workingDir = rootDir
    commandLine("docker", "compose", "up", "--build", "--detach", "--wait", "--wait-timeout", "180")
}

tasks.register<Exec>("composeDown") {
    description = "Stops the Docker Compose application"
    group = "application"
    workingDir = rootDir
    commandLine("docker", "compose", "down")
}
