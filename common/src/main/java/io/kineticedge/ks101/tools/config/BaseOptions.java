package io.kineticedge.ks101.tools.config;

import com.beust.jcommander.Parameter;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class BaseOptions {

    @Parameter(names = "--help", help = true, hidden = true)
    private boolean help;

    @Parameter(names = { "-b", "--bootstrap-servers" }, description = "cluster bootstrap servers")
    private String bootstrapServers = "localhost:19092,localhost:29092,localhost:39092";

    @Parameter(names = { "--customer-topic" }, description = "customer topic")
    private String customerTopic = "customers";

    @Parameter(names = { "--customer360-topic" }, description = "customer360 topic")
    private String customer360Topic = "customers360";

}
