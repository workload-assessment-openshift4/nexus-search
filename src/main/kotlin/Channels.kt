package no.not.none.nexus

import jakarta.json.JsonArray
import jakarta.json.JsonObject
import jakarta.ws.rs.core.MediaType
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import org.apache.commons.cli.CommandLine
import org.apache.maven.model.io.xpp3.MavenXpp3Reader
import org.codehaus.plexus.util.xml.pull.XmlPullParserException
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Paths

@ExperimentalCoroutinesApi
fun findRepos(cl: CommandLine): ReceiveChannel<Repository> {
    return runBlocking(Dispatchers.Default) {
        produce(Dispatchers.IO, Channel.UNLIMITED) {
            val client = buildClient(cl)

            val repos = client
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

            LOGGER.totalRepositories(repos.count())

            repos.forEach { send(it) }

            close()
        }
    }
}

@ExperimentalCoroutinesApi
fun findArtifacts(
        repoChannel: ReceiveChannel<Repository>,
        cl: CommandLine
): ReceiveChannel<Artifact> {

    return runBlocking(Dispatchers.Default) {
        produce(Dispatchers.Default, Channel.UNLIMITED) {

            val jobs = (1..10).map {
                launch(Dispatchers.IO) {

                    val target = buildSearchTarget(cl)

                    for (repo in repoChannel) {

                        var continuationToken: String? = null

                        do {
                            val (artifacts, token) = requestArtifacts(target, repo.name, continuationToken, cl)

                            continuationToken = token

                            val filteredArtifacts = artifacts.filter {
                                !it.repository.contains(
                                        "snapshot",
                                        true
                                ) || !it.name.contains("snapshot", true)
                            }

                            LOGGER.addToTotalArtifacts(filteredArtifacts.count())

                            filteredArtifacts.forEach { send(it) }


                        } while (continuationToken != null)

                        LOGGER.completeOneRepository()
                    }
                }
            }


            jobs.forEach { it.join() }
            close()
        }
    }
}

@ExperimentalCoroutinesApi
fun filterArtifacts(artifactChannel: ReceiveChannel<Artifact>): ReceiveChannel<Artifact> {
    return runBlocking {

        produce(Dispatchers.Default, Channel.UNLIMITED) {
            val allSelectedArtifacts = mutableListOf<Artifact>()

            for (artifact in artifactChannel) {
                allSelectedArtifacts.add(artifact)
            }

            val mappedArtifacts =
                    allSelectedArtifacts.groupBy({ "${it.repository}/${it.group}:${it.realName}" }, { it })

            LOGGER.ignoredArtifacts(allSelectedArtifacts.count() - mappedArtifacts.count())

            val artifacts = mappedArtifacts.mapNotNull {
                it.value.maxByOrNull { art -> art.nameEncodedVersion + art.version }
            }


            artifacts.forEach { send(it) }

            close()
        }
    }
}

@ExperimentalCoroutinesApi
fun downloadPom(artifactChannel: ReceiveChannel<Artifact>, cl: CommandLine): ReceiveChannel<Artifact> {

    return runBlocking {
        produce(Dispatchers.IO, Channel.UNLIMITED) {

            val rootLocation = cl.getOptionValue('d') ?: "poms"

            val rootPath = Paths.get(rootLocation)

            if (Files.exists(rootPath))
                File(rootLocation).deleteRecursively()

            val jobs = (1..10).map {

                launch(Dispatchers.IO) {
                    val client = buildClient(cl)

                    for (artifact in artifactChannel) {
                        if (artifact.pomDownloadUrl != null) {
                            val location = rootPath.resolve(artifact.repository).resolve(artifact.group)

                            Files.createDirectories(location)

                            val pom = client.target(artifact.pomDownloadUrl)
                                    .request()
                                    .get()
                                    .readEntity(InputStream::class.java)

                            val pomLocation = location.resolve("${artifact.name}.pom.xml")
                            Files.copy(pom, pomLocation)

                            val reader = MavenXpp3Reader()

                            try {
                                val model = reader.read(Files.newInputStream(pomLocation))

                                model.name?.let { artifact.fullName = it }
                                model.description?.let { artifact.description = it }
                            } catch (_: XmlPullParserException) {
                                LOGGER.log("Error reading $pomLocation downloaded from: ${artifact.pomDownloadUrl}")
                                println("Error reading $pomLocation downloaded from: ${artifact.pomDownloadUrl}")
                            }
                        }

                        send(artifact)
                    }
                }
            }

            jobs.joinAll()
            close()
        }
    }
}

//@ExperimentalCoroutinesApi
//fun dateArtifacts(artifactChannel: ReceiveChannel<Artifact>, cl: CommandLine): ReceiveChannel<Artifact> {
//    val assetTarget = buildAssetTarget(cl)
//    
//    return runBlocking (Dispatchers.Default) {
//        produce(Dispatchers.Default, Channel.UNLIMITED) {
//
//            val jobs: MutableList<Job> = mutableListOf()
//
//            for (artifact in artifactChannel) {
//
//                val job = launch(Dispatchers.IO) {
//                    val asset = assetTarget.path(artifact.id)
//                        .request()
//                        .get(JsonObject::class.java)
//                    
//                    LocalDateTime.parse()
//                }
//
//                jobs.add(job)
//            }
//
//            jobs.forEach { it.join() }
//            close()
//        }
//    }
//    }
//}

