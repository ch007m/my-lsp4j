package com.example.lsp;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AnnotationFinder {
    private static final Logger logger = LoggerFactory.getLogger(AnnotationFinder.class);

    public static void findAnnotationUsages(LanguageServer server, Path projectRoot) throws Exception {
        logger.info("--- Searching for @MySearchableAnnotation usages ---");

        // Define the location of the symbol we want to find references to.
        Path annotationFile = projectRoot.resolve("src/main/java/com/example/annotations/MySearchableAnnotation.java");
        TextDocumentIdentifier textDocument = new TextDocumentIdentifier(annotationFile.toUri().toString());

        // Find the position of the annotation dynamically using document symbols
        Position annotationPosition = findAnnotationPosition(server, textDocument, "MySearchableAnnotation");
        if (annotationPosition == null) {
            logger.error("Could not find annotation definition position");
            return;
        }

        ReferenceParams referenceParams = new ReferenceParams(textDocument, annotationPosition, new ReferenceContext(false));

        // Send the request and wait for the response
        CompletableFuture<List<? extends Location>> referencesFuture = server.getTextDocumentService().references(referenceParams);
        List<? extends Location> locations = referencesFuture.get();

        if (locations.isEmpty()) {
            logger.warn("No usages of the annotation were found.");
        } else {
            logger.info("Found {} usage(s) of @MySearchableAnnotation:", locations.size());
            for (Location loc : locations) {
                logger.info("  -> Found at: {} (line {}, char {})",
                    loc.getUri(),
                    loc.getRange().getStart().getLine() + 1, // Convert to 1-based for display
                    loc.getRange().getStart().getCharacter() + 1
                );
            }
        }
    }

    public static Position findAnnotationPosition(LanguageServer server, TextDocumentIdentifier textDocument, String annotationName) throws Exception {
        DocumentSymbolParams params = new DocumentSymbolParams(textDocument);
        CompletableFuture<List<Either<SymbolInformation, DocumentSymbol>>> symbolsFuture =
            server.getTextDocumentService().documentSymbol(params);
        List<Either<SymbolInformation, DocumentSymbol>> symbols = symbolsFuture.get();

        logger.info("Found {} symbols in the document", symbols.size());
        for (Either<SymbolInformation, DocumentSymbol> symbolEither : symbols) {
            if (symbolEither.isRight()) {
                DocumentSymbol symbol = symbolEither.getRight();
                logger.info("Found symbol: name='{}', kind={}", symbol.getName(), symbol.getKind());
                if (symbol.getName().equals(annotationName) && (symbol.getKind() == SymbolKind.Interface || symbol.getKind() == SymbolKind.Class)) {
                    logger.info("Found matching annotation symbol at position: {}", symbol.getRange().getStart());
                    return symbol.getRange().getStart();
                }
            } else if (symbolEither.isLeft()) {
                SymbolInformation symbolInfo = symbolEither.getLeft();
                logger.info("Found symbol info: name='{}', kind={}", symbolInfo.getName(), symbolInfo.getKind());
                if (symbolInfo.getName().equals(annotationName) && (symbolInfo.getKind() == SymbolKind.Interface || symbolInfo.getKind() == SymbolKind.Class)) {
                    logger.info("Found matching annotation symbol info at position: {}", symbolInfo.getLocation().getRange().getStart());
                    return symbolInfo.getLocation().getRange().getStart();
                }
            }
        }
        return null;
    }

    public static InitializeParams getInitializeParams(Path projectRoot) {
        InitializeParams params = new InitializeParams();
        params.setProcessId((int) ProcessHandle.current().pid());
        params.setRootUri(projectRoot.toUri().toString());
        params.setWorkspaceFolders(Collections.singletonList(new WorkspaceFolder(projectRoot.toUri().toString(), "sample-project")));
        params.setCapabilities(new ClientCapabilities());
        return params;
    }
}
