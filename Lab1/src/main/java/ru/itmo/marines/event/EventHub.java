package ru.itmo.marines.event;

import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Рассылает события об изменениях всем подключённым клиентам (Server-Sent Events). */
@Component
public class EventHub {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        return emitter;
    }

    /** Рассылка выполняется только после успешного коммита транзакции. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChange(ChangeEvent event) {
        broadcast(SseEmitter.event().name("change").data(event, MediaType.APPLICATION_JSON));
    }

    @Scheduled(fixedRate = 25_000)
    public void heartbeat() {
        broadcast(SseEmitter.event().comment("ping"));
    }

    private synchronized void broadcast(SseEmitter.SseEventBuilder message) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(message);
            } catch (IOException | IllegalStateException e) {
                emitters.remove(emitter);
            }
        }
    }
}
