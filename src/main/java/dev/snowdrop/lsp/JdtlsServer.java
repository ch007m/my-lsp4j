package dev.snowdrop.lsp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static dev.snowdrop.lsp.common.utils.FileUtils.getTempDir;

public class JdtlsServer {
    private static final Logger logger = LoggerFactory.getLogger(JdtlsServer.class);
    private static String JDT_LS_PATH;
    
    private Process jdtlsProcess;

    public void startServerWithSocket() throws IOException {
        JDT_LS_PATH = Optional
            .ofNullable(System.getenv("JDT_LS_PATH"))
            .orElseThrow(() -> new RuntimeException("JDT_LS_PATH en var is missing !"));

        // Start the JDT LS process
        jdtlsProcess = startJdtlsProcess();
    }
    
    private Process startJdtlsProcess() throws IOException {

        Path wksDir = getTempDir();
        logger.info("Created workspace project directory: " + wksDir);

        System.setProperty("CLIENT_PORT","3333");

        String os = System.getProperty("os.name").toLowerCase();

        Path configPath = os.contains("win") ? Paths.get(JDT_LS_PATH, "config_win") :
            os.contains("mac") ? Paths.get(JDT_LS_PATH, "config_mac_arm") :
                Paths.get(JDT_LS_PATH, "config_linux");

        String launcherJar = Objects
            .requireNonNull(
            new File(JDT_LS_PATH, "plugins")
                .listFiles((dir, name) -> name.startsWith("org.eclipse.equinox.launcher_")))[0].getName();

        ProcessBuilder pb = new ProcessBuilder(
            "java",
            // "-Declipse.application=org.eclipse.jdt.ls.core.id1",
            "-Dosgi.bundles.defaultStartLevel=4",
            "-Dosgi.checkConfiguration=true",
            "-Dosgi.sharedConfiguration.area.readOnly=true",
            "-Dosgi.configuration.cascaded=true",
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
            "-data", wksDir.resolve(".jdt_workspace").toString()
        );
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);
        
        Process process = pb.start();
        logger.info("JDT Language Server process started");
        
        return process;
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
            server.startServerWithSocket();
            
            // Keep the main thread alive
            while (server.jdtlsProcess != null && server.jdtlsProcess.isAlive()) {
                Thread.sleep(10000);
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
