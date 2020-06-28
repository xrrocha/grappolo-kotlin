plugins {

    val kotlinVersion = "1.3.72"

    application
    kotlin("jvm") version kotlinVersion
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

    val arrowVersion = "0.10.5"
    val hsqldbVersion = "2.5.0"
    val jacksonVersion = "2.11.0"
    val junitVersion = "4.13"
    val kotlinVersion = "1.3.72"
    val kotlinxVersion = "1.3.7"
    val logbackVersion = "1.2.3"
    val postgresVersion = "42.2.12"

    implementation(kotlin("stdlib"))

    implementation("org.jetbrains.kotlin:kotlin-scripting-jsr223:$kotlinVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxVersion")

    implementation("info.debatty:java-string-similarity:2.0.0")
    implementation("org.apache.lucene:lucene-spellchecker:3.6.2")
    implementation("com.wcohen:com.wcohen.secondstring:0.1")

    implementation("io.arrow-kt:arrow-core:$arrowVersion")
    implementation("io.arrow-kt:arrow-fx:$arrowVersion")

    runtimeOnly("org.postgresql:postgresql:$postgresVersion")

    testImplementation("junit:junit:$junitVersion")
    testImplementation("org.hsqldb:hsqldb:$hsqldbVersion")

    implementation("ch.qos.logback:logback-classic:$logbackVersion")

    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:$jacksonVersion")
}

//tasks {
//    compileKotlin {
//        kotlinOptions.jvmTarget = "1.8"
//    }
//    compileTestKotlin {
//        kotlinOptions.jvmTarget = "1.8"
//    }
//}