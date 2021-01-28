package no.not.none.nexus

import jakarta.json.JsonObject
import jakarta.json.JsonString
import jakarta.json.JsonValue
import jakarta.ws.rs.client.Client
import jakarta.ws.rs.client.ClientBuilder
import jakarta.ws.rs.client.WebTarget
import jakarta.ws.rs.core.MediaType
import org.apache.commons.cli.*
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature
import org.glassfish.json.jaxrs.JsonValueBodyReader
import java.io.File
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

        do {
            val (artifacts, token) = requestArtifacts(target, continuationToken)

            continuationToken = token

            allSelectedArtifacts.addAll(artifacts.filter {
                !it.repository.contains(
                    "snapshot",
                    true
                ) || !it.name.contains("snapshot", true)
            })

            println("${allSelectedArtifacts.size} artifacts so far!")

        } while (continuationToken != null)

        val mappedArtifacts = allSelectedArtifacts.groupBy({ "${it.repository}/${it.group}:${it.name}" }, { it })

        val distinctArtifacts = mappedArtifacts.mapNotNull {
            it.value.maxByOrNull { art -> art.version }
        }

        val csv = distinctArtifacts
            .joinToString(System.getProperty("line.separator")) {
                "${it.repository},${it.group},${it.name},${it.version}"
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

private fun requestArtifacts(target: WebTarget, continuationToken: String?): Response {

    val response = if (continuationToken != null) {
        target.queryParam("continuationToken", continuationToken)
    } else {
        target
    }.request(MediaType.APPLICATION_JSON_TYPE)
        .get(JsonObject::class.java)

    println(response)

    val value = response.getValue("/continuationToken")

    val responseToken = if (value != JsonValue.NULL) {
        (value as JsonString).string
    } else null

    val artifacts = response.getJsonArray("items").map { it as JsonObject }.map {
        Artifact(
            repository = it.getString("repository", ""),
            group = it.getString("group", ""),
            name = it.getString("name", ""),
            version = it.getString("version", ""),
        )
    }

    return Response(artifacts, responseToken)
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

fun createOptions(): Options {
    val options = Options()

    options.addRequiredOption("s", "server", true, "The Nexus API URL")
    options.addRequiredOption("e", "extension", true, "The artifacts' extension")
    options.addOption("g", "group", true, "The group to select from")
    options.addOption("v", "verbose", false, "Print the artifacts list")
    options.addOption("f", "file", true, "File to export too.")
    options.addOption("u", "username", true, "Username")
    options.addOption("p", "password", true, "Password")

    return options
}

data class Artifact(
    val repository: String,
    val group: String,
    val name: String,
    val version: String
)

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