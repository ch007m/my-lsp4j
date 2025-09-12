package dev.snowdrop;

import dev.snowdrop.ImportantAnnotation;
import dev.snowdrop.MySearchableAnnotation;

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
