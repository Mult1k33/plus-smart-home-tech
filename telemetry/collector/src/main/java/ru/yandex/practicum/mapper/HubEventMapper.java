package ru.yandex.practicum.mapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.model.hub.*;

import java.util.stream.Collectors;

@Slf4j
@Component
public class HubEventMapper {
    public HubEventAvro toAvro(HubEvent event) {
        log.info("Mapping HubEvent type: {}", event.getType());
        HubEventAvro.Builder builder = HubEventAvro.newBuilder()
                .setHubId(event.getHubId())
                .setTimestamp(event.getTimestamp());

        switch (event.getType()) {
            case DEVICE_ADDED -> {
                DeviceAddedEvent added = (DeviceAddedEvent) event;
                log.debug("Mapping DEVICE_ADDED event - id: {}, deviceType: {}", added.getId(), added.getDeviceType());
                builder.setPayload(
                        DeviceAddedEventAvro.newBuilder()
                                .setId(added.getId())
                                .setType(DeviceTypeAvro.valueOf(added.getDeviceType().name()))
                                .build()
                );
            }

            case DEVICE_REMOVED -> {
                DeviceRemovedEvent removed = (DeviceRemovedEvent) event;
                log.debug("Mapping DEVICE_REMOVED event - id: {}", removed.getId());
                builder.setPayload(
                        DeviceRemovedEventAvro.newBuilder()
                                .setId(removed.getId())
                                .build()
                );
            }
            case SCENARIO_ADDED -> {
                ScenarioAddedEvent added = (ScenarioAddedEvent) event;
                log.debug("Mapping SCENARIO_ADDED event - name: {}, conditions: {}, actions: {}",
                        added.getName(), added.getConditions().size(), added.getActions().size());
                builder.setPayload(
                        ScenarioAddedEventAvro.newBuilder()
                                .setName(added.getName())
                                .setConditions(added.getConditions().stream()
                                        .map(c -> ScenarioConditionAvro.newBuilder()
                                                .setSensorId(c.getSensorId())
                                                .setType(ConditionTypeAvro.valueOf(c.getType().name()))
                                                .setOperation(ConditionOperationAvro.valueOf(c.getOperation().name()))
                                                .setValue(c.getValue())
                                                .build())
                                        .collect(Collectors.toList()))
                                .setActions(added.getActions().stream()
                                        .map(a -> DeviceActionAvro.newBuilder()
                                                .setSensorId(a.getSensorId())
                                                .setType(ActionTypeAvro.valueOf(a.getType().name()))
                                                .setValue(a.getValue())
                                                .build())
                                        .collect(Collectors.toList()))
                                .build()
                );
            }
            case SCENARIO_REMOVED -> {
                ScenarioRemovedEvent removed = (ScenarioRemovedEvent) event;
                log.debug("Mapping SCENARIO_REMOVED event - name: {}", removed.getName());
                builder.setPayload(
                        ScenarioRemovedEventAvro.newBuilder()
                                .setName(removed.getName())
                                .build()
                );
            }
        }
        return builder.build();
    }
}
