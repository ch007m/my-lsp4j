package dev.snowdrop.lsp.common.services;

import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.*;

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
        // Non implémenté pour cet exemple
    }

    @Override
    public void didClose(DidCloseTextDocumentParams params) {
        // Non implémenté pour cet exemple
    }

    @Override
    public void didSave(DidSaveTextDocumentParams params) {
        // Non implémenté pour cet exemple
    }
}
