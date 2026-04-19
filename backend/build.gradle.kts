plugins {
    kotlin("jvm") version "2.0.20"
    kotlin("plugin.serialization") version "2.0.20"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    application
}

group = "com.equipay"
version = "1.0.0"

application {
    mainClass.set("com.equipay.api.ApplicationKt")
}

tasks.shadowJar {
    archiveFileName.set("app.jar")
}
repositories {
    mavenCentral()
}

val ktorVersion = "2.3.12"
val exposedVersion = "0.53.0"
val logbackVersion = "1.5.6"

dependencies {
    // Ktor server
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-request-validation:$ktorVersion")

    // Ktor client (для OpenRouter, Resend, Tatra)
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")

    // Logging
    implementation("ch.qos.logback:logback-classic:$logbackVersion")

    // JWT
    implementation("com.auth0:java-jwt:4.4.0")

    // BCrypt
    implementation("org.mindrot:jbcrypt:0.4")

    // Database
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("org.postgresql:postgresql:42.7.3")
    implementation("com.zaxxer:HikariCP:5.1.0")

    // Redis
    implementation("redis.clients:jedis:5.1.3")

    // Email (fallback SMTP)
    implementation("org.simplejavamail:simple-java-mail:8.11.2")

    testImplementation(kotlin("test"))
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.equipay.api.ApplicationKt"
    }
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    archiveBaseName.set("equipay-api")
    archiveClassifier.set("all")
    mergeServiceFiles()
}

kotlin {
    jvmToolchain(17)
}
