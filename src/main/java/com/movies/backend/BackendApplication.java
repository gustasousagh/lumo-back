package com.movies.backend;

import com.movies.backend.config.AppProperties;
import com.movies.backend.media.gocine.config.GocineProperties;
import com.movies.backend.media.local.config.LocalImportProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({AppProperties.class, GocineProperties.class, LocalImportProperties.class})
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

}
