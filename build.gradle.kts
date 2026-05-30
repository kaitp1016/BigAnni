plugins {
    kotlin("jvm") version "2.3.21"
    id("com.gradleup.shadow") version "8.3.0"
    id("xyz.jpenilla.run-paper") version "3.0.2"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.21"
}

group = "me.kaitp1016"
version = "1.3.2"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
}

dependencies {
    paperweight.paperDevBundle("26.1.2.build.+")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
}

tasks {
    runServer {
        minecraftVersion("26.1.2")
    }
}

val targetJavaVersion = 25
kotlin {
    jvmToolchain(targetJavaVersion)
}

tasks.build {
    dependsOn("shadowJar")
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("paper-plugin.yml") {
        expand(props)
    }
}
