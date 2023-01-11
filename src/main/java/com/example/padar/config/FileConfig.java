package com.example.padar.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class FileConfig {
    @Value("${logs.path}")
    private String filePath;
    public Path getLogsFilePath() {
        return Paths.get(filePath);
    }
}
