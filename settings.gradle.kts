rootProject.name = "psychics"
include(
    "psychics-abilities",
    "psychics-common"
)
File("psychics-abilities").listFiles()?.filter { it.isDirectory && it.name != "build" }?.forEach { file ->
    include(":psychics-abilities:${file.name}")
}
