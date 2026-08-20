plugins {
    kotlin("jvm") version "2.4.10"
    kotlin("plugin.spring") version "2.4.10"
    id("org.springframework.boot") version "4.0.7"
    id("io.spring.dependency-management") version "1.1.7"
    id("dev.detekt") version "2.0.0-alpha.6"
    application
}

apply(plugin = "idea")
apply(plugin = "java")

application {
// Note: the main class in Kotlin has a "Kt" suffix when compiled,
// so we need to specify it here for the application plugin to work

    mainClass = "de.thm.mni.backend.BackendApplicationKt"
}

group = "de.thm.mni"
version = "0.0.1-SNAPSHOT"
description = "backend"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("tools.jackson.module:jackson-module-kotlin")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation(platform("software.amazon.awssdk:bom:2.49.6"))
    implementation("software.amazon.awssdk:s3")
    implementation("software.amazon.awssdk:url-connection-client")
    runtimeOnly("org.postgresql:postgresql:42.7.13")
    runtimeOnly("com.h2database:h2")
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25)
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

tasks.withType<JavaCompile> {
    options.release.set(25)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("quality-assurance/detekt.yml"))
}

tasks.register("lint") {
    description = "Runs Detekt static code analysis"
    group = "verification"
    dependsOn("detekt")
}

tasks.named("check") {
    dependsOn("lint")
}
