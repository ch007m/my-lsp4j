package dev.snowdrop.lsp.common.utils;

import dev.snowdrop.lsp.proxy.SnowdropLanguageServer;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.InitializedParams;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for setting up LSP server and client communication in tests and demos.
 * Eliminates code duplication between test classes and main launcher classes.
 */
public class LanguageServer {
    private static final Logger logger = LoggerFactory.getLogger(LanguageServer.class);

    /**
     * Create and set up an LSP server and client using piped streams.
     * 
     * @return LSPConnection
     * @throws Exception if setup fails
     */
    public static SnowdropLS launchServerAndClient() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        
        try {
            // Create piped streams for client-server communication
            PipedInputStream serverIn = new PipedInputStream();
            PipedOutputStream clientOut = new PipedOutputStream(serverIn);
            
            PipedInputStream clientIn = new PipedInputStream();
            PipedOutputStream serverOut = new PipedOutputStream(clientIn);
            
            // Create and start the jdt-ls server
            SnowdropLanguageServer server = new SnowdropLanguageServer();
            Launcher<org.eclipse.lsp4j.services.LanguageServer> serverLauncher = new LSPLauncher.Builder<org.eclipse.lsp4j.services.LanguageServer>()
                .setLocalService(server)
                .setRemoteInterface(org.eclipse.lsp4j.services.LanguageServer.class)
                .setInput(serverIn)
                .setOutput(serverOut)
                .setExecutorService(executor)
                .create();
            
            serverLauncher.startListening();
            
            // Create client
            LspClient client = new LspClient();
            Launcher<LanguageClient> clientLauncher = new LSPLauncher.Builder<LanguageClient>()
                .setLocalService(client)
                .setRemoteInterface(LanguageClient.class)
                .setInput(clientIn)
                .setOutput(clientOut)
                .setExecutorService(executor)
                .create();

            clientLauncher.startListening();
            
            // Allow time for connection establishment
            Thread.sleep(100);
            
            logger.info("LSP setup completed");
            
            return new SnowdropLS(server);
            
        } catch (Exception e) {
            // Cleanup on failure
            if (executor != null && !executor.isShutdown()) {
                executor.shutdown();
            }
            throw e;
        }
    }
    
    /**
     * Initialize the language server with basic parameters.
     * 
     * @param serverProxy the server proxy to initialize
     * @param projectRoot the project root directory
     * @return the initialization result
     * @throws Exception if initialization fails
     */
    public static InitializeResult initializeLanguageServer(org.eclipse.lsp4j.services.LanguageServer serverProxy, Path projectRoot) throws Exception {
        InitializeParams initParams = new InitializeParams();
        initParams.setProcessId((int) ProcessHandle.current().pid());
        initParams.setRootUri(projectRoot.toUri().toString());
        
        CompletableFuture<InitializeResult> initResult = serverProxy.initialize(initParams);
        InitializeResult result = initResult.get(5, TimeUnit.SECONDS);
        
        // Complete the handshake
        serverProxy.initialized(new InitializedParams());
        
        logger.info("Language server initialized successfully");
        return result;
    }
}