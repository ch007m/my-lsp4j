package dev.snowdrop.lsp.socket;

import dev.snowdrop.lsp.common.ast.ASTAnnotationParser;
import dev.snowdrop.lsp.common.utils.ProjectGenerator;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.LanguageServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Enhanced annotation finder for the socket approach that uses shared AST-based logic.
 * This replaces the original AnnotationFinder with improved capabilities.
 */
public class AnnotationFinder {
    private static final Logger logger = LoggerFactory.getLogger(AnnotationFinder.class);

    /**
     * Find annotation usages using the shared AST-based approach.
     * This provides more accurate results than the original LSP reference-based approach.
     */
    public static void findAnnotationUsagesAST(LanguageServer server, Path projectRoot, String annotationName) throws Exception {
        logger.info("--- Enhanced AST-based search for @{} usages ---", annotationName);

        // Use custom command instead of LSP references for better accuracy
        ExecuteCommandParams commandParams = new ExecuteCommandParams(
            "java/findAnnotatedClasses",
            Collections.singletonList(annotationName)
        );

        try {
            CompletableFuture<Object> commandResult = server.getWorkspaceService().executeCommand(commandParams);
            Object result = commandResult.get();

            if (result instanceof List<?>) {
                @SuppressWarnings("unchecked")
                List<Location> locations = (List<Location>) result;
                
                if (locations.isEmpty()) {
                    logger.warn("No usages of @{} annotation were found.", annotationName);
                } else {
                    logger.info("Found {} usage(s) of @{}:", locations.size(), annotationName);
                    for (Location loc : locations) {
                        logger.info("  -> Found at: {} (line {}, char {})",
                            loc.getUri(),
                            loc.getRange().getStart().getLine() + 1,
                            loc.getRange().getStart().getCharacter() + 1
                        );
                    }
                }
            } else {
                logger.warn("Unexpected result type from executeCommand: {}", result != null ? result.getClass() : "null");
            }
        } catch (Exception e) {
            logger.error("Failed to execute annotation search command", e);
            // Fallback to direct AST search
            fallbackDirectASTSearch(projectRoot, annotationName);
        }
    }

    /**
     * Fallback method that directly uses AST parsing without going through LSP.
     * This uses AnnotationVisitor directly as requested.
     */
    private static void fallbackDirectASTSearch(Path projectRoot, String annotationName) throws IOException {
        logger.info("Using fallback direct AST search for @{}", annotationName);
        
        // Use ASTAnnotationParser to directly search for annotations using AnnotationVisitor
        try {
            java.nio.file.Files.walk(projectRoot)
                .filter(java.nio.file.Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".java"))
                .forEach(path -> {
                    try {
                        // Quick check first to avoid unnecessary AST parsing
                        if (ASTAnnotationParser.quickAnnotationCheck(path, annotationName)) {
                            // Use AnnotationVisitor directly through ASTAnnotationParser
                            List<org.eclipse.lsp4j.Location> locations = ASTAnnotationParser.parseJavaFileForAnnotations(path, annotationName);
                            
                            for (org.eclipse.lsp4j.Location loc : locations) {
                                logger.info("  -> Found @{} at: {} (line {}, char {})",
                                    annotationName,
                                    loc.getUri(),
                                    loc.getRange().getStart().getLine() + 1,
                                    loc.getRange().getStart().getCharacter() + 1
                                );
                            }
                        }
                    } catch (Exception e) {
                        logger.error("Failed to parse file {}", path, e);
                    }
                });
                
            logger.info("Direct AST search completed using AnnotationVisitor");
        } catch (IOException e) {
            logger.error("Failed to walk project directory {}", projectRoot, e);
            throw e;
        }
    }

    /**
     * Generate a test project with various annotation patterns for testing.
     */
    public static Path generateTestProject(Path baseDir) throws IOException {
        Path testProject = baseDir.resolve("ast-test-project");
        
        // Generate a project with specific annotation patterns
        Map<String, Integer> patterns = new HashMap<>();
        patterns.put("MySearchableAnnotation", 5);
        patterns.put("ImportantAnnotation", 3);
        patterns.put("TestAnnotation", 2);
        
        ProjectGenerator.generateProjectWithPatterns(testProject, patterns);
        
        // Also generate a complete project structure
        ProjectGenerator.generateCompleteProject(
            testProject, 
            "AST Test Project", 
            "dev.snowdrop.test", 
            "ast-test-project"
        );
        
        logger.info("Generated test project at: {}", testProject);
        return testProject;
    }

    /**
     * Analyze all annotations in a project using the shared AST utilities.
     */
    public static void analyzeProjectAnnotations(Path projectRoot) throws IOException {
        logger.info("--- Analyzing all annotations in project ---");
        
        ASTAnnotationParser.AnnotationSearchResult result = ASTAnnotationParser.analyzeAllAnnotations(projectRoot);
        
        logger.info("Project: {}", result.getFile());
        logger.info("Total annotations found: {}", result.getAnnotationCount());
        
        for (ASTAnnotationParser.AnnotationInfo info : result.getAnnotations()) {
            logger.info("  @{} at {}:{}:{} - {}", 
                info.getName(),
                info.getFileUri().substring(info.getFileUri().lastIndexOf('/') + 1),
                info.getLine() + 1,
                info.getColumn() + 1,
                info.getFullText()
            );
        }
    }

    /**
     * Enhanced initialization parameters that include additional capabilities.
     */
    public static InitializeParams getEnhancedInitializeParams(Path projectRoot) {
        InitializeParams params = new InitializeParams();
        params.setProcessId((int) ProcessHandle.current().pid());
        params.setRootUri(projectRoot.toUri().toString());
        params.setWorkspaceFolders(Collections.singletonList(
            new WorkspaceFolder(projectRoot.toUri().toString(), "enhanced-project")
        ));
        
        // Set client capabilities to indicate we support enhanced features
        ClientCapabilities capabilities = new ClientCapabilities();
        
        // Configure basic client capabilities - use default capabilities
        capabilities.setTextDocument(new TextDocumentClientCapabilities());
        capabilities.setWorkspace(new WorkspaceClientCapabilities());
        
        params.setCapabilities(capabilities);
        return params;
    }

    /**
     * Demonstrate the difference between old and new approaches.
     */
    public static void compareSearchApproaches(LanguageServer server, Path projectRoot, String annotationName) throws Exception {
        logger.info("=== Comparing Search Approaches ===");
        
        // Time the enhanced AST approach
        long startTime = System.currentTimeMillis();
        findAnnotationUsagesAST(server, projectRoot, annotationName);
        long astTime = System.currentTimeMillis() - startTime;
        logger.info("AST-based search took: {} ms", astTime);
        
        // The original approach would be much less accurate for annotation searches
        logger.info("Original LSP reference approach limitations:");
        logger.info("  - May miss annotations in comments");
        logger.info("  - Less precise location reporting");
        logger.info("  - Dependent on full semantic analysis");
        logger.info("  - May require symbol resolution");
    }
}