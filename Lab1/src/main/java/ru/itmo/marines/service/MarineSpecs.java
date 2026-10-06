package ru.itmo.marines.service;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import ru.itmo.marines.dto.MarineFilter;
import ru.itmo.marines.model.SpaceMarine;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Фильтрация по неполному совпадению (LIKE %значение%) без учёта регистра. */
final class MarineSpecs {

    private MarineSpecs() {
    }

    static Specification<SpaceMarine> filter(MarineFilter f) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            like(cb, root.<String>get("name"), f.name(), ps);
            like(cb, root.get("chapter").<String>get("name"), f.chapterName(), ps);
            like(cb, root.get("chapter").<String>get("parentLegion"), f.parentLegion(), ps);
            like(cb, root.get("chapter").<String>get("world"), f.world(), ps);
            like(cb, root.get("category").as(String.class), f.category(), ps);
            like(cb, root.get("weaponType").as(String.class), f.weaponType(), ps);
            return cb.and(ps.toArray(new Predicate[0]));
        };
    }

    private static void like(CriteriaBuilder cb, Expression<String> expr, String value, List<Predicate> ps) {
        if (value == null || value.isBlank()) {
            return;
        }
        String escaped = value.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        ps.add(cb.like(cb.lower(expr), "%" + escaped + "%", '\\'));
    }
}
