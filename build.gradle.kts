tasks.register("installDist") {
    description = "Builds mailclient and mailserver for distribution"
    group = "distribution"
    dependsOn(":frontend:installDist")
    dependsOn(":backend:installDist")
    doLast {
        copy {
            from(project(":backend").layout.buildDirectory.dir("install"))
            into(project.rootDir.resolve("build/install"))
        }
        copy {
            from(project(":frontend").layout.buildDirectory.dir("install/browser"))
            into(project.rootDir.resolve("build/install/frontend"))
        }
    }
}
tasks.register("clean") {
    description = "Cleans generated artifacts"
    group = "build"
    dependsOn(":frontend:clean")
    dependsOn(":backend:clean")
    delete(project.rootDir.resolve("build"))
}

tasks.register<Exec>("composeUp") {
    description = "Builds, verifies and starts the complete application with Docker Compose"
    group = "application"
    dependsOn(":backend:test", ":frontend:build", ":frontend:test")
    workingDir = project.rootDir
    commandLine("docker", "compose", "up", "--build", "--detach", "--wait", "--wait-timeout", "180")
}

tasks.register<Exec>("composeDown") {
    description = "Stops the Docker Compose application"
    group = "application"
    workingDir = project.rootDir
    commandLine("docker", "compose", "down")
}
