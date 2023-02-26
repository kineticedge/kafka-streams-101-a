package io.kineticedge.ks101.admin;


import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.config.ConfigResource;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Slf4j
public class Admin {

    final AdminClient kafkaAdmin;

    private final Options options;

    public Admin(final Options options) {
        this.options = options;
        this.kafkaAdmin = AdminClient.create(properties(options));
    }

    public void topics() throws InterruptedException, ExecutionException {

        final Set<String> topics = kafkaAdmin.listTopics().names().get();

        final List<ConfigResource> resources = topics.stream()
                .map(s -> new ConfigResource(ConfigResource.Type.TOPIC, s))
                .collect(Collectors.toList());

        Map<String, TopicDescription> map = kafkaAdmin.describeTopics(topics).allTopicNames().get();
        map.forEach((topic, description) -> {
            System.out.println(topic);
            //System.out.println(description.);
        });

        Map<ConfigResource, Config> configs = kafkaAdmin.describeConfigs(resources).all().get();

        configs.forEach((resource, config) -> {
            System.out.println(resource.name());
            config.entries().forEach(c -> {
                if (!c.isDefault()) {
                    System.out.println(c.name() + "=" + c.value() );
                }
            });
        });

    }

    public void close() {
        kafkaAdmin.close();
    }

    private Map<String, Object> properties(final Options options) {
        Map<String, Object> defaults = Map.ofEntries(
                Map.entry(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, options.getBootstrapServers()),
                Map.entry(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "PLAINTEXT")
        );

        Map<String, Object> map = new HashMap<>(defaults);

        return map;
    }

    public static Properties toProperties(final Map<String, Object> map) {
        final Properties properties = new Properties();
        properties.putAll(map);
        return properties;
    }
}
