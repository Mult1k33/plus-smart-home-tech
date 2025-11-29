package ru.yandex.practicum.starter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;
import ru.yandex.practicum.serializer.AvroSerializer;
import ru.yandex.practicum.service.SnapshotAggregator;

import java.time.Duration;
import java.util.Collections;
import java.util.Optional;
import java.util.Properties;

@Slf4j
@Component
@RequiredArgsConstructor
public class AggregationStarter {

    private final SnapshotAggregator snapshotAggregator;

    @Value("${aggregator.topics.sensors}")
    private String sensorsTopic;

    @Value("${aggregator.topics.snapshots}")
    private String snapshotsTopic;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    public void start() {
        log.info("Starting Aggregator. Subscribing to topic:{}", sensorsTopic);

        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "aggregator-group");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringDeserializer");
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                "ru.yandex.practicum.deserializer.SensorEventDeserializer");
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        Properties producerProps = new Properties();
        producerProps.put("bootstrap.servers", bootstrapServers);
        producerProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producerProps.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");

        try (
                KafkaConsumer<String, SensorEventAvro> consumer = new KafkaConsumer<>(consumerProps);
                KafkaProducer<String, byte[]> producer = new KafkaProducer<>(producerProps)) {

            consumer.subscribe(Collections.singletonList(sensorsTopic));

            while (true) {
                ConsumerRecords<String, SensorEventAvro> records = consumer.poll(Duration.ofMillis(100));

                for (ConsumerRecord<String, SensorEventAvro> record : records) {
                    SensorEventAvro event = record.value();

                    log.debug("Received event from sensor:{} in hub:{}", event.getId(), event.getHubId());

                    Optional<SensorsSnapshotAvro> updatedSnap = snapshotAggregator.updateState(event);

                    updatedSnap.ifPresent(snapshot -> {
                        byte[] payload = AvroSerializer.serialize(snapshot);
                        producer.send(new ProducerRecord<>(snapshotsTopic, snapshot.getHubId(), payload));
                        log.info("Sent updated snapshot for hub:{} to topic:{}", snapshot.getHubId(), snapshotsTopic);
                    });
                }
                consumer.commitAsync();
            }
        } catch (
                WakeupException ignore) {
        } catch (Exception e) {
            log.error("Error during event processing: ", e);
        } finally {
            log.info("Shutting down and closing resources");
        }
    }
}
