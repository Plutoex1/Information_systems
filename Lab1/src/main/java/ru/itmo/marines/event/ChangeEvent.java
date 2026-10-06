package ru.itmo.marines.event;

/** Событие изменения данных: entity — marine/chapter, action — created/updated/deleted. */
public record ChangeEvent(String entity, String action, Long id) {
}
