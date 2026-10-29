package by.shved.userservice.service.impl;

import by.shved.userservice.dto.paymentcard.PaymentCardRequest;
import by.shved.userservice.dto.paymentcard.PaymentCardResponse;
import by.shved.userservice.dto.paymentcard.PaymentCardUpdateRequest;
import by.shved.userservice.entity.PaymentCardEntity;
import by.shved.userservice.entity.UserEntity;
import by.shved.userservice.exception.BusinessException;
import by.shved.userservice.exception.ResourceNotFoundException;
import by.shved.userservice.mapper.PaymentCardMapper;
import by.shved.userservice.repository.PaymentCardRepository;
import by.shved.userservice.repository.UserRepository;
import by.shved.userservice.repository.specification.PaymentCardSpecification;
import by.shved.userservice.service.PaymentCardService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCardServiceImpl implements PaymentCardService {
    private final PaymentCardRepository paymentCardRepository;
    private final UserRepository userRepository;
    private final PaymentCardMapper paymentCardMapper;

    @Override
    @Transactional
    public PaymentCardResponse create(PaymentCardRequest request) {
        UserEntity user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.userId()));
        int cardCount = paymentCardRepository.countCardsByUserId(user.getId());
        if (cardCount >= 5) {
            throw new BusinessException("User cannot have more than 5 payment cards");
        }
        PaymentCardEntity card = paymentCardMapper.toEntity(request);
        card.setUser(user);
        PaymentCardEntity savedCard = paymentCardRepository.save(card);
        log.info("Payment card created: cardId={}, userId={}", savedCard.getId(), user.getId());
        return paymentCardMapper.toResponse(savedCard);
    }

    @Cacheable(cacheNames = "payment_cards", key = "#id")
    @Override
    public PaymentCardResponse getById(Long id) {
        PaymentCardEntity card = paymentCardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentCard not found with id: " + id));
        return paymentCardMapper.toResponse(card);
    }

    @Override
    public Page<PaymentCardResponse> getAll(String userName, String userSurname, Pageable pageable) {
        Specification<PaymentCardEntity> specification = Specification
                .where(PaymentCardSpecification.hasUserName(userName))
                .and(PaymentCardSpecification.hasUserSurname(userSurname));
        return paymentCardRepository.findAll(specification, pageable).map(paymentCardMapper::toResponse);
    }

    @Override
    public List<PaymentCardResponse> getAllByUserId(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
        List<PaymentCardEntity> foundCards = paymentCardRepository.findAllByUser_Id(userId);
        return foundCards.stream()
                .map(paymentCardMapper::toResponse)
                .toList();
    }

    @CacheEvict(cacheNames = "payment_cards", key = "#id")
    @Override
    @Transactional
    public PaymentCardResponse update(Long id, PaymentCardUpdateRequest request) {
        PaymentCardEntity card = paymentCardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment card not found with id: " + id));
        card.setNumber(request.number());
        card.setHolder(request.holder());
        card.setExpirationDate(request.expirationDate());
        PaymentCardEntity updatedCard = paymentCardRepository.save(card);
        log.info("Payment card updated: cardId={}", updatedCard.getId());
        return paymentCardMapper.toResponse(updatedCard);
    }

    @CacheEvict(cacheNames = "payment_cards", key = "#id")
    @Override
    @Transactional
    public PaymentCardResponse setActive(Long id, boolean active) {
        PaymentCardEntity card = paymentCardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment card not found with id: " + id));
        card.setActive(active);
        PaymentCardEntity updatedCard = paymentCardRepository.save(card);
        log.info("Payment card active status changed: cardId={}, active={}", updatedCard.getId(), active);
        return paymentCardMapper.toResponse(updatedCard);
    }

    @CacheEvict(cacheNames = "payment_cards", key = "#id")
    @Override
    @Transactional
    public void deleteById(Long id) {
        PaymentCardEntity card = paymentCardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment card not found with id: " + id));
        paymentCardRepository.delete(card);
        log.info("Payment card deleted: cardId={}", id);
    }

    @Override
    public boolean isOwner(Long cardId, Long userId) {
        return paymentCardRepository.findById(cardId)
                .map(card -> card.getUser().getId().equals(userId))
                .orElse(false);
    }
}
