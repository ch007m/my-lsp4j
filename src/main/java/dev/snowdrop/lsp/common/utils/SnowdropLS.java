package dev.snowdrop.lsp.common.utils;

import org.eclipse.lsp4j.services.LanguageServer;

public class SnowdropLS {

    private final LanguageServer server;

    public SnowdropLS(LanguageServer server) {
        this.server = server;
    }

    public LanguageServer getServer() {
        return server;
    }
}
