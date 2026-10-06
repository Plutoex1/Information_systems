package ru.itmo.marines.service;

import org.springframework.stereotype.Component;
import ru.itmo.marines.dto.ChapterRequest;
import ru.itmo.marines.error.AppValidationException;
import ru.itmo.marines.error.FieldProblem;

import java.util.ArrayList;
import java.util.List;

@Component
public class ChapterValidator {

    public void validate(ChapterRequest r) {
        if (r == null) {
            throw new AppValidationException("body", "Не переданы данные ордена");
        }
        List<FieldProblem> p = new ArrayList<>();

        if (r.name() == null || r.name().isBlank()) {
            p.add(new FieldProblem("name", "Название ордена не может быть пустым"));
        } else if (r.name().length() > 255) {
            p.add(new FieldProblem("name", "Название не должно быть длиннее 255 символов"));
        }

        if (r.parentLegion() != null && r.parentLegion().length() > 255) {
            p.add(new FieldProblem("parentLegion", "Название легиона не должно быть длиннее 255 символов"));
        }

        if (r.marinesCount() == null) {
            p.add(new FieldProblem("marinesCount", "Численность ордена обязательна"));
        } else if (r.marinesCount() <= 0) {
            p.add(new FieldProblem("marinesCount", "Численность должна быть больше 0"));
        } else if (r.marinesCount() > 1000) {
            p.add(new FieldProblem("marinesCount", "Численность не может превышать 1000"));
        }

        if (r.world() == null) {
            p.add(new FieldProblem("world", "Мир обязателен"));
        } else if (r.world().length() > 255) {
            p.add(new FieldProblem("world", "Название мира не должно быть длиннее 255 символов"));
        }

        if (!p.isEmpty()) {
            throw new AppValidationException(p);
        }
    }
}
