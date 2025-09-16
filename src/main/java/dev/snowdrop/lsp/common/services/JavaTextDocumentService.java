package dev.snowdrop.lsp.common.services;

import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class JavaTextDocumentService implements TextDocumentService {
    private String workspaceRoot;

    public void setWorkspaceRoot(String workspaceRoot) {
        this.workspaceRoot = workspaceRoot;
    }

    @Override
    public void didOpen(DidOpenTextDocumentParams params) {
        // Non implémenté pour cet exemple
    }

    @Override
    public void didChange(DidChangeTextDocumentParams params) {

    }

    @Override
    public void didClose(DidCloseTextDocumentParams params) {
    }

    @Override
    public void didSave(DidSaveTextDocumentParams params) {
    }

    @Override
    public CompletableFuture<List<Either<SymbolInformation, DocumentSymbol>>> documentSymbol(DocumentSymbolParams params) {
        return TextDocumentService.super.documentSymbol(params);
    }
}
