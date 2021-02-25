package no.not.none.nexus

import org.apache.commons.cli.*
import kotlin.system.exitProcess

fun createOptions(): Options {
    val options = Options()

    options.addRequiredOption("s", "server", true, "The Nexus API URL")
    options.addRequiredOption("e", "extension", true, "The artifacts' extension")
    options.addOption("g", "group", true, "The group to select from")
    options.addOption("f", "file", true, "File to export to.")
    options.addOption("u", "username", true, "Username")
    options.addOption("p", "password", true, "Password")
    options.addOption("d", "download", true, "Download Location")
    options.addOption("t", "type", true, "Repository type defaults to hosted")

    return options
}

fun readArguments(options: Options, vararg args: String): CommandLine {
    val formatter = HelpFormatter()

    try {
        val parser = DefaultParser()
        return parser.parse(options, args)
    } catch (ex: ParseException) {
        formatter.printHelp("Nexus Search", options)
        exitProcess(1)
    }
}