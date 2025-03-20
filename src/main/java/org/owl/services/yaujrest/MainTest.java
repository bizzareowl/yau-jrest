package org.owl.services.yaujrest;

import org.owl.services.yaujrest.controller.Controller;
import org.owl.services.yaujrest.controller.ControllerContainer;
import org.owl.services.yaujrest.http.HttpRequest;
import org.owl.services.yaujrest.http.HttpResponse;
import org.owl.services.yaujrest.http.Version;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class MainTest {

    public static void main(String[] args) {
        final DispatchController dispatchController = DispatchController.builder()
                .controllers(ControllerContainer.builder()
                        .addController(new Controller("/") {

                            @Override
                            public HttpResponse doGet(HttpRequest httpRequest) {
                                return new HttpResponse(new Version(1, 1), 200, "OK", Map.of("Content-Encoding", StandardCharsets.UTF_8.name(), "Content-Type", "text/plain"), "HELLO".getBytes(StandardCharsets.UTF_8));
                            }

                            @Override
                            public String getPath() {
                                return super.getPath();
                            }
                        })
                        .build())
                .port(8080)
                .build();

        dispatchController.listen();
    }

}
