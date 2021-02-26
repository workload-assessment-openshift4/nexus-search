package no.not.none.nexus

import jakarta.json.Json
import jakarta.json.JsonObject
import jakarta.json.JsonString
import jakarta.json.JsonValue
import jakarta.ws.rs.InternalServerErrorException
import jakarta.ws.rs.client.WebTarget
import jakarta.ws.rs.core.MediaType
import org.apache.commons.cli.CommandLine
import java.io.StringReader

val EMPTY_RESPONSE: JsonObject =
    Json.createReader(StringReader("{\"items\":[],\"continuationToken\":null}")).readObject()

suspend fun doRequest(
    target: WebTarget,
    repo: String,
    continuationToken: String?,
    i: Int = 0
): JsonObject {
    val repoTarget = target.queryParam("repository", repo)

    return try {
        if (continuationToken != null) {
            repoTarget.queryParam("continuationToken", continuationToken)
        } else {
            repoTarget
        }.request(MediaType.APPLICATION_JSON_TYPE)
            .get(JsonObject::class.java)
    } catch (_: InternalServerErrorException) {
        val a = i + 1
        LOGGER.log("Failed $a time(s)")

        println("Failed $a time(s) while reading repo: $repo")

        if (a < 5)
            doRequest(target, repo, continuationToken, a)
        else {
            LOGGER.log("We can't get past that error")
            println("We could not fully ready repo: $repo")
            EMPTY_RESPONSE
        }
    }
}

suspend fun requestArtifacts(target: WebTarget, repo: String, continuationToken: String?, cl: CommandLine): Response {

    val response = doRequest(target, repo, continuationToken)

    LOGGER.log("$response")

    val value = response.getValue("/continuationToken")

    val responseToken = if (value != JsonValue.NULL) {
        (value as JsonString).string
    } else null

    val artifacts = response.getJsonArray("items").map { it as JsonObject }.map {
        val name = it.getString("name", "")

        val downloadUrls = it.getJsonArray("assets").map { asset ->
            asset.asJsonObject()
        }.map { asset -> asset.getString("downloadUrl") }

        Artifact(
            repository = it.getString("repository", ""),
            group = it.getString("group", ""),
            name = name,
            version = it.getString("version", ""),
            downloadUrl = downloadUrls.first { url -> url.endsWith(cl.getOptionValue('e')) },
            pomDownloadUrl = downloadUrls.firstOrNull { url -> url.endsWith("pom") }
        )
    }

    return Response(artifacts, responseToken)
}