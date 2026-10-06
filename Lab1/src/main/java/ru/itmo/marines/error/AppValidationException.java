package ru.itmo.marines.error;

import java.util.List;

public class AppValidationException extends RuntimeException {

    private final List<FieldProblem> problems;

    public AppValidationException(List<FieldProblem> problems) {
        super("Некорректные данные");
        this.problems = problems;
    }

    public AppValidationException(String field, String message) {
        this(List.of(new FieldProblem(field, message)));
    }

    public List<FieldProblem> getProblems() {
        return problems;
    }
}
