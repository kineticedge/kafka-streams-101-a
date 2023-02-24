package io.kineticedge.ks101.consumer;

import io.kineticedge.ks101.domain.Customer360;
import io.kineticedge.ks101.consumer.serde.JsonDeserializer;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
public class Consumer {

    private final Options options;

    private KafkaConsumer<String, Customer360> kafkaConsumer;

    private boolean run = true;

    private CountDownLatch latch = new CountDownLatch(1);

    public Consumer(final Options options) {
        this.options = options;
        this.kafkaConsumer = new KafkaConsumer<String, Customer360>(properties(options));
    }

    public void close() {
        run = false;

        try {
            latch.await(options.getPollDuration().toMillis() * 3, TimeUnit.MILLISECONDS);
        } catch (final InterruptedException e) {
        }
    }

    public void consume() {

        kafkaConsumer.subscribe(Collections.singleton(options.getCustomer360Topic()));

        while (run) {
            ConsumerRecords<String, Customer360> records = kafkaConsumer.poll(options.getPollDuration());

            records.forEach(record -> {
                System.out.println(record.key());
                System.out.println(record.value());
            });
        }

        kafkaConsumer.close();

        latch.countDown();
    }

    private Map<String, Object> properties(final Options options) {
        Map<String, Object> defaults = Map.ofEntries(
                Map.entry(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, options.getBootstrapServers()),
                Map.entry(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "PLAINTEXT"),
                Map.entry(CommonClientConfigs.GROUP_ID_CONFIG, "GROUP_AAA"),
                Map.entry(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName()),
                Map.entry(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class.getName())
        );

        Map<String, Object> map = new HashMap<>(defaults);

        return map;
    }

    private static void dumpRecord(final ConsumerRecord<String, String> record) {
        log.info("Record:\n\ttopic     : {}\n\tpartition : {}\n\toffset    : {}\n\tkey       : {}\n\tvalue     : {}", record.topic(), record.partition(), record.offset(), record.key(), record.value());
    }

    public static Properties toProperties(final Map<String, Object> map) {
        final Properties properties = new Properties();
        properties.putAll(map);
        return properties;
    }
}
