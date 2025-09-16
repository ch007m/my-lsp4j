# Instructions to play with Eclipse JDT-LS & Protocol

This project demonstrates two different approaches to use the Eclipse Java Language Server & Protocol (aka: Eclipse jdt-ls) running:

1. In-process within the same JVM as the client
2. As separate JVM process

The project uses a maven java `example` project where some class contains the annotation `@MySearchableAnnotation` that we would like to search about.

## Setup

First, compile the project and generate the classpath file:

```shell
mvn clean compile
```

## Approach 1: In-process

For a complete demonstration that runs both client and server together, execute the following command:

```shell
mvn exec:java -Dexec.mainClass=dev.snowdrop.lsp.JdtLsEmbedded
```

This will:
- Start both the Java Language Server and client communicating using standard streams
- Initialize the server using the example project
- Search for annotated classes and display results with precise locations

## Approach 2: jdt-ls running separately

### Download jdt-ls

Download and unzip the Eclipse JDT Language Server:

```shell
wget https://www.eclipse.org/downloads/download.php?file=/jdtls/milestones/1.50.0/jdt-language-server-1.50.0-202509041425.tar.gz > jdt-language-server-1.50.0.tar.gz
mkdir jdt-ls
tar -vxf jdt-language-server-1.50.0.tar.gz -C jdt-ls
```

### Start the JDT Language Server and expose it using a Socket

Start it using the following command

```shell
set -gx JDT_LS_PATH "/Users/cmoullia/code/application-modernisation/lsp/jdt-ls" or
export JDT_LS_PATH="/Users/cmoullia/code/application-modernisation/lsp/jdt-ls"
mvn exec:java -Dexec.mainClass=dev.snowdrop.lsp.JdtlsServer
```

This will:
- Start the JDT Language Server
- Create a socket listening on port 3333 to access it

### Run the Client

In a separate terminal, run the client to connect and search for the annotation:

```shell
mvn exec:java -Dexec.mainClass=dev.snowdrop.lsp.JdtlsSocketClient
```
