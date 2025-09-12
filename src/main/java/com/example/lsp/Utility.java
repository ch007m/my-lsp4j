package com.example.lsp;

import java.nio.file.Files;
import java.nio.file.Path;

public class Utility {

    public static Path createSampleProject() throws Exception {
        Path root = Files.createTempDirectory("jdtls-sample-project");
        Path srcDir = root.resolve("src/main/java/com/example/annotations");
        Files.createDirectories(srcDir);

        String pomContent = """
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>
                <properties>
                    <maven.compiler.source>17</maven.compiler.source>
                    <maven.compiler.target>17</maven.compiler.target>
                </properties>
            </project>
            """;
        Files.writeString(root.resolve("pom.xml"), pomContent);

        String annotationContent = """
            package com.example.annotations;
            
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Target;
            
            @Target(ElementType.TYPE)
            public @interface MySearchableAnnotation {
            }
            """;
        Files.writeString(srcDir.resolve("MySearchableAnnotation.java"), annotationContent);

        String classContent = """
            package com.example.annotations;
            
            @MySearchableAnnotation
            public class MyAnnotatedClass {
                public void doSomething() {
                    // This is a sample class
                }
            }
            """;
        Files.writeString(srcDir.resolve("MyAnnotatedClass.java"), classContent);

        return root;
    }
}
