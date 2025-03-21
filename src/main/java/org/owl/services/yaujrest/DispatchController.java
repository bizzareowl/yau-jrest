package org.owl.services.yaujrest;

import org.owl.services.yaujrest.controller.Controller;
import org.owl.services.yaujrest.controller.ControllerContainer;
import org.owl.services.yaujrest.http.HttpRequest;
import org.owl.services.yaujrest.http.HttpResponse;
import org.owl.services.yaujrest.http.Method;
import org.owl.services.yaujrest.http.parser.HttpMessageParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;

/**
 * Главный обработчик входящий запросов.
 * <p>
 * Каждый запрос обрабатывается в отдельном потоке
 */
public final class DispatchController {

    private static final Logger log = LoggerFactory.getLogger(DispatchController.class);

    private final ControllerContainer controllerContainer;

    private final int port;

    private boolean isStopped = false;

    private DispatchController(final ControllerContainer controllerContainer, final int port) {
        this.controllerContainer = controllerContainer;
        this.port = port;
    }

    /**
     * Класс использующийся для получения экземпляра главного обработчика
     */
    public static final class DispatchControllerBuilder {

        private ControllerContainer controllerContainer;
        private int port = 80;

        private DispatchControllerBuilder() { }

        /**
         * Устанавливает значение порта на который будут приходить запросы
         * @param port порт для "прослушивания"
         * @return текущий объект-строитель
         */
        public DispatchControllerBuilder port(final int port) {
            this.port = port;
            return this;
        }

        /**
         * Устанавливает контейнер обработчиков запросов
         * @param controllerContainer контейнер обработчиков
         * @return текущий объект-строитель
         */
        public DispatchControllerBuilder controllers(final ControllerContainer controllerContainer) {
            this.controllerContainer = controllerContainer;
            return this;
        }

        /**
         * Создает экземпляр главного обработчика
         * @return экземпляр класса {@code DispatchController}
         */
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
        log.info("Dispatch controller stopped");
    }

    /**
     * Запускает обработчик входящий запросов
     */
    public void listen() {
        log.info("Starting new dispatch controller thread");
        new Thread(() -> {

            try (final ServerSocket serverSocket = new ServerSocket(this.port)) {
                log.info("Dispatch controller successfully started");
                log.info("Listening to messages at port {}", this.port);
                final HttpMessageParser httpMessageParser = new HttpMessageParser();
                while (!isStopped) {
                    final Socket socket = serverSocket.accept();
                    new Thread(() -> {
                        log.info("Processing http request with InetAddress: {}", socket.getRemoteSocketAddress());
                        try {
                            final HttpRequest httpRequest = httpMessageParser.parseHttpRequest(socket.getInputStream());

                            final Controller controller = this.controllerContainer.getByPath(httpRequest.uri().getPath());
                            if (Objects.isNull(controller)) {
                                socket.close();
                                throw new IllegalArgumentException("Controller that listen to " + httpRequest.uri().getPath() + " does not found");
                            }

                            final HttpResponse httpResponse = handleHttpRequest(httpRequest, controller);
                            socket.getOutputStream().write(httpResponse.serialize());
                            socket.getOutputStream().close();
                            socket.close();
                        } catch (Exception e) {
                            log.error("Unexpected error while processing request:");
                            log.error(e.getMessage());
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
