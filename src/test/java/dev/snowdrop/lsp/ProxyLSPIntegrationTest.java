package dev.snowdrop.lsp;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dev.snowdrop.lsp.proxy.JavaLanguageServer;
import dev.snowdrop.lsp.proxy.LspClient;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration test for the LSP proxy approach that tests the complete
 * client-server communication flow and annotation search functionality.
 */
public class ProxyLSPIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(ProxyLSPIntegrationTest.class);
    
    private Path tempDir;
    private ExecutorService executor;
    private LanguageServer serverProxy;
    private Launcher<LanguageServer> serverLauncher;
    private Launcher<LanguageServer> clientLauncher;
    
    @BeforeEach
    void setUp() throws Exception {
        // Create temporary workspace
        tempDir = Files.createTempDirectory("lsp-test-");
        logger.info("TEST: Created test workspace: {}", tempDir);
        
        // Create sample Java files for testing
        createTestFiles();
        
        // Setup LSP communication infrastructure
        setupLSPCommunication();
        
        // Initialize the language server
        initializeLanguageServer();
    }
    
    private void createTestFiles() throws Exception {
        // Create annotation file
        Path annotationFile = tempDir.resolve("MySearchableAnnotation.java");
        Files.writeString(annotationFile, """
            public @interface MySearchableAnnotation {
                String value() default "";
                int priority() default 0;
            }
            """);
        
        // Create a complex annotated class for comprehensive testing
        Path complexClassFile = tempDir.resolve("ComplexAnnotatedClass.java");
        Files.writeString(complexClassFile, """
            public class ComplexAnnotatedClass {
                @MySearchableAnnotation(value = "field", priority = 1)
                private int annotatedField;
                
                @MySearchableAnnotation("method")
                public void annotatedMethod() {
                    // This annotation in comment should not be found: @MySearchableAnnotation
                }
                
                @MySearchableAnnotation(priority = 10)
                public static final String CONSTANT = "value";
                
                // This is not an annotation: @MySearchableAnnotation in comment
                public void normalMethod() {
                    String str = "@MySearchableAnnotation"; // Not an annotation
                }
            }
            """);
        
        // Create another class with different annotation usage
        Path simpleClassFile = tempDir.resolve("SimpleClass.java");
        Files.writeString(simpleClassFile, """
            public class SimpleClass {
                @Override
                public String toString() {
                    return "Simple";
                }
                
                @MySearchableAnnotation
                private String simpleField;
            }
            """);
        
        logger.info("TEST: Created {} test files", 3);
    }
    
    private void setupLSPCommunication() throws Exception {
        executor = Executors.newFixedThreadPool(4);
        
        // Create piped streams for client-server communication
        PipedInputStream serverIn = new PipedInputStream();
        PipedOutputStream clientOut = new PipedOutputStream(serverIn);
        
        PipedInputStream clientIn = new PipedInputStream();
        PipedOutputStream serverOut = new PipedOutputStream(clientIn);
        
        // Create and start server
        JavaLanguageServer server = new JavaLanguageServer();
        serverLauncher = new LSPLauncher.Builder<LanguageServer>()
            .setLocalService(server)
            .setRemoteInterface(LanguageServer.class)
            .setInput(serverIn)
            .setOutput(serverOut)
            .setExecutorService(executor)
            .create();
        
        serverLauncher.startListening();
        
        // Create client
        LspClient client = new LspClient();
        clientLauncher = new LSPLauncher.Builder<LanguageServer>()
            .setLocalService(client)
            .setRemoteInterface(LanguageServer.class)
            .setInput(clientIn)
            .setOutput(clientOut)
            .setExecutorService(executor)
            .create();
        
        serverProxy = clientLauncher.getRemoteProxy();
        clientLauncher.startListening();
        
        // Allow time for connection establishment
        Thread.sleep(100);
        
        logger.info("TEST: LSP communication setup completed");
    }
    
    private void initializeLanguageServer() throws Exception {
        // Initialize the server
        InitializeParams initParams = new InitializeParams();
        initParams.setProcessId((int) ProcessHandle.current().pid());
        initParams.setRootUri(tempDir.toUri().toString());
        
        CompletableFuture<InitializeResult> initResult = serverProxy.initialize(initParams);
        InitializeResult result = initResult.get(5, TimeUnit.SECONDS);
        
        assertThat(result).isNotNull();
        assertThat(result.getCapabilities()).isNotNull();
        assertThat(result.getCapabilities().getExecuteCommandProvider()).isNotNull();
        assertThat(result.getCapabilities().getExecuteCommandProvider().getCommands())
            .contains("java/findAnnotatedClasses");
        
        serverProxy.initialized(new InitializedParams());
        
        logger.info("TEST: Language server initialized successfully");
    }
    
    @Test
    @Timeout(10)
    void testAnnotationSearchFindsCorrectAnnotations() throws Exception {
        // Execute the annotation search command
        ExecuteCommandParams commandParams = new ExecuteCommandParams(
            "java/findAnnotatedClasses",
            Collections.singletonList("MySearchableAnnotation")
        );
        
        CompletableFuture<Object> commandResult = serverProxy.getWorkspaceService()
            .executeCommand(commandParams);
        Object result = commandResult.get(5, TimeUnit.SECONDS);
        
        // Verify result is not null
        assertThat(result).isNotNull();
        
        // Parse the results
        Gson gson = new Gson();
        Type locationListType = new TypeToken<List<Location>>() {}.getType();
        List<Location> locations = gson.fromJson(gson.toJson(result), locationListType);
        
        // Verify we found the expected number of annotations
        assertThat(locations)
            .isNotEmpty()
            .hasSizeGreaterThanOrEqualTo(4); // At least 4 annotations in our test files
        
        // Verify specific locations were found
        assertThat(locations)
            .extracting(location -> getFileName(location.getUri()))
            .contains("ComplexAnnotatedClass.java", "SimpleClass.java");
        
        // Verify line numbers are reasonable (greater than 0)
        assertThat(locations)
            .extracting(location -> location.getRange().getStart().getLine())
            .allMatch(line -> line >= 0);
        
        // Verify character positions are reasonable
        assertThat(locations)
            .extracting(location -> location.getRange().getStart().getCharacter())
            .allMatch(character -> character >= 0);
        
        logger.info("TEST: Found {} annotation locations as expected", locations.size());
        
        // Log found locations for debugging
        for (Location location : locations) {
            logger.info("TEST: Found annotation at {}:{}:{}", 
                getFileName(location.getUri()),
                location.getRange().getStart().getLine() + 1,
                location.getRange().getStart().getCharacter() + 1);
        }
    }
    
    @Test
    @Timeout(10)
    void testAnnotationSearchWithNonExistentAnnotation() throws Exception {
        // Search for an annotation that doesn't exist
        ExecuteCommandParams commandParams = new ExecuteCommandParams(
            "java/findAnnotatedClasses",
            Collections.singletonList("NonExistentAnnotation")
        );
        
        CompletableFuture<Object> commandResult = serverProxy.getWorkspaceService()
            .executeCommand(commandParams);
        Object result = commandResult.get(5, TimeUnit.SECONDS);
        
        // Parse the results
        Gson gson = new Gson();
        Type locationListType = new TypeToken<List<Location>>() {}.getType();
        List<Location> locations = gson.fromJson(gson.toJson(result), locationListType);
        
        // Verify no results found
        assertThat(locations).isEmpty();
        
        logger.info("TEST: Correctly found no results for non-existent annotation");
    }
    
    @Test
    @Timeout(10)
    void testServerCapabilities() throws Exception {
        // Re-initialize to get fresh capabilities (this also tests idempotency)
        InitializeParams initParams = new InitializeParams();
        initParams.setProcessId((int) ProcessHandle.current().pid());
        initParams.setRootUri(tempDir.toUri().toString());
        
        CompletableFuture<InitializeResult> initResult = serverProxy.initialize(initParams);
        InitializeResult result = initResult.get(5, TimeUnit.SECONDS);
        
        // Verify server capabilities
        ServerCapabilities capabilities = result.getCapabilities();
        assertThat(capabilities).isNotNull();
        
        // Verify text document sync capability
        assertThat(capabilities.getTextDocumentSync()).isNotNull();
        
        // Verify execute command capability
        ExecuteCommandOptions executeCommandOptions = capabilities.getExecuteCommandProvider();
        assertThat(executeCommandOptions).isNotNull();
        assertThat(executeCommandOptions.getCommands())
            .contains("java/findAnnotatedClasses");
        
        logger.info("TEST: Server capabilities verified successfully");
    }
    
    @Test
    @Timeout(10)
    void testServerShutdown() throws Exception {
        // Test graceful shutdown
        CompletableFuture<Object> shutdownResult = serverProxy.shutdown();
        Object result = shutdownResult.get(5, TimeUnit.SECONDS);
        
        // Shutdown should complete without error
        assertThat(result).isNull(); // Shutdown returns null on success
        
        logger.info("TEST: Server shutdown completed successfully");
    }
    
    private String getFileName(String uri) {
        return uri.substring(uri.lastIndexOf('/') + 1);
    }
    
    // Cleanup method to be called after each test
    void tearDown() throws Exception {
        if (serverProxy != null) {
            try {
                serverProxy.shutdown().get(2, TimeUnit.SECONDS);
            } catch (Exception e) {
                logger.warn("TEST: Error during server shutdown: {}", e.getMessage());
            }
        }
        
        if (executor != null) {
            executor.shutdown();
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        }
        
        if (tempDir != null && Files.exists(tempDir)) {
            // Clean up temp directory
            Files.walk(tempDir)
                .sorted((a, b) -> b.compareTo(a)) // Delete files before directories
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (Exception e) {
                        logger.warn("TEST: Could not delete {}: {}", path, e.getMessage());
                    }
                });
        }
        
        logger.info("TEST: Cleanup completed");
    }
}