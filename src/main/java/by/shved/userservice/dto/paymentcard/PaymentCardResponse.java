package by.shved.userservice.dto.paymentcard;


import java.time.LocalDate;

public record PaymentCardResponse (
        Long id,
        Long userId,
        String number,
        String holder,
        LocalDate expirationDate,
        boolean active
) { }
