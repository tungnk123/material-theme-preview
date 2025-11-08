plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    id("org.jetbrains.intellij.platform") version "2.10.4"
}

group = "io.github.tungnk123"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    intellijPlatform { defaultRepositories() }
}

dependencies {
    intellijPlatform {
        create("IC", "2025.1.2")

        bundledPlugins(
            "com.intellij.java", "org.jetbrains.kotlin"
        )
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "242"
        }
        changeNotes = "Initial version"
    }
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
        options.encoding = "UTF-8"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = "21"
            languageVersion = "2.1"
            apiVersion = "2.1"
            freeCompilerArgs = listOf("-Xcontext-receivers")
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}
