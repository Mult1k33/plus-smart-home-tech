package ru.yandex.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.model.Scenario;

import java.util.List;
import java.util.Optional;

public interface ScenarioRepository extends JpaRepository<Scenario,Long> {

    @Query("""
    SELECT DISTINCT s FROM Scenario s
    LEFT JOIN FETCH s.conditions sc
    LEFT JOIN FETCH sc.sensor sens_cond
    LEFT JOIN FETCH sc.condition cond
    LEFT JOIN FETCH s.actions sa  
    LEFT JOIN FETCH sa.sensor sens_act
    LEFT JOIN FETCH sa.action act
    WHERE s.hubId = :hubId
    """)
    List<Scenario> findByHubId(@Param("hubId") String hubId);

    Optional<Scenario> findByHubIdAndName(String hubId, String name);

    @Modifying
    @Transactional
    @Query("DELETE FROM ScenarioCondition sc WHERE sc.sensor.id = :sensorId")
    void deleteConditionsBySensorId(@Param("sensorId") String sensorId);

    @Modifying
    @Transactional
    @Query("DELETE FROM ScenarioAction sa WHERE sa.sensor.id = :sensorId")
    void deleteActionsBySensorId(@Param("sensorId") String sensorId);
}
