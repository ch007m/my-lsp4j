package dev.snowdrop.lsp.common.utils;

import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.InitializedParams;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for setting up LSP server and client communication in tests and demos.
 * Eliminates code duplication between test classes and main launcher classes.
 */
public class MyLanguageServer {
    private static final Logger logger = LoggerFactory.getLogger(MyLanguageServer.class);

    /**
     * Create and set up an LSP server and client
     * 
     * @return SnowdropLS
     * @throws Exception if setup fails
     */
    public static SnowdropLS launchServerAndClient(boolean useSocket) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        InputStream jdtLSInput;
        OutputStream jdtLSOutput;

        try {
            if (useSocket) {
                ServerSocket socket = new ServerSocket(3333);
                // Create piped streams for client-server communication using socket
                jdtLSInput = socket.accept().getInputStream();
                jdtLSOutput = socket.accept().getOutputStream();
            } else {
                // Create streams for client-server communication
                jdtLSInput = System.in;
                jdtLSOutput = System.out;
            }

            // Create and start the jdt-ls server
            Launcher<LanguageClient> jdtLSClientLauncher = new LSPLauncher.Builder<LanguageClient>()
                .setLocalService(MyLanguageServer.class)
                .setRemoteInterface(LanguageClient.class)
                .setInput(jdtLSInput)
                .setOutput(jdtLSOutput)
                .setExecutorService(executor)
                .create();
            
            // Create the client calling the jdt-ls server
            Launcher<LanguageServer> jdtlLSServerLauncher = new LSPLauncher.Builder<LanguageServer>()
                .setLocalService(jdtLSClientLauncher.getRemoteProxy())
                .setRemoteInterface(LanguageServer.class)
                .setInput(jdtLSInput)
                .setOutput(jdtLSOutput)
                .create();

            LanguageServer jdtLS = jdtlLSServerLauncher.getRemoteProxy();

            // Launching the client
            jdtLSClientLauncher.startListening();
            
            // Allow time for connection establishment
            Thread.sleep(100);
            
            logger.info("LS Server and Client setup completed");
            
            return new SnowdropLS(jdtLS);
            
        } catch (Exception e) {
            if (executor != null && !executor.isShutdown()) {
                executor.shutdown();
            }
            throw e;
        }
    }
    
    /**
     * Initialize the language server with basic parameters.
     * 
     * @param server the server to initialize
     * @param projectRoot the project root directory
     * @throws Exception if initialization fails
     */
    public static CompletableFuture<InitializeResult> initializeLanguageServer(LanguageServer server, Path projectRoot) throws Exception {
        InitializeParams initParams = new InitializeParams();
        initParams.setProcessId((int) ProcessHandle.current().pid());
        initParams.setRootUri(projectRoot.toUri().toString());
        
        server.initialize(initParams);
        return CompletableFuture.completedFuture(new InitializeResult());
            /*.orTimeout(10, TimeUnit.SECONDS)
            .thenAcceptAsync(result -> {
                logger.info("CLIENT: Initialization successful.");
                server.initialized(new InitializedParams());
                logger.info("CLIENT: Handshake complete.");
            })
            .exceptionally(throwable -> {
                logger.error("CLIENT: An error occurred: ", throwable);
                return null;
            });*/
    }
}