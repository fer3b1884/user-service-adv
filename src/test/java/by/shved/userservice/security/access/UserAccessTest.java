package by.shved.userservice.security.access;

import by.shved.userservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAccessTest {
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private UserAccess userAccess;

    @Test
    void isActive_whenUserIsActive_returnsTrue() {
        when(userRepository.findActiveById(1L)).thenReturn(Optional.of(true));

        boolean result = userAccess.isActive(1L);

        assertTrue(result);
        verify(userRepository).findActiveById(1L);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void isActive_whenUserIsInactive_returnsFalse() {
        when(userRepository.findActiveById(1L)).thenReturn(Optional.of(false));

        boolean result = userAccess.isActive(1L);

        assertFalse(result);
        verify(userRepository).findActiveById(1L);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void isActive_whenUserDoesNotExist_returnsFalse() {
        when(userRepository.findActiveById(1L)).thenReturn(Optional.empty());

        boolean result = userAccess.isActive(1L);

        assertFalse(result);
        verify(userRepository).findActiveById(1L);
        verifyNoMoreInteractions(userRepository);
    }
}