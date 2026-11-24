package by.shved.userservice.security.access;

import by.shved.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("userAccess")
@RequiredArgsConstructor
public class UserAccess {
    private final UserRepository userRepository;

    public boolean isActive(Object principal) {
        if (!(principal instanceof Long userId)) {
            return false;
        }
        return userRepository.findActiveById(userId).orElse(false);
    }
}
