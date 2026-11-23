package by.shved.userservice.security.access;

import by.shved.userservice.repository.PaymentCardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("cardAccess")
@RequiredArgsConstructor
public class CardAccess {
    private final PaymentCardRepository paymentCardRepository;

    public boolean isOwner(Long cardId, Long userId) {
        return paymentCardRepository.existsByIdAndUser_Id(cardId, userId);
    }
}
