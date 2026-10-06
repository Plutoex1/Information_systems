package ru.itmo.marines.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.marines.dto.MarineDto;
import ru.itmo.marines.dto.MarineRequest;
import ru.itmo.marines.model.AstartesCategory;
import ru.itmo.marines.service.SpecialOperationsService;

import java.util.List;

@RestController
@RequestMapping("/api/special")
public class SpecialController {

    private final SpecialOperationsService service;

    public SpecialController(SpecialOperationsService service) {
        this.service = service;
    }

    @GetMapping("/min-weapon")
    public MarineDto minWeapon() {
        return service.marineWithMinWeapon();
    }

    @GetMapping("/max-id")
    public MarineDto maxId() {
        return service.marineWithMaxId();
    }

    @GetMapping("/categories")
    public List<AstartesCategory> categories() {
        return service.distinctCategories();
    }

    @PostMapping("/chapters/{chapterId}/marines")
    @ResponseStatus(HttpStatus.CREATED)
    public MarineDto addToChapter(@PathVariable Long chapterId, @RequestBody MarineRequest request) {
        return service.addMarineToChapter(chapterId, request);
    }
}
