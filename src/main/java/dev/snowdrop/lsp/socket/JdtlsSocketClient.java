package dev.snowdrop.lsp.socket;

import dev.snowdrop.lsp.common.utils.LspClient;
import dev.snowdrop.lsp.common.services.AnnotationSearchService;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        LanguageServer serverProxy = launcher.getRemoteProxy();

        try {
            runLspClient(serverProxy, tempDir, "MySearchableAnnotation").join();
        } catch (Exception e) {
            logger.error("The LSP workflow failed unexpectedly.", e);
        } finally {
            // Gracefully shut down all resources
            shutdownResources(listening, executor, socket);
        }
    }

    /**
     * Executes the entire client-side LSP workflow asynchronously.
     *
     * @param server                 The proxy to the language server.
     * @param projectRoot            The root directory of the project to be analyzed.
     * @param mySearchableAnnotation
     * @return A CompletableFuture that completes when the entire sequence is finished.
     */
    private static CompletableFuture<Void> runLspClient(LanguageServer server, Path projectRoot, String mySearchableAnnotation) {
        InitializeParams initParams = new InitializeParams();
        initParams.setProcessId((int) ProcessHandle.current().pid());
        initParams.setRootUri(projectRoot.toUri().toString());
        initParams.setCapabilities(new ClientCapabilities());

        // This is the full asynchronous chain of operations
        return server.initialize(initParams)
            .orTimeout(10, TimeUnit.SECONDS)
            .thenAccept(result -> {
                logger.info("CLIENT: Initialization successful.");
                server.initialized(new InitializedParams());
                logger.info("CLIENT: Handshake complete.");
            })
            .thenCompose(v -> searchWithAnnotationService(server, projectRoot, mySearchableAnnotation))
            //.thenCompose(v -> findAnnotationDefinition(server, mySearchableAnnotation))
            //.thenCompose(symbolResult -> findAnnotationReferences(server, symbolResult))
            //.thenAccept(JdtlsSocketClient::logReferenceResults)
            .exceptionally(throwable -> {
                logger.error("CLIENT: An error occurred in the LSP communication chain.", throwable);
                return null; // Recover from the error to allow shutdown to proceed
            })
            .thenCompose(v -> server.shutdown()) // Chain the shutdown
            .thenRun(server::exit);
    }

    /**
     * Search for MySearchable annotation using the AnnotationSearchService with AST and IAnnotation.
     */
    private static CompletableFuture<Void> searchWithAnnotationService(LanguageServer server, Path projectRoot, String annotationName) {
        logger.info("CLIENT: Starting AST-based search for @{} annotation...", annotationName);
        
        AnnotationSearchService searchService = new AnnotationSearchService(server);
        
        return searchService.searchAnnotation(projectRoot, annotationName)
            .thenAccept(result -> {
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
                /*
                logger.info("CLIENT: Found {} @{} annotation(s) using AST analysis:", result.getMatchCount(), annotationName);
                for (AnnotationSearchService.AnnotationMatch match : result.getMatches()) {
                    logger.info("CLIENT:  -> Found @{} on {} in file: {} (line {}, char {})",
                        annotationName,
                        match.getAnnotatedElement(),
                        match.getLocation().getUri(),
                        match.getLocation().getRange().getStart().getLine() + 1,
                        match.getLocation().getRange().getStart().getCharacter() + 1
                    );
                    
                    // Log annotation details if available
                    AnnotationSearchService.AnnotationDetails details = match.getDetails();
                    if (details.getMemberValues() != null && !details.getMemberValues().isEmpty()) {
                        logger.info("CLIENT:    Annotation details: {}", details.getSourceText());
                        for (AnnotationSearchService.AnnotationMemberValue member : details.getMemberValues()) {
                            logger.info("CLIENT:      {} = {}", member.getName(), member.getValue());
                        }
                    }
                }*/
                logger.info("CLIENT: --------------------------------");
            })
            .exceptionally(throwable -> {
                logger.error("CLIENT: LSP-based annotation search failed", throwable);
                return null;
            });
    }


    /**
     * Find the annotation definition first using workspace/symbol
     */
    /*
    private static CompletableFuture<Location> findAnnotationDefinition(LanguageServer server, String annotationName) {
        logger.info("CLIENT: Finding annotation definition for '@{}'...", annotationName);
        WorkspaceSymbolParams symbolParams = new WorkspaceSymbolParams(annotationName);
        return server.getWorkspaceService().symbol(symbolParams)
            .thenApply(eitherResult -> {
                if (eitherResult.isLeft()) {
                    List<? extends SymbolInformation> symbols = eitherResult.getLeft();
                    if (!symbols.isEmpty()) {
                        SymbolInformation symbol = symbols.get(0);
                        logger.info("CLIENT: Found annotation definition at: {}", symbol.getLocation().getUri());
                        return symbol.getLocation();
                    }
                } else {
                    List<? extends WorkspaceSymbol> symbols = eitherResult.getRight();
                    if (!symbols.isEmpty()) {
                        WorkspaceSymbol symbol = symbols.get(0);
                        Location location = symbol.getLocation().getLeft();
                        logger.info("CLIENT: Found annotation definition at: {}", location.getUri());
                        return location;
                    }
                }
                throw new RuntimeException("Annotation definition not found");
            });
       }*/

    /**
     * Find all references to the annotation using textDocument/references
     */
    private static CompletableFuture<List<? extends Location>> findAnnotationReferences(LanguageServer server, Location annotationLocation) {
        logger.info("CLIENT: Finding references to annotation...");
        
        ReferenceParams referenceParams = new ReferenceParams();
        referenceParams.setTextDocument(new TextDocumentIdentifier(annotationLocation.getUri()));
        referenceParams.setPosition(annotationLocation.getRange().getStart());
        referenceParams.setContext(new ReferenceContext(true)); // Include declaration
        
        return server.getTextDocumentService().references(referenceParams);
    }

    /**
     * Log the reference results
     */
    private static void logReferenceResults(List<? extends Location> locations) {
        logger.info("CLIENT: --- Search Results ---");
        
        if (locations == null || locations.isEmpty()) {
            logger.info("CLIENT: No annotation usages found.");
        } else {
            logger.info("CLIENT: Found {} usage(s) of the annotation:", locations.size());
            for (Location location : locations) {
                logger.info("CLIENT:  -> Found at: {} (line {}, char {})",
                    location.getUri(),
                    location.getRange().getStart().getLine() + 1,
                    location.getRange().getStart().getCharacter() + 1
                );
            }
        }
        logger.info("CLIENT: ----------------------");
    }

    /**
     * Gracefully shuts down all managed resources.
     */
    private static void shutdownResources(Future<Void> listening, ExecutorService executor, Socket socket) {
        logger.info("Shutting down all resources...");
        try {
            listening.cancel(true);
            executor.shutdown();
            if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
            socket.close();
        } catch (Exception e) {
            logger.warn("Error during resource shutdown: {}", e.getMessage());
        }
        logger.info("Shutdown complete.");
    }

}