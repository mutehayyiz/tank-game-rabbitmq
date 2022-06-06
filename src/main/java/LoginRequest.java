
public class LoginRequest {
    String username;

    LoginRequest(String username) {
        this.username = username;
    }

    public String Token() {
        return this.username;
    }
}
