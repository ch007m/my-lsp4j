package dev.snowdrop.lsp.common.utils;

import dev.snowdrop.lsp.common.SnowdropLanguageServer;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.InitializedParams;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
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
 * Utility class for setting up LSP
 */
public class LSUtils {
    private static final Logger logger = LoggerFactory.getLogger(LSUtils.class);

    /**
     * Create and set up an LSP server streams.
     * 
     * @return SnowdropLS
     * @throws Exception if setup fails
     */
    public static SnowdropLS launchServer() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        
        try {
            // Create streams for client-snowdropLanguageServer communication
            PipedInputStream jdtLSInput = new PipedInputStream();
            PipedOutputStream jdtLSOutput = new PipedOutputStream(jdtLSInput);
            
            // Create and start the jdt-ls snowdropLanguageServer
            SnowdropLanguageServer snowdropLanguageServer = new SnowdropLanguageServer();
            Launcher<org.eclipse.lsp4j.services.LanguageServer> serverLauncher = new LSPLauncher.Builder<org.eclipse.lsp4j.services.LanguageServer>()
                .setLocalService(snowdropLanguageServer)
                .setRemoteInterface(org.eclipse.lsp4j.services.LanguageServer.class)
                .setInput(jdtLSInput)
                .setOutput(jdtLSOutput)
                .setExecutorService(executor)
                .create();
            serverLauncher.startListening();
            return new SnowdropLS(snowdropLanguageServer);
            
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
        
        logger.info("SERVER: Language server initialized successfully");
        return result;
    }
}