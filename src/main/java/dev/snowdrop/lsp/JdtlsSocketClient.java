package dev.snowdrop.lsp;

import dev.snowdrop.lsp.common.utils.LspClient;
import dev.snowdrop.lsp.common.services.AnnotationSearchService;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.*;

import static dev.snowdrop.lsp.common.utils.FileUtils.getExampleDir;

public class JdtlsSocketClient {
    
    private static final Logger logger = LoggerFactory.getLogger(JdtlsSocketClient.class);
    private static final int SERVER_PORT = 3333;

    public static void main(String[] args) throws Exception {
        Path tempDir = getExampleDir();
        Launcher<LanguageServer> launcher;
        ExecutorService executor;

        logger.info("Connecting to the JDT Language Server on port {}...", SERVER_PORT);

        try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
            Socket socket = serverSocket.accept();
            executor = Executors.newSingleThreadExecutor();
            LspClient client = new LspClient();

            launcher = LSPLauncher.createClientLauncher(
                client,
                socket.getInputStream(),
                socket.getOutputStream(),
                executor,
                (writer) -> writer // No-op, we don't want to wrap the writer
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        LanguageServer languageServer = launcher.getRemoteProxy();

        try {
            InitializeParams initParams = new InitializeParams();
            initParams.setProcessId((int) ProcessHandle.current().pid());
            initParams.setRootUri(getExampleDir().toUri().toString());
            initParams.setCapabilities(new ClientCapabilities());

            CompletableFuture<InitializeResult> initResult = languageServer.initialize(initParams);
            //InitializeResult result = initResult.get(5, TimeUnit.SECONDS);

            // Complete the handshake
            languageServer.initialized(new InitializedParams());
            runLspClient(languageServer, tempDir, "MySearchableAnnotation").join();
        } catch (Exception e) {
            logger.error("The LSP workflow failed unexpectedly.", e);
        } finally {
            executor.shutdown();
        }
    }

    /**
     * Initialize the LS client-server and search about the annotation
     *
     * @param server                 The proxy to the language server.
     * @param projectRoot            The root directory of the project to be analyzed.
     * @param mySearchableAnnotation
     * @return A CompletableFuture that completes when the entire sequence is finished.
     */
    private static CompletableFuture<Void> runLspClient(LanguageServer server, Path projectRoot, String mySearchableAnnotation) {
        CompletableFuture<Void> response = searchWithAnnotationService(server, projectRoot, mySearchableAnnotation);
        return response
            .exceptionally(throwable -> {
                logger.error("CLIENT: An error occurred with the LS Server.", throwable);
                return null;
            })
            .thenCompose(v -> server.shutdown())
            .thenRun(server::exit);
    }

    /**
     * Search for MySearchable annotation
     */
    private static CompletableFuture<Void> searchWithAnnotationService(LanguageServer server, Path projectRoot, String annotationName) {
        logger.info("CLIENT: Starting search for @{} annotation...", annotationName);
        AnnotationSearchService searchService = new AnnotationSearchService(server);
        
        return searchService.searchAnnotation(projectRoot, annotationName)
            .thenAcceptAsync(result -> {
                logger.info("CLIENT: --- LSP TextReference {} ---",result.size());
                for(Location l : result) {
                    logger.info("CLIENT:  -> Found @{} on {} in file: {} (line {}, char {})",
                        annotationName,
                        "",
                        l.getUri(),
                        l.getRange().getStart().getLine() + 1,
                        l.getRange().getStart().getCharacter() + 1
                    );
                }
                logger.info("CLIENT: --------------------------------");
            });
    }

}