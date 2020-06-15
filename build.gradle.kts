plugins {
    application
    kotlin("jvm") version "1.3.72"
}

// TODO Fix Gradle 7 warnings
application {
    mainClassName = "grappolo.GrappoloKt"
}

group = "grappolo"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("info.debatty:java-string-similarity:2.0.0")
    implementation("org.apache.lucene:lucene-spellchecker:3.6.2")

    implementation("ch.qos.logback:logback-classic:1.2.3")

    testCompile("junit:junit:4.12")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}