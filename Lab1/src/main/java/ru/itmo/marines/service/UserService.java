package ru.itmo.marines.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.marines.error.AppValidationException;
import ru.itmo.marines.error.ConflictException;
import ru.itmo.marines.error.FieldProblem;
import ru.itmo.marines.model.AppUser;
import ru.itmo.marines.repository.AppUserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
@Transactional
public class UserService {

    private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9_.-]{3,32}");

    private final AppUserRepository users;
    private final PasswordEncoder encoder;

    public UserService(AppUserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    public void register(String username, String password) {
        List<FieldProblem> p = new ArrayList<>();
        if (username == null || !USERNAME.matcher(username).matches()) {
            p.add(new FieldProblem("username", "Логин: 3–32 символа, латиница, цифры, «_», «.», «-»"));
        }
        if (password == null || password.length() < 4 || password.length() > 72) {
            p.add(new FieldProblem("password", "Пароль должен содержать от 4 до 72 символов"));
        }
        if (!p.isEmpty()) {
            throw new AppValidationException(p);
        }
        if (users.existsByUsername(username)) {
            throw new ConflictException("Пользователь с таким логином уже существует");
        }
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPassword(encoder.encode(password));
        users.save(user);
    }
}
