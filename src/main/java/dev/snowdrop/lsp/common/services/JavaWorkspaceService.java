package dev.snowdrop.lsp.common.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Proxy-specific workspace service that extends the shared base class.
 * Uses AST-based annotation search provided by the base class.
 */
public class JavaWorkspaceService extends BaseWorkspaceService {
    private static final Logger logger = LoggerFactory.getLogger(JavaWorkspaceService.class);

    // The base class already provides all the functionality we need.
    // This class can be extended to add proxy-specific behavior if needed.
    
    // Example: Add proxy-specific logging or behavior
    @Override
    public void setWorkspaceRoot(String workspaceRoot) {
        super.setWorkspaceRoot(workspaceRoot);
        logger.info("PROXY: Workspace root set to: {}", workspaceRoot);
    }
}
