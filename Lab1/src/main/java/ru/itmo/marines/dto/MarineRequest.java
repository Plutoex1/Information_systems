package ru.itmo.marines.dto;

/** Данные для создания/изменения десантника. Типы-обёртки нужны, чтобы отличать «не задано» от нуля. */
public record MarineRequest(
        String name,
        Double x,
        Float y,
        Long chapterId,
        Long health,
        Integer height,
        String category,
        String weaponType) {
}
