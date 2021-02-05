plugins {
    kotlin("jvm") version "1.4.21"
    application
}

group = "no.not.none.nexus"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.apache.maven", "maven-artifact", "3.6.3")
    implementation("org.glassfish.jersey.core", "jersey-client", "3.0.0")
    implementation("org.glassfish", "jsonp-jaxrs", "2.0.0")
    implementation("commons-cli", "commons-cli", "1.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
    implementation("org.apache.maven", "maven-model", "3.6.3")
    compileOnly("jakarta.ws.rs", "jakarta.ws.rs-api", "3.0.0")
    compileOnly("jakarta.json", "jakarta.json-api", "2.0.0")
    runtimeOnly("org.glassfish", "jakarta.json", "2.0.0")
    runtimeOnly("org.glassfish.jersey.inject", "jersey-hk2", "3.0.0")
}

application {
    mainClass.set("no.not.none.nexus.AppKt")
}