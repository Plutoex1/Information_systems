package ru.itmo.marines.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.marines.model.Chapter;

public interface ChapterRepository extends JpaRepository<Chapter, Long> {
}
