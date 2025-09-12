package dev.snowdrop.lsp.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;

/**
 * Utility class to generate complete Maven Java projects with sample code
 * that can be used for testing LSP annotation search functionality.
 */
public class ProjectGenerator {
    private static final Logger logger = LoggerFactory.getLogger(ProjectGenerator.class);

    /**
     * Generate a complete Maven Java project with sample annotations and classes.
     * 
     * @param projectRoot The root directory where the project should be created
     * @param projectName The name of the Maven project
     * @param groupId The Maven group ID
     * @param artifactId The Maven artifact ID
     * @throws IOException If files cannot be created
     */
    public static void generateCompleteProject(Path projectRoot, String projectName, String groupId, String artifactId) throws IOException {
        logger.info("Generating complete Maven project at: {}", projectRoot);
        
        // Create directory structure
        createMavenDirectoryStructure(projectRoot);
        
        // Generate pom.xml
        generatePomXml(projectRoot, groupId, artifactId, "1.0.0");
        
        // Generate source files
        generateSourceFiles(projectRoot, groupId);
        
        // Generate test files
        generateTestFiles(projectRoot, groupId);
        
        // Generate additional resources
        generateResources(projectRoot);
        
        logger.info("Complete Maven project generated successfully");
    }

    /**
     * Generate a simple test project with just the essential annotation examples.
     * 
     * @param projectRoot The root directory where the project should be created
     * @throws IOException If files cannot be created
     */
    public static void generateSimpleTestProject(Path projectRoot) throws IOException {
        logger.info("Generating simple test project at: {}", projectRoot);
        
        // Create basic structure
        Path srcMain = projectRoot.resolve("src/main/java");
        Path srcTest = projectRoot.resolve("src/test/java");
        Files.createDirectories(srcMain);
        Files.createDirectories(srcTest);
        
        // Generate annotation
        generateAnnotationFile(srcMain.resolve("MySearchableAnnotation.java"));
        
        // Generate sample classes
        generateSampleClasses(srcMain);
        
        logger.info("Simple test project generated successfully");
    }

    private static void createMavenDirectoryStructure(Path projectRoot) throws IOException {
        // Standard Maven directory structure
        Files.createDirectories(projectRoot.resolve("src/main/java"));
        Files.createDirectories(projectRoot.resolve("src/main/resources"));
        Files.createDirectories(projectRoot.resolve("src/test/java"));
        Files.createDirectories(projectRoot.resolve("src/test/resources"));
        Files.createDirectories(projectRoot.resolve("target"));
    }

    private static void generatePomXml(Path projectRoot, String groupId, String artifactId, String version) throws IOException {
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
                                         http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                
                <groupId>%s</groupId>
                <artifactId>%s</artifactId>
                <version>%s</version>
                <packaging>jar</packaging>
                
                <properties>
                    <maven.compiler.source>17</maven.compiler.source>
                    <maven.compiler.target>17</maven.compiler.target>
                    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
                    <junit.version>5.10.0</junit.version>
                </properties>
                
                <dependencies>
                    <!-- JUnit 5 for testing -->
                    <dependency>
                        <groupId>org.junit.jupiter</groupId>
                        <artifactId>junit-jupiter</artifactId>
                        <version>${junit.version}</version>
                        <scope>test</scope>
                    </dependency>
                </dependencies>
                
                <build>
                    <plugins>
                        <plugin>
                            <groupId>org.apache.maven.plugins</groupId>
                            <artifactId>maven-compiler-plugin</artifactId>
                            <version>3.13.0</version>
                        </plugin>
                        <plugin>
                            <groupId>org.apache.maven.plugins</groupId>
                            <artifactId>maven-surefire-plugin</artifactId>
                            <version>3.2.2</version>
                        </plugin>
                    </plugins>
                </build>
            </project>
            """.formatted(groupId, artifactId, version);
        
        Files.writeString(projectRoot.resolve("pom.xml"), pomContent);
        logger.info("Generated pom.xml");
    }

    private static void generateSourceFiles(Path projectRoot, String groupId) throws IOException {
        Path srcMain = projectRoot.resolve("src/main/java");
        Path packageDir = createPackageDirectory(srcMain, groupId);
        
        // Generate custom annotations
        generateAnnotationFile(packageDir.resolve("MySearchableAnnotation.java"));
        generateAnnotationFile(packageDir.resolve("ImportantAnnotation.java"));
        
        // Generate sample classes
        generateSampleClasses(packageDir);
        
        // Generate service classes
        generateServiceClasses(packageDir);
        
        logger.info("Generated source files");
    }

    private static void generateTestFiles(Path projectRoot, String groupId) throws IOException {
        Path srcTest = projectRoot.resolve("src/test/java");
        Path packageDir = createPackageDirectory(srcTest, groupId);
        
        String testClassContent = """
            package %s;
            
            import org.junit.jupiter.api.Test;
            import static org.junit.jupiter.api.Assertions.*;
            
            public class AnnotationTest {
                
                @Test
                void testAnnotationUsage() {
                    // Test that annotations are properly applied
                    UserService service = new UserService();
                    assertNotNull(service);
                }
                
                @Test
                @MySearchableAnnotation("test method")
                void testWithCustomAnnotation() {
                    assertTrue(true);
                }
            }
            """.formatted(groupId);
        
        Files.writeString(packageDir.resolve("AnnotationTest.java"), testClassContent);
        logger.info("Generated test files");
    }

    private static void generateResources(Path projectRoot) throws IOException {
        // Generate application.properties
        String appProperties = """
            # Application configuration
            app.name=LSP Test Project
            app.version=1.0.0
            
            # Logging configuration
            logging.level=INFO
            """;
        
        Files.writeString(
            projectRoot.resolve("src/main/resources/application.properties"), 
            appProperties
        );
        
        // Generate README.md
        String readmeContent = """
            # LSP Test Project
            
            This is a generated Maven project for testing LSP annotation search functionality.
            
            ## Structure
            
            - `src/main/java` - Main source code with custom annotations
            - `src/test/java` - Test classes
            - Custom annotations: `@MySearchableAnnotation`, `@ImportantAnnotation`
            
            ## Build
            
            ```bash
            mvn clean compile
            mvn test
            ```
            
            ## Annotations Used
            
            This project contains various examples of annotation usage:
            - Method annotations
            - Field annotations  
            - Class annotations
            - Annotations with parameters
            - Simple marker annotations
            """;
        
        Files.writeString(projectRoot.resolve("README.md"), readmeContent);
        logger.info("Generated resources");
    }

    private static Path createPackageDirectory(Path srcDir, String groupId) throws IOException {
        Path packageDir = srcDir;
        for (String part : groupId.split("\\.")) {
            packageDir = packageDir.resolve(part);
        }
        Files.createDirectories(packageDir);
        return packageDir;
    }

    private static void generateAnnotationFile(Path annotationFile) throws IOException {
        String fileName = annotationFile.getFileName().toString().replace(".java", "");
        
        String annotationContent = """
            public @interface %s {
                String value() default "";
                int priority() default 0;
                String[] tags() default {};
            }
            """.formatted(fileName);
        
        Files.writeString(annotationFile, annotationContent);
    }

    private static void generateSampleClasses(Path packageDir) throws IOException {
        // Generate User class
        String userClassContent = """
            @MySearchableAnnotation("entity")
            public class User {
                
                @MySearchableAnnotation(value = "id field", priority = 1)
                private Long id;
                
                @MySearchableAnnotation("name field")
                private String name;
                
                @MySearchableAnnotation
                private String email;
                
                public User() {}
                
                @MySearchableAnnotation("constructor")
                public User(String name, String email) {
                    this.name = name;
                    this.email = email;
                }
                
                @MySearchableAnnotation(value = "getter", priority = 2)
                public String getName() {
                    return name;
                }
                
                public void setName(String name) {
                    this.name = name;
                }
                
                public String getEmail() {
                    return email;
                }
                
                public void setEmail(String email) {
                    this.email = email;
                }
            }
            """;
        
        Files.writeString(packageDir.resolve("User.java"), userClassContent);
        
        // Generate Product class
        String productClassContent = """
            public class Product {
                
                @MySearchableAnnotation(priority = 10)
                private String name;
                
                private double price;
                
                @ImportantAnnotation
                @MySearchableAnnotation("important method")
                public void calculateDiscount() {
                    // Business logic here
                }
                
                // This annotation in comment should not be found: @MySearchableAnnotation
                public void regularMethod() {
                    String str = "@MySearchableAnnotation"; // Not a real annotation
                }
            }
            """;
        
        Files.writeString(packageDir.resolve("Product.java"), productClassContent);
    }

    private static void generateServiceClasses(Path packageDir) throws IOException {
        // Generate UserService
        String userServiceContent = """
            @MySearchableAnnotation("service class")
            public class UserService {
                
                @MySearchableAnnotation(value = "repository field", priority = 5)
                private UserRepository repository;
                
                @MySearchableAnnotation("find user method")
                public User findUser(Long id) {
                    return repository.findById(id);
                }
                
                @MySearchableAnnotation
                public void saveUser(User user) {
                    repository.save(user);
                }
            }
            """;
        
        Files.writeString(packageDir.resolve("UserService.java"), userServiceContent);
        
        // Generate UserRepository interface
        String userRepositoryContent = """
            public interface UserRepository {
                
                @MySearchableAnnotation("find method")
                User findById(Long id);
                
                @MySearchableAnnotation(value = "save method", priority = 1)
                void save(User user);
                
                @MySearchableAnnotation
                void delete(User user);
            }
            """;
        
        Files.writeString(packageDir.resolve("UserRepository.java"), userRepositoryContent);
    }

    /**
     * Generate a project with specific annotation patterns for testing.
     * 
     * @param projectRoot The root directory
     * @param patterns Map of annotation patterns to generate
     * @throws IOException If files cannot be created
     */
    public static void generateProjectWithPatterns(Path projectRoot, Map<String, Integer> patterns) throws IOException {
        logger.info("Generating project with custom annotation patterns");
        
        Files.createDirectories(projectRoot.resolve("src/main/java"));
        
        for (Map.Entry<String, Integer> entry : patterns.entrySet()) {
            String annotationName = entry.getKey();
            int count = entry.getValue();
            
            generateClassWithAnnotationCount(projectRoot, annotationName, count);
        }
        
        logger.info("Generated project with {} annotation patterns", patterns.size());
    }

    private static void generateClassWithAnnotationCount(Path projectRoot, String annotationName, int count) throws IOException {
        StringBuilder classContent = new StringBuilder();
        classContent.append("public class ").append(annotationName).append("TestClass {\n\n");
        
        for (int i = 0; i < count; i++) {
            classContent.append("    @").append(annotationName).append("\n");
            classContent.append("    private String field").append(i).append(";\n\n");
        }
        
        classContent.append("}\n");
        
        Path classFile = projectRoot.resolve("src/main/java/" + annotationName + "TestClass.java");
        Files.writeString(classFile, classContent.toString());
    }
}