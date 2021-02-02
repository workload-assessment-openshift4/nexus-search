package no.not.none.nexus

import jakarta.json.*
import jakarta.ws.rs.InternalServerErrorException
import jakarta.ws.rs.client.Client
import jakarta.ws.rs.client.ClientBuilder
import jakarta.ws.rs.client.WebTarget
import jakarta.ws.rs.core.MediaType
import org.apache.commons.cli.*
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature
import org.glassfish.json.jaxrs.JsonValueBodyReader
import java.io.File
import java.io.InputStream
import java.io.StringReader
import java.nio.file.Files
import java.nio.file.Paths
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager


fun main(vararg args: String) {

    val parser = DefaultParser()

    val options = createOptions()

    val formatter = HelpFormatter()

    try {
        val cl = parser.parse(options, args)

        val target = buildTarget(cl)

        var continuationToken: String? = null

        val allSelectedArtifacts = mutableListOf<Artifact>()

        getRepos(cl).forEach { repo ->

            do {
                val (artifacts, token) = requestArtifacts(target, repo.name, continuationToken)

                continuationToken = token

                allSelectedArtifacts.addAll(artifacts.filter {
                    !it.repository.contains(
                        "snapshot",
                        true
                    ) || !it.name.contains("snapshot", true)
                })

                println("${allSelectedArtifacts.size} artifacts so far!")

            } while (continuationToken != null)
        }

        val mappedArtifacts = allSelectedArtifacts.groupBy({ "${it.repository}/${it.group}:${it.realName}" }, { it })

        val distinctArtifacts = mappedArtifacts.mapNotNull {
            it.value.maxByOrNull { art -> art.nameEncodedVersion + art.version }
        }

        val csv = distinctArtifacts
            .joinToString(System.getProperty("line.separator")) {
                "${it.repository}, ${it.group}, ${it.name}, ${it.version}, ${it.downloadUrl}, ${it.pomDownloadUrl}"
            }

        if (cl.hasOption('v') || !cl.hasOption('f')) {
            println(csv)
        }

        if (cl.hasOption('f')) {
            File(cl.getOptionValue('f')).writeText(csv)
        }

    } catch (ex: ParseException) {
        formatter.printHelp("Nexus Search", options)
    }

}

private fun requestArtifacts(target: WebTarget, repo: String, continuationToken: String?): Response {

    val response = doRequest(target, repo, continuationToken)

    println(response)

    val value = response.getValue("/continuationToken")

    val responseToken = if (value != JsonValue.NULL) {
        (value as JsonString).string
    } else null

    val artifacts = response.getJsonArray("items").map { it as JsonObject }.map {
        val name = it.getString("name", "")
        Artifact(
            repository = it.getString("repository", ""),
            group = it.getString("group", ""),
            name = name,
            version = it.getString("version", ""),
            downloadUrl = it.getJsonArray("assets")[0].asJsonObject().getString("downloadUrl")
        )
    }

    return Response(artifacts, responseToken)
}

val emptyResponse: JsonObject =
    Json.createReader(StringReader("{\"items\":[],\"continuationToken\":null}")).readObject()

private fun doRequest(
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
        println("Failed $a time(s)")
        if (a < 5)
            doRequest(target, repo, continuationToken, a)
        else {
            println("We can't get past that error")
            emptyResponse
        }
    }
}

private fun buildTarget(cl: CommandLine): WebTarget {
    val client = buildClient(cl)

    var target = client
        .target(cl.getOptionValue('s'))
        .path("service/rest/v1/search")
        .queryParam("format", "maven2")
        .queryParam("assets.attributes.maven2.extension", cl.getOptionValue('e'))

    if (cl.hasOption('g')) {
        target = target
            .queryParam("group", cl.getOptionValue('g'))
    }

    return target
}

private fun getRepos(cl: CommandLine): List<Repository> {
    val client = buildClient(cl)

    return client
        .target(cl.getOptionValue('s'))
        .path("service/rest/v1/repositories")
        .request(MediaType.APPLICATION_JSON_TYPE)
        .get(JsonArray::class.java)
        .asSequence()
        .map { it as JsonObject }
        .map {
            Repository(
                name = it.getString("name", ""),
                format = it.getString("format", ""),
                type = it.getString("type", ""),
                url = it.getString("url", "")
            )
        }.filter {
            it.type == if (cl.hasOption('t')) cl.getOptionValue('t') else "hosted"
        }.filter {
            it.format == "maven2"
        }.filter {
            !it.name.contains("snapshot", true)
        }
        .toList()
}

fun createOptions(): Options {
    val options = Options()

    options.addRequiredOption("s", "server", true, "The Nexus API URL")
    options.addRequiredOption("e", "extension", true, "The artifacts' extension")
    options.addOption("g", "group", true, "The group to select from")
    options.addOption("v", "verbose", false, "Print the artifacts list")
    options.addOption("f", "file", true, "File to export too.")
    options.addOption("u", "username", true, "Username")
    options.addOption("p", "password", true, "Password")
    options.addOption("d", "download", true, "Download Location")
    options.addOption("t", "type", true, "Repository type defaults to hosted")

    return options
}

data class Artifact(
    val repository: String,
    val group: String,
    val name: String,
    val version: String,
    val downloadUrl: String,
) {
    val realName = name.replace(Regex("[_-]v?\\d.*"), "")
    val nameEncodedVersion = if (realName != name) name.drop(realName.length + 1) else ""
    val extension: String = downloadUrl.substringAfterLast('.')
    val pomDownloadUrl = downloadUrl.replaceAfterLast('.', "pom")
}

data class Response(val artifacts: List<Artifact>, val continuationToken: String?)

fun buildClient(cl: CommandLine): Client {
    val noopTrustManager = arrayOf(
        object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {

            }

            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            }

            override fun getAcceptedIssuers(): Array<X509Certificate>? {
                return null
            }
        }
    )

    val sc = SSLContext.getInstance("ssl")
    sc.init(null, noopTrustManager, null)

    var builder = ClientBuilder.newBuilder().sslContext(sc)
        .register(JsonValueBodyReader::class.java)

    if (cl.hasOption('u') || cl.hasOption('p')) {
        val feature = HttpAuthenticationFeature.basicBuilder()
            .credentials(cl.getOptionValue('u', ""), cl.getOptionValue('p', ""))
            .build()

        builder = builder.register(feature)
    }

    return builder
        .build()
}

data class Repository(
    val name: String,
    val format: String,
    val type: String,
    val url: String
)

fun download(artifact: Artifact, cl: CommandLine) {
    val client = buildClient(cl)

    val jar = client.target(artifact.downloadUrl)
        .request()
        .get()
        .readEntity(InputStream::class.java)

    val location = Paths.get(cl.getOptionValue('d'), artifact.repository, artifact.group)

    Files.copy(jar, location.resolve("${artifact.name}.${artifact.extension}"))

    val pom = client.target(artifact.pomDownloadUrl)
        .request()
        .get()
        .readEntity(InputStream::class.java)

    Files.copy(pom, location.resolve("${artifact.name}.pom.xml"))
}