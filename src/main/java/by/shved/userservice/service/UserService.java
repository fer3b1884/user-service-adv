package by.shved.userservice.service;

import by.shved.userservice.dto.user.UserRequest;
import by.shved.userservice.dto.user.UserResponse;
import by.shved.userservice.entity.UserEntity;
import by.shved.userservice.repository.specification.UserSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;


public interface UserService {
    UserResponse create(UserRequest request);
    UserResponse getById(Long id);
    Page<UserResponse> getAll(String name, String surname, Pageable pageable);
    UserResponse update(Long id, UserRequest request);
    UserResponse setActive(Long id, boolean active);
    void deleteById(Long id);
}
