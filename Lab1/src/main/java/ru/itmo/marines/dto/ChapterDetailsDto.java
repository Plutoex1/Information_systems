package ru.itmo.marines.dto;

import java.util.List;

/** Орден вместе со связанными с ним десантниками. */
public record ChapterDetailsDto(ChapterDto chapter, List<MarineDto> marines) {
}
