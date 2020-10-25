import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.palantir.gradle.gitversion.VersionDetails
import com.palantir.gradle.gitversion.GitVersionPlugin
import groovy.lang.Closure
import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.bmuschko.gradle.docker.tasks.image.DockerPushImage
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    idea
    kotlin("jvm") version "1.3.70"
    id("com.palantir.git-version") version "0.12.2"
    id("com.bmuschko.docker-remote-api") version "5.2.0"
    id("com.github.johnrengelman.shadow") version "5.1.0"
}

repositories {
    mavenCentral()
    jcenter()
}

buildscript {
    repositories {
        mavenCentral()
        jcenter()
    }

    dependencies {
        classpath(kotlin("gradle-plugin", version = "1.3.50"))
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.5")
    implementation("com.github.kittinunf.fuel:fuel:2.2.2")
    implementation("com.beust:klaxon:5.2")
    implementation("org.junit.jupiter:junit-jupiter:5.4.2")
}

// https://github.com/palantir/gradle-git-version/issues/105#issuecomment-523192407
val gitVersion: Closure<*> by extra
version = gitVersion()

val versionDetails: Closure<VersionDetails> by extra

val jar by tasks.getting(Jar::class) {
    manifest {
        attributes["Main-Class"] = "ch.nliechti.solaredge.MainKt"
    }
}

val shadowJar by tasks.getting(ShadowJar::class) {
    baseName = "operator"
    classifier = ""
    archiveVersion.value("")
}


tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "11"
}
val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "11"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "11"
}

val dockerImageName = "nliechti/solaredge_heater"

val dockerhubUsername: String by project
val dockerhubPassword: String by project

docker {
    registryCredentials {
        url.set("https://index.docker.io/v1/")
        username.set("$dockerhubUsername")
        password.set("$dockerhubPassword")
    }
}

fun isCleanVersion(): Boolean {
    return !version.toString().contains("dirty") && versionDetails().isCleanTag
}

tasks.create("buildDockerImage", DockerBuildImage::class) {
    dependsOn("shadowJar")
    inputDir.set(file("."))
    dockerFile.set(file("./docker/Dockerfile"))
    if (isCleanVersion()) {
        tags.add("$dockerImageName:$version")
    }
    tags.add("$dockerImageName:latest")
}

tasks.create("pushDockerImage", DockerPushImage::class) {
    dependsOn("buildDockerImage")
    imageName.set("$dockerImageName:$version")
}

tasks.create("pushLatestDockerImage", DockerPushImage::class) {
    dependsOn("buildDockerImage")
    imageName.set("$dockerImageName:latest")
}

tasks.create("pushVersionedDockerImage", DockerPushImage::class) {
    dependsOn("buildDockerImage")

    imageName.set("$dockerImageName:$version")
}

tasks.create("pushDockerImages") {
    if (isCleanVersion()) {
        dependsOn("pushVersionedDockerImage")
    }
    dependsOn("pushLatestDockerImage")
}