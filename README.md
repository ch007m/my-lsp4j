# Instructions to play with Eclipse JDT-LS & Protocol

This project demonstrates two different approaches to use the Java Language Server & Protocol part of the project: Eclipse jdt-ls

1. **Custom LSP Proxy Server**: A self-contained implementation using piped streams
2. **JDT-LS Binary with Socket Bridge**: Using the official JDT-LS binary with socket communication

## Setup

First, compile the project and generate the classpath file:

```shell
mvn clean compile
mvn dependency:build-classpath -Dmdep.outputFile=cp.txt
```

## Approach 1: Custom LS Proxy Server

For a complete demonstration that runs both client and server together, execute the following command:

```shell
java -cp target/classes:$(cat cp.txt) dev.snowdrop.lsp.proxy.CombinedLauncher
```

This will:
- Create a temporary Java project with sample annotation and class files
- Start both the Java Language Server and client using piped streams
- Demonstrate the complete LSP communication flow using Eclipse JDT AST parsing
- Search for annotated classes and display results with precise locations
- Shutdown gracefully

**Note**: The CombinedLauncher provides a complete end-to-end demonstration of LSP communication using piped streams and AST-based annotation search.

## Approach 2: JDT-LS Binary with Socket Bridge

### Download and Setup JDT-LS

Download and unzip the Eclipse JDT Language Server:

```shell
wget https://www.eclipse.org/downloads/download.php?file=/jdtls/milestones/1.50.0/jdt-language-server-1.50.0-202509041425.tar.gz > jdt-language-server-1.50.0.tar.gz
mkdir jdt-ls
tar -vxf jdt-language-server-1.50.0.tar.gz -C jdt-ls
```

### Start the JDT Language Server (Socket Bridge)

Start the socket bridge server launching the language server using the `jdt-ls` jar:

```shell
java -cp target/classes:$(cat cp.txt) dev.snowdrop.lsp.socket.JdtlsServer
```

This will:
- Start the JDT Language Server process
- Create a socket bridge listening on port 3333
- Bridge communication between socket clients and the JDT-LS stdio interface

### Run the Client

In a separate terminal, run the client to connect and search for annotations:

```shell
java -cp target/classes:$(cat cp.txt) dev.snowdrop.lsp.socket.JdtlsSocketClient
```

This will:
- Connect to the socket bridge on port 3333
- Initialize the Language Server Protocol connection
- Search for usages of the `@MySearchableAnnotation` in the example project
- Display the results and shutdown gracefully

## Features Demonstrated

- **AST-based Annotation Search**: Uses Eclipse JDT's Abstract Syntax Tree for precise Java code analysis
- **LSP Protocol Implementation**: Complete client-server communication flow
- **Multiple Communication Methods**: Both piped streams and socket-based approaches
- **Real-time Code Analysis**: Demonstrates how LSP servers can provide semantic information about Java code

