package dev.snowdrop.lsp.common.utils;

import dev.snowdrop.lsp.proxy.JavaLanguageServer;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.services.LanguageServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Container for LSP communication components.
 */
public class LSPConnection {

    private static final Logger logger = LoggerFactory.getLogger(LSPConnection.class);

    private final ExecutorService executor;
    private final Launcher<LanguageServer> serverLauncher;
    private final Launcher<LanguageServer> clientLauncher;
    private final LanguageServer serverProxy;
    private final JavaLanguageServer server;
    private final LspClient client;

    public LSPConnection(ExecutorService executor,
                         Launcher<LanguageServer> serverLauncher,
                         Launcher<LanguageServer> clientLauncher,
                         LanguageServer serverProxy,
                         JavaLanguageServer server,
                         LspClient client) {
        this.executor = executor;
        this.serverLauncher = serverLauncher;
        this.clientLauncher = clientLauncher;
        this.serverProxy = serverProxy;
        this.server = server;
        this.client = client;
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public Launcher<LanguageServer> getServerLauncher() {
        return serverLauncher;
    }

    public Launcher<LanguageServer> getClientLauncher() {
        return clientLauncher;
    }

    public LanguageServer getServerProxy() {
        return serverProxy;
    }

    public JavaLanguageServer getServer() {
        return server;
    }

    public LspClient getClient() {
        return client;
    }

    /**
     * Shutdown the LSP connection gracefully.
     */
    public void shutdown() {
        try {
            if (serverProxy != null) {
                serverProxy.shutdown().get(2, TimeUnit.SECONDS);
                serverProxy.exit();
            }
        } catch (Exception e) {
            logger.warn("Error during server shutdown: {}", e.getMessage());
        }

        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
