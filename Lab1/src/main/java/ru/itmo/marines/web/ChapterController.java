package ru.itmo.marines.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.marines.dto.ChapterDetailsDto;
import ru.itmo.marines.dto.ChapterDto;
import ru.itmo.marines.dto.ChapterRequest;
import ru.itmo.marines.service.ChapterService;

import java.util.List;

@RestController
@RequestMapping("/api/chapters")
public class ChapterController {

    private final ChapterService service;

    public ChapterController(ChapterService service) {
        this.service = service;
    }

    @GetMapping
    public List<ChapterDto> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public ChapterDetailsDto details(@PathVariable Long id) {
        return service.details(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ChapterDto create(@RequestBody ChapterRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public ChapterDto update(@PathVariable Long id, @RequestBody ChapterRequest request) {
        return service.update(id, request);
    }

    /** Роспуск ордена: связанные десантники переводятся в орден reassignTo. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void dissolve(@PathVariable Long id, @RequestParam(required = false) Long reassignTo) {
        service.dissolve(id, reassignTo);
    }
}
