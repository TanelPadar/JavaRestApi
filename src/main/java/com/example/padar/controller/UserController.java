package com.example.padar.controller;

import com.example.padar.dao.UserDao;
import com.example.padar.model.Kasutajad;
import com.example.padar.model.User;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class UserController {
    private final UserDao UserDao;

    @Autowired
    SimpMessagingTemplate template;

    public UserController(UserDao UserDao) {
        this.UserDao = UserDao;
    }

    @GetMapping("/users")
    @Operation(
            tags = {"Get all users"}
    )
    public List<User> getAllUsers() {
        template.convertAndSend("/topic/get" , new Kasutajad(UserDao.getAllUsers()));
        return UserDao.getAllUsers();
    }


    @PostMapping("/users")
    @Operation(
            tags = {"Create new user"}
    )
    public List<User> addUsers(@RequestBody User user){
        UserDao.addUser(user);
        template.convertAndSend("/topic/post" ,user);
        template.convertAndSend("/topic/get" , new Kasutajad(UserDao.getAllUsers()));
        return UserDao.showPostedUser();
    }

    @PutMapping("/users/{id}")
    @Operation(
            tags = {"Edit existing user"}
    )
    public List<User> editUsers(@PathVariable int id,@RequestBody User user){
        UserDao.updateUser(user,id);
        template.convertAndSend("/topic/update", UserDao.showUpdatedUser(id));
        return UserDao.showUpdatedUser(id);
    }

    @DeleteMapping("/users/{id}")
    @Operation(
            tags = {"Delete existing user"}
    )
    public String deleteUsersById(@PathVariable int id){
        UserDao.deleteUserById(id);
        template.convertAndSend("/topic/delete", id);
        return "{}";
    }

    @SendTo("/topic/get")
    public Kasutajad broadcastGet(@Payload  Kasutajad Kasutajad) {;
        return  Kasutajad;
    }

    @SendTo("/topic/post")
    public User broadcastMessage(@Payload User user) {
        return user;
    }


    @SendTo("/topic/delete")
    public int broadcastDelete(@Payload int id) {
        return id;
    }

    @SendTo("/topic/update")
    public User broadcastUpdate(@Payload User user) {
        return user;
    }



}
