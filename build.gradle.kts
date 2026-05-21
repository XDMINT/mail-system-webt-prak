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