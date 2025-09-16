package dev.snowdrop.lsp.common.utils;

import org.eclipse.lsp4j.services.LanguageServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
