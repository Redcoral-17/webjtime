package it.unicam.cs.awmc.webjtime.security;

import it.unicam.cs.awmc.webjtime.model.User;
import it.unicam.cs.awmc.webjtime.repository.UserRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {
    private final UserRepository uRepo;
    private final PasswordEncoder pwEncoder;
    @Value("${app.admin.username}")
    private String adminUsername;
    @Value("${app.admin.password}")
    private String adminPassword;

    public DataInitializer(UserRepository uRepo, PasswordEncoder pwEncoder) {
        this.uRepo = uRepo;
        this.pwEncoder = pwEncoder;
    }

    @Override
    public void run(String @NonNull ... args) {
        if (uRepo.findByUsername(adminUsername).isEmpty()) {
            uRepo.save(new User(adminUsername, pwEncoder.encode(adminPassword)));
        }
    }

}
