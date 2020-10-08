import org.gradle.kotlin.dsl.support.zipTo

subprojects {
    if (version == "unspecified") version = parent!!.version

    dependencies {
        compileOnly(project(":psychics-common"))
    }

    tasks {
        shadowJar {
            archiveBaseName.set("${project.group}.${project.name}")
        }
        create<Copy>("copyShadowJarToParent") {
            from(shadowJar)
            into { File(parent!!.buildDir, "libs") }
        }
        create<Copy>("copyShadowJarToDocker") {
            from(shadowJar)
            var dest = File(rootDir, ".docker/plugins/Psychics/abilities")
            if (File(dest, shadowJar.get().archiveFileName.get()).exists()) dest = File(dest, "update")
            into(dest)
            doLast { println("${shadowJar.get().archiveFileName.get()} copied to ${dest.path}") }
        }
        assemble {
            dependsOn(named("copyShadowJarToParent"))
        }
    }
}

tasks.filter { it.name != "clean" }.forEach { it.enabled = false }

gradle.buildFinished {
    val libs = File(buildDir, "libs")

    if (libs.exists())
        zipTo(File(buildDir, "abilities.zip"), libs)
}