package dev.snowdrop.lsp.socket;

import dev.snowdrop.lsp.common.utils.LspClient;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.*;

import static dev.snowdrop.lsp.common.utils.FileUtils.getExampleDir;
import static dev.snowdrop.lsp.common.utils.FileUtils.getTempDir;

public class JdtlsSocketClient {
    
    private static final Logger logger = LoggerFactory.getLogger(JdtlsSocketClient.class);
    private static final int SERVER_PORT = 3333;

    public static void main(String[] args) throws Exception {
        Path tempDir = getExampleDir();

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

        Future<Void> listening = launcher.startListening();

        // The server proxy is used to send requests from our client to the server
        LanguageServer languageServer = launcher.getRemoteProxy();

        try {
            logger.info("Sending 'initialize' request...");
            
            // Initialize the JDT-LS server directly
            InitializeParams initParams = new InitializeParams();
            initParams.setProcessId((int) ProcessHandle.current().pid());
            initParams.setRootUri(tempDir.toUri().toString());
            
            CompletableFuture<InitializeResult> initResult = languageServer.initialize(initParams);
            InitializeResult initializeResult = initResult.get(10, TimeUnit.SECONDS);
            
            //logger.info("Server capabilities: {}", initializeResult.getCapabilities());
            languageServer.initialized(new InitializedParams());
            logger.info("Handshake complete.");

            // Search for annotation using standard LSP workspace/symbol
            String annotationToFind = "MySearchableAnnotation";
            logger.info("CLIENT: Searching for '@{}' using workspace/symbol...", annotationToFind);

            WorkspaceSymbolParams symbolParams = new WorkspaceSymbolParams(annotationToFind);
            CompletableFuture<Either<List<? extends SymbolInformation>, List<? extends WorkspaceSymbol>>> symbolResult = 
                languageServer.getWorkspaceService().symbol(symbolParams);
            
            Either<List<? extends SymbolInformation>, List<? extends WorkspaceSymbol>> result = symbolResult.get();

            logger.info("CLIENT: --- Search Results ---");
            if (result.isLeft()) {
                List<? extends SymbolInformation> symbols = result.getLeft();
                if (symbols.isEmpty()) {
                    logger.info("CLIENT: No symbols found for '@{}'.", annotationToFind);
                } else {
                    logger.info("CLIENT: Found {} symbol(s) matching '@{}':", symbols.size(), annotationToFind);
                    for (SymbolInformation symbol : symbols) {
                        logger.info("CLIENT:  -> Found: {} at {} (line {}, char {})",
                            symbol.getName(),
                            symbol.getLocation().getUri(),
                            symbol.getLocation().getRange().getStart().getLine() + 1,
                            symbol.getLocation().getRange().getStart().getCharacter() + 1
                        );
                    }
                }
            } else {
                List<? extends WorkspaceSymbol> symbols = result.getRight();
                if (symbols.isEmpty()) {
                    logger.info("CLIENT: No symbols found for '@{}'.", annotationToFind);
                } else {
                    logger.info("CLIENT: Found {} symbol(s) matching '@{}':", symbols.size(), annotationToFind);
                    for (WorkspaceSymbol symbol : symbols) {
                        logger.info("CLIENT:  -> Found: {} at {} (line {}, char {})",
                            symbol.getName(),
                            symbol.getLocation().getLeft().getUri(),
                            symbol.getLocation().getLeft().getRange().getStart().getLine() + 1,
                            symbol.getLocation().getLeft().getRange().getStart().getCharacter() + 1
                        );
                    }
                }
            }
            logger.info("CLIENT: ----------------------");

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
            
            // Stop the listening thread first
            try {
                listening.cancel(true);
            } catch (Exception e) {
                logger.debug("Error canceling listener: {}", e.getMessage());
            }
            
            // Shutdown executor before closing socket
            executor.shutdown();
            try {
                if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            
            try {
                socket.close();
            } catch (IOException e) {
                logger.debug("Socket close exception (expected): {}", e.getMessage());
            }
            
            logger.info("Shutdown complete.");
        }
    }
}