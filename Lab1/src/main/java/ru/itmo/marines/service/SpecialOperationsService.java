package ru.itmo.marines.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.marines.dto.MarineDto;
import ru.itmo.marines.dto.MarineRequest;
import ru.itmo.marines.error.NotFoundException;
import ru.itmo.marines.model.AstartesCategory;
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

    private final SpaceMarineRepository marines;
    private final ChapterRepository chapters;
    private final MarineService marineService;

    public SpecialOperationsService(SpaceMarineRepository marines, ChapterRepository chapters,
                                    MarineService marineService) {
        this.marines = marines;
        this.chapters = chapters;
        this.marineService = marineService;
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

    /**
     * Добавить нового десантника в указанный орден. Создание и увеличение численности
     * ордена выполняются в {@link MarineService#create(MarineRequest)} в одной транзакции.
     */
    public MarineDto addMarineToChapter(Long chapterId, MarineRequest request) {
        if (!chapters.existsById(chapterId)) {
            throw new NotFoundException("Орден с id " + chapterId + " не найден");
        }
        MarineRequest inChapter = request == null ? null : new MarineRequest(
                request.name(), request.x(), request.y(), chapterId,
                request.health(), request.height(), request.category(), request.weaponType());
        return marineService.create(inChapter);
    }
}