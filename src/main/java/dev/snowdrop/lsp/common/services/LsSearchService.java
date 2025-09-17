package dev.snowdrop.lsp.common.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.LanguageServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        if (result != null) {
            logger.info("CLIENT: --- Search Results using as command: {}.", customCmd);
            // TODO This code should be reviewed to adapt it according to the objects returned as response
            logger.info("CLIENT: --- Result: {}", gson.toJson(result));

/*            Gson gson = new Gson();
            Type SymbolInformationListType = new TypeToken<List<SymbolInformation>>() {}.getType();
            List<SymbolInformation> symbolInformationList = gson.fromJson(gson.toJson(result), SymbolInformationListType);

            if (symbolInformationList.isEmpty()) {
                logger.info("CLIENT: SymbolInformation List is empty.");
            } else {
                logger.info("CLIENT: Found {} usage(s)':", symbolInformationList.size());
                for (SymbolInformation si : symbolInformationList) {
                    logger.info("CLIENT:  -> Found at: {} (line {}, char {})",
                        si.getLocation().getUri(),
                        si.getLocation().getRange().getStart().getLine() + 1,
                        si.getLocation().getRange().getStart().getCharacter() + 1
                    );
                }
            }*/
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