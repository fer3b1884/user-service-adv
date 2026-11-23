package by.shved.userservice.config;

import by.shved.userservice.config.property.AdminProperties;
import by.shved.userservice.entity.UserEntity;
import by.shved.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {
    private final UserRepository userRepository;
    private final AdminProperties adminProperties;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        var existingByEmail = userRepository.findByEmail(adminProperties.email());
        if (existingByEmail.isPresent()) {
            UserEntity existingAdmin = existingByEmail.get();
            if (!existingAdmin.getId().equals(adminProperties.userId())) {
                throw new IllegalStateException("Configured admin email belongs to another user ID");
            }
            log.info("Admin profile already exists: userId={}", existingAdmin.getId());
            return;
        }
        if (userRepository.existsById(adminProperties.userId())) {
            throw new IllegalStateException("Configured admin user ID is already assigned to another user");
        }

        UserEntity admin = new UserEntity();
        admin.setName(adminProperties.name());
        admin.setSurname(adminProperties.surname());
        admin.setEmail(adminProperties.email());
        admin.setActive(true);

        UserEntity savedAdmin = userRepository.saveAndFlush(admin);
        if (!savedAdmin.getId().equals(adminProperties.userId())) {
            throw new IllegalStateException("Admin profile received unexpected ID: " + savedAdmin.getId());
        }
        log.info("Admin profile initialized: userId={}", savedAdmin.getId());
    }
}
