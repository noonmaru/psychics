tasks {
    shadowJar {
        archiveBaseName.set("Psychics")
    }
    create<Jar>("sourcesJar") {
        archiveClassifier.set("sources")
        from(sourceSets["main"].allSource)
    }
    create<Copy>("copyShadowJarToDocker") {
        from(shadowJar)
        var dest = File(rootDir, ".docker/plugins")
        if (File(dest, shadowJar.get().archiveFileName.get()).exists()) dest = File(dest, "update")
        into(dest)
        doLast { println("${shadowJar.get().archiveFileName.get()} copied to ${dest.path}") }
    }
}

publishing {
    publications {
        create<MavenPublication>("Psychics") {
            artifactId = "psychics"
            from(components["java"])
            artifact(tasks["sourcesJar"])
        }
    }
}