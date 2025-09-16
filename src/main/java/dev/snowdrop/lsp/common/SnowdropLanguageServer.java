package dev.snowdrop.lsp.common;

import dev.snowdrop.lsp.common.services.JavaTextDocumentService;
import dev.snowdrop.lsp.common.services.JavaWorkspaceService;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class SnowdropLanguageServer implements LanguageServer {
    private static final Logger logger = LoggerFactory.getLogger(SnowdropLanguageServer.class);

    private final JavaTextDocumentService textDocumentService;
    private final JavaWorkspaceService workspaceService;
    private int exitCode = 0;

    public SnowdropLanguageServer() {
        this.textDocumentService = new JavaTextDocumentService();
        this.workspaceService = new JavaWorkspaceService();
    }

    public void setWorkSpaceRoot(String workSpaceRoot) {
        this.workspaceService.setWorkspaceRoot(workSpaceRoot);
    }

    @Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
        logger.info("SERVER: Initializing Java Language Server...");

        String projectRoot = params.getRootUri();
        if (projectRoot != null) {
            textDocumentService.setWorkspaceRoot(projectRoot);
            workspaceService.setWorkspaceRoot(projectRoot);
        }

        ServerCapabilities capabilities = new ServerCapabilities();
        capabilities.setTextDocumentSync(TextDocumentSyncKind.Full);

        ExecuteCommandOptions executeCommandOptions = new ExecuteCommandOptions();
        executeCommandOptions.getCommands().add("java/findAnnotatedClasses");
        capabilities.setExecuteCommandProvider(executeCommandOptions);

        logger.info("SERVER: Initialization complete.");
        return CompletableFuture.completedFuture(new InitializeResult(capabilities));
    }

    @Override
    public void initialized(InitializedParams params) {
        logger.info("SERVER: Client has been initialized.");
    }

    @Override
    public CompletableFuture<Object> shutdown() {
        logger.info("SERVER: Shutdown requested.");
        this.exitCode = 0;
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void exit() {
        logger.info("SERVER: Exit requested.");
        System.exit(exitCode);
    }

    @Override
    public TextDocumentService getTextDocumentService() {
        return this.textDocumentService;
    }

    @Override
    public WorkspaceService getWorkspaceService() {
        return this.workspaceService;
    }

}
