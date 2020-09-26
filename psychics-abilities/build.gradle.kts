subprojects {
    dependencies {
        compileOnly(project(":psychics-common"))
    }

    tasks {
        shadowJar {
            archiveBaseName.set("${project.group}.${project.name}")
        }
        create<Copy>("copyJarToDocker") {
            from(shadowJar)
            var dest = File(rootDir, ".docker/plugins/Psychics/abilities")
            if (File(dest, shadowJar.get().archiveFileName.get()).exists()) dest = File(dest, "update")
            into(dest)
            doLast { println("${shadowJar.get().archiveFileName.get()} copied to ${dest.path}") }
        }
    }
}

// Remove build dir after build
gradle.buildFinished { buildDir.deleteRecursively() }