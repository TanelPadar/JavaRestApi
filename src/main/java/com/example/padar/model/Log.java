package com.example.padar.model;

public class Log {
    private String id;
    private String endpoint;
    private String method;
    private RequestBody body;
    private String date;

    public Log(String id, String endpoint, String method, RequestBody body, String date) {
        this.id = id;
        this.endpoint = endpoint;
        this.method = method;
        this.body = body;
        this.date = date;
    }

    public Log() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public RequestBody getBody() {
        return body;
    }

    public void setBody(String name, String username, String email) {
        this.body = new RequestBody(name, username, email);
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}

class RequestBody {
    private String name;
    private String username;
    private String email;

    public RequestBody(String name, String username, String email) {
        this.name = name;
        this.username = username;
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}