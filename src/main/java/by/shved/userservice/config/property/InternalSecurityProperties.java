package by.shved.userservice.config.property;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.internal")
public record InternalSecurityProperties(
        @NotBlank(message = "Internal service secret must not be blank")
        String secret
) {
}
