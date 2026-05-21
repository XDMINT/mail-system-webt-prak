import com.github.gradle.node.npm.task.NpmTask

plugins {
  id("com.github.node-gradle.node") version "7.1.0"
}

apply(plugin = "idea")

node {
  download = true
  version = "24.12.0"
  npmVersion = "11.6.2"
}

tasks.register<com.github.gradle.node.npm.task.NpmTask>("build") {
  group = "build"
  dependsOn("npmSetup", "npmInstall")
  args.set(listOf("run", "build"))
  workingDir.set(project.projectDir)
}

tasks.register("installDist") {
  dependsOn("build")
  doLast {
    copy {
      from("dist/browser")
      into("build/install/browser")
    }
  }
}

tasks.register<Delete>("clean") {
  val modulePath = project.rootDir.resolve("frontend")
  delete(modulePath.resolve(".angular"))
  delete(modulePath.resolve(".gradle"))
  delete(modulePath.resolve("build"))
  delete(modulePath.resolve("dist"))
  //delete(modulePath.resolve("node_modules"))
}
