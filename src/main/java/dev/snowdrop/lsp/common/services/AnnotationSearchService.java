package dev.snowdrop.lsp.common.services;

import dev.snowdrop.lsp.common.ast.ASTAnnotationParser;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.LanguageServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * Service class that provides annotation search capabilities using Eclipse JDT AST 
 * and IAnnotation interface. Integrates with LanguageServer for LSP operations.
 */
public class AnnotationSearchService {
    
    private static final Logger logger = LoggerFactory.getLogger(AnnotationSearchService.class);
    private final LanguageServer languageServer;
    private final ASTParser astParser;
    
    public AnnotationSearchService(LanguageServer languageServer) {
        this.languageServer = languageServer;
        this.astParser = createASTParser();
    }
    
    /**
     * Creates and configures an AST parser for Java source code analysis.
     */
    private ASTParser createASTParser() {
        ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setResolveBindings(true); // Enable binding resolution for IAnnotation support
        parser.setBindingsRecovery(true);
        return parser;
    }
    
    /**
     * Searches for annotations using LSP workspace operations combined with AST analysis and IAnnotation.
     * This approach leverages LSP for workspace discovery and AST for detailed annotation analysis.
     * 
     * @param projectRoot The root directory to search
     * @param annotationName The annotation name to search for (without @)
     * @return CompletableFuture containing search results
     */
    public CompletableFuture<AnnotationSearchResult> searchAnnotation(Path projectRoot, String annotationName) {
        logger.info("Starting LSP + AST search for @{} annotation in project: {}", annotationName, projectRoot);
        
        // First use LSP to discover workspace symbols, then combine with AST analysis
        return findJavaFilesUsingLSP(annotationName)
            .thenCompose(lspResults -> {
                logger.info("LSP found {} potential files, performing detailed AST analysis...", lspResults.size());
                return performDetailedASTAnalysis(projectRoot, annotationName, lspResults);
            })
            .thenApply(matches -> {
                logger.info("Combined LSP + AST search completed. Found {} matches for @{}", matches.size(), annotationName);
                return new AnnotationSearchResult(matches);
            })
            .exceptionally(throwable -> {
                logger.warn("LSP search failed, falling back to file system approach: {}", throwable.getMessage());
                return fallbackFileSystemSearch(projectRoot, annotationName);
            });
    }
    
    /**
     * Uses LSP workspace/symbol to find potential Java files containing the annotation.
     */
    private CompletableFuture<List<LSPSymbolInfo>> findJavaFilesUsingLSP(String annotationName) {
        if (languageServer == null) {
            logger.error("No LanguageServer available, skipping LSP discovery");
            return CompletableFuture.completedFuture(new ArrayList<>());
        }
        
        WorkspaceSymbolParams symbolParams = new WorkspaceSymbolParams(annotationName);
        
        return languageServer.getWorkspaceService().symbol(symbolParams)
            .thenApply(eitherResult -> {
                List<LSPSymbolInfo> lspSymbols = new ArrayList<>();
                
                if (eitherResult.isLeft()) {
                    List<? extends SymbolInformation> symbols = eitherResult.getLeft();
                    for (SymbolInformation symbol : symbols) {
                        lspSymbols.add(new LSPSymbolInfo(
                            symbol.getName(),
                            symbol.getLocation().getUri(),
                            symbol.getKind(),
                            symbol.getLocation()
                        ));
                    }
                } else {
                    List<? extends WorkspaceSymbol> symbols = eitherResult.getRight();
                    for (WorkspaceSymbol symbol : symbols) {
                        if (symbol.getLocation().isLeft()) {
                            Location location = symbol.getLocation().getLeft();
                            lspSymbols.add(new LSPSymbolInfo(
                                symbol.getName(),
                                location.getUri(),
                                symbol.getKind(),
                                location
                            ));
                        }
                    }
                }
                
                logger.info("LSP workspace/symbol found {} symbols for '{}'", lspSymbols.size(), annotationName);
                return lspSymbols;
            });
    }
    
    /**
     * Performs detailed AST analysis on discovered files, enriched with LSP symbol information.
     */
    private CompletableFuture<List<AnnotationMatch>> performDetailedASTAnalysis(Path projectRoot, String annotationName, List<LSPSymbolInfo> lspResults) {
        return CompletableFuture.supplyAsync(() -> {
            List<AnnotationMatch> matches = new ArrayList<>();
            
            // Get unique file URIs from LSP results
            Set<String> lspFileUris = lspResults.stream()
                .map(LSPSymbolInfo::getFileUri)
                .collect(java.util.stream.Collectors.toSet());
            
            // First, analyze files discovered by LSP
            for (String fileUri : lspFileUris) {
                try {
                    Path javaFile = Path.of(java.net.URI.create(fileUri));
                    if (Files.exists(javaFile)) {
                        List<AnnotationMatch> fileMatches = searchAnnotationInFileWithLSPContext(javaFile, annotationName, lspResults);
                        matches.addAll(fileMatches);
                    }
                } catch (Exception e) {
                    logger.debug("Could not process LSP-discovered file {}: {}", fileUri, e.getMessage());
                }
            }
            
            // Also scan the entire project for annotations LSP might have missed
            try (Stream<Path> allJavaFiles = Files.walk(projectRoot)
                    .filter(path -> path.toString().endsWith(".java"))) {
                
                allJavaFiles.forEach(javaFile -> {
                    String fileUri = javaFile.toUri().toString();
                    if (!lspFileUris.contains(fileUri)) {
                        try {
                            List<AnnotationMatch> fileMatches = searchAnnotationInFile(javaFile, annotationName);
                            matches.addAll(fileMatches);
                        } catch (Exception e) {
                            logger.warn("Failed to process additional file {}: {}", javaFile, e.getMessage());
                        }
                    }
                });
            } catch (IOException e) {
                logger.warn("Error walking project directory: {}", e.getMessage());
            }
            
            return matches;
        });
    }
    
    /**
     * Fallback to file system search when LSP is unavailable.
     */
    private AnnotationSearchResult fallbackFileSystemSearch(Path projectRoot, String annotationName) {
        try {
            logger.info("Performing fallback file system search for @{}", annotationName);
            List<AnnotationMatch> matches = new ArrayList<>();
            
            try (Stream<Path> javaFiles = Files.walk(projectRoot)
                    .filter(path -> path.toString().endsWith(".java"))) {
                
                javaFiles.forEach(javaFile -> {
                    try {
                        List<AnnotationMatch> fileMatches = searchAnnotationInFile(javaFile, annotationName);
                        matches.addAll(fileMatches);
                    } catch (Exception e) {
                        logger.warn("Failed to process file {}: {}", javaFile, e.getMessage());
                    }
                });
            }
            
            logger.info("Fallback search completed. Found {} matches for @{}", matches.size(), annotationName);
            return new AnnotationSearchResult(matches);
            
        } catch (Exception e) {
            logger.error("Error during fallback annotation search", e);
            throw new RuntimeException("Annotation search failed", e);
        }
    }
    
    /**
     * Searches for a specific annotation in a single Java file using AST and IAnnotation.
     * 
     * @param javaFile The Java file to search
     * @param annotationName The annotation name to search for (without @)
     * @return List of annotation matches found
     */
    public List<AnnotationMatch> searchAnnotationInFile(Path javaFile, String annotationName) throws IOException {
        String source = Files.readString(javaFile);
        List<AnnotationMatch> matches = new ArrayList<>();
        
        synchronized (astParser) {
            astParser.setSource(source.toCharArray());
            astParser.setUnitName(javaFile.getFileName().toString());
            
            CompilationUnit compilationUnit = (CompilationUnit) astParser.createAST(null);
            
            // Create visitor to find annotations
            MySearchableAnnotationVisitor visitor = new MySearchableAnnotationVisitor(
                annotationName, compilationUnit, javaFile, matches);
            
            compilationUnit.accept(visitor);
        }
        
        return matches;
    }
    
    /**
     * Searches for annotations in a file with LSP context for enhanced information.
     */
    private List<AnnotationMatch> searchAnnotationInFileWithLSPContext(Path javaFile, String annotationName, List<LSPSymbolInfo> lspResults) throws IOException {
        // For now, use the same logic as regular search but could be enhanced with LSP symbol context
        List<AnnotationMatch> matches = searchAnnotationInFile(javaFile, annotationName);
        
        // Enhance matches with LSP symbol information if available
        String fileUri = javaFile.toUri().toString();
        for (AnnotationMatch match : matches) {
            for (LSPSymbolInfo lspSymbol : lspResults) {
                if (fileUri.equals(lspSymbol.getFileUri())) {
                    logger.debug("Enhanced annotation match with LSP symbol: {}", lspSymbol.getName());
                    // Could add additional LSP context here
                }
            }
        }
        
        return matches;
    }
    
    /**
     * Gets detailed annotation information using IAnnotation interface.
     * 
     * @param annotation The annotation node from AST
     * @param binding The annotation binding (IAnnotation)
     * @return Detailed annotation information
     */
    private AnnotationDetails getAnnotationDetails(Annotation annotation, IAnnotationBinding binding) {
        String typeName = binding != null ? binding.getAnnotationType().getQualifiedName() : "Unknown";
        
        List<AnnotationMemberValue> memberValues = new ArrayList<>();
        
        if (binding != null) {
            for (IMemberValuePairBinding pair : binding.getAllMemberValuePairs()) {
                memberValues.add(new AnnotationMemberValue(
                    pair.getName(), 
                    pair.getValue() != null ? pair.getValue().toString() : "null"
                ));
            }
        }
        
        return new AnnotationDetails(typeName, memberValues, annotation.toString());
    }
    
    /**
     * AST Visitor that specifically looks for MySearchable annotations and uses IAnnotation.
     */
    private class MySearchableAnnotationVisitor extends ASTVisitor {
        private final String targetAnnotationName;
        private final CompilationUnit compilationUnit;
        private final Path sourceFile;
        private final List<AnnotationMatch> matches;
        
        public MySearchableAnnotationVisitor(String targetAnnotationName, 
                                           CompilationUnit compilationUnit, 
                                           Path sourceFile, 
                                           List<AnnotationMatch> matches) {
            this.targetAnnotationName = targetAnnotationName;
            this.compilationUnit = compilationUnit;
            this.sourceFile = sourceFile;
            this.matches = matches;
        }
        
        @Override
        public boolean visit(MarkerAnnotation node) {
            return processAnnotation(node);
        }
        
        @Override
        public boolean visit(NormalAnnotation node) {
            return processAnnotation(node);
        }
        
        @Override
        public boolean visit(SingleMemberAnnotation node) {
            return processAnnotation(node);
        }
        
        private boolean processAnnotation(Annotation annotation) {
            String annotationName = getSimpleAnnotationName(annotation);
            
            if (targetAnnotationName.equals(annotationName)) {
                logger.debug("Found @{} annotation in file: {}", annotationName, sourceFile);
                
                // Get IAnnotation binding for detailed information
                IAnnotationBinding binding = annotation.resolveAnnotationBinding();
                AnnotationDetails details = getAnnotationDetails(annotation, binding);
                
                // Convert to LSP location
                Location location = createLSPLocation(annotation);
                
                // Find the annotated element (method, class, field, etc.)
                ASTNode parent = annotation.getParent();
                String annotatedElementInfo = getAnnotatedElementInfo(parent);
                
                AnnotationMatch match = new AnnotationMatch(
                    location,
                    details,
                    annotatedElementInfo,
                    sourceFile.toString()
                );
                
                matches.add(match);
                
                logger.info("Found @{} annotation on {} in {}", 
                           annotationName, annotatedElementInfo, sourceFile.getFileName());
            }
            
            return true;
        }
        
        private String getSimpleAnnotationName(Annotation annotation) {
            Name typeName = annotation.getTypeName();
            if (typeName instanceof SimpleName) {
                return ((SimpleName) typeName).getIdentifier();
            } else if (typeName instanceof QualifiedName) {
                return ((QualifiedName) typeName).getName().getIdentifier();
            }
            return typeName.toString();
        }
        
        private Location createLSPLocation(Annotation annotation) {
            int startPosition = annotation.getStartPosition();
            int lineNumber = compilationUnit.getLineNumber(startPosition) - 1; // LSP is 0-based
            int columnNumber = compilationUnit.getColumnNumber(startPosition);
            
            Position position = new Position(lineNumber, columnNumber);
            Range range = new Range(position, position);
            
            return new Location(sourceFile.toUri().toString(), range);
        }
        
        private String getAnnotatedElementInfo(ASTNode node) {
            if (node instanceof MethodDeclaration) {
                MethodDeclaration method = (MethodDeclaration) node;
                return "method: " + method.getName().getIdentifier();
            } else if (node instanceof TypeDeclaration) {
                TypeDeclaration type = (TypeDeclaration) node;
                return "class: " + type.getName().getIdentifier();
            } else if (node instanceof FieldDeclaration) {
                FieldDeclaration field = (FieldDeclaration) node;
                return "field: " + field.fragments().get(0).toString();
            } else if (node instanceof VariableDeclarationFragment) {
                return "variable: " + ((VariableDeclarationFragment) node).getName().getIdentifier();
            }
            return "unknown element: " + node.getClass().getSimpleName();
        }
    }
    
    /**
     * Result of annotation search operation.
     */
    public static class AnnotationSearchResult {
        private final List<AnnotationMatch> matches;
        
        public AnnotationSearchResult(List<AnnotationMatch> matches) {
            this.matches = matches;
        }
        
        public List<AnnotationMatch> getMatches() {
            return matches;
        }
        
        public int getMatchCount() {
            return matches.size();
        }
        
        public List<Location> getLocations() {
            return matches.stream().map(AnnotationMatch::getLocation).toList();
        }
    }
    
    /**
     * Represents a single annotation match.
     */
    public static class AnnotationMatch {
        private final Location location;
        private final AnnotationDetails details;
        private final String annotatedElement;
        private final String sourceFile;
        
        public AnnotationMatch(Location location, AnnotationDetails details, 
                             String annotatedElement, String sourceFile) {
            this.location = location;
            this.details = details;
            this.annotatedElement = annotatedElement;
            this.sourceFile = sourceFile;
        }
        
        public Location getLocation() { return location; }
        public AnnotationDetails getDetails() { return details; }
        public String getAnnotatedElement() { return annotatedElement; }
        public String getSourceFile() { return sourceFile; }
    }
    
    /**
     * Detailed information about an annotation extracted using IAnnotation.
     */
    public static class AnnotationDetails {
        private final String fullyQualifiedName;
        private final List<AnnotationMemberValue> memberValues;
        private final String sourceText;
        
        public AnnotationDetails(String fullyQualifiedName, 
                               List<AnnotationMemberValue> memberValues, 
                               String sourceText) {
            this.fullyQualifiedName = fullyQualifiedName;
            this.memberValues = memberValues;
            this.sourceText = sourceText;
        }
        
        public String getFullyQualifiedName() { return fullyQualifiedName; }
        public List<AnnotationMemberValue> getMemberValues() { return memberValues; }
        public String getSourceText() { return sourceText; }
    }
    
    /**
     * Represents a member-value pair from an annotation.
     */
    public static class AnnotationMemberValue {
        private final String name;
        private final String value;
        
        public AnnotationMemberValue(String name, String value) {
            this.name = name;
            this.value = value;
        }
        
        public String getName() { return name; }
        public String getValue() { return value; }
    }
    
    /**
     * Represents LSP symbol information used for enhanced search context.
     */
    public static class LSPSymbolInfo {
        private final String name;
        private final String fileUri;
        private final SymbolKind kind;
        private final Location location;
        
        public LSPSymbolInfo(String name, String fileUri, SymbolKind kind, Location location) {
            this.name = name;
            this.fileUri = fileUri;
            this.kind = kind;
            this.location = location;
        }
        
        public String getName() { return name; }
        public String getFileUri() { return fileUri; }
        public SymbolKind getKind() { return kind; }
        public Location getLocation() { return location; }
    }
}