package dev.snowdrop.lsp.utils;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileUtils {
    private static PipedInputStream inClient;
    private static PipedOutputStream outServer;
    private static PipedInputStream inServer;
    private static PipedOutputStream outClient;
    private static Path tempDir;

    public static Path getTempDir() throws IOException {
        if ( tempDir == null) {
            tempDir = Files.createTempDirectory("lsp-java-project-");
        }
        return tempDir;
    }

    public static PipedInputStream inClient() {
        inClient = new PipedInputStream();
        return inClient;
    }

    public static PipedInputStream inServer() {
        inServer = new PipedInputStream();
        return inServer;
    }

    public static PipedOutputStream outServer() throws IOException {
        if (inClient == null) {
            inClient();
        }
        outServer = new PipedOutputStream(inClient);
        return outServer;
    }

    public static PipedOutputStream outClient() throws IOException {
        if (inServer == null) {
            inServer();
        }
        outClient = new PipedOutputStream(inServer);
        return outClient;
    }
}
