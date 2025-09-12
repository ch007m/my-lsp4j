package dev.snowdrop;

public @interface ImportantAnnotation {
    String value() default "";
    int priority() default 0;
    String[] tags() default {};
}
