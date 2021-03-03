# Overview

This tool uses the nexus API to search through all non-snapshots repositories filtering based on provided parameters.

## Server Parameters

* Nexus Server (Required): The URL to access the Nexus API.
* Username (Optional): The username for the Nexus API.
* Password (Optional): The password for the Nexus API.

## Filtering Parameters

* Extension (Required): The artifacts' extension (jar, war, ear).
* Group (Optional): The maven group id to use as a filter. Wildcard can be used.
* Download (Optional): Optionally define the location to download the pom files. Defaults to "poms".
* Repository Type (Optional): Optionally define the repository types. Defaults to "hosted".
* Report File (Optional): Result output file.

# Building

```shell
./gradlew build
```

The resulting `zip` and `tar` compressed files are located under: `build/distributions`.

Uncompress and start using.

# Usage

Use the `search` command under `bin`.

```
usage: Nexus Search
 -d,--download <arg>    Optionally define the location to download the pom files. Defaults to "poms" (Optional).
 -e,--extension <arg>    The artifacts' extension (jar, war, ear) (Required).
 -f,--file <arg>        File to export to (Optional).
 -g,--group <arg>       The maven group id to use as a filter. Wildcard can be used.(Optional).
 -p,--password <arg>    The password for the Nexus API (Optional).
 -s,--server <arg>      The URL to access the Nexus API (Required).
 -t,--type <arg>        Optionally define the repository types. Defaults to "hosted" (Optional).
 -u,--username <arg>    The username for the Nexus API (Optional).
```