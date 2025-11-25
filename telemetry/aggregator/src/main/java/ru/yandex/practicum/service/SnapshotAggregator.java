package ru.yandex.practicum.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorStateAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class SnapshotAggregator {

    private final Map<String, SensorsSnapshotAvro> snapshots = new HashMap<>();

    public Optional<SensorsSnapshotAvro> updateState(SensorEventAvro event) {
        String hubId = event.getHubId();
        String sensorId = event.getId();

        log.debug("Processing event for sensor:{} in hub:{}", sensorId, hubId);

        SensorsSnapshotAvro snapshot = snapshots.computeIfAbsent(hubId, id -> {
            log.debug("Creating new snapshot for hub:{}", hubId);
            return SensorsSnapshotAvro.newBuilder()
                    .setHubId(hubId)
                    .setTimestamp(event.getTimestamp())
                    .setSensorsState(new HashMap<>())
                    .build();
        });

        Map<String, SensorStateAvro> stateMap = new HashMap<>(snapshot.getSensorsState());

        SensorStateAvro oldState = stateMap.get(sensorId);
        Instant eventTimestamp = event.getTimestamp();

        if (oldState != null) {
            Instant oldTimestamp = oldState.getTimestamp();

            if (eventTimestamp.isBefore(oldTimestamp)) {
                log.debug("Skipping outdated event for sensor{}: {} < {}", sensorId, eventTimestamp, oldTimestamp);
                return Optional.empty();
            }

            if (oldState.getData().equals(event.getPayload())) {
                log.debug("No changes for sensor{}: data unchanged", sensorId);
                return Optional.empty();
            }
        }

        SensorStateAvro newState = SensorStateAvro.newBuilder()
                .setTimestamp(eventTimestamp)
                .setData(event.getPayload())
                .build();

        stateMap.put(sensorId, newState);

        SensorsSnapshotAvro updateSnapshot = SensorsSnapshotAvro.newBuilder(snapshot)
                .setSensorsState(stateMap)
                .setTimestamp(eventTimestamp)
                .build();

        snapshots.put(hubId, updateSnapshot);

        log.info("Snapshot updated - hub:{}, sensor:{}", hubId, sensorId);

        return Optional.of(updateSnapshot);
    }
}
