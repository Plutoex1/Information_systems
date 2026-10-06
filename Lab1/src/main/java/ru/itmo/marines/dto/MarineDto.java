package ru.itmo.marines.dto;

import ru.itmo.marines.model.AstartesCategory;
import ru.itmo.marines.model.SpaceMarine;
import ru.itmo.marines.model.Weapon;

import java.util.Date;

public record MarineDto(
        Long id,
        String name,
        double x,
        float y,
        Date creationDate,
        ChapterDto chapter,
        long health,
        int height,
        AstartesCategory category,
        Weapon weaponType) {

    public static MarineDto from(SpaceMarine m) {
        return new MarineDto(
                m.getId(),
                m.getName(),
                m.getCoordinates().getX(),
                m.getCoordinates().getY(),
                m.getCreationDate(),
                ChapterDto.from(m.getChapter()),
                m.getHealth(),
                m.getHeight(),
                m.getCategory(),
                m.getWeaponType());
    }
}
