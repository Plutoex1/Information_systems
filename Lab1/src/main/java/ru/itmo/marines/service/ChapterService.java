package ru.itmo.marines.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.marines.dto.ChapterDetailsDto;
import ru.itmo.marines.dto.ChapterDto;
import ru.itmo.marines.dto.ChapterRequest;
import ru.itmo.marines.dto.MarineDto;
import ru.itmo.marines.error.AppValidationException;
import ru.itmo.marines.error.ConflictException;
import ru.itmo.marines.error.NotFoundException;
import ru.itmo.marines.event.ChangeEvent;
import ru.itmo.marines.model.Chapter;
import ru.itmo.marines.model.SpaceMarine;
import ru.itmo.marines.repository.ChapterRepository;
import ru.itmo.marines.repository.SpaceMarineRepository;

import java.util.List;

@Service
@Transactional
public class ChapterService {

    private final ChapterRepository chapters;
    private final SpaceMarineRepository marines;
    private final ChapterValidator validator;
    private final ChapterCounter counter;
    private final ApplicationEventPublisher events;

    public ChapterService(ChapterRepository chapters, SpaceMarineRepository marines,
                          ChapterValidator validator, ChapterCounter counter,
                          ApplicationEventPublisher events) {
        this.chapters = chapters;
        this.marines = marines;
        this.validator = validator;
        this.counter = counter;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public List<ChapterDto> list() {
        return chapters.findAll(Sort.by("id")).stream().map(ChapterDto::from).toList();
    }

    /** Орден вместе со связанными десантниками. */
    @Transactional(readOnly = true)
    public ChapterDetailsDto details(Long id) {
        Chapter chapter = find(id);
        List<MarineDto> members = marines.findByChapterId(id).stream().map(MarineDto::from).toList();
        return new ChapterDetailsDto(ChapterDto.from(chapter), members);
    }

    public ChapterDto create(ChapterRequest request) {
        validator.validate(request);
        Chapter chapter = new Chapter();
        apply(chapter, request);
        chapter = chapters.saveAndFlush(chapter);
        events.publishEvent(new ChangeEvent("chapter", "created", chapter.getId()));
        return ChapterDto.from(chapter);
    }

    public ChapterDto update(Long id, ChapterRequest request) {
        Chapter chapter = find(id);
        validator.validate(request);
        apply(chapter, request);
        chapter = chapters.saveAndFlush(chapter);
        events.publishEvent(new ChangeEvent("chapter", "updated", chapter.getId()));
        return ChapterDto.from(chapter);
    }

    /**
     * Роспуск (удаление) ордена. Если с орденом связаны десантники, их необходимо
     * перевести в другой орден, выбранный пользователем (reassignTo);
     * численность целевого ордена увеличивается на число переведённых десантников.
     */
    public void dissolve(Long id, Long reassignTo) {
        // блокируем ордена в порядке возрастания id, чтобы избежать взаимной блокировки
        Chapter source = null;
        Chapter target = null;
        boolean needTarget = reassignTo != null && !reassignTo.equals(id);
        List<Long> order = needTarget
                ? List.of(Math.min(id, reassignTo), Math.max(id, reassignTo))
                : List.of(id);
        for (Long chapterId : order) {
            Chapter locked = counter.lock(chapterId).orElse(null);
            if (chapterId.equals(id)) {
                source = locked;
            } else {
                target = locked;
            }
        }
        if (source == null) {
            throw new NotFoundException("Орден с id " + id + " не найден");
        }

        List<SpaceMarine> members = marines.findByChapterId(id);
        if (!members.isEmpty()) {
            if (reassignTo == null) {
                throw new ConflictException("С орденом связано десантников: " + members.size()
                        + ". Выберите орден, в который их нужно перевести");
            }
            if (reassignTo.equals(id)) {
                throw new AppValidationException("reassignTo", "Нельзя перевести десантников в распускаемый орден");
            }
            if (target == null) {
                throw new AppValidationException("reassignTo", "Орден с id " + reassignTo + " не найден");
            }
            counter.change(target, members.size());
            Chapter newChapter = target;
            members.forEach(m -> m.setChapter(newChapter));
            marines.saveAllAndFlush(members);
        }

        chapters.delete(source);
        chapters.flush();

        if (!members.isEmpty()) {
            events.publishEvent(new ChangeEvent("marine", "updated", null));
        }
        events.publishEvent(new ChangeEvent("chapter", "deleted", id));
    }

    private Chapter find(Long id) {
        return chapters.findById(id)
                .orElseThrow(() -> new NotFoundException("Орден с id " + id + " не найден"));
    }

    private void apply(Chapter chapter, ChapterRequest r) {
        chapter.setName(r.name().trim());
        String legion = r.parentLegion() == null ? null : r.parentLegion().trim();
        chapter.setParentLegion(legion == null || legion.isEmpty() ? null : legion);
        chapter.setMarinesCount(r.marinesCount());
        chapter.setWorld(r.world().trim());
    }
}