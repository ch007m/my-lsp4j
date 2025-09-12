package dev.snowdrop;

public @interface MySearchableAnnotation {
    String value() default "";
    int priority() default 0;
    String[] tags() default {};
}
