plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "1.2.1"
}

group = "tel.schich.dockcross"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
}

gradlePlugin {
    website = "https://github.com/pschichtel/dockcross-gradle-plugin"
    vcsUrl = "https://github.com/pschichtel/dockcross-gradle-plugin"
    plugins {
        create("dockcrossPlugin") {
            id = "tel.schich.dockcross"
            implementationClass = "tel.schich.dockcross.DockcrossPlugin"
        }
    }
}
