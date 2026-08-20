import com.github.gradle.node.npm.task.NpmTask

plugins {
  id("com.github.node-gradle.node") version "7.1.0"
}

apply(plugin = "idea")

node {
  download = true
  version = "24.19.0"
  npmVersion = "11.19.0"
}

val npmCi by tasks.registering(NpmTask::class) {
  group = "build setup"
  dependsOn("npmSetup")
  args.set(listOf("ci"))
  workingDir.set(project.projectDir)
}

tasks.register<NpmTask>("build") {
  group = "build"
  dependsOn(npmCi)
  args.set(listOf("run", "build"))
  workingDir.set(project.projectDir)
}

tasks.register<NpmTask>("test") {
  group = "verification"
  dependsOn(npmCi)
  args.set(listOf("test", "--", "--watch=false"))
  workingDir.set(project.projectDir)
}

tasks.register<NpmTask>("lint") {
  group = "verification"
  dependsOn(npmCi)
  args.set(listOf("run", "lint"))
  workingDir.set(project.projectDir)
}

tasks.register("installDist") {
  dependsOn("build")
}

tasks.register<Delete>("clean") {
  val modulePath = layout.projectDirectory
  delete(modulePath.dir(".angular"))
  delete(modulePath.dir(".gradle"))
  delete(modulePath.dir("build"))
  delete(modulePath.dir("dist"))
}
