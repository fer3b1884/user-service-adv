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
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserServiceImpl(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public UserResponse create(UserRequest request) {
        UserEntity user = userMapper.toEntity(request);
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new DuplicateResourceException("User with this email already exists");
        }
        UserEntity savedUser = userRepository.save(user);
        return userMapper.toResponse(savedUser);
    }

    @Override
    public UserResponse getById(Long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return userMapper.toResponse(user);
    }

    @Override
    public Page<UserResponse> getAll(String name, String surname, Pageable pageable) {
        Specification<UserEntity> specification = Specification
                .where(UserSpecification.hasName(name))
                .and(UserSpecification.hasSurname(surname));
        return userRepository.findAll(specification, pageable).map(userMapper::toResponse);
    }

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
        return userMapper.toResponse(updatedUser);
    }

    @Override
    @Transactional
    public UserResponse setActive(Long id, boolean active) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        user.setActive(active);
        UserEntity updatedUser = userRepository.save(user);
        return userMapper.toResponse(updatedUser);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        userRepository.delete(user);
    }
}
