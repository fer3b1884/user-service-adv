package by.shved.userservice.service.impl;

import by.shved.userservice.dto.user.UserRequest;
import by.shved.userservice.dto.user.UserResponse;
import by.shved.userservice.entity.UserEntity;
import by.shved.userservice.exception.DuplicateResourceException;
import by.shved.userservice.exception.ResourceNotFoundException;
import by.shved.userservice.mapper.UserMapper;
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
class UserServiceImplTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @InjectMocks
    private UserServiceImpl userService;

    private UserEntity user;
    private UserRequest request;
    private UserResponse response;

    @BeforeEach
    void setUp() {
        user = new UserEntity();
        user.setId(1L);
        user.setName("Ivan");
        user.setSurname("Ivanov");
        user.setBirthDate(LocalDate.of(2000, 2, 20));
        user.setEmail("ivan.ivanov@example.com");
        user.setActive(true);

        request = new UserRequest(
                "Ivan",
                "Ivanov",
                LocalDate.of(2000, 2, 20),
                "ivan.ivanov@example.com"
        );

        response = new UserResponse(
                1L,
                "Ivan",
                "Ivanov",
                LocalDate.of(2000, 2, 20),
                "ivan.ivanov@example.com",
                true
        );
    }

    @Test
    void create_whenEmailIsUnique_returnsCreatedUser() {
        when(userMapper.toEntity(request)).thenReturn(user);
        when(userRepository.existsByEmail(user.getEmail())).thenReturn(false);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponse result = userService.create(request);

        assertEquals(response, result);
        verify(userRepository).save(user);
    }

    @Test
    void create_whenEmailAlreadyExists_throwsException() {
        when(userMapper.toEntity(request)).thenReturn(user);
        when(userRepository.existsByEmail(user.getEmail())).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> userService.create(request));

        verify(userRepository, never()).save(any(UserEntity.class));
        verify(userMapper, never()).toResponse(any(UserEntity.class));
    }

    @Test
    void getById_whenUserExists_returnsUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponse result = userService.getById(1L);

        assertEquals(response, result);
        verify(userRepository).findById(1L);
    }

    @Test
    void getById_whenUserDoesNotExist_throwsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getById(1L));

        verify(userMapper, never()).toResponse(any(UserEntity.class));
    }

    @Test
    void getAll_returnsMappedPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<UserEntity> entityPage = new PageImpl<>(List.of(user));
        when(userRepository.findAll(ArgumentMatchers.<Specification<UserEntity>>any(), eq(pageable))).thenReturn(entityPage);
        when(userMapper.toResponse(user)).thenReturn(response);

        Page<UserResponse> result = userService.getAll("Ivan", "Ivanov", pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(response, result.getContent().getFirst());
        verify(userRepository).findAll(ArgumentMatchers.<Specification<UserEntity>>any(), eq(pageable));
    }

    @Test
    void update_whenUserExists_updatesUser() {
        UserRequest updatedRequest = new UserRequest(
                "Petr",
                "Petrov",
                LocalDate.of(1995, 4, 10),
                "petr.petrov@example.com"
        );
        UserResponse updatedResponse = new UserResponse(
                1L,
                "Petr",
                "Petrov",
                LocalDate.of(1995, 4, 10),
                "petr.petrov@example.com",
                true
        );
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(updatedResponse);

        UserResponse result = userService.update(1L, updatedRequest);

        assertEquals(updatedResponse, result);
        assertEquals(updatedRequest.name(), user.getName());
        assertEquals(updatedRequest.surname(), user.getSurname());
        assertEquals(updatedRequest.birthDate(), user.getBirthDate());
        assertEquals(updatedRequest.email(), user.getEmail());
        verify(userRepository).save(user);
    }

    @Test
    void update_whenUserDoesNotExist_throwsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.update(1L, request));

        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    void setActive_whenUserExists_updatesStatus() {
        UserResponse inactiveResponse = new UserResponse(
                1L,
                "Ivan",
                "Ivanov",
                LocalDate.of(2000, 2, 20),
                "ivan.ivanov@example.com",
                false
        );
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(inactiveResponse);

        UserResponse result = userService.setActive(1L, false);

        assertFalse(user.isActive());
        assertEquals(inactiveResponse, result);
        verify(userRepository).save(user);
    }

    @Test
    void setActive_whenUserDoesNotExist_throwsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.setActive(1L, false));

        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    void deleteById_whenUserExists_deletesUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.deleteById(1L);

        verify(userRepository).delete(user);
    }

    @Test
    void deleteById_whenUserDoesNotExist_throwsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.deleteById(1L));

        verify(userRepository, never()).delete(any(UserEntity.class));
    }

    @AfterEach
    void tearDown() {
    }
}