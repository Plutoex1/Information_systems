package ru.itmo.marines.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "chapter")
public class Chapter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank // Поле не может быть null, строка не может быть пустой
    @Column(nullable = false)
    private String name;

    @Column(name = "parent_legion")
    private String parentLegion;

    @NotNull
    @Min(1)
    @Max(1000) // Больше 0, максимум 1000
    @Column(name = "marines_count", nullable = false)
    private Integer marinesCount;

    @NotNull // Поле не может быть null
    @Column(nullable = false)
    private String world;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getParentLegion() {
        return parentLegion;
    }

    public void setParentLegion(String parentLegion) {
        this.parentLegion = parentLegion;
    }

    public Integer getMarinesCount() {
        return marinesCount;
    }

    public void setMarinesCount(Integer marinesCount) {
        this.marinesCount = marinesCount;
    }

    public String getWorld() {
        return world;
    }

    public void setWorld(String world) {
        this.world = world;
    }
}
