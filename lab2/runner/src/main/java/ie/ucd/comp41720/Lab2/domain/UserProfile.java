package ie.ucd.comp41720.Lab2.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "UserProfile")
public class UserProfile {
    @Id
    private String id;
    private String userId;
    private String username;
    private String email;
    private Long lastLoginTime;
    public UserProfile() {}
    public UserProfile(String id, String userId, String username, String email, Long lastLoginTime) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.lastLoginTime = lastLoginTime;
    }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Long getLastLoginTime() { return lastLoginTime; }
    public void setLastLoginTime(Long lastLoginTime) { this.lastLoginTime = lastLoginTime; }
}