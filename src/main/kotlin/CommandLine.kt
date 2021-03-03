package no.not.none.nexus

import org.apache.commons.cli.*
import kotlin.system.exitProcess

fun createOptions(): Options {
    val options = Options()

    options.addRequiredOption("s", "server", true, "The URL to access the Nexus API (Required).")
    options.addOption("u", "username", true, "The username for the Nexus API (Optional).")
    options.addOption("p", "password", true, "The password for the Nexus API (Optional).")

    options.addRequiredOption("e", "extension", true, " The artifacts' extension (jar, war, ear) (Required).")
    options.addOption("g", "group", true, "The maven group id to use as a filter. Wildcard can be used.(Optional).")
    options.addOption("f", "file", true, "File to export to (Optional).")
    options.addOption(
        "d",
        "download",
        true,
        "Optionally define the location to download the pom files. Defaults to \"poms\" (Optional)."
    )
    options.addOption("t", "type", true, "Optionally define the repository types. Defaults to \"hosted\" (Optional).")

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