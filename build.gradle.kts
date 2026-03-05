import pl.allegro.tech.build.axion.release.domain.PredefinedVersionCreator

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    alias(libs.plugins.pluginPublish)
    alias(libs.plugins.axionRelease)
    alias(libs.plugins.detekt)
}

scmVersion {
    tag {
        prefix = "v"
    }
    nextVersion {
        suffix = "SNAPSHOT"
        separator = "-"
    }
    versionCreator = PredefinedVersionCreator.SIMPLE.versionCreator
}

group = "tel.schich.dockcross"
version = scmVersion.version

java.toolchain {
    languageVersion = JavaLanguageVersion.of(8)
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
}

gradlePlugin {
    displayName
    website = "https://github.com/pschichtel/dockcross-gradle-plugin"
    vcsUrl = "https://github.com/pschichtel/dockcross-gradle-plugin"
    plugins {
        create("dockcrossPlugin") {
            id = "tel.schich.dockcross"
            implementationClass = "tel.schich.dockcross.DockcrossPlugin"
            displayName = "Dockcross Gradle Plugin"
            description = "A simple gradle plugin to run tasks in dockcross containers"
            tags = listOf("dockcross", "cross-compilation")
        }
    }
}

tasks.check {
    dependsOn(tasks.detektMain, tasks.detektTest)
}

detekt {
    parallel = true
    buildUponDefaultConfig = true
    config.setFrom(files(project.rootDir.resolve("detekt.yml")))
}
