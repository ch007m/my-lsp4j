package dev.snowdrop.lsp.common.utils;

import dev.snowdrop.lsp.proxy.JavaLanguageServer;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.InitializedParams;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
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
                // Create piped streams for client-server communication
                jdtLSInput = new PipedInputStream();
                jdtLSOutput = new PipedOutputStream();
            }

            // Create and start the jdt-ls server
            LanguageServer server = new LanguageServer();
            Launcher<org.eclipse.lsp4j.services.LanguageServer> jdtLSClientLauncher = new LSPLauncher.Builder<org.eclipse.lsp4j.services.LanguageServer>()
                .setLocalService(server)
                .setRemoteInterface(org.eclipse.lsp4j.services.LanguageServer.class)
                .setInput(jdtLSInput)
                .setOutput(jdtLSOutput)
                .setExecutorService(executor)
                .create();
            
            // Create the client calling the jdt-ls server
            org.eclipse.lsp4j.services.LanguageServer jdtLS = jdtLSClientLauncher.getRemoteProxy();
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