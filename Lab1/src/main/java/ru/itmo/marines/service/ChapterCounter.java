package ru.itmo.marines.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import ru.itmo.marines.error.ConflictException;
import ru.itmo.marines.event.ChangeEvent;
import ru.itmo.marines.model.Chapter;

import java.util.Optional;

/**
 * Единое место, где изменяется численность ордена (marinesCount) при добавлении,
 * удалении и переводе десантников. Вызывается только внутри транзакций сервисов.
 */
@Component
public class ChapterCounter {

    public static final int MAX_MARINES = 1000;

    @PersistenceContext
    private EntityManager em;

    private final ApplicationEventPublisher events;

    public ChapterCounter(ApplicationEventPublisher events) {
        this.events = events;
    }

    /**
     * Блокирует строку ордена на запись до конца транзакции (SELECT ... FOR UPDATE),
     * чтобы параллельные операции не потеряли изменения счётчика.
     */
    public Optional<Chapter> lock(Long id) {
        Chapter chapter = em.find(Chapter.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (chapter != null) {
            em.refresh(chapter, LockModeType.PESSIMISTIC_WRITE); // актуальное значение после блокировки
        }
        return Optional.ofNullable(chapter);
    }

    /** Изменяет численность ордена на delta, не выходя за допустимый диапазон 1..1000. */
    public void change(Chapter chapter, int delta) {
        if (delta == 0) {
            return;
        }
        int next = chapter.getMarinesCount() + delta;
        if (next > MAX_MARINES) {
            throw new ConflictException("Численность ордена «" + chapter.getName()
                    + "» не может превышать " + MAX_MARINES);
        }
        if (next < 1) {
            throw new ConflictException("Численность ордена «" + chapter.getName()
                    + "» не может быть меньше 1");
        }
        chapter.setMarinesCount(next);
        events.publishEvent(new ChangeEvent("chapter", "updated", chapter.getId()));
    }
}