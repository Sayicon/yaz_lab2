package com.yazlab.user.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "users")
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    private String username;

    private String email;
    private String fullName;

    public User() {}

    public User(String username, String email, String fullName) {
        this.username = username;
        this.email = email;
        this.fullName = fullName;
    }

    public String getId()           { return id; }
    public String getUsername()     { return username; }
    public String getEmail()        { return email; }
    public String getFullName()     { return fullName; }

    public void setId(String id)               { this.id = id; }
    public void setUsername(String username)   { this.username = username; }
    public void setEmail(String email)         { this.email = email; }
    public void setFullName(String fullName)   { this.fullName = fullName; }
}
