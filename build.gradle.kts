/*
 * Copyright (c) 2020 Noonmaru
 *
 *  Licensed under the General Public License, Version 3.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/gpl-3.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

plugins {
    kotlin("jvm") version "1.4.10"
    id("com.github.johnrengelman.shadow") version "5.2.0"
    `maven-publish`
}

group = requireNotNull(properties["pluginGroup"]) { "Group is undefined in properties" }
version = requireNotNull(properties["pluginVersion"]) { "Version is undefined in properties" }

repositories {
    mavenLocal()
    mavenCentral()
    maven(url = "https://papermc.io/repo/repository/maven-public/")
    maven(url = "https://jitpack.io/")
}

dependencies {
    compileOnly(kotlin("stdlib-jdk8")) //kotlin
    compileOnly(kotlin("reflect"))
    compileOnly("com.destroystokyo.paper:paper-api:1.16.2-R0.1-SNAPSHOT")
    compileOnly("com.github.noonmaru:inv-fx:1.1.1")
    implementation("com.github.noonmaru:tap:3.1.2")
    implementation("com.github.noonmaru:kommand:0.3.2")

    testImplementation("junit:junit:4.13")
    testImplementation("org.mockito:mockito-core:3.3.3")
    testImplementation("org.powermock:powermock-module-junit4:2.0.7")
    testImplementation("org.powermock:powermock-api-mockito2:2.0.7")
    testImplementation("org.slf4j:slf4j-api:1.7.25")
    testImplementation("org.apache.logging.log4j:log4j-core:2.8.2")
    testImplementation("org.apache.logging.log4j:log4j-slf4j-impl:2.8.2")
    testImplementation("org.spigotmc:spigot:1.16.2-R0.1-SNAPSHOT")
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
    processResources {
        filesMatching("**/*.yml") {
            expand(project.properties)
        }
    }
    create<Jar>("sourcesJar") {
        archiveClassifier.set("sources")
        from(sourceSets["main"].allSource)
    }
    // build for distributes
    shadowJar {
        archiveClassifier.set("dist")
        relocate("com.github.noonmaru.kommand", "${rootProject.group}.${rootProject.name}.kommand")
        relocate("com.github.noonmaru.tap", "${rootProject.group}.${rootProject.name}.tap")
    }
    create<Copy>("copyJarToServer") {
        from(shadowJar)
        into("W:\\Servers\\psychics\\plugins")
    }
}

publishing {
    publications {
        create<MavenPublication>("Psychics") {
            from(components["java"])
            artifact(tasks["sourcesJar"])
        }
    }
}

//cancel relocate
if (hasProperty("debug")) {
    tasks {
        shadowJar {
            relocators.clear()
        }
    }
}