package dev.snowdrop.lsp.socket;

import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.InitializedParams;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class JdtlsSocketClient {
    
    private static final Logger logger = LoggerFactory.getLogger(JdtlsSocketClient.class);
    private static final String PROJECT_ROOT = "/Users/cmoullia/code/application-modernisation/lsp-tuto/example";
    private static final int SERVER_PORT = 3333;

    public static void main(String[] args) throws Exception {
        logger.info("Connecting to JDT Language Server on port {}...", SERVER_PORT);
        Socket socket = new Socket("localhost", SERVER_PORT);
        logger.info("Connected to server.");

        ExecutorService executor = Executors.newSingleThreadExecutor();
        LspClient client = new LspClient();
        Launcher<LanguageServer> launcher = LSPLauncher.createClientLauncher(
            client,
            socket.getInputStream(),
            socket.getOutputStream(),
            executor,
            (writer) -> writer // No-op, we don't want to wrap the writer
        );

        launcher.startListening();

        // The server proxy is used to send requests from our client to the server
        LanguageServer languageServer = launcher.getRemoteProxy();

        try {
            logger.info("Sending 'initialize' request...");
            InitializeResult initializeResult = languageServer.initialize(AnnotationFinder.getEnhancedInitializeParams(Paths.get(PROJECT_ROOT))).get();
            logger.info("Server capabilities: {}", initializeResult.getCapabilities());
            languageServer.initialized(new InitializedParams());
            logger.info("Handshake complete.");

            AnnotationFinder.findAnnotationUsagesAST(languageServer, Paths.get(PROJECT_ROOT), "MySearchableAnnotation");

        } finally {
            // 5. Gracefully shut down the connection
            logger.info("Shutting down the language server connection...");
            try {
                if (languageServer != null) {
                    languageServer.shutdown().get();
                    languageServer.exit();
                }
                // Give the server a moment to process the exit
                Thread.sleep(100);
            } catch (Exception e) {
                logger.warn("Error during language server shutdown: {}", e.getMessage());
            }
            
            try {
                socket.close();
            } catch (IOException e) {
                logger.debug("Socket close exception (expected): {}", e.getMessage());
            }
            
            executor.shutdown();
            logger.info("Shutdown complete.");
        }
    }
}