package dev.snowdrop.lsp.common.services;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dev.snowdrop.lsp.common.utils.SnowdropLS;
import dev.snowdrop.lsp.model.LSPSymbolInfo;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.LanguageServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class AnnotationSearchService {
    
    private static final Logger logger = LoggerFactory.getLogger(AnnotationSearchService.class);
    private final LanguageServer languageServer;
    
    public AnnotationSearchService(LanguageServer languageServer) {
        this.languageServer = languageServer;
    }

    public static void executeCmd(String annotationToFind, LanguageServer LS) {
        ExecuteCommandParams commandParams = new ExecuteCommandParams(
            "java/findAnnotatedClasses",
            Collections.singletonList(annotationToFind)
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
        }
    }

    public static void useSymbol(String annotationToFind, SnowdropLS snowdropLS) {
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

    }
    
    /**
     * Searches for annotations using LSP workspace operations.
     * 
     * @param projectRoot The root directory to search
     * @param annotationName The annotation name to search for (without @)
     * @return CompletableFuture containing search results
     */
    public CompletableFuture<List<? extends Location>> searchAnnotation(Path projectRoot, String annotationName) {
        logger.info("Starting LSP search for @{} annotation in project: {}", annotationName, projectRoot);
        
        // First use LSP to discover workspace symbols
        return findUsingLsSymbol(annotationName)
            .thenCompose(optionalDefinition -> {
            if (optionalDefinition.isEmpty()) {
                logger.warn("Could not find a definition for annotation '{}'. Cannot search for references.", annotationName);
                return CompletableFuture.completedFuture(new ArrayList<Location>());
            }
            // If definition is found, proceed to find its references
            // TODO: Improve this code to iterate
            return findAnnotationReferences(optionalDefinition.get(0));
        });
    }
    
    /**
     * Uses LSP workspace/symbol to find potential Java files containing the annotation.
     */
    private CompletableFuture<List<LSPSymbolInfo>> findUsingLsSymbol(String annotationName) {
        if (languageServer == null) {
            logger.error("No LanguageServer available, skipping LSP discovery");
            return CompletableFuture.completedFuture(new ArrayList<>());
        }
        
        WorkspaceSymbolParams symbolParams = new WorkspaceSymbolParams(annotationName);
        
        return languageServer.getWorkspaceService().symbol(symbolParams)
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
                
                logger.info("LSP workspace/symbol found {} symbols for '{}'", lspSymbols.size(), annotationName);
                return lspSymbols;
            });
    }

    /**
     * Given the symbol for an annotation's definition, this method finds all its references.
     */
    private CompletableFuture<List<? extends Location>> findAnnotationReferences(LSPSymbolInfo definitionSymbol) {
        if (languageServer == null) {
            return CompletableFuture.completedFuture(new ArrayList<>());
        }

        logger.info("Found definition of '{}' at {}. Now searching for all references...",
            definitionSymbol.getName(), definitionSymbol.getFileUri());

        // 1. Create a ReferenceParams object for the request
        ReferenceParams params = new ReferenceParams();

        // 2. Set the document identifier from the definition we found in step 1
        params.setTextDocument(new TextDocumentIdentifier(definitionSymbol.getFileUri()));

        // 3. Set the position of the symbol in that document
        params.setPosition(definitionSymbol.getLocation().getRange().getStart());

        // 4. Set the context to include the declaration itself in the results
        params.setContext(new ReferenceContext(true));

        // 5. Call the 'textDocument/references' endpoint
        logger.debug("Issuing 'textDocument/references' request with params: {}", params);
        return languageServer.getTextDocumentService().references(params);
    }

}