package no.not.none.nexus

import jakarta.ws.rs.client.Client
import jakarta.ws.rs.client.ClientBuilder
import jakarta.ws.rs.client.WebTarget
import org.apache.commons.cli.CommandLine
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature
import org.glassfish.json.jaxrs.JsonValueBodyReader
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

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

    val client = builder.build()

    return client as Client
}

fun buildSearchTarget(cl: CommandLine): WebTarget {
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