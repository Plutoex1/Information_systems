package ru.itmo.marines.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.marines.dto.MarineDto;
import ru.itmo.marines.dto.MarineFilter;
import ru.itmo.marines.dto.MarineRequest;
import ru.itmo.marines.dto.PageDto;
import ru.itmo.marines.error.AppValidationException;
import ru.itmo.marines.error.NotFoundException;
import ru.itmo.marines.event.ChangeEvent;
import ru.itmo.marines.model.AstartesCategory;
import ru.itmo.marines.model.Chapter;
import ru.itmo.marines.model.Coordinates;
import ru.itmo.marines.model.SpaceMarine;
import ru.itmo.marines.model.Weapon;
import ru.itmo.marines.repository.SpaceMarineRepository;

import java.util.Map;

@Service
@Transactional
public class MarineService {

    /** Допустимые колонки сортировки: ключ из UI -> путь в сущности. */
    private static final Map<String, String> SORT_FIELDS = Map.ofEntries(
            Map.entry("id", "id"),
            Map.entry("name", "name"),
            Map.entry("x", "coordinates.x"),
            Map.entry("y", "coordinates.y"),
            Map.entry("creationDate", "creationDate"),
            Map.entry("chapterName", "chapter.name"),
            Map.entry("parentLegion", "chapter.parentLegion"),
            Map.entry("marinesCount", "chapter.marinesCount"),
            Map.entry("world", "chapter.world"),
            Map.entry("health", "health"),
            Map.entry("height", "height"),
            Map.entry("category", "category"),
            Map.entry("weaponType", "weaponType"));

    private final SpaceMarineRepository marines;
    private final ChapterCounter counter;
    private final MarineValidator validator;
    private final ApplicationEventPublisher events;

    public MarineService(SpaceMarineRepository marines, ChapterCounter counter,
                         MarineValidator validator, ApplicationEventPublisher events) {
        this.marines = marines;
        this.counter = counter;
        this.validator = validator;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public PageDto<MarineDto> list(MarineFilter filter, int page, int size, String sort, String dir) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        String property = SORT_FIELDS.getOrDefault(sort, "id");
        Sort.Direction direction = "desc".equalsIgnoreCase(dir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort order = Sort.by(direction, property);
        if (!property.equals("id")) {
            order = order.and(Sort.by(Sort.Direction.ASC, "id")); // стабильная пагинация
        }
        Page<SpaceMarine> result = marines.findAll(MarineSpecs.filter(filter), PageRequest.of(safePage, safeSize, order));
        return new PageDto<>(
                result.getContent().stream().map(MarineDto::from).toList(),
                result.getTotalElements(), result.getNumber(), result.getSize(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public MarineDto get(Long id) {
        return MarineDto.from(find(id));
    }

    /** Создание десантника: численность выбранного ордена увеличивается на 1. */
    public MarineDto create(MarineRequest request) {
        validator.validate(request);
        Chapter chapter = lockChapter(request.chapterId());
        counter.change(chapter, +1);

        SpaceMarine marine = new SpaceMarine();
        apply(marine, request, chapter);
        marine = marines.saveAndFlush(marine);
        events.publishEvent(new ChangeEvent("marine", "created", marine.getId()));
        return MarineDto.from(marine);
    }

    /** Изменение десантника; при смене ордена численность старого уменьшается, нового увеличивается. */
    public MarineDto update(Long id, MarineRequest request) {
        SpaceMarine marine = find(id);
        validator.validate(request);

        Long oldId = marine.getChapter().getId();
        Long newId = request.chapterId();
        Chapter target;
        if (oldId.equals(newId)) {
            target = marine.getChapter();
        } else {
            // блокируем оба ордена в порядке возрастания id, чтобы избежать взаимной блокировки
            Chapter first = lockChapter(Math.min(oldId, newId));
            Chapter second = lockChapter(Math.max(oldId, newId));
            Chapter old = first.getId().equals(oldId) ? first : second;
            target = first.getId().equals(newId) ? first : second;
            counter.change(old, -1);
            counter.change(target, +1);
        }

        apply(marine, request, target);
        marine = marines.saveAndFlush(marine);
        events.publishEvent(new ChangeEvent("marine", "updated", marine.getId()));
        return MarineDto.from(marine);
    }

    /** Удаление десантника: численность его ордена уменьшается на 1. */
    public void delete(Long id) {
        SpaceMarine marine = find(id);
        Chapter chapter = lockChapter(marine.getChapter().getId());
        counter.change(chapter, -1);

        marines.delete(marine);
        marines.flush();
        events.publishEvent(new ChangeEvent("marine", "deleted", id));
    }

    private SpaceMarine find(Long id) {
        return marines.findById(id)
                .orElseThrow(() -> new NotFoundException("Десантник с id " + id + " не найден"));
    }

    private Chapter lockChapter(Long id) {
        return counter.lock(id)
                .orElseThrow(() -> new AppValidationException("chapterId", "Орден с id " + id + " не найден"));
    }

    private void apply(SpaceMarine m, MarineRequest r, Chapter chapter) {
        m.setName(r.name().trim());
        m.setCoordinates(new Coordinates(r.x(), r.y()));
        m.setChapter(chapter);
        m.setHealth(r.health());
        m.setHeight(r.height());
        m.setCategory(AstartesCategory.valueOf(r.category()));
        m.setWeaponType(Weapon.valueOf(r.weaponType()));
    }
}