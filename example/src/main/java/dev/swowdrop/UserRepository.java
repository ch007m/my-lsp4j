public interface UserRepository {

    @MySearchableAnnotation("find method")
    User findById(Long id);

    @MySearchableAnnotation(value = "save method", priority = 1)
    void save(User user);

    @MySearchableAnnotation
    void delete(User user);
}
