package dev.buesing.ksd.analytics;

import ch.qos.logback.classic.ViewStatusMessagesServlet;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.buesing.ksd.analytics.domain.ByFoo;
import dev.buesing.ksd.analytics.domain.BySku;
import dev.buesing.ksd.analytics.domain.Window;
import dev.buesing.ksd.analytics.jackson.ByFooSerializer;
import dev.buesing.ksd.analytics.jackson.BySkuSerializer;
import dev.buesing.ksd.analytics.domain.ByWindow;
import dev.buesing.ksd.analytics.jackson.ByWindowSerializer;
import dev.buesing.ksd.analytics.jackson.WindowSerializer;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.util.Headers;

import io.undertow.util.HttpString;


import java.util.Deque;
import java.util.Optional;
import javax.servlet.ServletException;

public class ServletDeployment {

    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .registerModule(new SimpleModule("uuid-module", new Version(1, 0, 0, null, "", ""))
                            .addSerializer(ByFoo.class, new ByFooSerializer())
                            .addSerializer(ByWindow.class, new ByWindowSerializer())
                            .addSerializer(BySku.class, new BySkuSerializer())
                            .addSerializer(Window.class, new WindowSerializer())
                    ).registerModule(new JavaTimeModule());

    final StateObserver stateObserver;
    final int port;

    public ServletDeployment(StateObserver stateObserver, int port) {
        this.stateObserver = stateObserver;
        this.port = port;
    }


    public void start() {
        try {
            x();
        } catch (final ServletException e) {
            throw new RuntimeException(e);
        }
    }

    public void x() throws ServletException {

        DeploymentInfo servletBuilder = Servlets.deployment()
                .setClassLoader(ServletDeployment.class.getClassLoader())
                .setContextPath("/")
                .setDeploymentName("streams")
                .addServlets(
                        Servlets.servlet("MessageServlet", ViewStatusMessagesServlet.class)
                                .addInitParam("message", "Hello World")
                                .addMapping("/*"),
                        Servlets.servlet("MyServlet", ViewStatusMessagesServlet.class)
                                .addInitParam("message", "MyServlet")
                                .addMapping("/myservlet"));

        DeploymentManager manager = Servlets.defaultContainer().addDeployment(servletBuilder);
        manager.deploy();
        PathHandler path = Handlers.path(Handlers.redirect("/myapp"))
                .addPrefixPath("/myapp", manager.start());

        Undertow server = Undertow.builder()
                .addHttpListener(port, "0.0.0.0")
                //  .setHandler(path)
                .setHandler(new HttpHandler() {
                    @Override
                    public void handleRequest(final HttpServerExchange exchange) throws Exception {

                        String groupType = "windowing";

                        Deque<String> deque = exchange.getQueryParameters().get("group-type");
                        if (deque != null && deque.size() > 0) {
                            groupType = deque.getFirst();
                        }

                        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
                        // need to make this more restrictive
                        exchange.getResponseHeaders().put(new HttpString("Access-Control-Allow-Origin"), "*");
                        exchange.getResponseSender().send(OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(stateObserver.getState(groupType)));
                    }
                })
                .build();
        server.start();
    }
}
