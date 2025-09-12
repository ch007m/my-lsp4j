## Instructions

### Setup

Download and unzip the Eclipse LSP server

```shell
wget https://www.eclipse.org/downloads/download.php?file=/jdtls/milestones/1.50.0/jdt-language-server-1.50.0-202509041425.tar.gz > jdt-language-server-1.50.0.tar.gz
mkdir jdt-ls
tar -vxf jdt-language-server-1.50.0.tar.gz -C jdt-ls
```

### Running the Application

#### 1. Start the JDT Language Server (Socket Bridge)

First, compile the project and start the socket bridge server:

```shell
mvn compile
mvn dependency:build-classpath -Dmdep.outputFile=cp.txt
java -cp target/classes:$(cat cp.txt) com.example.lsp.JdtlsServer
```

This will:
- Start the JDT Language Server process
- Create a socket bridge listening on port 3333
- Bridge communication between socket clients and the JDT LS stdio interface

#### 2. Run the Client

In a separate terminal, run the client to connect and search for annotations:

```shell
java -cp target/classes:$(cat cp.txt) com.example.lsp.JdtlsSocketClient
```

This will:
- Connect to the socket bridge on port 3333
- Initialize the Language Server Protocol connection
- Search for usages of the `@MySearchableAnnotation` in the example project
- Display the results and shutdown gracefully

#### 3. Run the CombinedLauncher (Complete Demo)

For a complete demonstration that runs both client and server together:

```shell
java -cp target/classes:$(cat cp.txt) dev.snowdrop.lsp.CombinedLauncher
```

This will:
- Create a temporary Java project with sample annotation and class files
- Start both the Java Language Server and client using piped streams
- Demonstrate the complete LSP communication flow
- Search for annotated classes and display results
- Shutdown gracefully

**Note**: The CombinedLauncher provides a complete end-to-end demonstration of LSP communication using piped streams.

### Architecture

The application consists of:
- **JdtlsServer**: A socket bridge that starts the JDT Language Server and bridges socket connections to its stdio interface
- **JdtlsSocketClient**: A client that connects via socket and uses LSP4J to communicate with the server
- **AnnotationFinder**: Core logic for finding annotation usages using LSP features
- **ServerLauncher**: Alternative LSP server implementation using standard I/O for communication
- **ClientLauncher**: Alternative LSP client implementation (requires modification to work with ServerLauncher)
- **CombinedLauncher**: Complete demonstration that runs both client and server together using piped streams