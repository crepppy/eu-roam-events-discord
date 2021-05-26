import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.32"
    application
    id ("com.github.johnrengelman.shadow") version "6.1.0"
}

group = "com.jackchapman"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-scripting-jsr223:1.4.32")
    implementation(kotlin("stdlib-jdk8"))

    // Logger
    compileOnly("org.apache.logging.log4j:log4j-core:2.14.0")
    compileOnly("org.apache.logging.log4j:log4j-api:2.14.0")
    implementation("ch.qos.logback:logback-classic:1.2.3")

    // Discord Library
    implementation("dev.kord:kord-core:0.7.x-SNAPSHOT")
    implementation("dev.kord.x:emoji:0.5.0-SNAPSHOT")

    // Reflection
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.4.32")
    implementation("org.reflections:reflections:0.9.12")

    // Database
    implementation("org.jetbrains.exposed:exposed-core:0.30.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.30.1")
    implementation("mysql:mysql-connector-java:8.0.19")
    implementation("com.zaxxer:HikariCP:3.4.2")

    // Config
    implementation("com.sksamuel.hoplite:hoplite-core:1.4.0")
    implementation("com.sksamuel.hoplite:hoplite-toml:1.4.0")

    // WebServer
    implementation("io.ktor:ktor-server-core:1.5.3")
    implementation("io.ktor:ktor-server-netty:1.5.3")
    implementation("io.ktor:ktor-gson:1.5.3")
    implementation("io.ktor:ktor-html-builder:1.5.3")

    // HTTP Client
    implementation("io.ktor:ktor-client-gson:1.5.3")
    implementation("io.ktor:ktor-client-core:1.5.3")

    // Google Sheets
    implementation("com.google.api-client:google-api-client:1.30.4")
    implementation("com.google.apis:google-api-services-sheets:v4-rev581-1.25.0")
    implementation("com.google.oauth-client:google-oauth-client-jetty:1.30.6")

    implementation("io.insert-koin:koin-core-ext:3.0.1-beta-2") // Dependency Injection
    implementation("org.openid4java:openid4java:1.0.0") // Steam Authentication
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

tasks.withType<Jar> {
    manifest {
        attributes(
            mapOf(
                "Main-Class" to application.mainClass
            )
        )
    }
}

application {
    mainClass.set("com.jackchapman.eurustevents.BotKt")

    @kotlin.Suppress("DEPRECATION")
    mainClassName = "com.jackchapman.eurustevents.BotKt"
}