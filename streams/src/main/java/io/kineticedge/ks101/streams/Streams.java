package io.kineticedge.ks101.streams;

import io.kineticedge.ks101.domain.Customer360;
import io.kineticedge.ks101.streams.reporter.KafkaMetricsReporter;
import io.kineticedge.ks101.tools.serde.JsonSerde;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.metrics.JmxReporter;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.errors.LogAndContinueExceptionHandler;
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.state.KeyValueStore;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

@Slf4j
public class Streams {

    private static final Duration SHUTDOWN = Duration.ofSeconds(30);

    private Map<String, Object> properties(final Options options) {

        final Map<String, Object> defaults = Map.ofEntries(
                Map.entry(ProducerConfig.LINGER_MS_CONFIG, 100),
                // Map.entry(StreamsConfig.NUM_STANDBY_REPLICAS_CONFIG, 2);
                Map.entry(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, options.getBootstrapServers()),
                Map.entry(StreamsConfig.SECURITY_PROTOCOL_CONFIG, "PLAINTEXT"),
                Map.entry(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.StringSerde.class.getName()),
                Map.entry(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, JsonSerde.class.getName()),
                Map.entry(StreamsConfig.APPLICATION_ID_CONFIG, options.getApplicationId()),
                Map.entry(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, options.getAutoOffsetReset()),
                Map.entry(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true"),
                Map.entry(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 100),
                //Map.entry(CommonClientConfigs.SESSION_TIMEOUT_MS_CONFIG, 10_000),
                Map.entry(StreamsConfig.CLIENT_ID_CONFIG, options.getClientId()),
                Map.entry(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG, LogAndContinueExceptionHandler.class),
                Map.entry(StreamsConfig.TOPOLOGY_OPTIMIZATION_CONFIG, StreamsConfig.OPTIMIZE),
                Map.entry(StreamsConfig.METRICS_RECORDING_LEVEL_CONFIG, "DEBUG"),
                Map.entry(StreamsConfig.NUM_STREAM_THREADS_CONFIG, 2),
                Map.entry(StreamsConfig.METRIC_REPORTER_CLASSES_CONFIG, JmxReporter.class.getName() + "," + KafkaMetricsReporter.class.getName())
                //Map.entry(CommonConfigs.METRICS_REPORTER_CONFIG, options.getCustomMetricsTopic())
        );


        final Map<String, Object> map = new HashMap<>(defaults);

        //
        // If set the consumer is treated as a static member; this ID must be unique for every member in the group.
        //
        // * if you look at docker/entrypoint.sh it uses the numerical value within docker to ensure a uniqe instance.
        //
        // * if you are running stand-alone, you need to set this uniquely for every instance you plan on running.
        //
        if (options.getGroupInstanceId() != null) {
            map.put(CommonClientConfigs.GROUP_INSTANCE_ID_CONFIG, options.getGroupInstanceId());
        }


        try {
            final Properties properties = new Properties();
            final File file = new File("./streams.properties");
            if (file.exists() && file.isFile()) {
                log.info("applying streams.properties");
                properties.load(new FileInputStream(file));
                map.putAll(properties.entrySet().stream().collect(Collectors.toMap(e -> e.getKey().toString(), Map.Entry::getValue)));
            }
        } catch (final IOException e) {
            log.info("no streams.properties override file found");
        }

        return map;
    }


    public void start(final Options options) {

        Properties p = toProperties(properties(options));

        log.info("starting streams : " + options.getClientId());

        final Topology topology = streamsBuilder(options).build(p);

//        StreamsMetrics.register(topology.describe());

        log.info("Topology:\n" + topology.describe());

        final KafkaStreams streams = new KafkaStreams(topology, p);

        streams.setUncaughtExceptionHandler(e -> {
            log.error("unhandled streams exception, shutting down (a warning of 'Detected that shutdown was requested. All clients in this app will now begin to shutdown' will repeat every 100ms for the duration of session timeout).", e);
            return StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.SHUTDOWN_APPLICATION;
        });

        streams.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Runtime shutdown hook, state={}", streams.state());
            if (streams.state().isRunningOrRebalancing()) {

                // New to Kafka Streams 3.3, you can have the application leave the group on shutting down (when member.id / static membership is used).
                //
                // There are reasons to do this and not to do it; from a development standpoint this makes starting/stopping
                // the application a lot easier reducing the time needed to rejoin the group.
                boolean leaveGroup = true;

                log.info("closing KafkaStreams with leaveGroup={}", leaveGroup);

                KafkaStreams.CloseOptions closeOptions = new KafkaStreams.CloseOptions().timeout(SHUTDOWN).leaveGroup(leaveGroup);

                boolean isClean = streams.close(closeOptions);
                if (!isClean) {
                    System.out.println("KafkaStreams was not closed cleanly");
                }

            } else if (streams.state().isShuttingDown()) {
                log.info("Kafka Streams is already shutting down with state={}, will wait {} to ensure proper shutdown.", streams.state(), SHUTDOWN);
                boolean isClean = streams.close(SHUTDOWN);
                if (!isClean) {
                    System.out.println("KafkaStreams was not closed cleanly");
                }
                System.out.println("final KafkaStreams state=" + streams.state());
            }
        }));

    }

    private StreamsBuilder streamsBuilder(final Options options) {

        final var builder = new StreamsBuilder();

        final var materialized = Materialized.<String, Customer360, KeyValueStore<Bytes, byte[]>>as("customer360");

        builder.<String, Customer360>stream(options.getCustomerTopic(), Consumed.as("customer-input"))
                .peek((k, v) -> log.debug("key={}, value={}", k, v))
                .groupByKey()
                .aggregate(
                        () -> new Customer360(),
                        (key, customer, customer360) -> {
                            return customer360;
                        },
                        Named.as("aggregator"),
                        materialized
                )
                .toStream()
                .peek((k, v) -> log.debug("key={}, value={}", k, v))
                .to(options.getCustomer360Topic(), Produced.as("customer360-output"));

        return builder;
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
