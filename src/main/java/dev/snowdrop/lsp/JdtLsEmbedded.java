package dev.snowdrop.lsp;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dev.snowdrop.lsp.model.LSPSymbolInfo;
import dev.snowdrop.lsp.common.utils.FileUtils;
import dev.snowdrop.lsp.common.utils.LSUtils;
import dev.snowdrop.lsp.common.utils.SnowdropLS;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.LanguageServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static dev.snowdrop.lsp.common.services.AnnotationSearchService.executeCmd;
import static dev.snowdrop.lsp.common.utils.LSUtils.initializeLanguageServer;

public class JdtLsEmbedded {
    private static final Logger logger = LoggerFactory.getLogger(JdtLsEmbedded.class);

    public static void main(String[] args) throws Exception {

        Path exampleDir = FileUtils.getExampleDir();
        logger.info("Created project directory: " + exampleDir);

        // Setup LSP
        SnowdropLS snowdropLS = LSUtils.launchServer();

        // Initialize the language server with Project Path ...
        logger.info("CLIENT: Initializing language server...");
        initializeLanguageServer(snowdropLS.getServer(), exampleDir);
        logger.info("CLIENT: Language server initialized successfully.");

        // Send custom command
        String annotationToFind = "MySearchableAnnotation";
        logger.info("CLIENT: Sending custom command 'java/findAnnotatedClasses' to find '@{}'...", annotationToFind);

        // TEST using ExecuteCommand - OK
        //executeCmd(annotationToFind, snowdropLS);

        // TEST using symbol - NOK
        // useSymbol(annotationToFind, snowdropLS);

        logger.info("CLIENT: Shutting down the language server...");
        snowdropLS.getServer().shutdown();
        snowdropLS.getServer().exit();
        logger.info("CLIENT: Done.");
    }
}