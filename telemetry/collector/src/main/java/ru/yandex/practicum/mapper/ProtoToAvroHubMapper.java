package ru.yandex.practicum.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.*;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.time.Instant;
import java.util.stream.Collectors;

@Component
public class ProtoToAvroHubMapper {
    public HubEventAvro toAvro(HubEventProto proto) {
        HubEventAvro.Builder builder = HubEventAvro.newBuilder()
                .setHubId(proto.getHubId())
                .setTimestamp(Instant.ofEpochSecond(
                        proto.getTimestamp().getSeconds(),
                        proto.getTimestamp().getNanos()));

        return switch (proto.getPayloadCase()) {
            case DEVICE_ADDED -> {
                DeviceAddedEventProto d = proto.getDeviceAdded();
                yield builder.setPayload(
                        DeviceAddedEventAvro.newBuilder()
                                .setId(d.getId())
                                .setType(DeviceTypeAvro.valueOf(d.getType().name()))
                                .build()
                ).build();
            }
            case DEVICE_REMOVED -> {
                DeviceRemovedEventProto d = proto.getDeviceRemoved();
                yield builder.setPayload(
                        DeviceRemovedEventAvro.newBuilder()
                                .setId(d.getId())
                                .build()
                ).build();
            }
            case SCENARIO_ADDED -> {
                ScenarioAddedEventProto s = proto.getScenarioAdded();
                yield builder.setPayload(
                        ScenarioAddedEventAvro.newBuilder()
                                .setName(s.getName())
                                .setConditions(
                                        s.getConditionList().stream()
                                                .map(this::mapCondition)
                                                .collect(Collectors.toList())
                                )
                                .setActions(
                                        s.getActionList().stream()
                                                .map(this::mapAction)
                                                .collect(Collectors.toList())
                                )
                                .build()
                ).build();
            }
            case SCENARIO_REMOVED -> {
                ScenarioRemovedEventProto s = proto.getScenarioRemoved();
                yield builder.setPayload(
                        ScenarioRemovedEventAvro.newBuilder()
                                .setName(s.getName())
                                .build()
                ).build();
            }
            case PAYLOAD_NOT_SET -> builder.build();
        };
    }

    private ScenarioConditionAvro mapCondition(ScenarioConditionProto proto) {
        ScenarioConditionAvro.Builder b = ScenarioConditionAvro.newBuilder()
                .setSensorId(proto.getSensorId())
                .setType(ConditionTypeAvro.valueOf(proto.getType().name()))
                .setOperation(ConditionOperationAvro.valueOf(proto.getOperation().name()));

        switch (proto.getValueCase()) {
            case BOOL_VALUE -> b.setValue(proto.getBoolValue());
            case INT_VALUE -> b.setValue(proto.getIntValue());
            case VALUE_NOT_SET -> b.setValue(null);
        }
        return b.build();
    }

    private DeviceActionAvro mapAction(DeviceActionProto proto) {
        DeviceActionAvro.Builder b = DeviceActionAvro.newBuilder()
                .setSensorId(proto.getSensorId())
                .setType(ActionTypeAvro.valueOf(proto.getType().name()));

        if (proto.hasValue()) {
            b.setValue(proto.getValue());
        } else {
            b.setValue(null);
        }
        return b.build();
    }
}
