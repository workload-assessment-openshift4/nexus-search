package no.not.none.nexus

import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths


object LOGGER {

    private const val DEFAULT: String = "logs.log"
    private var completedRepositories = 0
    private var totalRepositories = 0
    private var completedArtifacts = 0
    private var ignoredArtifacts = 0
    private var totalArtifacts = 0

    private val artifactsPerRepo = mutableMapOf<String, Int>()

    private val counterContext = newSingleThreadContext("CounterContext")
    private val logContext = newSingleThreadContext("LogContext")

    init {
        Files.deleteIfExists(Paths.get(DEFAULT))
    }


    suspend fun totalRepositories(total: Int) {
        withContext(counterContext) {
            totalRepositories = total
            update()
        }
    }

    suspend fun completeOneRepository() {
        withContext(counterContext) {

            completedRepositories += 1
            update()
        }
    }

    suspend fun ignoredArtifacts(quantity: Int) {
        withContext(counterContext) {

            ignoredArtifacts += quantity
            update()
        }
    }

    suspend fun addToTotalArtifacts(repo: String, quantity: Int) {
        withContext(counterContext) {

            artifactsPerRepo.merge(repo, quantity) { _, count ->
                count + quantity
            }

            totalArtifacts += quantity
            update()
        }
    }

    suspend fun completeOneArtifact() {
        withContext(counterContext) {

            completedArtifacts += 1
            update()
        }
    }

    private fun update() {
        print("\r$completedRepositories/$totalRepositories repositories ; $completedArtifacts/$ignoredArtifacts/$totalArtifacts artifacts")
    }

    suspend fun log(content: String) {
        withContext(logContext) {
            File(DEFAULT).appendText("$content${System.lineSeparator()}")
        }
    }

    fun printReport() {
        artifactsPerRepo.forEach {
            println("Found ${it.value} artifacts in ${it.key}")
        }
    }
}