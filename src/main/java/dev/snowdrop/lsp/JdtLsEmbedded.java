package dev.snowdrop.lsp;

import dev.snowdrop.lsp.common.utils.FileUtils;
import dev.snowdrop.lsp.common.utils.LSUtils;
import dev.snowdrop.lsp.common.utils.SnowdropLS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

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