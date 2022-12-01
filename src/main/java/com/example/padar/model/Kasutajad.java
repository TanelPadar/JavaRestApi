package com.example.padar.model;
import com.example.padar.model.User;

import java.util.ArrayList;
import java.util.List;

public class Kasutajad {

    private List<User> users;

    public Kasutajad(List<User> users) {
        this.users = users;
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }
}


