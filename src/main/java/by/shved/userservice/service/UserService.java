package by.shved.userservice.service;

import by.shved.userservice.dto.user.UserRequest;
import by.shved.userservice.dto.user.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
    UserResponse create(UserRequest request);
    UserResponse getById(Long id);
    Page<UserResponse> getAll(String name, String surname, Pageable pageable);
    UserResponse update(Long id, UserRequest request);
    UserResponse setActive(Long id, boolean active);
    void deleteById(Long id);
}
