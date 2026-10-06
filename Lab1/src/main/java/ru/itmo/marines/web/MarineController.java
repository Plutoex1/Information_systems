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
import ru.itmo.marines.dto.MarineDto;
import ru.itmo.marines.dto.MarineFilter;
import ru.itmo.marines.dto.MarineRequest;
import ru.itmo.marines.dto.PageDto;
import ru.itmo.marines.service.MarineService;

@RestController
@RequestMapping("/api/marines")
public class MarineController {

    private final MarineService service;

    public MarineController(MarineService service) {
        this.service = service;
    }

    @GetMapping
    public PageDto<MarineDto> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "asc") String dir,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String chapterName,
            @RequestParam(required = false) String parentLegion,
            @RequestParam(required = false) String world,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String weaponType) {
        MarineFilter filter = new MarineFilter(name, chapterName, parentLegion, world, category, weaponType);
        return service.list(filter, page, size, sort, dir);
    }

    @GetMapping("/{id}")
    public MarineDto get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MarineDto create(@RequestBody MarineRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public MarineDto update(@PathVariable Long id, @RequestBody MarineRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
