package authservice.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(Long id){
        super("User not found by id: " + id);
    }
    public UserNotFoundException(String username) {
        super("User not found by username: " + username);
    }
}
