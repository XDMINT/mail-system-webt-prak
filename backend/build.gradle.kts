plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.spring") version "2.2.21"
    id("org.springframework.boot") version "4.0.6"
    id("io.spring.dependency-management") version "1.1.7"
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
    implementation("org.springframework.boot:spring-boot-starter-webmvc:4.0.6")
    implementation("org.springframework.boot:spring-boot-starter-mail:4.0.6")
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.2.21")
    implementation("tools.jackson.module:jackson-module-kotlin:3.1.2")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test:4.0.6")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:2.2.21")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.0.3")
    implementation("org.springframework.boot:spring-boot-starter-security:4.0.6")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa:4.0.6")
    implementation("com.auth0:java-jwt:4.4.0")
    implementation("org.springframework.boot:spring-boot-starter-validation:4.0.6")
    runtimeOnly("org.postgresql:postgresql:42.7.10")
    runtimeOnly("com.h2database:h2:2.4.240")
}

kotlin {
    // Kotlin 2.2.x kann noch kein JVM-25-Bytecode erzeugen; wir kompilieren daher auf 24,
    // laufen aber weiterhin mit JDK 25 (Toolchain) fuer Build und Runtime.
    jvmToolchain(25)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_24)
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

tasks.withType<JavaCompile> {
    options.release.set(24)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.register("hello world") {
    group = "hello"
    description = "Hello World"
    dependsOn("build")
    println("Hello World, during confuguration")

    doFirst {
        println("firts Hello world")
    }
    doLast {
        println("last Hello world")
    }
}
