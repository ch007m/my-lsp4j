package dev.snowdrop.lsp.proxy;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dev.snowdrop.lsp.common.utils.FileUtils;
import dev.snowdrop.lsp.common.utils.LSPConnection;
import dev.snowdrop.lsp.common.utils.LanguageServer;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ServerAndClientLauncher {
    private static final Logger logger = LoggerFactory.getLogger(ServerAndClientLauncher.class);

    public static void main(String[] args) throws Exception {

        Path exampleDir = FileUtils.getExampleDir();
        logger.info("Created project directory: " + exampleDir);

        // Setup LSP communication using utility class
        LSPConnection lspConnection = LanguageServer.launchServerAndClient();
        
        // Initialize the language server with Project Path, ...
        logger.info("CLIENT: Initializing language server...");
        LanguageServer.initializeLanguageServer(lspConnection.getServerProxy(), exampleDir);
        logger.info("CLIENT: Language server initialized successfully.");

        // Send custom command
        String annotationToFind = "MySearchableAnnotation";
        logger.info("CLIENT: Sending custom command 'java/findAnnotatedClasses' to find '@{}'...", annotationToFind);

        ExecuteCommandParams commandParams = new ExecuteCommandParams(
            "java/findAnnotatedClasses",
            Collections.singletonList(annotationToFind)
        );

        CompletableFuture<Object> commandResult = lspConnection.getServerProxy().getWorkspaceService().executeCommand(commandParams);
        Object result = commandResult.get();

        if (result != null) {
            Gson gson = new Gson();
            Type locationListType = new TypeToken<List<Location>>() {}.getType();
            List<Location> locations = gson.fromJson(gson.toJson(result), locationListType);

            logger.info("CLIENT: --- Search Results ---");
            if (locations.isEmpty()) {
                logger.info("CLIENT: No classes found with the annotation '@{}'.", annotationToFind);
            } else {
                logger.info("CLIENT: Found {} usage(s) of '@{}':", locations.size(), annotationToFind);
                for (Location loc : locations) {
                    logger.info("CLIENT:  -> Found at: {} (line {}, char {})",
                        loc.getUri(),
                        loc.getRange().getStart().getLine() + 1,
                        loc.getRange().getStart().getCharacter() + 1
                    );
                }
            }
            logger.info("CLIENT: ----------------------");
        } else {
            logger.warn("CLIENT: Received null result for command.");
        }

        // Shutdown using utility class
        logger.info("CLIENT: Shutting down the language server...");
        lspConnection.shutdown();
        logger.info("CLIENT: Done.");
    }
}