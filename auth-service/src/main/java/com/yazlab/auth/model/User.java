package com.yazlab.auth.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

@Document(collection = "users")
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    private String username;

    private String password;

    @Indexed(unique = true)
    private String email;

    public User() {}

    public User(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
    }

    public String getId()               { return id; }
    public String getUsername()         { return username; }
    public String getPassword()         { return password; }
    public String getEmail()            { return email; }

    public void setId(String id)               { this.id = id; }
    public void setUsername(String username)   { this.username = username; }
    public void setPassword(String password)   { this.password = password; }
    public void setEmail(String email)         { this.email = email; }
}
