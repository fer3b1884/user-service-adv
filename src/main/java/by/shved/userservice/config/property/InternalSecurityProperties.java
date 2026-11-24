package by.shved.userservice.config.property;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.internal")
public record InternalSecurityProperties(
        @NotBlank(message = "Internal service secret must not be blank")
        @Size(min = 32, message = "Internal service secret must be at least 32 characters long")
        String secret
) {
}
