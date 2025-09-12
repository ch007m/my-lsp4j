# Instructions to play with Eclipse JDT-LS & Protocol

This project demonstrates three different approaches to use the Eclipse Java Language Server & Protocol (aka: Eclipse jdt-ls) for annotation search:

1. **Custom LSP Proxy Server**: A self-contained implementation using piped streams
2. **JDT-LS Binary with Socket Bridge**: Using the official JDT-LS binary with socket communication
3. **Enhanced AST-based Annotation Search**: Direct AST parsing with IAnnotation interface integration

The project creates with the help of the [ProjectGenerator.java](src/main/java/dev/snowdrop/lsp/common/utils/ProjectGenerator.java) class a maven java project under a temporary folder where a class contains the annotation `@MySearchableAnnotation` that we would like to search about using Eclipse AST.

## Setup

First, compile the project and generate the classpath file:

```shell
mvn clean compile
mvn dependency:build-classpath -Dmdep.outputFile=cp.txt
```

## Approach 1: Custom LS Proxy Server

For a complete demonstration that runs both client and server together, execute the following command:

```shell
java -cp target/classes:$(cat cp.txt) dev.snowdrop.lsp.proxy.ServerAndClientLauncher
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

In a separate terminal, run the client to connect and search for the annotation ``:

```shell
java -cp target/classes:$(cat cp.txt) dev.snowdrop.lsp.socket.JdtlsSocketClient
```

This will:
- Connect to the socket bridge on port 3333
- Initialize the Language Server Protocol connection
- **Execute AST-based annotation search** using the new AnnotationSearchService
- Search for usages of the `@MySearchableAnnotation` using standard LSP references
- Display detailed results from both approaches and shutdown gracefully

## Approach 3: Enhanced AST-based Annotation Search

The latest enhancement integrates a sophisticated AST-based annotation search service directly into the socket client approach. This combines the best of both worlds.

### Key Features

The [AnnotationSearchService](src/main/java/dev/snowdrop/lsp/common/services/AnnotationSearchService.java) provides:

- **Direct AST Parsing**: Uses Eclipse JDT's AST parser with binding resolution enabled
- **IAnnotation Interface**: Leverages `org.eclipse.jdt.core.IAnnotation` for detailed annotation metadata
- **Element Detection**: Identifies what element is annotated (method, class, field, etc.)
- **Member Value Extraction**: Extracts annotation parameters and values using `IMemberValuePairBinding`
- **LSP Integration**: Returns results in LSP `Location` format for seamless integration

### How It Works

1. **AST Parsing**: Uses Eclipse JDT's AST parser with binding resolution to accurately parse Java source code
2. **Visitor Pattern**: Custom AST visitor traverses the AST to find specific annotations
3. **IAnnotation Binding**: Uses `IAnnotationBinding` to extract detailed annotation information
4. **Element Analysis**: Determines the annotated element type and provides context
5. **LSP Compatibility**: Converts AST positions to LSP locations for integration

When you run Approach 2, you'll now see **both** search results:
1. AST-based search results with detailed annotation information
2. Standard LSP reference search results

## Comparison of Annotation Search Approaches

The three approaches provide different capabilities and results:

### Approach 1: Custom LSP Proxy Server
- **Method**: Custom `java/findAnnotatedClasses` command with Eclipse JDT AST parsing
- **Results**: Finds **only annotation usages** (classes/methods actually annotated)
- **Count**: ~16 results
- **Strengths**: Lightweight, focused on actual usages, custom LSP command
- **Excludes**: Import statements, annotation definition

### Approach 2: JDT-LS Socket Client (Standard LSP)
- **Method**: Standard LSP `textDocument/references` after finding annotation definition
- **Results**: Finds **all references** including imports, definition, and usages
- **Count**: ~20 results  
- **Strengths**: Standard LSP behavior, comprehensive reference finding
- **Includes**: Import statements, annotation definition, and annotation usages

### Approach 3: Enhanced AST-based Search (New)
- **Method**: Direct AST parsing with `IAnnotation` interface + LSP references
- **Results**: **Detailed annotation analysis** with member values + comprehensive references
- **Count**: AST results (~16) + LSP results (~20)
- **Strengths**: 
  - **Most comprehensive**: Combines precision of AST with completeness of LSP
  - **Detailed metadata**: Extracts annotation parameters and values
  - **Element context**: Identifies what is annotated (method, class, field)
  - **Binding resolution**: Uses `IAnnotationBinding` for type information
  - **Dual results**: Both targeted annotation usages and comprehensive references

## 🎯 Which Approach Should You Use?

### **Recommended: Approach 3 (Enhanced AST-based)**
**Best for: Production applications, IDE plugins, code analysis tools**

- ✅ **Most feature-complete**: Combines AST precision with LSP completeness  
- ✅ **Rich metadata**: Gets annotation parameters, values, and context
- ✅ **Type safety**: Uses `IAnnotation` interface for robust annotation handling
- ✅ **Flexible**: Can be adapted for any annotation type
- ✅ **Professional**: Suitable for enterprise code analysis

### **Alternative: Approach 1 (Custom LSP Proxy)**
**Best for: Learning, prototyping, lightweight solutions**

- ✅ **Simple**: Easy to understand and modify
- ✅ **Focused**: Only finds actual annotation usages
- ✅ **Educational**: Great for understanding LSP concepts
- ❌ **Limited**: Basic annotation information only

### **Standard: Approach 2 (Basic Socket Client)**
**Best for: Standard LSP compliance, reference finding**

- ✅ **Standard**: Uses official LSP protocol methods
- ✅ **Comprehensive**: Finds all references including imports
- ✅ **Compatible**: Works with any LSP-compliant server
- ❌ **Basic**: No detailed annotation metadata

**💡 Pro Tip**: Start with **Approach 3** for real applications, as it provides the most comprehensive annotation analysis capabilities while maintaining LSP compatibility.

## Features Demonstrated

- **AST-based Annotation Search**: Uses Eclipse JDT's Abstract Syntax Tree for precise Java code analysis
- **LSP Protocol Implementation**: Complete client-server communication flow
- **Multiple Communication Methods**: Both piped streams and socket-based approaches
- **Real-time Code Analysis**: Demonstrates how LSP servers can provide semantic information about Java code

