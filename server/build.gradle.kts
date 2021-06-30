import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.10"
    application
}

group = "com.jackchapman"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven")
}

dependencies {
    runtimeOnly("org.jetbrains.kotlin:kotlin-scripting-jsr223:1.4.32")
    implementation(kotlin("stdlib"))

    // Logger
    compileOnly("org.apache.logging.log4j:log4j-core:2.14.0")
    compileOnly("org.apache.logging.log4j:log4j-api:2.14.0")
    implementation("ch.qos.logback:logback-classic:1.2.3")

    // Discord Library
    implementation("dev.kord:kord-core:0.7.0")

    // Database
    implementation("org.jetbrains.exposed:exposed-core:0.30.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.30.1")
    implementation("mysql:mysql-connector-java:8.0.19")
    implementation("com.zaxxer:HikariCP:3.4.2")

    // Config
    implementation("com.sksamuel.hoplite:hoplite-core:1.4.0")
    implementation("com.sksamuel.hoplite:hoplite-toml:1.4.0")

    // WebServer
    implementation("io.ktor:ktor-server-core:1.6.0")
    implementation("io.ktor:ktor-server-netty:1.6.0")
    implementation("io.ktor:ktor-gson:1.6.0")
    implementation("io.ktor:ktor-html-builder:1.6.0")
    implementation("io.ktor:ktor-websockets:1.6.0")
    implementation("io.ktor:ktor-auth:1.6.0")

    // HTTP Client
    implementation("io.ktor:ktor-client-gson:1.6.0")
    implementation("io.ktor:ktor-client-core:1.6.0")

    implementation("io.insert-koin:koin-core-ext:3.0.1-beta-2") // Dependency Injection
    implementation("org.openid4java:openid4java:1.0.0") // Steam Authentication
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = application.mainClass
    }
}
tasks {
    "processResources" {
        dependsOn(":frontend:build")
    }
}

application {
    mainClass.set("com.jackchapman.eurustevents.BotKt")
}