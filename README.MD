# Building

```shell
./gradlew build
```

The resulting `zip` and `tar` compressed files are located under: `build/distributions`.

Uncompress and start using.

# Usage

Use the `search` command under `bin`.

```
Nexus Search
-e,--extension <arg>   The artifacts' extension
-f,--file <arg>        File to export too.
-g,--group <arg>       The group to select from
-p,--password <arg>    Password
-s,--server <arg>      The Nexus API URL
-u,--username <arg>    Username
-v,--verbose           Print the artifacts list
```