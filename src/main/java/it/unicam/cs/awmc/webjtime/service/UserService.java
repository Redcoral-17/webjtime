package it.unicam.cs.awmc.webjtime.service;

import it.unicam.cs.awmc.webjtime.model.User;
import it.unicam.cs.awmc.webjtime.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository uRepo;
    private final PasswordEncoder pwEncoder;

    public UserService(UserRepository uRepo, PasswordEncoder pwEncoder) {
        this.uRepo = uRepo;
        this.pwEncoder = pwEncoder;
    }

    @Transactional
    public void register(String u, String pw) {
        if (u == null || u.isBlank()) throw new IllegalArgumentException("Username is required");
        if (pw == null || pw.isBlank()) throw new IllegalArgumentException("Password is required");
        if (uRepo.findByUsername(u).isPresent())
            throw new IllegalArgumentException("Username " + u + " is already taken");
        uRepo.save(new User(u, pwEncoder.encode(pw)));
    }
}

