package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.serializer.AvroSerializer;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    @Value("${collector.topics.sensors}")
    private String sensorsTopic;

    @Value("${collector.topics.hubs}")
    private String hubsTopic;

    public void sendSensorEvent(SensorEventAvro sensorEvent) {
        byte[] payload = AvroSerializer.serialize(sensorEvent);
        kafkaTemplate.send(sensorsTopic, sensorEvent.getId(), payload);
    }

    public void sendHubEvent(HubEventAvro hubEvent) {
        byte[] payload = AvroSerializer.serialize(hubEvent);
        kafkaTemplate.send(hubsTopic, hubEvent.getHubId(), payload);
    }
}
