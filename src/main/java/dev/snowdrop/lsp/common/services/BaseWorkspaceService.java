package dev.snowdrop.lsp.common.services;

import dev.snowdrop.lsp.common.ast.ASTAnnotationParser;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * Base workspace service that provides search functionality.
 */
public abstract class BaseWorkspaceService implements WorkspaceService {
    private static final Logger logger = LoggerFactory.getLogger(BaseWorkspaceService.class);
    
    protected LanguageClient client;
    protected String workspaceRoot;

    public void setWorkspaceRoot(String workspaceRoot) {
        this.workspaceRoot = workspaceRoot;
    }

    public void setClient(LanguageClient client) {
        this.client = client;
    }

    @Override
    public CompletableFuture<Object> executeCommand(ExecuteCommandParams params) {
        logger.info("SERVER: Received executeCommand request: {}", params.getCommand());
        
        if ("java/findAnnotatedClasses".equals(params.getCommand())) {
            return handleFindAnnotatedClassesCommand(params);
        }
        
        // Allow subclasses to handle additional commands
        return handleCustomCommand(params);
    }

    /**
     * Handle the java/findAnnotatedClasses command using AST-based search.
     */
    protected CompletableFuture<Object> handleFindAnnotatedClassesCommand(ExecuteCommandParams params) {
        if (params.getArguments() == null || params.getArguments().isEmpty()) {
            logger.error("SERVER: Missing annotation name argument.");
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        Object arg = params.getArguments().get(0);
        String annotationSimpleName;
        if (arg instanceof com.google.gson.JsonPrimitive) {
            annotationSimpleName = ((com.google.gson.JsonPrimitive) arg).getAsString();
        } else {
            annotationSimpleName = arg.toString();
        }
        
        return findClassesWithAnnotationAST(annotationSimpleName);
    }

    /**
     * Override this method in subclasses to handle additional custom commands.
     */
    protected CompletableFuture<Object> handleCustomCommand(ExecuteCommandParams params) {
        logger.warn("SERVER: Unsupported command '{}'", params.getCommand());
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Find classes with the specified annotation using AST-based parsing.
     * This is the shared implementation that provides accurate annotation search.
     */
    protected CompletableFuture<Object> findClassesWithAnnotationAST(String annotationSimpleName) {
        logger.info("SERVER: Searching for classes with annotation '@{}' in workspace: {}", annotationSimpleName, workspaceRoot);
        List<Location> locations = new ArrayList<>();
        
        if (workspaceRoot == null) {
            return CompletableFuture.completedFuture(locations);
        }

        try {
            Path rootPath = Paths.get(URI.create(workspaceRoot));
            try (Stream<Path> paths = Files.walk(rootPath)) {
                paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> {
                        try {
                            // Use quick check first to avoid unnecessary AST parsing
                            if (ASTAnnotationParser.quickAnnotationCheck(path, annotationSimpleName)) {
                                locations.addAll(ASTAnnotationParser.parseJavaFileForAnnotations(path, annotationSimpleName));
                            }
                        } catch (Exception e) {
                            logger.error("SERVER: Failed to parse file {}", path, e);
                        }
                    });
            }
        } catch (IOException e) {
            logger.error("SERVER: Failed to walk workspace path {}", workspaceRoot, e);
        }

        logger.info("SERVER: Found {} locations.", locations.size());
        return CompletableFuture.completedFuture(locations);
    }

    // Default WorkspaceService method implementations
    @Override
    public void didChangeConfiguration(DidChangeConfigurationParams params) {
        // Default implementation - subclasses can override
    }

    @Override
    public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {
        // Default implementation - subclasses can override
    }

    @Override
    public void didChangeWorkspaceFolders(DidChangeWorkspaceFoldersParams params) {
        // Default implementation - subclasses can override
    }
}