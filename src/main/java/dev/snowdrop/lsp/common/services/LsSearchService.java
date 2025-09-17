package dev.snowdrop.lsp.common.services;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.LanguageServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class LsSearchService {

    private static final Logger logger = LoggerFactory.getLogger(LsSearchService.class);
    private final LanguageServer languageServer;

    public LsSearchService(LanguageServer languageServer) {
        this.languageServer = languageServer;
    }

    public static void executeCmd(String customCmd, List<Object> arguments, LanguageServer LS) {
        List<Object> cmdArguments = (arguments != null && !arguments.isEmpty())
            ? arguments
            : Collections.EMPTY_LIST;

        ExecuteCommandParams commandParams = new ExecuteCommandParams(
            customCmd,
            cmdArguments
        );

        CompletableFuture<Object> commandResult = LS.getWorkspaceService()
            .executeCommand(commandParams)
            .exceptionally(
                t -> {
                    t.printStackTrace();
                    return null;
                });

        Object result = commandResult.join();

        if (result != null) {
            logger.info("CLIENT: --- Search Results using as command: {}.",customCmd);
            if (result instanceof ArrayList<?>) {
                logger.info("CLIENT: Project path {}", result.toString());

            } else {
                Gson gson = new Gson();
                Type locationListType = new TypeToken<List<Location>>() {
                }.getType();
                List<Location> locations = gson.fromJson(gson.toJson(result), locationListType);

                if (locations.isEmpty()) {
                    logger.info("CLIENT: No classes found.");
                } else {
                    logger.info("CLIENT: Found {} usage(s)':", locations.size());
                    for (Location loc : locations) {
                        logger.info("CLIENT:  -> Found at: {} (line {}, char {})",
                            loc.getUri(),
                            loc.getRange().getStart().getLine() + 1,
                            loc.getRange().getStart().getCharacter() + 1
                        );
                    }
                }
            }
            logger.info("CLIENT: ----------------------");
        } else {
            logger.warn("CLIENT: Received null result for command.");
        }
    }

    public static CompletableFuture<Optional<SymbolInformation>> searchWksSymbol(String annotationToFind, LanguageServer LS) {
        logger.info("CLIENT: Searching for the definition of '{}' within the java project...", annotationToFind);
        WorkspaceSymbolParams symbolParams = new WorkspaceSymbolParams(annotationToFind);

        return LS.getWorkspaceService().symbol(symbolParams)
            .thenApply(eitherResult -> {
                List<SymbolInformation> symbols = new ArrayList<>();
                if (eitherResult != null) {
                    if (eitherResult.isLeft()) symbols.addAll(eitherResult.getLeft());
                    else
                        symbols.addAll(eitherResult.getRight().stream().filter(ws -> ws.getLocation().isLeft()).map(ws -> new SymbolInformation(ws.getName(), ws.getKind(), ws.getLocation().getLeft())).collect(Collectors.toList()));
                }

                // An annotation in Java has the SymbolKind 'Interface' in LSP.
                return symbols.stream()
                    .filter(s -> s.getKind() == SymbolKind.Interface && s.getName().equals(annotationToFind))
                    .findFirst();
            })
            .exceptionally(t -> {
                t.printStackTrace();
                return null;
            });
    }

                /* OLD code
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

                })
                .thenAccept(lspSymbols -> {
                    logger.info("CLIENT: --- LSP workspace/symbol {} ---", lspSymbols.size());
                    for (LSPSymbolInfo l : lspSymbols) {
                        logger.info("CLIENT:  -> Found @{} on {} in file: {} (line {}, char {})",
                            annotationToFind,
                            "",
                            l.getFileUri(),
                            l.getLocation().getRange().getStart().getLine() + 1,
                            l.getLocation().getRange().getStart().getCharacter() + 1
                        );
                    }
                })
                .exceptionally((ex) -> {
                    logger.error("Failed to initialize language server: {}", ex.getMessage());
                    return null;
                });
                */

}