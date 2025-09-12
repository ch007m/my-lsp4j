package dev.snowdrop;

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
