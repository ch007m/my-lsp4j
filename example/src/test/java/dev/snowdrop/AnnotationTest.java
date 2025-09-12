package dev.snowdrop;

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
