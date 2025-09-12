package dev.snowdrop.lsp;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dev.snowdrop.lsp.utils.FileUtils;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CombinedLauncher {
    private static final Logger logger = LoggerFactory.getLogger(CombinedLauncher.class);

    public static void main(String[] args) throws Exception {

        Path tempDir = FileUtils.getTempDir();
        logger.info("Created temporary project directory: " + tempDir);

        Path annotationFile = tempDir.resolve("MySearchableAnnotation.java");
        Files.writeString(annotationFile, "public @interface MySearchableAnnotation {}");

        Path classFile = tempDir.resolve("AnnotatedClass.java");
        Files.writeString(classFile, "public class AnnotatedClass {\n    @MySearchableAnnotation\n    private int myField;\n}");

        // Create piped streams for communication
        PipedInputStream serverIn = new PipedInputStream();
        PipedOutputStream clientOut = new PipedOutputStream(serverIn);
        
        PipedInputStream clientIn = new PipedInputStream();
        PipedOutputStream serverOut = new PipedOutputStream(clientIn);

        ExecutorService executor = Executors.newFixedThreadPool(4);

        // Create and start server
        JavaLanguageServer server = new JavaLanguageServer();
        Launcher<LanguageServer> serverLauncher = new LSPLauncher.Builder<LanguageServer>()
            .setLocalService(server)
            .setRemoteInterface(LanguageServer.class)
            .setInput(serverIn)
            .setOutput(serverOut)
            .setExecutorService(executor)
            .create();

        // Start server listening in background
        serverLauncher.startListening();

        // Create client
        LspClient client = new LspClient();
        Launcher<LanguageServer> clientLauncher = new LSPLauncher.Builder<LanguageServer>()
            .setLocalService(client)
            .setRemoteInterface(LanguageServer.class)
            .setInput(clientIn)
            .setOutput(clientOut)
            .setExecutorService(executor)
            .create();

        LanguageServer serverProxy = clientLauncher.getRemoteProxy();
        clientLauncher.startListening();

        // Give a moment for connections to establish
        Thread.sleep(100);

        // Initialize the server
        logger.info("CLIENT: Sending 'initialize' request...");
        InitializeParams initParams = new InitializeParams();
        initParams.setProcessId((int) ProcessHandle.current().pid());
        initParams.setRootUri(tempDir.toUri().toString());

        CompletableFuture<InitializeResult> initResult = serverProxy.initialize(initParams);
        initResult.get();
        logger.info("CLIENT: 'initialize' request completed.");

        serverProxy.initialized(new InitializedParams());
        logger.info("CLIENT: Sent 'initialized' notification.");

        // Send custom command
        String annotationToFind = "MySearchableAnnotation";
        logger.info("CLIENT: Sending custom command 'java/findAnnotatedClasses' to find '@{}'...", annotationToFind);

        ExecuteCommandParams commandParams = new ExecuteCommandParams(
            "java/findAnnotatedClasses",
            Collections.singletonList(annotationToFind)
        );

        CompletableFuture<Object> commandResult = serverProxy.getWorkspaceService().executeCommand(commandParams);
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

        // Shutdown
        logger.info("CLIENT: Shutting down the language server...");
        serverProxy.shutdown().get();
        serverProxy.exit();
        
        executor.shutdown();
        logger.info("CLIENT: Done.");
    }
}