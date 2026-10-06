package ru.itmo.marines.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import ru.itmo.marines.model.SpaceMarine;

import java.util.List;

public interface SpaceMarineRepository extends JpaRepository<SpaceMarine, Long>, JpaSpecificationExecutor<SpaceMarine> {

    List<SpaceMarine> findByChapterId(Long chapterId);
}
