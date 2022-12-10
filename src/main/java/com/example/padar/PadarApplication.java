package com.example.padar;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;


@SpringBootApplication
@OpenAPIDefinition(
        info = @Info(title = "Rest API documentation", version = "1.0.0"),
        servers = {@Server(url="http://localhost:8080")}
)

public class PadarApplication {

    public static void main(String[] args) throws IOException {
        SpringApplication.run(PadarApplication.class, args);


    }

}
