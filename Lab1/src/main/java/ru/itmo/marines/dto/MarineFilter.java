package ru.itmo.marines.dto;

/** Фильтры по строковым колонкам таблицы (неполное совпадение, без учёта регистра). */
public record MarineFilter(
        String name,
        String chapterName,
        String parentLegion,
        String world,
        String category,
        String weaponType) {
}
