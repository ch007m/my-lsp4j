package com.example.lsp;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.LanguageClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * An implementation of the LanguageClient interface.
 * This class receives notifications and requests from the Language Server.
 */
public class LspClient implements LanguageClient {

    private static final Logger logger = LoggerFactory.getLogger(LspClient.class);
    private ClientProxy server;

    public void setServer(ClientProxy server) {
        this.server = server;
    }

    @Override
    public void telemetryEvent(Object object) {
        logger.info("telemetryEvent: {}", object);
    }

    @Override
    public void publishDiagnostics(PublishDiagnosticsParams diagnostics) {
        logger.info("publishDiagnostics: {}", diagnostics);
    }

    @Override
    public void showMessage(MessageParams messageParams) {
        logger.info("showMessage: {}", messageParams);
    }

    @Override
    public CompletableFuture<MessageActionItem> showMessageRequest(ShowMessageRequestParams requestParams) {
        logger.info("showMessageRequest: {}", requestParams);
        return new CompletableFuture<>();
    }

    @Override
    public void logMessage(MessageParams message) {
        // Log messages from the server are very useful for debugging
        logger.info("[SERVER LOG] {}: {}", message.getType(), message.getMessage());
    }
}
