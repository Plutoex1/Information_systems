package ru.itmo.marines.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.marines.model.AppUser;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);

    boolean existsByUsername(String username);
}
