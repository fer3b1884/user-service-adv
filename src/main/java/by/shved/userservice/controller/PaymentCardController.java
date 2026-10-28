package by.shved.userservice.controller;

import by.shved.userservice.dto.paymentcard.PaymentCardRequest;
import by.shved.userservice.dto.paymentcard.PaymentCardResponse;
import by.shved.userservice.dto.paymentcard.PaymentCardUpdateRequest;
import by.shved.userservice.service.PaymentCardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payment-cards")
@RequiredArgsConstructor
public class PaymentCardController {
    private final PaymentCardService paymentCardService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<PaymentCardResponse>> getAll(
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) String userSurname,
            Pageable pageable) {
        Page<PaymentCardResponse> response = paymentCardService.getAll(userName, userSurname, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @paymentCardServiceImpl.isOwner(#id, authentication.principal)")
    public ResponseEntity<PaymentCardResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(paymentCardService.getById(id));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal")
    public ResponseEntity<List<PaymentCardResponse>> getAllByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(paymentCardService.getAllByUserId(userId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentCardResponse> create(@Valid @RequestBody PaymentCardRequest request) {
        PaymentCardResponse response = paymentCardService.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @paymentCardServiceImpl.isOwner(#id, authentication.principal)")
    public ResponseEntity<PaymentCardResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody PaymentCardUpdateRequest request) {
        return ResponseEntity.ok(paymentCardService.update(id, request));
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentCardResponse> setActive(@PathVariable Long id, @RequestParam boolean active) {
        return ResponseEntity.ok(paymentCardService.setActive(id, active));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        paymentCardService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
