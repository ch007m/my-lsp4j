package dev.snowdrop.lsp.common.services;

import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.WorkspaceSymbol;
import org.eclipse.lsp4j.WorkspaceSymbolParams;
import org.eclipse.lsp4j.adapters.WorkspaceSymbolResponseAdapter;
import org.eclipse.lsp4j.jsonrpc.json.ResponseJsonAdapter;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Proxy-specific workspace service that extends the shared base class.
 */
public class JavaWorkspaceService extends BaseWorkspaceService {
    private static final Logger logger = LoggerFactory.getLogger(JavaWorkspaceService.class);
    
    // Example: Add proxy-specific logging or behavior
    @Override
    public void setWorkspaceRoot(String workspaceRoot) {
        super.setWorkspaceRoot(workspaceRoot);
        logger.info("PROXY: Workspace root set to: {}", workspaceRoot);
    }

    @Override
    @JsonRequest
    @ResponseJsonAdapter(WorkspaceSymbolResponseAdapter.class)
    public CompletableFuture<Either<List<? extends SymbolInformation>, List<? extends WorkspaceSymbol>>> symbol(WorkspaceSymbolParams params) {
        String query = params.getQuery();
        logger.info("SERVER: Received 'workspace/symbol' request with query: '{}'", query);
        return null;
    }
}
