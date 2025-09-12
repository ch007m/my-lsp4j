package dev.snowdrop.lsp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple test runner demonstrating how to execute the LSP integration tests.
 * This class provides information about running the tests.
 */
public class TestRunner {
    private static final Logger logger = LoggerFactory.getLogger(TestRunner.class);

    public static void main(String[] args) {
        logger.info("LSP Integration Test Runner");
        logger.info("==========================");
        logger.info("");
        logger.info("To run all tests:");
        logger.info("  mvn test");
        logger.info("");
        logger.info("To run specific test:");
        logger.info("  mvn test -Dtest=ProxyLSPIntegrationTest#testAnnotationSearchFindsCorrectAnnotations");
        logger.info("");
        logger.info("Available test methods in ProxyLSPIntegrationTest:");
        logger.info("  - testAnnotationSearchFindsCorrectAnnotations: Tests AST-based annotation search");
        logger.info("  - testAnnotationSearchWithNonExistentAnnotation: Tests handling of missing annotations");
        logger.info("  - testServerCapabilities: Tests LSP server capabilities");
        logger.info("  - testServerShutdown: Tests graceful server shutdown");
        logger.info("");
        logger.info("These tests validate:");
        logger.info("  ✓ Complete LSP client-server communication");
        logger.info("  ✓ AST-based Java annotation parsing using Eclipse JDT");
        logger.info("  ✓ Accurate location reporting (line/column numbers)");
        logger.info("  ✓ Proper handling of piped stream communication");
        logger.info("  ✓ Server lifecycle management (init, execute, shutdown)");
        logger.info("");
        logger.info("Note: Tests automatically create temporary workspaces and clean up after execution.");
    }
}