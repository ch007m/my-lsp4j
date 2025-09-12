package dev.snowdrop.lsp.common.ast;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * AST visitor that searches for specific annotations in Java source code.
 * Uses Eclipse JDT's AST parsing for accurate syntax analysis.
 */
public class AnnotationVisitor extends ASTVisitor {
    private static final Logger logger = LoggerFactory.getLogger(AnnotationVisitor.class);
    
    private final String targetAnnotationName;
    private final List<Location> locations;
    private final CompilationUnit compilationUnit;
    private final URI fileUri;

    public AnnotationVisitor(String targetAnnotationName, CompilationUnit compilationUnit, URI fileUri) {
        this.targetAnnotationName = targetAnnotationName;
        this.locations = new ArrayList<>();
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
        
        if (targetAnnotationName.equals(annotationName)) {
            logger.debug("Found annotation @{} at position {}", annotationName, annotation.getStartPosition());
            
            // Convert AST position to LSP position
            int startPosition = annotation.getStartPosition();
            Position lspPosition = convertToLSPPosition(startPosition);
            Range range = new Range(lspPosition, lspPosition);
            
            Location location = new Location(fileUri.toString(), range);
            locations.add(location);
            
            logger.info("SERVER: Found annotation @{} in file: {} at line {}, column {}", 
                       annotationName, fileUri, lspPosition.getLine() + 1, lspPosition.getCharacter() + 1);
        }
        
        return true; // Continue visiting
    }

    /**
     * Extracts the simple name of an annotation from its type name.
     */
    private String getAnnotationName(Name typeName) {
        if (typeName instanceof SimpleName) {
            return ((SimpleName) typeName).getIdentifier();
        } else if (typeName instanceof QualifiedName) {
            return ((QualifiedName) typeName).getName().getIdentifier();
        }
        return typeName.toString();
    }

    /**
     * Converts an AST character position to LSP Position (line, character).
     */
    private Position convertToLSPPosition(int astPosition) {
        int lineNumber = compilationUnit.getLineNumber(astPosition) - 1; // LSP is 0-based
        int columnNumber = compilationUnit.getColumnNumber(astPosition); // Already 0-based
        return new Position(lineNumber, columnNumber);
    }

    /**
     * Returns the list of locations where the target annotation was found.
     */
    public List<Location> getLocations() {
        return locations;
    }
}