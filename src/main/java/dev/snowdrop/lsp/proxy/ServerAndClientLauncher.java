package dev.snowdrop.lsp.proxy;

import dev.snowdrop.lsp.common.utils.FileUtils;
import dev.snowdrop.lsp.common.utils.MyLanguageServer;
import dev.snowdrop.lsp.common.utils.SnowdropLS;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializedParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

public class ServerAndClientLauncher {
    private static final Logger logger = LoggerFactory.getLogger(ServerAndClientLauncher.class);

    public static void main(String[] args) throws Exception {

        Path exampleDir = FileUtils.getExampleDir();
        logger.info("Created project directory: " + exampleDir);

        // Set up the launchers, server and client
        SnowdropLS snowdropLS = MyLanguageServer.launchServerAndClient(false);

        // Initialize the language server with Project Path, ...
        logger.info("CLIENT: Initializing language server...");
        MyLanguageServer.initializeLanguageServer(snowdropLS.getServer(), exampleDir);
        logger.info("CLIENT: Language server initialized successfully.");

        Thread.sleep(1000);

        // Send custom command
        String annotationToFind = "MySearchableAnnotation";
        logger.info("CLIENT: Sending custom command 'java/findAnnotatedClasses' to find '@{}'...", annotationToFind);

        ExecuteCommandParams commandParams = new ExecuteCommandParams(
            "java/findAnnotatedClasses",
            Collections.singletonList(annotationToFind)
        );

        CompletableFuture<Object> result = snowdropLS.getServer().getWorkspaceService().executeCommand(commandParams);
        logger.info("CLIENT: Found: " + result.get());

        logger.info("CLIENT: --------------------------------");

/*        if (result != null) {
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
        }*/

        // Shutdown using utility class
        logger.info("CLIENT: Shutting down the language server...");
        // TODO: Add shutdown
        logger.info("CLIENT: Done.");
    }
}