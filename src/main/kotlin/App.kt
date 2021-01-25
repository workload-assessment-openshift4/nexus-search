package no.not.none.nexus

import jakarta.json.JsonObject
import jakarta.ws.rs.client.Client
import jakarta.ws.rs.client.ClientBuilder
import jakarta.ws.rs.core.MediaType
import org.apache.commons.cli.*
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature
import org.glassfish.json.jaxrs.JsonValueBodyReader
import java.io.File
import java.security.cert.X509Certificate
import java.time.ZonedDateTime
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager


fun main(vararg args: String) {

    val parser = DefaultParser()

    val options = createOptions()

    val formatter = HelpFormatter()

    try {
        val cl = parser.parse(options, args)


        val client = buildClient(cl)

        val response =
            client
                .target(cl.getOptionValue('s'))
                .path("service/rest/v1/search/assets")
                .queryParam("assets.attributes.maven2.extension", cl.getOptionValue('e'))
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get(JsonObject::class.java)

        println(response)

        val artifacts = response.getJsonArray("items").map { it as JsonObject }.map {
            val mavenArtifact = it.getJsonObject("maven2")
            val lastModified = ZonedDateTime.parse(it.getString("lastModified"))
            val url = it.getString("downloadUrl")

            Artifact(
                group = mavenArtifact.getString("groupId"),
                name = mavenArtifact.getString("artifactId"),
                version = mavenArtifact.getString("version"),
                extension = mavenArtifact.getString("extension"),
                lastModified, url
            )
        }

        val mappedArtifacts = artifacts.groupBy({ "${it.group}:${it.name}" }, { it })

        var distinctArtifacts = mappedArtifacts.mapNotNull {
            it.value.maxByOrNull { art -> art.lastModified }
        }

        if (cl.hasOption('g')) {
            distinctArtifacts = distinctArtifacts.filter { it.group.startsWith(cl.getOptionValue('g')) }
        }

        val csv = distinctArtifacts
            .joinToString(System.getProperty("line.separator")) {
                "${it.group}:${it.name}:${it.version}:${it.extension},${it.url}"
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
    val group: String,
    val name: String,
    val version: String,
    val extension: String,
    val lastModified: ZonedDateTime,
    val url: String
)

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