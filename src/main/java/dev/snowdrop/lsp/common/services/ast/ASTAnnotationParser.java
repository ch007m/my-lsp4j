package dev.snowdrop.lsp.common.services.ast;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.lsp4j.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * AST Parser utility with singleton pattern for efficient parser reuse.
 */
public class ASTAnnotationParser {
    private static final Logger logger = LoggerFactory.getLogger(ASTAnnotationParser.class);
    
    // Singleton AST parser instance for efficient reuse
    private static final ASTParser PARSER_INSTANCE = createParserInstance();
    
    /**
     * Create a singleton ASTParser instance configured for Java parsing.
     */
    private static ASTParser createParserInstance() {
        ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setResolveBindings(false); // We don't need full binding resolution for annotation names
        return parser;
    }
    
    /**
     * Get the singleton ASTParser instance.
     */
    public static ASTParser getParserInstance() {
        return PARSER_INSTANCE;
    }

    /**
     * Parse a Java file using Eclipse JDT AST to find annotations.
     * This provides more accurate parsing than simple string matching.
     * 
     * @param javaFile The Java file to parse
     * @param annotationName The annotation name to search for (without @)
     * @return List of locations where the annotation was found
     * @throws IOException If the file cannot be read
     */
    public static List<Location> parseJavaFileForAnnotations(Path javaFile, String annotationName) throws IOException {
        String source = Files.readString(javaFile);
        
        // Use singleton parser instance
        synchronized (PARSER_INSTANCE) {
            PARSER_INSTANCE.setSource(source.toCharArray());
            
            // Parse the source code
            CompilationUnit compilationUnit = (CompilationUnit) PARSER_INSTANCE.createAST(null);
            
            // Create and run the annotation visitor
            AnnotationVisitor visitor = new AnnotationVisitor(annotationName, compilationUnit, javaFile.toUri());
            compilationUnit.accept(visitor);
            
            return visitor.getLocations();
        }
    }

    /**
     * Check if a Java file contains any annotations without parsing the full AST.
     * This is a quick pre-check to avoid unnecessary AST parsing.
     * 
     * @param javaFile The Java file to check
     * @param annotationName The annotation name to search for
     * @return true if the file might contain the annotation (requires AST parsing to confirm)
     * @throws IOException If the file cannot be read
     */
    public static boolean quickAnnotationCheck(Path javaFile, String annotationName) throws IOException {
        String source = Files.readString(javaFile);
        return source.contains("@" + annotationName);
    }

    /**
     * Parse a Java file and return detailed information about all annotations found.
     * 
     * @param javaFile The Java file to parse
     * @return AnnotationSearchResult containing all annotation information
     * @throws IOException If the file cannot be read
     */
    public static AnnotationSearchResult analyzeAllAnnotations(Path javaFile) throws IOException {
        String source = Files.readString(javaFile);
        
        // Use singleton parser instance
        synchronized (PARSER_INSTANCE) {
            PARSER_INSTANCE.setSource(source.toCharArray());
            
            CompilationUnit compilationUnit = (CompilationUnit) PARSER_INSTANCE.createAST(null);
            
            AllAnnotationsVisitor visitor = new AllAnnotationsVisitor(compilationUnit, javaFile.toUri());
            compilationUnit.accept(visitor);
            
            return new AnnotationSearchResult(javaFile, visitor.getAnnotationInfo());
        }
    }

    /**
     * Visitor that collects information about all annotations in a compilation unit.
     */
    private static class AllAnnotationsVisitor extends ASTVisitor {
        private final CompilationUnit compilationUnit;
        private final java.net.URI fileUri;
        private final java.util.List<AnnotationInfo> annotationInfo = new java.util.ArrayList<>();

        public AllAnnotationsVisitor(CompilationUnit compilationUnit, java.net.URI fileUri) {
            this.compilationUnit = compilationUnit;
            this.fileUri = fileUri;
        }

        @Override
        public boolean visit(MarkerAnnotation node) {
            return visitAnnotation(node.getTypeName(), node);
        }

        @Override
        public boolean visit(NormalAnnotation node) {
            return visitAnnotation(node.getTypeName(), node);
        }

        @Override
        public boolean visit(SingleMemberAnnotation node) {
            return visitAnnotation(node.getTypeName(), node);
        }

        private boolean visitAnnotation(Name typeName, Annotation annotation) {
            String annotationName = getAnnotationName(typeName);
            int startPosition = annotation.getStartPosition();
            int lineNumber = compilationUnit.getLineNumber(startPosition) - 1;
            int columnNumber = compilationUnit.getColumnNumber(startPosition);
            
            AnnotationInfo info = new AnnotationInfo(
                annotationName,
                fileUri.toString(),
                lineNumber,
                columnNumber,
                annotation.toString()
            );
            
            annotationInfo.add(info);
            return true;
        }

        private String getAnnotationName(Name typeName) {
            if (typeName instanceof SimpleName) {
                return ((SimpleName) typeName).getIdentifier();
            } else if (typeName instanceof QualifiedName) {
                return ((QualifiedName) typeName).getName().getIdentifier();
            }
            return typeName.toString();
        }

        public List<AnnotationInfo> getAnnotationInfo() {
            return annotationInfo;
        }
    }

    /**
     * Information about a single annotation found in the code.
     */
    public static class AnnotationInfo {
        private final String name;
        private final String fileUri;
        private final int line;
        private final int column;
        private final String fullText;

        public AnnotationInfo(String name, String fileUri, int line, int column, String fullText) {
            this.name = name;
            this.fileUri = fileUri;
            this.line = line;
            this.column = column;
            this.fullText = fullText;
        }

        public String getName() { return name; }
        public String getFileUri() { return fileUri; }
        public int getLine() { return line; }
        public int getColumn() { return column; }
        public String getFullText() { return fullText; }
    }

    /**
     * Result of analyzing all annotations in a file.
     */
    public static class AnnotationSearchResult {
        private final Path file;
        private final List<AnnotationInfo> annotations;

        public AnnotationSearchResult(Path file, List<AnnotationInfo> annotations) {
            this.file = file;
            this.annotations = annotations;
        }

        public Path getFile() { return file; }
        public List<AnnotationInfo> getAnnotations() { return annotations; }
        public int getAnnotationCount() { return annotations.size(); }
        
        public List<AnnotationInfo> getAnnotationsWithName(String name) {
            return annotations.stream()
                .filter(info -> name.equals(info.getName()))
                .toList();
        }
    }
}