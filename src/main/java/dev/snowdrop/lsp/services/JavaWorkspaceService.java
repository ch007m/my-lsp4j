package dev.snowdrop.lsp.services;

import dev.snowdrop.lsp.AnnotationVisitor;
import org.eclipse.jdt.core.dom.*;
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

public class JavaWorkspaceService implements WorkspaceService {
    private static final Logger logger = LoggerFactory.getLogger(JavaWorkspaceService.class);
    private LanguageClient client;
    private String workspaceRoot;

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
            return findClassesWithAnnotation(annotationSimpleName);
        }
        logger.warn("SERVER: Unsupported command '{}'", params.getCommand());
        return CompletableFuture.completedFuture(null);
    }

    private CompletableFuture<Object> findClassesWithAnnotation(String annotationSimpleName) {
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
                            locations.addAll(parseJavaFileForAnnotations(path, annotationSimpleName));
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

    /**
     * Parse a Java file using Eclipse JDT AST to find annotations.
     * This provides more accurate parsing than simple string matching.
     */
    private List<Location> parseJavaFileForAnnotations(Path javaFile, String annotationName) throws IOException {
        String source = Files.readString(javaFile);
        
        // Create AST parser
        ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
        parser.setSource(source.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setResolveBindings(false); // We don't need full binding resolution for annotation names
        
        // Parse the source code
        CompilationUnit compilationUnit = (CompilationUnit) parser.createAST(null);
        
        // Create and run the annotation visitor
        AnnotationVisitor visitor = new AnnotationVisitor(annotationName, compilationUnit, javaFile.toUri());
        compilationUnit.accept(visitor);
        
        return visitor.getLocations();
    }


    @Override
    public void didChangeConfiguration(DidChangeConfigurationParams params) {
    }

    @Override
    public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {
    }

    @Override
    public void didChangeWorkspaceFolders(DidChangeWorkspaceFoldersParams params) {
    }
}
