package com.example.padar.controller;

import com.example.padar.config.FileConfig;
import com.example.padar.dao.UserDao;
import com.example.padar.filter.LoggingFilter;
import com.example.padar.model.Kasutajad;
import com.example.padar.model.Log;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/")
public class UserController {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingFilter.class);
    @Autowired
    SimpMessagingTemplate template;
    private Bucket bucket;
    @Autowired
    private UserDao userDao;
    @Autowired
    private FileConfig fileConfig;

    public UserController() {
    }

    @GetMapping("/users")
    @Operation(
            tags = {"Get all users"},
            responses = {@ApiResponse(responseCode = "200",
                    content = @Content(schema =
                    @Schema(implementation = User.class), mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {@ExampleObject(name = "", value = "{\n" +
                                    "        \"id\": 586,\n" +
                                    "        \"name\": \"tanel\",\n" +
                                    "        \"username\": \"tanel\",\n" +
                                    "        \"email\": \"tanel\"\n" +
                                    "    }")}),
                    description = "Success Response."),
            }
    )
    public List<User> getAllUsers() {
        template.convertAndSend("/topic/get", new Kasutajad(userDao.getAllUsers()));
        return userDao.getAllUsers();
    }


    @GetMapping("/logs")
    public List<Log> getLogs() {
        try {
            List<Log> logs = new ArrayList<>();

            Pattern processing_pattern = Pattern.compile("FINISHED PROCESSING");
            Pattern id_pattern = Pattern.compile("((?<=ID=)(\\S*(?=;)))");
            Pattern endpoint_pattern = Pattern.compile("(?<=REQUESTURL=)(\\S*(?=;))");
            Pattern method_pattern = Pattern.compile("(?<=METHOD=)(\\S*(?=;))");
            Pattern name_pattern = Pattern.compile("(?<=name\":\")(\\S*(?=\",\"username))");
            Pattern username_pattern = Pattern.compile("(?<=username\":\")(\\S*(?=\",\"email))");
            Pattern email_pattern = Pattern.compile("(?<=email\":\")(\\S*(?=\"}))");
            Pattern date_pattern = Pattern.compile("\\d+-\\d+-\\d+ \\d+:\\d+:\\d+.\\d+");

            for (String line : Files.readAllLines(fileConfig.getLogsFilePath())) {
                Matcher processing_matcher = processing_pattern.matcher(line);
                if (processing_matcher.find()) {
                    Matcher method_matcher = method_pattern.matcher(line);
                    if (method_matcher.find() && method_matcher.group(0).matches("POST|DELETE|PUT")) {
                        Log log = new Log();
                        log.setMethod(method_matcher.group(0));

                        Matcher id_matcher = id_pattern.matcher(line);
                        Matcher endpoint_matcher = endpoint_pattern.matcher(line);
                        Matcher date_matcher = date_pattern.matcher(line);
                        Matcher name_matcher = name_pattern.matcher(line);
                        Matcher username_matcher = username_pattern.matcher(line);
                        Matcher email_matcher = email_pattern.matcher(line);

                        if (id_matcher.find()) log.setId(id_matcher.group());
                        if (endpoint_matcher.find()) log.setEndpoint(endpoint_matcher.group());
                        if (date_matcher.find()) log.setDate(date_matcher.group());
                        if (date_matcher.find()) log.setDate(date_matcher.group());
                        while (name_matcher.find() && username_matcher.find() && email_matcher.find()) {
                            if (log.getBody() == null)
                                log.setBody(
                                        name_matcher.group(),
                                        username_matcher.group(),
                                        email_matcher.group()
                                );
                            else {
                                log.setOldBody(
                                        name_matcher.group(),
                                        username_matcher.group(),
                                        email_matcher.group()
                                );
                            }
                        }
                        if (log.getOldBody() != null) log.distinctBody();
                        logs.add(log);
                    }
                }
            }
            return logs;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    @PostMapping("/users")
    @Operation(
            tags = {"Create new user"},
            responses = {@ApiResponse(responseCode = "200",
                    content = @Content(schema =
                    @Schema(implementation = User.class), mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {@ExampleObject(name = "", value = "{\n" +
                                    "        \"id\": 586,\n" +
                                    "        \"name\": \"tanel\",\n" +
                                    "        \"username\": \"tanel\",\n" +
                                    "        \"email\": \"tanel\"\n" +
                                    "    }")}),
                    description = "Success Response."),
            }
    )
    public ResponseEntity<?> addUsers(@RequestBody User user, HttpSession session) {
        if (bucket.tryConsume(1)) {
            userDao.addUser(user);
            template.convertAndSend("/topic/post", user);
            template.convertAndSend("/topic/get", new Kasutajad(userDao.getAllUsers()));
            return ResponseEntity.ok(userDao.getAllUsers().get(userDao.getAllUsers().size() - 1));

        } else {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }
    }

    @PutMapping("/users/{id}")
    @Operation(
            tags = {"Edit existing user"},
            responses = {@ApiResponse(responseCode = "200",
                    content = @Content(schema =
                    @Schema(implementation = User.class), mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {@ExampleObject(name = "", value = "{\n" +
                                    "        \"id\": 586,\n" +
                                    "        \"name\": \"tanel\",\n" +
                                    "        \"username\": \"tanel\",\n" +
                                    "        \"email\": \"tanel\"\n" +
                                    "    }")}),
                    description = "Success Response."),
            }
    )


    public ResponseEntity<?> editUsers(@PathVariable int id, @RequestBody User user, @Autowired HttpServletRequest request) {
        if (bucket.tryConsume(1)) {
            LOG.info(
                    "FINISHED PROCESSING : METHOD={}; REQUESTURL={}; ID={}; REQUESTBODY={}; OLDUSER={};",
                    request.getMethod(), request.getRequestURI(), request.getSession().getId(),
                    user.toString(), userDao.getUserById(id).get(0).toString());
            if (userDao.updateUser(user, id) == 1) {
                user.setid(id);
                template.convertAndSend("/topic/update", List.of(user));
                return ResponseEntity.ok(user);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
            }
        }
        else {
                return new ResponseEntity<>("Too many requests. Please try again later.", HttpStatus.TOO_MANY_REQUESTS);
        }
    }


    @Secured("ADMIN")
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
    public ResponseEntity<?> deleteUsersById(@PathVariable int id) {
        if (bucket.tryConsume(1)) {
            if (userDao.deleteUserById(id) == 1) {
                template.convertAndSend("/topic/delete", id);
                return ResponseEntity.ok("{}");
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
        }
        else {
            return new ResponseEntity<>("Too many requests. Please try again later.", HttpStatus.TOO_MANY_REQUESTS);
        }
    }

    @PostConstruct
    public void setupBucket() {
        Bandwidth limit = Bandwidth.classic(3, Refill.intervally(3, Duration.ofMinutes(1)));
        this.bucket = Bucket4j.builder().addLimit(limit).build();
    }

    @SendTo("/topic/get")
    public Kasutajad broadcastGet(@Payload Kasutajad Kasutajad) {
        ;
        return Kasutajad;
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
