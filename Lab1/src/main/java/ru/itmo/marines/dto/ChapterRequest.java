package ru.itmo.marines.dto;

public record ChapterRequest(String name, String parentLegion, Integer marinesCount, String world) {
}
