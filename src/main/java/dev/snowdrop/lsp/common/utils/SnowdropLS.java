package dev.snowdrop.lsp.common.utils;

import dev.snowdrop.lsp.proxy.JavaLanguageServer;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class SnowdropLS {

    private static final Logger logger = LoggerFactory.getLogger(SnowdropLS.class);
    private final LanguageServer server;

    public SnowdropLS(LanguageServer server) {
        this.server = server;
    }

    public LanguageServer getServer() {
        return server;
    }

}
