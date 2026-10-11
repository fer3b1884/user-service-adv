package by.shved.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import tools.jackson.databind.annotation.JsonPOJOBuilder;

@SpringBootApplication
public class UserServiceAdvApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceAdvApplication.class, args);
    }

}
