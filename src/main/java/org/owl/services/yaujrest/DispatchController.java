package org.owl.services.yaujrest;

import org.owl.services.yaujrest.controller.Controller;
import org.owl.services.yaujrest.controller.ControllerContainer;
import org.owl.services.yaujrest.http.HttpRequest;
import org.owl.services.yaujrest.http.HttpResponse;
import org.owl.services.yaujrest.http.Method;
import org.owl.services.yaujrest.http.parser.HttpMessageParser;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Главный обработчик входящий запросов.
 * <p>
 * Каждый запрос обрабатывается в отдельном потоке
 */
public final class DispatchController {

    private final ControllerContainer controllerContainer;

    private final int port;

    private boolean isStopped = false;

    private DispatchController(final ControllerContainer controllerContainer, final int port) {
        this.controllerContainer = controllerContainer;
        this.port = port;
    }

    public static final class DispatchControllerBuilder {

        private ControllerContainer controllerContainer;
        private int port = 80;

        private DispatchControllerBuilder() { }

        public DispatchControllerBuilder port(final int port) {
            this.port = port;
            return this;
        }

        public DispatchControllerBuilder controllers(final ControllerContainer controllerContainer) {
            this.controllerContainer = controllerContainer;
            return this;
        }

        public DispatchController build() {
            return new DispatchController(this.controllerContainer, this.port);
        }

    }

    /**
     * Возвращает объект-строитель главного обработчика
     * @return объект-строитель главного обработчика
     */
    public static DispatchControllerBuilder builder() {
        return new DispatchControllerBuilder();
    }

    /**
     * Останавливает обработчик входящий запросов
     */
    public void stop() {
        this.isStopped = true;
    }

    /**
     * Запускает обработчик входящий запросов
     */
    public void listen() {
        new Thread(() -> {

            try (final ServerSocket serverSocket = new ServerSocket(this.port)) {
                final HttpMessageParser httpMessageParser = new HttpMessageParser();
                while (!isStopped) {
                    final Socket socket = serverSocket.accept();
                    new Thread(() -> {
                        try {
                            final HttpRequest httpRequest = httpMessageParser.parseHttpRequest(socket.getInputStream());
                            final Controller controller = this.controllerContainer.getByPath(httpRequest.uri().getPath());
                            final HttpResponse httpResponse = handleHttpRequest(httpRequest, controller);
                            socket.getOutputStream().write(httpResponse.serialize());
                            socket.getOutputStream().close();
                            socket.close();
                        } catch (Exception e) {
                            // TODO: logging
                        }
                    }).start();
                }
            } catch (Exception e) {
                throw new DispatchControllerStartUpException("Error while starting dispatch controller", e);
            }

        }).start();
    }

    private HttpResponse handleHttpRequest(final HttpRequest httpRequest, final Controller controller) {
        return switch (httpRequest.method()) {
            case Method.GET -> controller.doGet(httpRequest);
            case Method.POST -> controller.doPost(httpRequest);
            case Method.PUT -> controller.doPut(httpRequest);
            case Method.DELETE -> controller.doDelete(httpRequest);
        };
    }

}
