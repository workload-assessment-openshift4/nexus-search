package no.not.none.nexus

import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.runBlocking
import java.io.File

fun writeOutput(downloadedPomChannel: ReceiveChannel<Artifact>, fileLocation: String) {
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

            File(fileLocation).appendText(resultLine)

            LOGGER.completeOneArtifact()
        }

        println()
    }
}