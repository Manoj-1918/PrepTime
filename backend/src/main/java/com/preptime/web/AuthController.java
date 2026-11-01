package com.preptime.web;

import com.preptime.model.*;
import com.preptime.repo.*;
import java.util.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AuthController {
    record Reg(String name, String email, String password) {}
    record Login(String email, String password) {}

    private final UserRepo users;
    private final TokenRepo tokens;
    private final PasswordEncoder enc;

    public AuthController(UserRepo users, TokenRepo tokens, PasswordEncoder enc) {
        this.users = users; this.tokens = tokens; this.enc = enc;
    }

    @GetMapping("/health")
    public Map<String, String> health() { return Map.of("status", "ok"); }

    @PostMapping("/auth/register")
    public Map<String, Object> register(@RequestBody Reg r) {
        String email = r.email() == null ? "" : r.email().trim().toLowerCase();
        if (r.name() == null || r.name().isBlank()) throw new ApiException(400, "Enter your name");
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) throw new ApiException(400, "Enter a valid email address");
        if (r.password() == null || r.password().length() < 6) throw new ApiException(400, "Password must be at least 6 characters");
        if (users.findByEmail(email).isPresent()) throw new ApiException(409, "This email is already registered");
        User u = new User();
        u.name = r.name().trim();
        u.email = email;
        u.passwordHash = enc.encode(r.password());
        users.save(u);
        return session(u);
    }

    @PostMapping("/auth/login")
    public Map<String, Object> login(@RequestBody Login r) {
        String email = r.email() == null ? "" : r.email().trim().toLowerCase();
        User u = users.findByEmail(email).orElse(null);
        if (u == null || r.password() == null || !enc.matches(r.password(), u.passwordHash))
            throw new ApiException(401, "Wrong email or password");
        return session(u);
    }

    @GetMapping("/auth/me")
    public Map<String, Object> me(@RequestAttribute("userId") Long uid) {
        User u = users.findById(uid).orElseThrow(() -> new ApiException(401, "Please log in"));
        return Map.<String, Object>of("id", u.id, "name", u.name, "email", u.email);
    }

    private Map<String, Object> session(User u) {
        AuthToken t = new AuthToken();
        t.token = UUID.randomUUID().toString();
        t.userId = u.id;
        tokens.save(t);
        return Map.<String, Object>of("token", t.token,
                "user", Map.<String, Object>of("id", u.id, "name", u.name, "email", u.email));
    }
}
