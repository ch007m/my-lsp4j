package dev.snowdrop.lsp.socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static dev.snowdrop.lsp.common.utils.FileUtils.getTempDir;

public class JdtlsServer {
    private static final Logger logger = LoggerFactory.getLogger(JdtlsServer.class);
    private static final String JDT_LS_PATH = "/Users/cmoullia/code/application-modernisation/lsp/jdt-ls";
    
    private Process jdtlsProcess;

    public void startServerWithSocket(int port) throws IOException {
        logger.info("Starting JDT Language Server with socket on port {}...", port);

        // Start the JDT LS process
        jdtlsProcess = startJdtlsProcess();
        
        // Create socket server to bridge communication
        ServerSocket serverSocket = new ServerSocket(port);
        logger.info("Socket server listening on port {}", port);
        
        ExecutorService executor = Executors.newCachedThreadPool();
        
        // Accept connections and bridge them to JDT LS
        executor.submit(() -> {
            try {
                while (!serverSocket.isClosed()) {
                    Socket clientSocket = serverSocket.accept();
                    logger.info("Client connected: {}", clientSocket.getRemoteSocketAddress());
                    
                    // Bridge client socket to JDT LS process
                    bridgeStreams(clientSocket, jdtlsProcess);
                }
            } catch (IOException e) {
                if (!serverSocket.isClosed()) {
                    logger.error("Error accepting connections", e);
                }
            }
        });
    }
    
    private Process startJdtlsProcess() throws IOException {

        Path tempDir = getTempDir(); // getExampleDir();
        logger.info("Created temporary project directory: " + tempDir);
        //ProjectGenerator.generateCompleteProject(tempDir,"lsp-proxy","dev.swowdrop","lsp-proxy");

        String os = System.getProperty("os.name").toLowerCase();

        Path configPath = os.contains("win") ? Paths.get(JDT_LS_PATH, "config_win") :
            os.contains("mac") ? Paths.get(JDT_LS_PATH, "config_mac") :
                Paths.get(JDT_LS_PATH, "config_linux");

        String launcherJar = Objects.requireNonNull(
            new File(JDT_LS_PATH, "plugins").listFiles((dir, name) -> name.startsWith("org.eclipse.equinox.launcher_")))[0].getName();

        ProcessBuilder pb = new ProcessBuilder(
            "java",
            "-Declipse.application=org.eclipse.jdt.ls.core.id1",
            "-Dosgi.bundles.defaultStartLevel=4",
            "-Declipse.product=org.eclipse.jdt.ls.core.product",
            "-Dlog.level=ALL",
            "-Djdt.ls.debug=true",
            "-noverify",
            "-Xmx1G",
            "--add-modules=ALL-SYSTEM",
            "--add-opens", "java.base/java.util=ALL-UNNAMED",
            "--add-opens", "java.base/java.lang=ALL-UNNAMED",
            "-jar", Paths.get(JDT_LS_PATH, "plugins", launcherJar).toString(),
            "-configuration", configPath.toString(),
            "-data", tempDir.resolve(".jdt_workspace").toString()
        );
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);
        
        Process process = pb.start();
        logger.info("JDT Language Server process started");
        
        return process;
    }
    
    private void bridgeStreams(Socket clientSocket, Process jdtlsProcess) {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        
        // Bridge client input to JDT LS output
        executor.submit(() -> {
            try (InputStream clientIn = clientSocket.getInputStream();
                 OutputStream jdtlsOut = jdtlsProcess.getOutputStream()) {
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = clientIn.read(buffer)) != -1) {
                    jdtlsOut.write(buffer, 0, bytesRead);
                    jdtlsOut.flush();
                }
            } catch (IOException e) {
                logger.debug("Client to JDT LS stream closed", e);
            }
        });
        
        // Bridge JDT LS input to client output
        executor.submit(() -> {
            try (InputStream jdtlsIn = jdtlsProcess.getInputStream();
                 OutputStream clientOut = clientSocket.getOutputStream()) {
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = jdtlsIn.read(buffer)) != -1) {
                    clientOut.write(buffer, 0, bytesRead);
                    clientOut.flush();
                }
            } catch (IOException e) {
                logger.debug("JDT LS to client stream closed", e);
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    logger.debug("Error closing client socket", e);
                }
            }
        });
    }

    public void stopServer() {
        if (jdtlsProcess != null) {
            logger.info("Stopping JDT Language Server...");
            jdtlsProcess.destroy();
            logger.info("JDT Language Server stopped.");
        }
    }

    public static void main(String[] args) {
        try {
            JdtlsServer server = new JdtlsServer();
            server.startServerWithSocket(3333);
            
            // Keep the main thread alive
            while (server.jdtlsProcess != null && server.jdtlsProcess.isAlive()) {
                Thread.sleep(1000);
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
