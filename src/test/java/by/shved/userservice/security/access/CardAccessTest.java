package by.shved.userservice.security.access;

import by.shved.userservice.repository.PaymentCardRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardAccessTest {
    @Mock
    private PaymentCardRepository paymentCardRepository;
    @InjectMocks
    private CardAccess cardAccess;

    @Test
    void isOwner_whenCardBelongsToUser_returnsTrue() {
        when(paymentCardRepository.existsByIdAndUser_Id(10L, 1L)).thenReturn(true);

        boolean result = cardAccess.isOwner(10L, 1L);

        assertTrue(result);
        verify(paymentCardRepository).existsByIdAndUser_Id(10L, 1L);
        verifyNoMoreInteractions(paymentCardRepository);
    }

    @Test
    void isOwner_whenCardDoesNotBelongToUser_returnsFalse() {
        when(paymentCardRepository.existsByIdAndUser_Id(10L, 2L)).thenReturn(false);

        boolean result = cardAccess.isOwner(10L, 2L);

        assertFalse(result);
        verify(paymentCardRepository).existsByIdAndUser_Id(10L, 2L);
        verifyNoMoreInteractions(paymentCardRepository);
    }
}