package ru.itmo.marines.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.marines.dto.MarineDto;
import ru.itmo.marines.dto.MarineRequest;
import ru.itmo.marines.error.ConflictException;
import ru.itmo.marines.error.NotFoundException;
import ru.itmo.marines.event.ChangeEvent;
import ru.itmo.marines.model.AstartesCategory;
import ru.itmo.marines.model.Chapter;
import ru.itmo.marines.model.SpaceMarine;
import ru.itmo.marines.repository.ChapterRepository;
import ru.itmo.marines.repository.SpaceMarineRepository;

import java.util.Comparator;
import java.util.List;

/**
 * Специальные операции. Вся логика выполняется в Java (без функций и процедур БД):
 * данные читаются через репозитории, агрегаты считаются в коде.
 * Роспуск ордена реализован в {@link ChapterService#dissolve(Long, Long)}.
 */
@Service
@Transactional
public class SpecialOperationsService {

    private static final int MAX_CHAPTER_SIZE = 1000;

    private final SpaceMarineRepository marines;
    private final ChapterRepository chapters;
    private final MarineService marineService;
    private final ApplicationEventPublisher events;

    public SpecialOperationsService(SpaceMarineRepository marines, ChapterRepository chapters,
                                    MarineService marineService, ApplicationEventPublisher events) {
        this.marines = marines;
        this.chapters = chapters;
        this.marineService = marineService;
        this.events = events;
    }

    /** Один (любой) объект с минимальным значением weaponType (порядок констант enum). */
    @Transactional(readOnly = true)
    public MarineDto marineWithMinWeapon() {
        return marines.findAll().stream()
                .min(Comparator.comparing(SpaceMarine::getWeaponType))
                .map(MarineDto::from)
                .orElseThrow(() -> new NotFoundException("В системе нет ни одного десантника"));
    }

    /** Один (любой) объект с максимальным id. */
    @Transactional(readOnly = true)
    public MarineDto marineWithMaxId() {
        return marines.findAll().stream()
                .max(Comparator.comparing(SpaceMarine::getId))
                .map(MarineDto::from)
                .orElseThrow(() -> new NotFoundException("В системе нет ни одного десантника"));
    }

    /** Уникальные значения category по всем объектам. */
    @Transactional(readOnly = true)
    public List<AstartesCategory> distinctCategories() {
        return marines.findAll().stream()
                .map(SpaceMarine::getCategory)
                .distinct()
                .sorted()
                .toList();
    }

    /** Добавить нового десантника в указанный орден; численность ордена увеличивается на 1. */
    public MarineDto addMarineToChapter(Long chapterId, MarineRequest request) {
        Chapter chapter = chapters.findById(chapterId)
                .orElseThrow(() -> new NotFoundException("Орден с id " + chapterId + " не найден"));
        if (chapter.getMarinesCount() >= MAX_CHAPTER_SIZE) {
            throw new ConflictException("В ордене уже максимальная численность (" + MAX_CHAPTER_SIZE + ")");
        }
        MarineRequest inChapter = request == null ? null : new MarineRequest(
                request.name(), request.x(), request.y(), chapterId,
                request.health(), request.height(), request.category(), request.weaponType());

        MarineDto created = marineService.create(inChapter);

        chapter.setMarinesCount(chapter.getMarinesCount() + 1);
        chapters.saveAndFlush(chapter);
        events.publishEvent(new ChangeEvent("chapter", "updated", chapterId));
        return created;
    }
}
