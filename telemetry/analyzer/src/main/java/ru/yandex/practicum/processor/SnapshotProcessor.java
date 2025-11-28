package ru.yandex.practicum.processor;

import com.google.protobuf.Timestamp;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.*;
import ru.yandex.practicum.grpc.telemetry.hubrouter.HubRouterControllerGrpc.HubRouterControllerBlockingStub;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.model.*;
import ru.yandex.practicum.repository.ScenarioRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

@Slf4j
@Component
@RequiredArgsConstructor
public class SnapshotProcessor {

    private final ScenarioRepository scenarioRepository;

    @GrpcClient("hub-router")
    private HubRouterControllerBlockingStub hubRouterClient;

    // Адреса Kafka brokers
    @Value("${analyzer.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // Топик для снапшотов
    @Value("${analyzer.topics.snapshots}")
    private String snapshotsTopic;

    /**
     * Основной метод - запускает бесконечный цикл обработки снапшотов из Kafka.
     * Выполняется в основном потоке приложения
     */
    public void start() {
        log.info("Starting SnapshotProcessor. Subscribing to topic: {}", snapshotsTopic);

        Properties props = createKafkaConsumerProperties();

        KafkaConsumer<String, SensorsSnapshotAvro> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList(snapshotsTopic));

        try {
            startPollingLoop(consumer);
        } catch (WakeupException ignored) {
            log.info("SnapshotProcessor stopped via wakeup");
        } catch (Exception e) {
            log.error("Error in SnapshotProcessor: ", e);
        } finally {
            try {
                consumer.commitSync();
            } catch (Exception e) {
                log.warn("Error during final commit:{}", e.getMessage());
            } finally {
                consumer.close();
                log.info("SnapshotProcessor consumer closed");
            }
        }
    }

    /**
     * Создание настроек для Kafka Consumer снапшотов
     */
    private Properties createKafkaConsumerProperties() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "analyzer-snapshots-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                "ru.yandex.practicum.deserializer.SensorsSnapshotDeserializer");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        return props;
    }


    /**
     * Метод запускает бесконечный цикл опроса Kafka для снапшотов
     */
    private void startPollingLoop(KafkaConsumer<String, SensorsSnapshotAvro> consumer) {
        while (true) {
            // Получение пачки сообщений
            ConsumerRecords<String, SensorsSnapshotAvro> records = consumer.poll(Duration.ofMillis(200));

            // Проверка активности SnapshotProcessor
            if (!records.isEmpty()) {
                log.debug("Processing {} sensor snapshots", records.count());
            }

            // Обработка каждого сообщения в пачке
            for (ConsumerRecord<String, SensorsSnapshotAvro> record : records) {
                processSnapshot(record.value());
            }

            // Асинхронный коммит offset-ов после обработки всей пачки
            consumer.commitAsync();
        }
    }

    /**
     * Обработка снапшота состояния датчиков - анализ сценария и выполнение действия
     */
    private void processSnapshot(SensorsSnapshotAvro snapshot) {
        String hubId = snapshot.getHubId();
        log.debug("Processing snapshot for hub: {}", hubId);

        // Получаем все сценарии для данного хаба
        List<Scenario> scenarios = scenarioRepository.findByHubId(hubId);
        if (scenarios.isEmpty()) {
            log.debug("No scenarios found for hub: {}", hubId);
            return;
        }

        // Проверяем каждый сценарий на выполнение условий
        for (Scenario scenario : scenarios) {
            boolean conditionsMet = checkScenarioConditions(scenario, snapshot);

            if (conditionsMet) {
                log.info("Scenario '{}' conditions met. Executing actions...", scenario.getName());
                executeScenarioActions(hubId, scenario);
            } else {
                log.debug("Scenario '{}' conditions not met", scenario.getName());
            }
        }
    }

    /**
     * Проверка всех условий сценария на соответствие данным снапшота
     */
    private boolean checkScenarioConditions(Scenario scenario, SensorsSnapshotAvro snapshot) {
        if (scenario.getConditions().isEmpty()) {
            log.warn("Scenario '{}' has no conditions!", scenario.getName());
            return false;
        }

        // Все условия должны быть выполнены
        for (ScenarioCondition scenarioCondition : scenario.getConditions()) {
            String sensorId = scenarioCondition.getSensor().getId();
            SensorStateAvro sensorState = snapshot.getSensorsState().get(sensorId);

            if (sensorState == null) {
                log.debug("Sensor {} not found in snapshot, skipping condition", sensorId);
                return false;
            }

            Condition condition = scenarioCondition.getCondition();
            boolean conditionResult = evaluateCondition(condition, sensorState.getData());

            // Если есть одно невыполненное условие - весь сценарий не выполняется
            if (!conditionResult) {
                return false;
            }
        }

        // Все условия выполнены
        return true;
    }

    /**
     * Вычисление выполнения одного условия на основе данных сенсора
     */
    private boolean evaluateCondition(Condition condition, Object sensorData) {
        ConditionOperation operation = condition.getOperation();
        Integer expectedValue = condition.getValue();

        if (expectedValue == null) {
            return false;
        }

        // Обработка разных типов сенсоров
        if (sensorData instanceof TemperatureSensorAvro temperatureSensor) {
            return compare(temperatureSensor.getTemperatureC(), expectedValue, operation);
        } else if (sensorData instanceof ClimateSensorAvro climateSensor) {
            return compare(climateSensor.getTemperatureC(), expectedValue, operation);
        } else if (sensorData instanceof LightSensorAvro lightSensor) {
            return compare(lightSensor.getLuminosity(), expectedValue, operation);
        } else if (sensorData instanceof MotionSensorAvro motionSensor) {
            return motionSensor.getMotion() && expectedValue == 1;
        } else if (sensorData instanceof SwitchSensorAvro switchSensor) {
            return switchSensor.getState() == (expectedValue == 1);
        }

        log.warn("Unknown sensor data type: {}", sensorData.getClass().getSimpleName());
        return false;
    }

    /**
     * Сравнение значения сенсора с ожидаемым по заданной операции
     */
    private boolean compare(int sensorValue, int expectedValue, ConditionOperation operation) {
        return switch (operation) {
            case GREATER_THAN -> sensorValue > expectedValue;
            case LOWER_THAN -> sensorValue < expectedValue;
            case EQUALS -> sensorValue == expectedValue;
            default -> {
                log.warn("Unknown comparison operation: {}", operation);
                yield false;
            }
        };
    }
    /**
     * Выполняет все действия сценария через gRPC вызовы к Hub Router
     */
    private void executeScenarioActions(String hubId, Scenario scenario) {
        Instant timestamp = Instant.now();

        for (ScenarioAction scenarioAction : scenario.getActions()) {
            Action action = scenarioAction.getAction();
            String sensorId = scenarioAction.getSensor().getId();

            // Безопасное извлечение значения действия
            Integer rawValue = action.getValue();
            int safeValue = (rawValue != null) ? rawValue : 0;

            if (rawValue == null) {
                log.debug("Action {} for sensor {} has no value, using default: 0",
                        action.getType(), sensorId);
            }

            // Создание gRPC запроса
            DeviceActionProto grpcAction = DeviceActionProto.newBuilder()
                    .setSensorId(sensorId)
                    .setType(ActionTypeProto.valueOf(action.getType().name()))
                    .setValue(safeValue)
                    .build();

            DeviceActionRequest request = DeviceActionRequest.newBuilder()
                    .setHubId(hubId)
                    .setScenarioName(scenario.getName())
                    .setAction(grpcAction)
                    .setTimestamp(Timestamp.newBuilder()
                            .setSeconds(timestamp.getEpochSecond())
                            .setNanos(timestamp.getNano())
                            .build())
                    .build();

            // Выполнение gRPC вызова
            try {
                hubRouterClient.handleDeviceAction(request);
                log.info("Executed action {} for sensor {} (hub:{})", action.getType(), sensorId, hubId);
            } catch (StatusRuntimeException e) {
                log.error("gRPC call to HubRouter failed: {}", e.getStatus(), e);
            }
        }
    }
}
