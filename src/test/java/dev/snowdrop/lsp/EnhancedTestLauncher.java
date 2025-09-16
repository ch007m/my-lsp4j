package dev.snowdrop.lsp;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dev.snowdrop.lsp.common.utils.FileUtils;
import dev.snowdrop.lsp.common.utils.SnowdropLS;
import dev.snowdrop.lsp.common.utils.LanguageServer;
import org.eclipse.lsp4j.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Enhanced test launcher that demonstrates AST-based annotation search
 * with more complex Java code examples.
 */
public class EnhancedTestLauncher {
    private static final Logger logger = LoggerFactory.getLogger(EnhancedTestLauncher.class);

    public static void main(String[] args) throws Exception {
        // Create temporary project with more complex examples
        Path tempDir = FileUtils.getExampleDir();
        logger.info("Created temporary project directory: " + tempDir);

        // Create annotation
        Path annotationFile = tempDir.resolve("MySearchableAnnotation.java");
        Files.writeString(annotationFile, """
            public @interface MySearchableAnnotation {
                String value() default "";
                int priority() default 0;
            }
            """);

        // Create a more complex annotated class
        Path classFile = tempDir.resolve("ComplexAnnotatedClass.java");
        Files.writeString(classFile, """
            public class ComplexAnnotatedClass {
                @MySearchableAnnotation(value = "field", priority = 1)
                private int annotatedField;
                
                @MySearchableAnnotation("method")
                public void annotatedMethod() {
                    // This annotation in comments should not be found: @MySearchableAnnotation
                }
                
                @MySearchableAnnotation(priority = 10)
                public static final String CONSTANT = "value";
                
                // This is not an annotation: @MySearchableAnnotation in comment
                public void normalMethod() {
                    String str = "@MySearchableAnnotation"; // Not an annotation
                }
            }
            """);

        // Create another file with different annotations
        Path otherFile = tempDir.resolve("OtherClass.java");
        Files.writeString(otherFile, """
            public class OtherClass {
                @Override
                public String toString() {
                    return "Other";
                }
                
                @Deprecated
                @MySearchableAnnotation
                public void deprecatedMethod() {
                }
            }
            """);

        // Setup LSP communication using utility class
        SnowdropLS snowdropLS = dev.snowdrop.lsp.common.utils.LanguageServer.launchServerAndClient();

        // Initialize the language server with Project Path, ...
        logger.info("CLIENT: Initializing language server...");
        LanguageServer.initializeLanguageServer(snowdropLS.getServer(), tempDir);
        logger.info("CLIENT: Language server initialized successfully.");

        Thread.sleep(100);

        // Search for annotations
        String annotationToFind = "MySearchableAnnotation";
        logger.info("CLIENT: Sending custom command 'java/findAnnotatedClasses' to find '@{}'...", annotationToFind);

        ExecuteCommandParams commandParams = new ExecuteCommandParams(
            "java/findAnnotatedClasses",
            Collections.singletonList(annotationToFind)
        );

        CompletableFuture<Object> commandResult = snowdropLS.getServer().getWorkspaceService().executeCommand(commandParams);
        Object result = commandResult.get();

        if (result != null) {
            Gson gson = new Gson();
            Type locationListType = new TypeToken<List<Location>>() {}.getType();
            List<Location> locations = gson.fromJson(gson.toJson(result), locationListType);

            logger.info("CLIENT: --- Enhanced AST-based Search Results ---");
            if (locations.isEmpty()) {
                logger.info("CLIENT: No classes found with the annotation '@{}'.", annotationToFind);
            } else {
                logger.info("CLIENT: Found {} usage(s) of '@{}':", locations.size(), annotationToFind);
                for (Location loc : locations) {
                    String fileName = loc.getUri().substring(loc.getUri().lastIndexOf('/') + 1);
                    logger.info("CLIENT:  -> Found at: {} (line {}, char {})",
                        fileName,
                        loc.getRange().getStart().getLine() + 1,
                        loc.getRange().getStart().getCharacter() + 1
                    );
                }
            }
            logger.info("CLIENT: ------------------------------------------------");
        } else {
            logger.warn("CLIENT: Received null result for command.");
        }

        // Shutdown
        logger.info("CLIENT: Shutting down the language server...");
        snowdropLS.getServer().shutdown().get();
        snowdropLS.getServer().exit();

        logger.info("CLIENT: Enhanced test completed successfully!");
    }
}