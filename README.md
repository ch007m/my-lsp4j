# Instructions to play with Eclipse JDT-LS & Protocol

This project demonstrates two different approaches to use the Eclipse Java Language Server & Protocol (aka: Eclipse jdt-ls) running:

1. As separate JVM process
2. In-process within the same JVM as the client

The project uses a maven java `example` project where some class contains the annotation `@MySearchableAnnotation` that we would like to search about.

## Setup

First, compile the project and generate the classpath file:

```shell
mvn clean compile
```

## Approach 1: jdt-ls running as separate process

### Download jdt-ls

Download and unzip the Eclipse JDT Language Server:

```shell
wget https://www.eclipse.org/downloads/download.php?file=/jdtls/milestones/1.50.0/jdt-language-server-1.50.0-202509041425.tar.gz > jdt-language-server-1.50.0.tar.gz
mkdir jdt-ls
tar -vxf jdt-language-server-1.50.0.tar.gz -C jdt-ls
```

To test the jdt-ls server of konveyor and their java bundle, extract it like this
```shell
set VERSION latest

set ID $(podman create --name kantra-download quay.io/konveyor/kantra:$VERSION)
podman cp $ID:/jdtls ./konveyor-jdtls
```

### Start the jdt client listening on a socket

Start it using the following command and set the property:
- `JDT_LS_PATH`: Path of the jdt-ls folder
- `LS_CMD`: Language server command to be executed. Example: java.project.getAll

```shell
# LS_CMD "java.project.getAll"
# LS_CMD "io.konveyor.tackle.samplecommand"
# LS_CMD "io.konveyor.tackle.ruleEntry" 

# JDT_LS_PATH "/Users/cmoullia/code/application-modernisation/lsp/jdtls"
# JDT_LS_PATH "/Users/cmoullia/code/application-modernisation/lsp/konveyor-jdtls"

mvn exec:java -DJDT_LS_PATH=/Users/cmoullia/code/application-modernisation/lsp/jdtls -DLS_CMD=java.project.getAll
```
You can check the log of the server from the parent folder within: `.jdt_workspace/.metadata/.log` !

## Approach 2: In-process

For a complete demonstration that runs both client and an embedded server, execute the following command:

```shell
mvn exec:java -Dexec.mainClass=dev.snowdrop.lsp.JdtLsEmbedded
```

This will:
- Start both the Java Language Server and client communicating using standard streams
- Initialize the server using the example project
- Search for annotated classes and display results with precise locations
