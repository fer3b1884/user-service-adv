package by.shved.userservice.service.impl;

import by.shved.userservice.dto.user.UserRequest;
import by.shved.userservice.dto.user.UserResponse;
import by.shved.userservice.entity.UserEntity;
import by.shved.userservice.exception.DuplicateResourceException;
import by.shved.userservice.exception.ResourceNotFoundException;
import by.shved.userservice.mapper.UserMapper;
import by.shved.userservice.repository.UserRepository;
import by.shved.userservice.repository.specification.UserSpecification;
import by.shved.userservice.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public UserResponse create(UserRequest request) {
        UserEntity user = userMapper.toEntity(request);
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new DuplicateResourceException("User with this email already exists");
        }
        UserEntity savedUser = userRepository.save(user);
        log.info("User created: userId={}", savedUser.getId());
        return userMapper.toResponse(savedUser);
    }

    @Cacheable(cacheNames = "users", key = "#id")
    @Override
    public UserResponse getById(Long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        log.debug("Retrieving user: userId={}", id);
        return userMapper.toResponse(user);
    }

    @Override
    public Page<UserResponse> getAll(String name, String surname, Pageable pageable) {
        Specification<UserEntity> specification = Specification
                .where(UserSpecification.hasName(name))
                .and(UserSpecification.hasSurname(surname));
        return userRepository.findAll(specification, pageable).map(userMapper::toResponse);
    }

    @CacheEvict(cacheNames = "users", key = "#id")
    @Override
    @Transactional
    public UserResponse update(Long id, UserRequest request) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        user.setName(request.name());
        user.setSurname(request.surname());
        user.setBirthDate(request.birthDate());
        user.setEmail(request.email());
        UserEntity updatedUser = userRepository.save(user);
        log.info("User updated: userId={}", updatedUser.getId());
        return userMapper.toResponse(updatedUser);
    }

    @CacheEvict(cacheNames = "users", key = "#id")
    @Override
    @Transactional
    public UserResponse setActive(Long id, boolean active) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        user.setActive(active);
        UserEntity updatedUser = userRepository.save(user);
        log.info("User active status changed: userId={}, active={}", updatedUser.getId(), active);
        return userMapper.toResponse(updatedUser);
    }

    @Caching (evict = {
            @CacheEvict(cacheNames = "users", key = "#id"),
            @CacheEvict(cacheNames = "payment_cards", allEntries = true)
    })
    @Override
    @Transactional
    public void deleteById(Long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        log.info("User deleted: userId={}", id);
        userRepository.delete(user);
    }
}
