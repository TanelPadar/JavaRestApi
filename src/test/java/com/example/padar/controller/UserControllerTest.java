package com.example.padar.controller;

import com.example.padar.config.FileConfig;
import com.example.padar.dao.UserDao;
import com.example.padar.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bucket;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(MockitoJUnitRunner.class)
@SpringBootTest
public class UserControllerTest {

    User user1 = new User(1, "Alice", "alice", "alice@example.com");
    User user2 = new User(2, "Bob", "bob", "bob@example.com");

    private MockMvc mockMvc;
    @InjectMocks
    private UserController userController;

    @Mock
    private UserDao userDao;

    @Mock
    private SimpMessagingTemplate template;

    @Mock
    private FileConfig fileConfig;

    @Mock
    private Bucket bucket;

    @Before
    public void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);
        this.mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
        initMockLogs();
    }




    public void initMockLogs() throws IOException {
        File tempFile = File.createTempFile("logs", "log");
        when(fileConfig.getLogsFilePath()).thenReturn(tempFile.toPath());
    }

    /**
     * We're testing that when we call the `/users` endpoint, we get back a list of users with the correct names
     */
    @Test
    public void testGetAllUsers() throws Exception {
        List<User> expectedUsers = Arrays.asList(user1, user2);
        when(userDao.getAllUsers()).thenReturn(expectedUsers);

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is(user1.getName())))
                .andExpect(jsonPath("$[1].name", is(user2.getName())));

        verify(userDao, atLeastOnce()).getAllUsers();
    }


    /**
     * We're testing the `addUser` function in the `UserController` class
     */

    @Test
    @WithMockUser(username = "user1", password = "password",authorities = { "ADMIN" })
    public void testAddUser() throws Exception {
        when(userDao.getAllUsers()).thenReturn(List.of(user1));
        when(bucket.tryConsume(1)).thenReturn(true);

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(user1);

        MockHttpServletRequestBuilder requestBuilder = post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json);

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is(user1.getName())))
                .andExpect(jsonPath("$.username", is(user1.getUsername())))
                .andExpect(jsonPath("$.email", is(user1.getEmail())));

        verify(userDao).addUser(refEq(user1));
    }

    /**
     * We're testing the updateUser function in the UserController class
     */
    @Test
    @WithMockUser(username = "user1", password = "password",authorities = { "ADMIN" })
    public void testUpdateUser() throws Exception {
        when(userDao.getUserById(1)).thenReturn(Collections.singletonList(user1));
        when(userDao.updateUser(refEq(user2), eq(1))).thenReturn(1);
        when(bucket.tryConsume(1)).thenReturn(true);


        User updatedUser = new User(user1.getId(), user2.getName(), user2.getUsername(), user2.getEmail());

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(user2);

        MockHttpServletRequestBuilder requestBuilder = put("/users/{$}", 1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json);

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(updatedUser.getId())))
                .andExpect(jsonPath("$.name", is(updatedUser.getName())))
                .andExpect(jsonPath("$.username", is(updatedUser.getUsername())));

        verify(userDao).updateUser(refEq(updatedUser), eq(1));
        verify(template).convertAndSend(eq("/topic/update"), any(List.class));
    }

    /**
     * It reads the logs file, parses the logs, and returns them as JSON
     */
    @Test
    public void testGetLogs() throws Exception {
        String log1 = "FINISHED PROCESSING : ID=123; " +
                "REQUESTURL=users; " +
                "METHOD=PUT; " +
                "REQUESTBODY={name\":\"Bob\",\"username\":\"bob\",\"email\":\"bob@example.com\"};";
        String log2 = "FINISHED PROCESSING : " +
                "ID=456; " +
                "REQUESTURL=users; " +
                "METHOD=POST; " +
                "REQUESTBODY={name\":\"Alice\",\"username\":\"alice\",\"email\":\"alice@example.com\"};";

        FileWriter writer = new FileWriter(fileConfig.getLogsFilePath().toFile());
        writer.write(log1 + System.lineSeparator() + log2);
        writer.close();

        mockMvc.perform(get("/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is("123")))
                .andExpect(jsonPath("$[0].endpoint", is("users")))
                .andExpect(jsonPath("$[0].method", is("PUT")))
                .andExpect(jsonPath("$[0].body.name", is("Bob")))
                .andExpect(jsonPath("$[0].body.username", is("bob")))
                .andExpect(jsonPath("$[0].body.email", is("bob@example.com")))
                .andExpect(jsonPath("$[1].id", is("456")))
                .andExpect(jsonPath("$[1].endpoint", is("users")))
                .andExpect(jsonPath("$[1].method", is("POST")))
                .andExpect(jsonPath("$[1].body.name", is("Alice")))
                .andExpect(jsonPath("$[1].body.username", is("alice")))
                .andExpect(jsonPath("$[1].body.email", is("alice@example.com")));

        verify(fileConfig, atLeastOnce()).getLogsFilePath();
    }

    /**
     * We're testing the deleteUserById() function in the UserController class
     */
    @Test
    @WithMockUser(username = "admin", password = "password",roles = "ADMIN")
    public void testDeleteUserById() throws Exception {
        when(userDao.deleteUserById(1)).thenReturn(1);
        when(bucket.tryConsume(1)).thenReturn(true);
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders
                .delete("/users/{id}", 1)
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(content().string("{}"));
        verify(userDao).deleteUserById(1);
    }

}


