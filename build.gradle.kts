plugins {
    kotlin("jvm") version "1.4.10"
    id("com.github.johnrengelman.shadow") version "5.2.0"
    `maven-publish`
}

val relocate = (findProperty("relocate") as? String)?.toBoolean() ?: true

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "com.github.johnrengelman.shadow")

    if (project == project(":psychics-abilities")) return@subprojects

    repositories {
        maven(url = "https://papermc.io/repo/repository/maven-public/")
        maven(url = "https://repo.dmulloy2.net/nexus/repository/public/")
        maven(url = "https://jitpack.io/")
        mavenLocal()
    }

    // implementation only :psychics-common project
    fun DependencyHandlerScope.implementationOnlyCommon(dependencyNotation: Any): Dependency? {
        return if (this@subprojects == this@subprojects.project(":psychics-common"))
            implementation(dependencyNotation)
        else
            compileOnly(dependencyNotation)
    }

    dependencies {
        compileOnly(kotlin("stdlib-jdk8"))
        compileOnly(kotlin("reflect"))
        compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.9")
        compileOnly("com.destroystokyo.paper:paper-api:1.16.4-R0.1-SNAPSHOT")
        compileOnly("com.comphenix.protocol:ProtocolLib:4.6.0-SNAPSHOT")
        compileOnly("com.github.noonmaru:invfx:1.3.0")

        implementationOnlyCommon("com.github.noonmaru:tap:3.2.5")
        implementationOnlyCommon("com.github.noonmaru:kommand:0.6.3")

        testImplementation("junit:junit:4.13")
        testImplementation("org.mockito:mockito-core:3.3.3")
        testImplementation("org.powermock:powermock-module-junit4:2.0.7")
        testImplementation("org.powermock:powermock-api-mockito2:2.0.7")
        testImplementation("org.slf4j:slf4j-api:1.7.25")
        testImplementation("org.apache.logging.log4j:log4j-core:2.8.2")
        testImplementation("org.apache.logging.log4j:log4j-slf4j-impl:2.8.2")
        testImplementation("org.spigotmc:spigot:1.16.3-R0.1-SNAPSHOT")
    }

    tasks {
        compileJava {
            options.encoding = "UTF-8"
        }
        javadoc {
            options.encoding = "UTF-8"
        }
        compileKotlin {
            kotlinOptions.jvmTarget = "1.8"
        }
        compileTestKotlin {
            kotlinOptions.jvmTarget = "1.8"
        }
        // process yml
        processResources {
            filesMatching("*.yml") {
                expand(project.properties)
            }
        }
        shadowJar {
            archiveClassifier.set("")
            archiveVersion.set("")

            if (relocate) {
                relocate("com.github.noonmaru.tap", "com.github.noonmaru.psychics.tap")
                relocate("com.github.noonmaru.kommand", "com.github.noonmaru.psychics.kommand")
            }
        }
        assemble {
            dependsOn(shadowJar)
        }
    }
}

project(":psychics-common") {
    apply(plugin = "maven-publish")
}

tasks {
    create<de.undercouch.gradle.tasks.download.Download>("downloadBuildTools") {
        src("https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar")
        dest(".buildtools/BuildTools.jar")
    }
    create<DefaultTask>("setupWorkspace") {
        doLast {
            for (v in listOf("1.16.3")) {
                javaexec {
                    workingDir(".buildtools/")
                    main = "-jar"
                    args = listOf(
                        "./BuildTools.jar",
                        "--rev",
                        v
                    )
                }
            }
            File(".buildtools/").deleteRecursively()
        }

        dependsOn(named("downloadBuildTools"))
    }
}

gradle.buildFinished { buildDir.deleteRecursively() }