package by.shved.userservice.dto.paymentcard;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PaymentCardUpdateRequest(
        @NotBlank(message = "Card number is required")
        @Size(max = 20, message = "Card number must not exceed 20 characters")
        String number,

        @NotBlank(message = "Card holder is required")
        @Size(max = 60, message = "Card holder must not exceed 60 characters")
        String holder,

        @NotNull(message = "Expiration date is required")
        LocalDate expirationDate
) { }
