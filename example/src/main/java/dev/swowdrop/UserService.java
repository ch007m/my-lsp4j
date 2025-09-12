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
