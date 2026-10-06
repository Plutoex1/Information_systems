package ru.itmo.marines.web;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import ru.itmo.marines.event.EventHub;

/** Поток событий об изменениях для синхронизации интерфейсов всех пользователей. */
@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventHub hub;

    public EventController(EventHub hub) {
        this.hub = hub;
    }

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe() {
        return hub.subscribe();
    }
}
