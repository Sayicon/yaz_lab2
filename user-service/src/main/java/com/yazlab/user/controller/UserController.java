package com.yazlab.user.controller;

import com.yazlab.user.model.User;
import com.yazlab.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<User> create(@RequestBody User user, UriComponentsBuilder ucb) {
        User created = userService.create(user);
        URI location = ucb.path("/users/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{id}")
    public User getById(@PathVariable String id) {
        return userService.findById(id);
    }

    @GetMapping
    public List<User> getAll() {
        return userService.findAll();
    }

    @PutMapping("/{id}")
    public User update(@PathVariable String id, @RequestBody User user) {
        return userService.update(id, user);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        userService.delete(id);
    }
}
