package dev.snowdrop.lsp.common.utils;

import org.eclipse.lsp4j.services.LanguageServer;

import java.util.concurrent.ExecutorService;

public class SnowdropLS {

    private final LanguageServer server;
    private final LanguageServer lsInstance;

    public SnowdropLS(LanguageServer server,  LanguageServer lsInstance) {
        this.server = server;
        this.lsInstance = lsInstance;
    }

    public LanguageServer getServer() {
        return server;
    }
    public LanguageServer getLsInstance() {
        return lsInstance;
    }
}
