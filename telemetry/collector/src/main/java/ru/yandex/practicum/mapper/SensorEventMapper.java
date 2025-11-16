package ru.yandex.practicum.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.model.sensor.*;

@Component
public class SensorEventMapper {
    public SensorEventAvro toAvro(SensorEvent event) {
        SensorEventAvro.Builder builder = SensorEventAvro.newBuilder()
                .setId(event.getId())
                .setHubId(event.getHubId())
                .setTimestamp(event.getTimestamp());

        switch (event.getType()) {
            case LIGHT_SENSOR_EVENT -> builder.setPayload(
                    LightSensorAvro.newBuilder()
                            .setLinkQuality(((LightSensorEvent) event).getLinkQuality())
                            .setLuminosity(((LightSensorEvent) event).getLuminosity())
                            .build());

            case MOTION_SENSOR_EVENT -> builder.setPayload(
                    MotionSensorAvro.newBuilder()
                            .setLinkQuality(((MotionSensorEvent) event).getLinkQuality())
                            .setMotion(((MotionSensorEvent) event).isMotion())
                            .setVoltage((((MotionSensorEvent) event).getVoltage()))
                            .build());

            case CLIMATE_SENSOR_EVENT -> builder.setPayload(
                    ClimateSensorAvro.newBuilder()
                            .setTemperatureC(((ClimateSensorEvent) event).getTemperatureC())
                            .setHumidity(((ClimateSensorEvent) event).getHumidity())
                            .setCo2Level(((ClimateSensorEvent) event).getCo2Level())
                            .build());

            case SWITCH_SENSOR_EVENT -> builder.setPayload(
                    SwitchSensorAvro.newBuilder()
                            .setState(((SwitchSensorEvent) event).isState())
                            .build());

            case TEMPERATURE_SENSOR_EVENT -> builder.setPayload(
                    TemperatureSensorAvro.newBuilder()
                            .setTemperatureC(((TemperatureSensorEvent) event).getTemperatureC())
                            .setTemperatureF(((TemperatureSensorEvent) event).getTemperatureF())
                            .build());
        }

        return builder.build();
    }
}
