package no.not.none.nexus

import java.time.LocalDate

data class Artifact(
    val repository: String,
    val group: String,
    val name: String,
    val version: String,
    val downloadUrl: String,
    val pomDownloadUrl: String
) {
    val realName = name.replace(Regex("[_-]v?\\d.*"), "")
    val nameEncodedVersion = if (realName != name) name.drop(realName.length + 1) else ""
    val extension: String = downloadUrl.substringAfterLast('.')
    var fullName = ""
    var description: String = ""
    var lastModifiedDate: LocalDate? = null
}

data class Response(val artifacts: List<Artifact>, val continuationToken: String?)

data class Repository(
    val name: String,
    val format: String,
    val type: String,
    val url: String
)