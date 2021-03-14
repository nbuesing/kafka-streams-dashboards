package dev.buesing.ksd.streams;

public class ServletDeployment {
//    public void x() throws ServletException {
//        DeploymentInfo servletBuilder = Servlets.deployment()
//                .setClassLoader(ServletDeployment.class.getClassLoader())
//                .setContextPath("/")
//                .setDeploymentName("streams")
//                .addServlets(
//                        Servlets.servlet("MessageServlet", ViewStatusMessagesServlet.class)
//                                .addInitParam("message", "Hello World")
//                                .addMapping("/*"),
//                        Servlets.servlet("MyServlet", ViewStatusMessagesServlet.class)
//                                .addInitParam("message", "MyServlet")
//                                .addMapping("/myservlet"));
//
//        DeploymentManager manager = Servlets.defaultContainer().addDeployment(servletBuilder);
//        manager.deploy();
//        PathHandler path = Handlers.path(Handlers.redirect("/myapp"))
//                .addPrefixPath("/myapp", manager.start());
//
//        Undertow server = Undertow.builder()
//                .addHttpListener(9999, "0.0.0.0")
//              //  .setHandler(path)
//                .setHandler(new HttpHandler() {
//                    @Override
//                    public void handleRequest(final HttpServerExchange exchange) throws Exception {
//
//                        System.out.println(">>> " + exchange.getRequestURI());
//                        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
//                        exchange.getResponseSender().send("Hello World");
//                    }
//                })
//                .build();
//        server.start();
//    }
}
