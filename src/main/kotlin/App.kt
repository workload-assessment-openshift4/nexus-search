package no.not.none.nexus

import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.nio.file.Files
import java.nio.file.Paths


@ExperimentalCoroutinesApi
fun main(vararg args: String) {
    val options = createOptions()

    val cl = readArguments(options, *args)

    val repoChannel = findRepos(cl)

    val allSelectedArtifactChannel = findArtifacts(repoChannel, cl)

    val filteredArtifactChannel = filterArtifacts(allSelectedArtifactChannel)

    val downloadedPomChannel = downloadPom(filteredArtifactChannel, cl)

    if (cl.hasOption('f')) {
        val fileLocation = cl.getOptionValue('f')
        Files.deleteIfExists(Paths.get(fileLocation))

        writeOutput(downloadedPomChannel, fileLocation)
    }
}