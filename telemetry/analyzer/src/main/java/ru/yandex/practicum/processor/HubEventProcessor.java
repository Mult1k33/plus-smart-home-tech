package ru.yandex.practicum.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.model.*;
import ru.yandex.practicum.repository.*;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

@Slf4j
@Component
@RequiredArgsConstructor
public class HubEventProcessor implements Runnable {

    private final SensorRepository sensorRepository;

    private final ScenarioRepository scenarioRepository;

    private final ConditionRepository conditionRepository;

    private final ActionRepository actionRepository;

    // Адреса Kafka brokers
    @Value("${analyzer.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // Топик для событий хабов
    @Value("${analyzer.topics.hub-events}")
    private String hubEventsTopic;

    /**
     * Основной метод - запускает бесконечный цикл обработки сообщений Kafka в отдельном потоке
     */
    @Override
    public void run() {
        log.info("Starting HubEventProcessor. Subscribing to topic: {}", hubEventsTopic);

        Properties props = createKafkaConsumerProperties();

        KafkaConsumer<String, HubEventAvro> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList(hubEventsTopic));

        try {
            startPollingLoop(consumer);
        } catch (WakeupException ignored) {
            log.info("HubEventProcessor stopped via wakeup");
        } catch (Exception e) {
            log.error("Error in HubEventProcessor: ", e);
        } finally {
            try {
                consumer.commitSync();
            } catch (Exception e) {
                log.warn("Error during final commit:{}", e.getMessage());
            } finally {
                consumer.close();
                log.info("HubEventProcessor consumer closed");
            }
        }
    }

    /**
     * Создание настроек для Kafka Consumer
     */
    private Properties createKafkaConsumerProperties() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "analyzer-hub-events-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                "ru.yandex.practicum.deserializer.HubEventDeserializer");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        return props;
    }

    /**
     * Метод запускает бесконечный цикл опроса Kafka событий хаба
     */
    private void startPollingLoop(KafkaConsumer<String, HubEventAvro> consumer) {
        while (true) {
            // Получение пачки сообщений
            ConsumerRecords<String, HubEventAvro> records = consumer.poll(Duration.ofMillis(5000));

            // Проверка активности HubEventProcessor
            if (!records.isEmpty()) {
                log.debug("Processing {} hub events", records.count());
            }

            // Обработка каждого сообщения в пачке
            for (ConsumerRecord<String, HubEventAvro> record : records) {
                processEvent(record.value());
            }

            // Асинхронный коммит offset-ов после обработки всей пачки
            consumer.commitAsync();
        }
    }

    /**
     * Диспетчер событий - определяет тип события и направляет на соответствующий обработчик
     */
    private void processEvent(HubEventAvro event) {
        log.debug("Received HubEventAvro event:{}", event);

        // Определение типа события по классу payload
        switch (event.getPayload().getClass().getSimpleName()) {
            case "DeviceAddedEventAvro" -> handleDeviceAdded(event);      // Добавление устройства
            case "DeviceRemovedEventAvro" -> handleDeviceRemoved(event);  // Удаление устройства
            case "ScenarioAddedEventAvro" -> handleScenarioAdded(event);  // Добавление сценария
            case "ScenarioRemovedEventAvro" -> handleScenarioRemoved(event); // Удаление сценария
            default -> log.warn("Unknown event type:{}", event.getPayload().getClass());
        }
    }

    /**
     * Обработчик добавления устройства.
     * Сохраняет новый сенсор в БД, если его еще нет
     */
    private void handleDeviceAdded(HubEventAvro event) {
        DeviceAddedEventAvro payload = (DeviceAddedEventAvro) event.getPayload();
        String sensorId = payload.getId();
        String hubId = event.getHubId();

        // Проверка существования сенсора, если нет - создать
        sensorRepository.findById(sensorId).ifPresentOrElse(
                s -> log.debug("Sensor '{}' already exists in hub '{}'", sensorId, hubId),
                () -> {
                    sensorRepository.save(Sensor.builder()
                            .id(sensorId)
                            .hubId(hubId)
                            .build());
                    log.info("Added sensor '{}' to hub '{}'", sensorId, hubId);
                });
    }

    /**
     * Обработчик удаления устройства.
     * Удаляет сенсор из БД, если он существует
     */
    private void handleDeviceRemoved(HubEventAvro event) {
        DeviceRemovedEventAvro payload = (DeviceRemovedEventAvro) event.getPayload();
        String sensorId = payload.getId();

        if (sensorRepository.existsById(sensorId)) {
            scenarioRepository.deleteConditionsBySensorId(sensorId);
            scenarioRepository.deleteActionsBySensorId(sensorId);
            sensorRepository.deleteById(sensorId);
            log.info("Removed sensor '{}'", sensorId);
        } else {
            log.debug("Sensor '{}' not found for removal", sensorId);
        }
    }

    /**
     * Обработчик добавления сценария.
     * Создает/обновляет сценарий, его условия и действия в БД
     */
    private void handleScenarioAdded(HubEventAvro event) {
        ScenarioAddedEventAvro payload = (ScenarioAddedEventAvro) event.getPayload();
        String hubId = event.getHubId();
        String name = payload.getName();

        // Находим существующий сценарий или создаем новый
        Scenario scenario = scenarioRepository.findByHubIdAndName(hubId, name)
                .orElseGet(() -> Scenario.builder()
                        .hubId(hubId)
                        .name(name)
                        .build());

        scenarioRepository.save(scenario);

        // Очищаем старые условия и действия (для обновления сценария)
        scenario.getConditions().clear();
        scenario.getActions().clear();

        // Обработка условия сценария
        payload.getConditions().forEach(cond -> {
            // Преобразование значения условия
            Integer value = null;
            Object rawValue = cond.getValue();
            if (rawValue instanceof Integer i) {
                value = i;
            } else if (rawValue instanceof Boolean b) {
                value = b ? 1 : 0; // boolean -> int (1/0)
            }

            // Сохраняем условие в БД
            Condition condition = conditionRepository.save(Condition.builder()
                    .type(ConditionType.valueOf(cond.getType().name()))
                    .operation(ConditionOperation.valueOf(cond.getOperation().name()))
                    .value(value)
                    .build());

            // Связываем условие с сенсором и сценарием (если сенсор существует)
            sensorRepository.findByIdAndHubId(cond.getSensorId(), hubId).ifPresent(sensor -> {
                ScenarioCondition sc = ScenarioCondition.builder()
                        .id(new ScenarioConditionId(scenario.getId(), sensor.getId(), condition.getId()))
                        .scenario(scenario)
                        .sensor(sensor)
                        .condition(condition)
                        .build();
                scenario.getConditions().add(sc);
            });
        });

        // Обработка действий сценария
        payload.getActions().forEach(act -> {
            Action action = actionRepository.save(Action.builder()
                    .type(ActionType.valueOf(act.getType().name()))
                    .value(act.getValue() != null ? act.getValue() : null)
                    .build());

            sensorRepository.findByIdAndHubId(act.getSensorId(), hubId).ifPresent(sensor -> {
                ScenarioAction sa = ScenarioAction.builder()
                        .id(new ScenarioActionId(scenario.getId(), sensor.getId(), action.getId()))
                        .scenario(scenario)
                        .sensor(sensor)
                        .action(action)
                        .build();
                scenario.getActions().add(sa);
            });
        });

        // Сохраняем полностью собранный сценарий
        scenarioRepository.save(scenario);
        log.info("Scenario '{}' added to hub '{}' with {} conditions and {} actions",
                name, hubId, payload.getConditions().size(), payload.getActions().size());
    }

    /**
     * Обработчик удаления сценария.
     * Удаляет сценарий из БД, если он существует
     */
    private void handleScenarioRemoved(HubEventAvro event) {
        ScenarioRemovedEventAvro payload = (ScenarioRemovedEventAvro) event.getPayload();
        String hubId = event.getHubId();
        String name = payload.getName();

        scenarioRepository.findByHubIdAndName(hubId, name).ifPresentOrElse(
                s -> {
                    scenarioRepository.delete(s);
                    log.info("Removed scenario '{}' from hub '{}'", name, hubId);
                },
                () -> log.debug("Scenario '{}' not found in hub '{}'", name, hubId)
        );
    }
}
