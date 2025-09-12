package dev.snowdrop;

import dev.snowdrop.MySearchableAnnotation;
import dev.snowdrop.User;

public interface UserRepository {

    @MySearchableAnnotation("find method")
    User findById(Long id);

    @MySearchableAnnotation(value = "save method", priority = 1)
    void save(User user);

    @MySearchableAnnotation
    void delete(User user);
}
