package it.unicam.cs.awmc.webjtime.security;

import it.unicam.cs.awmc.webjtime.model.User;
import it.unicam.cs.awmc.webjtime.repository.UserRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepo;
    private final PasswordEncoder pwEncoder;

    public DataInitializer(UserRepository userRepo, PasswordEncoder pwEncoder) {
        this.userRepo = userRepo;
        this.pwEncoder = pwEncoder;
    }

    @Override
    public void run(String @NonNull ... args) {
        if(userRepo.findByUsername("admin").isEmpty()) {
            userRepo.save(new User("admin", pwEncoder.encode("admin")));
        }
    }

}
