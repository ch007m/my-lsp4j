package dev.snowdrop.lsp.common.utils;

import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;

/**
 * The Language Server Protocol defines communication in both directions.
 * This interface combines both client and server interfaces to represent
 * the full functionality of the remote endpoint.
 */
public interface ClientProxy extends LanguageClient, LanguageServer {
}