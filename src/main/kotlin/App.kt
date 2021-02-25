package no.not.none.nexus

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import java.io.File
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
        Files.deleteIfExists(Paths.get(cl.getOptionValue('f')))
    }

    runBlocking {
        for (artifact in downloadedPomChannel) {
            val resultLine = sequenceOf(
                artifact.downloadUrl,
                artifact.repository,
                artifact.group,
                artifact.name,
                artifact.version,
                artifact.fullName,
                artifact.description.replace(Regex("[\\n\\t,\\r]"), " ")
            ).joinToString(",", postfix = "\n")

            if (cl.hasOption('f')) {
                File(cl.getOptionValue('f')).appendText(resultLine)
            }

            LOGGER.completeOneArtifact()
        }

        println()
    }
}