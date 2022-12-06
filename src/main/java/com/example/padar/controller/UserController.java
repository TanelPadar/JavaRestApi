package com.example.padar.controller;

import com.example.padar.dao.UserDao;
import com.example.padar.model.Kasutajad;
import com.example.padar.model.User;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.List;

@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/")
public class UserController {


    private Bucket bucket;
    private final UserDao UserDao;
    @Autowired
    SimpMessagingTemplate template;

    public UserController(UserDao UserDao) {
        this.UserDao = UserDao;
    }

    @GetMapping("/users")
    @Operation(
            tags = {"Get all users"},
            responses = {@ApiResponse(responseCode = "200",
                    content = @Content(schema =
                    @Schema(implementation =  User.class),mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {@ExampleObject(name = "", value="{\n" +
                                    "        \"id\": 586,\n" +
                                    "        \"name\": \"tanel\",\n" +
                                    "        \"username\": \"tanel\",\n" +
                                    "        \"email\": \"tanel\"\n" +
                                    "    }")}),
                    description = "Success Response."),
            }
    )
    public List<User> getAllUsers() {
        template.convertAndSend("/topic/get" , new Kasutajad(UserDao.getAllUsers()));
        return UserDao.getAllUsers();
    }



    @PostMapping("/users")
    @Operation(
            tags = {"Create new user"},
            responses = {@ApiResponse(responseCode = "200",
            content = @Content(schema =
            @Schema(implementation =  User.class),mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = {@ExampleObject(name = "", value="{\n" +
                            "        \"id\": 586,\n" +
                            "        \"name\": \"tanel\",\n" +
                            "        \"username\": \"tanel\",\n" +
                            "        \"email\": \"tanel\"\n" +
                            "    }")}),
            description = "Success Response."),
    }
    )
    public ResponseEntity<?> addUsers(@RequestBody User user){
            if (bucket.tryConsume(1)) {
                UserDao.addUser(user);
                template.convertAndSend("/topic/post", user);
                template.convertAndSend("/topic/get", new Kasutajad(UserDao.getAllUsers()));
                return ResponseEntity.ok(UserDao.getAllUsers().get(UserDao.getAllUsers().size() - 1));

            } else {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
            }
    }



    @PutMapping("/users/{id}")
    @Operation(
            tags = {"Edit existing user"},
            responses = {@ApiResponse(responseCode = "200",
                    content = @Content(schema =
                    @Schema(implementation =  User.class),mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {@ExampleObject(name = "", value="{\n" +
                                    "        \"id\": 586,\n" +
                                    "        \"name\": \"tanel\",\n" +
                                    "        \"username\": \"tanel\",\n" +
                                    "        \"email\": \"tanel\"\n" +
                                    "    }")}),
                    description = "Success Response."),
            }
    )
    public ResponseEntity<?> editUsers(@PathVariable int id,@RequestBody User user){
        if (bucket.tryConsume(1)) {
            UserDao.updateUser(user,id);
            template.convertAndSend("/topic/update", UserDao.showUpdatedUser(id));
            return ResponseEntity.ok( UserDao.showUpdatedUser(id).get(0));
        } else {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }

    }
    @DeleteMapping("/users/{id}")
    @Operation(
            tags = {"Delete existing user"},
            description = "Deletes an existing `User`",
            responses = {@ApiResponse(responseCode = "200",
                    content = @Content(schema =
                    @Schema(implementation = User.class), mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {@ExampleObject(name = "", value = "{}")}),
                    description = "Success Response.")
            }
    )
    public ResponseEntity<String> deleteUsersById(@PathVariable int id){
        if (bucket.tryConsume(1)) {
            UserDao.deleteUserById(id);
            template.convertAndSend("/topic/delete", id);
            return ResponseEntity.ok("{}");
        } else {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }
    }

    @PostConstruct
    public void setupBucket() {
        Bandwidth limit = Bandwidth.classic(3, Refill.intervally(3, Duration.ofMinutes(1)));
        this.bucket = Bucket4j.builder().addLimit(limit).build();
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
