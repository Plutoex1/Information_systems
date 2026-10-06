package ru.itmo.marines.service;

import org.springframework.stereotype.Component;
import ru.itmo.marines.dto.MarineRequest;
import ru.itmo.marines.error.AppValidationException;
import ru.itmo.marines.error.FieldProblem;
import ru.itmo.marines.model.AstartesCategory;
import ru.itmo.marines.model.Weapon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Проверка входных данных десантника с понятными сообщениями об ошибках. */
@Component
public class MarineValidator {

    public void validate(MarineRequest r) {
        if (r == null) {
            throw new AppValidationException("body", "Не переданы данные десантника");
        }
        List<FieldProblem> p = new ArrayList<>();

        if (r.name() == null || r.name().isBlank()) {
            p.add(new FieldProblem("name", "Имя не может быть пустым"));
        } else if (r.name().length() > 255) {
            p.add(new FieldProblem("name", "Имя не должно быть длиннее 255 символов"));
        }

        if (r.x() == null || r.x().isNaN() || r.x().isInfinite()) {
            p.add(new FieldProblem("x", "Координата X обязательна и должна быть числом"));
        } else if (!(r.x() > -634)) {
            p.add(new FieldProblem("x", "Координата X должна быть больше -634"));
        }

        if (r.y() == null || r.y().isNaN() || r.y().isInfinite()) {
            p.add(new FieldProblem("y", "Координата Y обязательна и должна быть числом"));
        } else if (!(r.y() > -126)) {
            p.add(new FieldProblem("y", "Координата Y должна быть больше -126"));
        }

        if (r.chapterId() == null) {
            p.add(new FieldProblem("chapterId", "Необходимо выбрать орден"));
        }

        if (r.health() == null) {
            p.add(new FieldProblem("health", "Здоровье обязательно"));
        } else if (r.health() <= 0) {
            p.add(new FieldProblem("health", "Здоровье должно быть больше 0"));
        }

        if (r.height() == null) {
            p.add(new FieldProblem("height", "Рост обязателен"));
        }

        if (r.category() == null || r.category().isBlank()) {
            p.add(new FieldProblem("category", "Категория обязательна"));
        } else if (Arrays.stream(AstartesCategory.values()).noneMatch(c -> c.name().equals(r.category()))) {
            p.add(new FieldProblem("category", "Недопустимая категория. Допустимо: "
                    + Arrays.toString(AstartesCategory.values())));
        }

        if (r.weaponType() == null || r.weaponType().isBlank()) {
            p.add(new FieldProblem("weaponType", "Оружие обязательно"));
        } else if (Arrays.stream(Weapon.values()).noneMatch(w -> w.name().equals(r.weaponType()))) {
            p.add(new FieldProblem("weaponType", "Недопустимое оружие. Допустимо: "
                    + Arrays.toString(Weapon.values())));
        }

        if (!p.isEmpty()) {
            throw new AppValidationException(p);
        }
    }
}
