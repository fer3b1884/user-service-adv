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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentCardServiceImplTest {
    @Mock
    private PaymentCardRepository paymentCardRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PaymentCardMapper paymentCardMapper;
    @InjectMocks
    private PaymentCardServiceImpl paymentCardService;

    private UserEntity user;
    private PaymentCardEntity card;
    private PaymentCardRequest request;
    private PaymentCardUpdateRequest updateRequest;
    private PaymentCardResponse response;

    @BeforeEach
    void setUp() {
        user = new UserEntity();
        user.setId(1L);
        user.setName("Ivan");
        user.setSurname("Ivanov");
        user.setEmail("ivan.ivanov@example.com");
        user.setActive(true);

        card = new PaymentCardEntity();
        card.setId(10L);
        card.setUser(user);
        card.setNumber("1234567890123456");
        card.setHolder("IVAN IVANOV");
        card.setExpirationDate(LocalDate.of(2028, 12, 31));
        card.setActive(true);

        request = new PaymentCardRequest(
                1L,
                "1234567890123456",
                "IVAN IVANOV",
                LocalDate.of(2028, 12, 31)
        );

        updateRequest = new PaymentCardUpdateRequest(
                "9876543210987654",
                "PETR PETROV",
                LocalDate.of(2029, 10, 31)
        );

        response = new PaymentCardResponse(
                10L,
                1L,
                "1234567890123456",
                "IVAN IVANOV",
                LocalDate.of(2028, 12, 31),
                true
        );
    }

    @Test
    void create_whenUserExists_returnsCard() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(paymentCardRepository.countCardsByUserId(1L)).thenReturn(2);
        when(paymentCardMapper.toEntity(request)).thenReturn(card);
        when(paymentCardRepository.save(card)).thenReturn(card);
        when(paymentCardMapper.toResponse(card)).thenReturn(response);

        PaymentCardResponse result = paymentCardService.create(request);

        assertEquals(response, result);
        assertEquals(user, card.getUser());
        verify(paymentCardRepository).save(card);
    }

    @Test
    void create_whenUserDoesNotExist_throwsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> paymentCardService.create(request));

        verify(paymentCardRepository, never()).save(any(PaymentCardEntity.class));
    }

    @Test
    void create_whenCardLimitReached_throwsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(paymentCardRepository.countCardsByUserId(1L)).thenReturn(5);

        assertThrows(BusinessException.class, () -> paymentCardService.create(request));

        verify(paymentCardRepository, never()).save(any(PaymentCardEntity.class));
    }

    @Test
    void getById_whenCardExists_returnsCard() {
        when(paymentCardRepository.findById(10L)).thenReturn(Optional.of(card));
        when(paymentCardMapper.toResponse(card)).thenReturn(response);

        PaymentCardResponse result = paymentCardService.getById(10L);

        assertEquals(response, result);
        verify(paymentCardRepository).findById(10L);
    }

    @Test
    void getById_whenCardDoesNotExist_throwsException() {
        when(paymentCardRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> paymentCardService.getById(10L));

        verify(paymentCardMapper, never()).toResponse(any(PaymentCardEntity.class));
    }

    @Test
    void getAll_returnsMappedPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<PaymentCardEntity> entityPage = new PageImpl<>(List.of(card));
        when(paymentCardRepository.findAll(ArgumentMatchers.<Specification<PaymentCardEntity>>any(), eq(pageable))).thenReturn(entityPage);
        when(paymentCardMapper.toResponse(card)).thenReturn(response);

        Page<PaymentCardResponse> result = paymentCardService.getAll("Ivan", "Ivanov", pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(List.of(response), result.getContent());
        verify(paymentCardRepository).findAll(ArgumentMatchers.<Specification<PaymentCardEntity>>any(), eq(pageable));
    }

    @Test
    void getAllByUserId_whenUserExists_returnsUserCards() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(paymentCardRepository.findAllByUser_Id(1L)).thenReturn(List.of(card));
        when(paymentCardMapper.toResponse(card)).thenReturn(response);

        List<PaymentCardResponse> result = paymentCardService.getAllByUserId(1L);

        assertEquals(List.of(response), result);
        verify(paymentCardRepository).findAllByUser_Id(1L);
    }

    @Test
    void getAllByUserId_whenUserDoesNotExist_throwsException() {
        when(userRepository.existsById(1L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> paymentCardService.getAllByUserId(1L));

        verify(paymentCardRepository, never()).findAllByUser_Id(anyLong());
    }

    @Test
    void update_whenCardExists_updatesCard() {
        PaymentCardResponse updatedResponse = new PaymentCardResponse(
                10L,
                1L,
                updateRequest.number(),
                updateRequest.holder(),
                updateRequest.expirationDate(),
                true
        );
        when(paymentCardRepository.findById(10L)).thenReturn(Optional.of(card));
        when(paymentCardRepository.save(card)).thenReturn(card);
        when(paymentCardMapper.toResponse(card)).thenReturn(updatedResponse);

        PaymentCardResponse result = paymentCardService.update(10L, updateRequest);

        assertEquals(updatedResponse, result);
        assertEquals(updateRequest.number(), card.getNumber());
        assertEquals(updateRequest.holder(), card.getHolder());
        assertEquals(updateRequest.expirationDate(), card.getExpirationDate());
        verify(paymentCardRepository).save(card);
    }

    @Test
    void update_whenCardDoesNotExist_throwsException() {
        when(paymentCardRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> paymentCardService.update(10L, updateRequest));

        verify(paymentCardRepository, never()).save(any(PaymentCardEntity.class));
    }

    @Test
    void setActive_whenCardExists_updatesStatus() {
        PaymentCardResponse inactiveResponse = new PaymentCardResponse(
                10L,
                1L,
                card.getNumber(),
                card.getHolder(),
                card.getExpirationDate(),
                false
        );
        when(paymentCardRepository.findById(10L)).thenReturn(Optional.of(card));
        when(paymentCardRepository.save(card)).thenReturn(card);
        when(paymentCardMapper.toResponse(card)).thenReturn(inactiveResponse);

        PaymentCardResponse result = paymentCardService.setActive(10L, false);

        assertFalse(card.isActive());
        assertEquals(inactiveResponse, result);
        verify(paymentCardRepository).save(card);
    }

    @Test
    void setActive_whenCardDoesNotExist_throwsException() {
        when(paymentCardRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> paymentCardService.setActive(10L, false));

        verify(paymentCardRepository, never()).save(any(PaymentCardEntity.class));
    }

    @Test
    void deleteById_whenCardExists_deletesCard() {
        when(paymentCardRepository.findById(10L)).thenReturn(Optional.of(card));

        paymentCardService.deleteById(10L);

        verify(paymentCardRepository).delete(card);
    }

    @Test
    void deleteById_whenCardDoesNotExist_throwsException() {
        when(paymentCardRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> paymentCardService.deleteById(10L));

        verify(paymentCardRepository, never()).delete(any(PaymentCardEntity.class));
    }

    @AfterEach
    void tearDown() {
    }
}