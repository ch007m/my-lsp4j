package dev.snowdrop.lsp.socket;

import dev.snowdrop.lsp.common.services.BaseWorkspaceService;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Socket-specific workspace service that extends the shared base class.
 * Uses AST-based annotation search provided by the base class and can
 * add socket-specific functionality as needed.
 */
public class SocketWorkspaceService extends BaseWorkspaceService {
    private static final Logger logger = LoggerFactory.getLogger(SocketWorkspaceService.class);

    @Override
    public void setWorkspaceRoot(String workspaceRoot) {
        super.setWorkspaceRoot(workspaceRoot);
        logger.info("SOCKET: Workspace root set to: {}", workspaceRoot);
    }

    /**
     * Handle additional socket-specific commands if needed.
     */
    @Override
    protected CompletableFuture<Object> handleCustomCommand(ExecuteCommandParams params) {
        // Example: Add socket-specific commands
        if ("socket/customCommand".equals(params.getCommand())) {
            logger.info("SOCKET: Handling socket-specific custom command");
            // Handle socket-specific logic here
            return CompletableFuture.completedFuture("Socket command handled");
        }
        
        // Delegate to parent for standard behavior
        return super.handleCustomCommand(params);
    }
}