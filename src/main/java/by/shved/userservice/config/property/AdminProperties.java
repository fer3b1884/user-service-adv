package by.shved.userservice.config.property;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.admin")
public record AdminProperties(
        @NotNull
        @Positive
        Long userId,

        @NotBlank
        String name,

        @NotBlank
        String surname,

        @NotBlank
        @Email
        String email
) {
}
