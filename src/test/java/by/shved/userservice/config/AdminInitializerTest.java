package by.shved.userservice.config;

import by.shved.userservice.config.property.AdminProperties;
import by.shved.userservice.entity.UserEntity;
import by.shved.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminInitializerTest {
    private static final Long ADMIN_ID = 1L;
    private static final String ADMIN_NAME = "admin";
    private static final String ADMIN_SURNAME = "administrator";
    private static final String ADMIN_EMAIL = "admin@admin.com";

    @Mock
    private UserRepository userRepository;
    @Mock
    private ApplicationArguments applicationArguments;

    private AdminInitializer adminInitializer;

    @BeforeEach
    void setUp() {
        AdminProperties adminProperties = new AdminProperties(
                ADMIN_ID,
                ADMIN_NAME,
                ADMIN_SURNAME,
                ADMIN_EMAIL
        );
        adminInitializer = new AdminInitializer(userRepository, adminProperties);
    }

    @Test
    void run_whenAdminAlreadyExists_doesNotCreateAnotherAdmin() {
        UserEntity existingAdmin = new UserEntity();
        existingAdmin.setId(ADMIN_ID);
        when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.of(existingAdmin));

        assertDoesNotThrow(() -> adminInitializer.run(applicationArguments));

        verify(userRepository).findByEmail(ADMIN_EMAIL);
        verify(userRepository, never()).saveAndFlush(any());
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void run_whenEmailBelongsToAnotherUser_throwsException() {
        UserEntity existingUser = new UserEntity();
        existingUser.setId(2L);
        when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.of(existingUser));

        assertThrows(IllegalStateException.class, () -> adminInitializer.run(applicationArguments));

        verify(userRepository).findByEmail(ADMIN_EMAIL);
        verify(userRepository, never()).existsById(anyLong());
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void run_whenAdminIdIsAlreadyAssigned_throwsException() {
        when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.empty());
        when(userRepository.existsById(ADMIN_ID)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> adminInitializer.run(applicationArguments));

        verify(userRepository).findByEmail(ADMIN_EMAIL);
        verify(userRepository).existsById(ADMIN_ID);
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void run_whenAdminDoesNotExist_createsAdmin() {
        when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.empty());
        when(userRepository.existsById(ADMIN_ID)).thenReturn(false);
        UserEntity savedAdmin = new UserEntity();
        savedAdmin.setId(ADMIN_ID);
        when(userRepository.saveAndFlush(any(UserEntity.class))).thenReturn(savedAdmin);

        adminInitializer.run(applicationArguments);

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).saveAndFlush(captor.capture());
        UserEntity createdAdmin = captor.getValue();
        assertEquals(ADMIN_NAME, createdAdmin.getName());
        assertEquals(ADMIN_SURNAME, createdAdmin.getSurname());
        assertEquals(ADMIN_EMAIL, createdAdmin.getEmail());
        assertTrue(createdAdmin.isActive());
        verify(userRepository).findByEmail(ADMIN_EMAIL);
        verify(userRepository).existsById(ADMIN_ID);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void run_whenDatabaseReturnsUnexpectedId_throwsException() {
        when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.empty());
        when(userRepository.existsById(ADMIN_ID)).thenReturn(false);
        UserEntity savedAdmin = new UserEntity();
        savedAdmin.setId(2L);
        when(userRepository.saveAndFlush(any(UserEntity.class))).thenReturn(savedAdmin);

        assertThrows(IllegalStateException.class, () -> adminInitializer.run(applicationArguments));

        verify(userRepository).saveAndFlush(any(UserEntity.class));
    }
}