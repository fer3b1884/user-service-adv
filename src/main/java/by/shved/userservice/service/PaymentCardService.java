package by.shved.userservice.service;

import by.shved.userservice.dto.paymentcard.PaymentCardRequest;
import by.shved.userservice.dto.paymentcard.PaymentCardResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PaymentCardService {
    PaymentCardResponse create(Long userId, PaymentCardRequest request);
    PaymentCardResponse getById(Long id);
    Page<PaymentCardResponse> getAll(String userName, String userSurname, Pageable pageable);
    List<PaymentCardResponse> getAllByUserId(Long userId);
    PaymentCardResponse update(Long id, PaymentCardRequest request);
    PaymentCardResponse setActive(Long id, boolean active);
    void deleteById(Long id);
}
