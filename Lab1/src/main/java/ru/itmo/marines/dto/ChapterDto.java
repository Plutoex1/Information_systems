package ru.itmo.marines.dto;

import ru.itmo.marines.model.Chapter;

public record ChapterDto(Long id, String name, String parentLegion, Integer marinesCount, String world) {

    public static ChapterDto from(Chapter c) {
        return new ChapterDto(c.getId(), c.getName(), c.getParentLegion(), c.getMarinesCount(), c.getWorld());
    }
}
