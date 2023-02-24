package io.kineticedge.ks101.consumer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.RoutingHandler;
import io.undertow.server.handlers.BlockingHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;

import java.util.TimeZone;

public class ServletDeployment {

    private ObjectMapper objectMapper = new ObjectMapper()
            .setTimeZone(TimeZone.getDefault())
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .registerModule(new JavaTimeModule());

    private Undertow server;

    private Admin admin;

    public ServletDeployment(final Options options) {

        admin = new Admin(options);

    }

    public void start() {
        DeploymentInfo servletBuilder = Servlets.deployment()
                .setClassLoader(ServletDeployment.class.getClassLoader())
                .setContextPath("/")
                .setDeploymentName("streams");

        DeploymentManager manager = Servlets.defaultContainer().addDeployment(servletBuilder);
        manager.deploy();

        RoutingHandler routingHandler = Handlers.routing();

        routingHandler
                .add("GET", "/topics", new BlockingHandler(exchange -> {

                            admin.topics();

                            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
                            exchange.setStatusCode(StatusCodes.ACCEPTED);

                            exchange.getResponseSender().send("xxx");
                        })
                );

        server = Undertow.builder()
                .addHttpListener(9998, "0.0.0.0")
                .setHandler(routingHandler)
                .build();
        server.start();
    }


}
