package com.example.padar.controller;

import com.example.padar.dao.UserDao;
import com.example.padar.model.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class UserController {
    private final UserDao UserDao;

    public UserController(UserDao UserDao) {
        this.UserDao = UserDao;
    }

    @GetMapping("/users")
    public List<User> getAllUsers() {
        return UserDao.getAllUsers();
    }


    @PostMapping("/users")
    public List<User> addUsers(@RequestBody User user){
        UserDao.addUser(user);
        return UserDao.showPostedUser();
    }

    @PutMapping("/users/{id}")
    public List<User> editUsers(@PathVariable int id,@RequestBody User user){
        UserDao.updateUser(user,id);
        return UserDao.showUpdatedUser(id);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<String> deleteUsersById(@PathVariable int id){
        UserDao.deleteUserById(id);
        return ResponseEntity.ok("{}");
    }
}
