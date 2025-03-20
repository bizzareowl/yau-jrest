package org.owl.services.yaujrest.controller;

import org.owl.services.yaujrest.http.HttpRequest;
import org.owl.services.yaujrest.http.HttpResponse;

/**
 * Обработчик HTTP-запросов по определенному URI.
 * <p>
 * Реализации данного абстрактного класса должны переопределять только те методы,
 * HTTP-методы которых поддерживаются определенными реализациями.
 * <p>
 * Например:
 * <p>
 * <blockquote><pre>
 * public class GetController extends Controller {
 *
 *     {@code @Override}
 *     public HttpResponse doGet(HttpRequest httpRequest) {
 *         return new HttpResponse(new Version(1, 1), 200, "OK", null, "Hello, World!".getBytes(StandardCharsets.UTF_8);
 *     }
 * }
 * </pre></blockquote>
 */
public abstract class Controller {

    /**
     * Относительный URI для "прослушивания" обработчиков
     */
    private final String path;

    /**
     * Создает обработчик с установленным относительным URI для "прослушивания"
     * @param path относительный URI
     */
    protected Controller(String path) {
        this.path = path;
    }

    /**
     * Возвращает относительный URI, который "слушает" данный обработчик
     * @return строковое представление относительного URI
     */
    public String getPath() {
        return path;
    }

    /**
     * Выполняет GET-запрос
     * @param httpRequest HTTP-запрос c GET методом
     * @return результат выполнения GET-запроса
     */
    public HttpResponse doGet(final HttpRequest httpRequest) {
        throw new NotImplementedException("Method not implemented");
    }

    /**
     * Выполняет POST-запрос
     * @param httpRequest HTTP-запрос c POST методом
     * @return результат выполнения POST-запроса
     */
    public HttpResponse doPost(final HttpRequest httpRequest) {
        throw new NotImplementedException("Method not implemented");
    }

    /**
     * Выполняет PUT-запрос
     * @param httpRequest HTTP-запрос c PUT методом
     * @return результат выполнения PUT-запроса
     */
    public HttpResponse doPut(final HttpRequest httpRequest) {
        throw new NotImplementedException("Method not implemented");
    }

    /**
     * Выполняет DELETE-запрос
     * @param httpRequest HTTP-запрос c DELETE методом
     * @return результат выполнения DELETE-запроса
     */
    public HttpResponse doDelete(final HttpRequest httpRequest) {
        throw new NotImplementedException("Method not implemented");
    }

}
