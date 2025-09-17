package dev.snowdrop.lsp.socket;

import dev.snowdrop.lsp.common.utils.LSClient;
import dev.snowdrop.lsp.model.LSPSymbolInfo;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static dev.snowdrop.lsp.common.services.LsSearchService.executeCmd;
import static dev.snowdrop.lsp.common.utils.FileUtils.getExampleDir;

public class JdtlsSocketClient {

    private static final Logger logger = LoggerFactory.getLogger(JdtlsSocketClient.class);
    private static final int SERVER_PORT = 3333;
    private static final long TIMEOUT = 2000;

    public static void main(String[] args) throws Exception {
        Launcher<LanguageServer> launcher;
        ExecutorService executor;

        logger.info("Connecting to the JDT Language Server on port {}...", SERVER_PORT);

        try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
            Socket socket = serverSocket.accept();
            executor = Executors.newSingleThreadExecutor();
            LSClient client = new LSClient();

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

        launcher.startListening();

        LanguageServer remoteProxy = launcher.getRemoteProxy();

        InitializeParams p = new InitializeParams();
        p.setProcessId((int) ProcessHandle.current().pid());
        p.setRootUri(getExampleDir().toUri().toString());
        p.setCapabilities(new ClientCapabilities());

        CompletableFuture<InitializeResult> future = remoteProxy.initialize(p);
        future.get(TIMEOUT, TimeUnit.MILLISECONDS).toString();

        InitializedParams initialized = new InitializedParams();
        remoteProxy.initialized(initialized);


        String annotationToFind = "MySearchableAnnotation";
        //logger.info("CLIENT: Sending custom command '{}' to find '@{}'...", customCmd, annotationToFind);

        // Send by example the command java.project.getAll to the jdt-ls as it supports it
        String customCmd = Optional.ofNullable(System.getenv("LS_CMD")).orElse("java.project.getAll");
        logger.info("CLIENT: Sending custom command '{}' ...", customCmd);

        future
            .thenRunAsync(() -> {
                executeCmd(customCmd, null, remoteProxy);
            })
            .exceptionally(
                t -> {
                    t.printStackTrace();
                    return null;
                }
            );
    }

    /**
     * Search about the annotation
     *
     * @param server                 The proxy to the language server.
     * @param projectRoot            The root directory of the project to be analyzed.
     * @param mySearchableAnnotation
     * @return A CompletableFuture that completes when the entire sequence is finished.
     */
    private static CompletableFuture<List<LSPSymbolInfo>> searchAnnotation(LanguageServer server, Path projectRoot, String mySearchableAnnotation) {
        logger.info("CLIENT: Starting search for @{} annotation...", mySearchableAnnotation);

        if (server == null) {
            logger.error("No LanguageServer available, skipping LSP discovery");
            return CompletableFuture.completedFuture(null);
        }

        return server.getWorkspaceService().symbol(new WorkspaceSymbolParams(mySearchableAnnotation))
            .thenApplyAsync(eitherResult -> {
                List<LSPSymbolInfo> lspSymbols = new ArrayList<>();

                if (eitherResult.isLeft()) {
                    List<? extends SymbolInformation> symbols = eitherResult.getLeft();
                    for (SymbolInformation symbol : symbols) {
                        lspSymbols.add(new LSPSymbolInfo(
                            symbol.getName(),
                            symbol.getLocation().getUri(),
                            symbol.getKind(),
                            symbol.getLocation()
                        ));
                    }
                } else {
                    List<? extends WorkspaceSymbol> symbols = eitherResult.getRight();
                    for (WorkspaceSymbol symbol : symbols) {
                        if (symbol.getLocation().isLeft()) {
                            Location location = symbol.getLocation().getLeft();
                            lspSymbols.add(new LSPSymbolInfo(
                                symbol.getName(),
                                location.getUri(),
                                symbol.getKind(),
                                location
                            ));
                        }
                    }
                }

                logger.info("LSP workspace/symbol found {} symbols for '{}'", lspSymbols.size(), mySearchableAnnotation);
                return lspSymbols;
            });
    }
}