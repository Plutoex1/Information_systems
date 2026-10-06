package ru.itmo.marines.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class Coordinates {

    @Column(name = "coord_x", nullable = false)
    private double x; // Значение поля должно быть больше -634

    @Column(name = "coord_y", nullable = false)
    private float y; // Значение поля должно быть больше -126

    public Coordinates() {
    }

    public Coordinates(double x, float y) {
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }
}
