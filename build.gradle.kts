plugins {
    java
    application
    id("org.jetbrains.kotlin.jvm") version "2.4.0"
    id("org.javamodularity.moduleplugin") version "2.0.1"
    id("org.openjfx.javafxplugin") version "0.1.0"
    id("org.beryx.jlink") version "4.1.0"
}

group = "org.dba"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val junitVersion = "5.12.1"


tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

application {
    mainModule.set("org.dba.datafx")
    mainClass.set("org.dba.datafx.MainApplication")
}
kotlin {
    jvmToolchain( 26 )
}

javafx {
    version = "26.0.2"
    modules = listOf("javafx.controls", "javafx.fxml")
}

dependencies {
    implementation("org.mongodb:mongodb-driver-kotlin-coroutine:5.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-javafx:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    implementation("org.apache.commons:commons-lang3:3.20.0")
    implementation("org.apache.poi:poi-ooxml:5.5.1")
    implementation("org.slf4j:slf4j-simple:2.0.18")
    implementation("org.apache.logging.log4j:log4j-to-slf4j:2.26.1")

    testImplementation("org.junit.jupiter:junit-jupiter-api:${junitVersion}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${junitVersion}")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<Jar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes["Main-Class"] = application.mainClass.get()
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}

jlink {
    imageZip.set(layout.buildDirectory.file("/distributions/app-${javafx.platform.classifier}.zip"))
    options.set(listOf("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages"))
    launcher {
        name = "app"
    }
}
