package io.kineticedge.ks101.publisher;

import ch.qos.logback.classic.ViewStatusMessagesServlet;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.kineticedge.ks101.domain.Customer;
import io.kineticedge.ks101.domain.Email;
import io.kineticedge.ks101.domain.Phone;
import io.kineticedge.ks101.event.CustomerCreated;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.RoutingHandler;
import io.undertow.server.handlers.BlockingHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.util.Headers;

import javax.servlet.ServletException;
import java.nio.charset.StandardCharsets;

public class ServletDeployment {

    private ObjectMapper objectMapper = new ObjectMapper();

    private final Producer producer;

    private Undertow server;

    public ServletDeployment(final Options options) {
         producer = new Producer(options);
    }

    public void start() throws ServletException {
        DeploymentInfo servletBuilder = Servlets.deployment()
                .setClassLoader(ServletDeployment.class.getClassLoader())
                .setContextPath("/")
                .setDeploymentName("streams")
                ;
//                .addServlets(
//                        Servlets.servlet("MessageServlet", ViewStatusMessagesServlet.class)
//                                .addInitParam("message", "Hello World")
//                                .addMapping("/*"),
//                        Servlets.servlet("MyServlet", ViewStatusMessagesServlet.class)
//                                .addInitParam("message", "MyServlet")
//                                .addMapping("/myservlet"));

        DeploymentManager manager = Servlets.defaultContainer().addDeployment(servletBuilder);
        manager.deploy();

        RoutingHandler routingHandler= Handlers.routing();

        routingHandler.add("POST", "/customer", new BlockingHandler(exchange -> {
            ObjectNode jsonNode = (ObjectNode) objectMapper.readTree(exchange.getInputStream());
            jsonNode.put("$type", CustomerCreated.class.getName());

            CustomerCreated customerCreated = objectMapper.convertValue(jsonNode, CustomerCreated.class);

            producer.publish(customerCreated);

            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
            exchange.getResponseSender().send("{}");
        }));

//        PathHandler pathHandler = Handlers.path(exchange -> {
//            System.out.println("11111");
//            System.out.println(exchange.getRequestURI());
//        });
//
//        pathHandler.addPrefixPath("/customer", new BlockingHandler(exchange -> {
//
//            ObjectNode jsonNode = (ObjectNode) objectMapper.readTree(exchange.getInputStream());
//
//            jsonNode.put("$type", CustomerCreated.class.getName());
//
//            CustomerCreated customerCreated = objectMapper.convertValue(jsonNode, CustomerCreated.class);
//
//            System.out.println(customerCreated);
//
//            String text = new String(exchange.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
//
//            System.out.println(text);
//            System.out.println(">>> " + exchange.getRequestURI());
//            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
//            exchange.getResponseSender().send("{}");
//        }));
//
//        pathHandler.addPrefixPath("/aaa", new BlockingHandler(exchange -> {
//            System.out.println("aaa");
//            String text = new String(exchange.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
//
//            System.out.println(text);
//            System.out.println(">>> " + exchange.getRequestURI());
//            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
//            exchange.getResponseSender().send("{}");
//        }));


        server = Undertow.builder()
                .addHttpListener(9999, "0.0.0.0")
                .setHandler(routingHandler)
                .build();
        server.start();
    }

    public void stop() {
        if (server != null) {
            server.stop();
        }

        producer.close();
    }


    private Customer createCustomer() {

        final Customer customer = new Customer();

        customer.setCustomerId("1");
        customer.setFirstName("FN");
        customer.setLastName("LN");
        customer.setEmail(new Email("type", "email"));
        customer.setPhone(new Phone("type", "phone"));

        return customer;
    }
}
