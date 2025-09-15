package dev.snowdrop.lsp.proxy;

import dev.snowdrop.lsp.common.services.LSPSymbolInfo;
import dev.snowdrop.lsp.common.utils.FileUtils;
import dev.snowdrop.lsp.common.utils.SnowdropLS;
import dev.snowdrop.lsp.common.utils.LanguageServer;
import org.eclipse.lsp4j.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ServerAndClientLauncher {
    private static final Logger logger = LoggerFactory.getLogger(ServerAndClientLauncher.class);

    public static void main(String[] args) throws Exception {

        Path exampleDir = FileUtils.getExampleDir();
        logger.info("Created project directory: " + exampleDir);

        // Set up the launchers, server and client
        SnowdropLS snowdropLS = LanguageServer.launchServerAndClient(false);
        
        // Initialize the language server with Project Path, ...
        logger.info("CLIENT: Initializing language server...");
        LanguageServer.initializeLanguageServer(snowdropLS.getServer(), exampleDir);
        logger.info("CLIENT: Language server initialized successfully.");

        // Send custom command
        String annotationToFind = "MySearchableAnnotation";
        logger.info("CLIENT: Sending custom command 'java/findAnnotatedClasses' to find '@{}'...", annotationToFind);

/*        ExecuteCommandParams commandParams = new ExecuteCommandParams(
            "java/findAnnotatedClasses",
            Collections.singletonList(annotationToFind)
        );

        CompletableFuture<Object> commandResult = lspConnection.getServerProxy().getWorkspaceService().executeCommand(commandParams);
        Object result = commandResult.get();*/

        WorkspaceSymbolParams symbolParams = new WorkspaceSymbolParams(annotationToFind);
        snowdropLS.getServer().getWorkspaceService().symbol(symbolParams)
            .thenApply(eitherResult -> {
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

                logger.info("LSP workspace/symbol found {} symbols for '{}'", lspSymbols.size(), annotationToFind);
                return lspSymbols;
            }).thenAccept(lspSymbols -> {
                logger.info("CLIENT: --- LSP workspace/symbol {} ---",lspSymbols.size());
                for(LSPSymbolInfo l : lspSymbols) {
                    logger.info("CLIENT:  -> Found @{} on {} in file: {} (line {}, char {})",
                        annotationToFind,
                        "",
                        l.getFileUri(),
                        l.getLocation().getRange().getStart().getLine() + 1,
                        l.getLocation().getRange().getStart().getCharacter() + 1
                    );
                }
            });

        logger.info("CLIENT: --------------------------------");

/*        if (result != null) {
            Gson gson = new Gson();
            Type locationListType = new TypeToken<List<Location>>() {}.getType();
            List<Location> locations = gson.fromJson(gson.toJson(result), locationListType);

            logger.info("CLIENT: --- Search Results ---");
            if (locations.isEmpty()) {
                logger.info("CLIENT: No classes found with the annotation '@{}'.", annotationToFind);
            } else {
                logger.info("CLIENT: Found {} usage(s) of '@{}':", locations.size(), annotationToFind);
                for (Location loc : locations) {
                    logger.info("CLIENT:  -> Found at: {} (line {}, char {})",
                        loc.getUri(),
                        loc.getRange().getStart().getLine() + 1,
                        loc.getRange().getStart().getCharacter() + 1
                    );
                }
            }
            logger.info("CLIENT: ----------------------");
        } else {
            logger.warn("CLIENT: Received null result for command.");
        }*/

        // Shutdown using utility class
        logger.info("CLIENT: Shutting down the language server...");
        // TODO: Add shutdown
        logger.info("CLIENT: Done.");
    }
}