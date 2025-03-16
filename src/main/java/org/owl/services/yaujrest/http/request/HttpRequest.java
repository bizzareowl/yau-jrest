package org.owl.services.yaujrest.http.request;

import org.owl.services.yaujrest.http.Method;
import org.owl.services.yaujrest.http.Version;
import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

public class HttpRequest {

    private final Method method;

    private final URI uri;

    private final Version version;

    private final Map<String, String> headers;

    private final int[] body;

    public HttpRequest(Method method, URI uri, Version version) {
        this(method, uri, version, null, null);
    }

    public HttpRequest(Method method, URI uri, Version version, Map<String, String> headers) {
        this(method, uri, version, headers, null);
    }

    public HttpRequest(Method method, URI uri, Version version, Map<String, String> headers, int[] body) {
        this.method = method;
        this.uri = uri;
        this.version = version;
        this.headers = headers;
        this.body = body;
    }

    public Method getMethod() {
        return this.method;
    }

    public URI getUri() {
        return this.uri;
    }

    public Version getVersion() {
        return this.version;
    }

    public Map<String, String> getHeaders() {
        return this.headers;
    }

    public int[] getBody() {
        return this.body;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof HttpRequest that)) return false;
        return method == that.method && Objects.equals(uri, that.uri) && version == that.version && Objects.equals(headers, that.headers) && Objects.deepEquals(body, that.body);
    }

    @Override
    public int hashCode() {
        return Objects.hash(method, uri, version, headers, Arrays.hashCode(body));
    }

    @Override
    public String toString() {
        return this.method.toString() +
                " " + this.uri + " " + "HTTP/1.1" + "\r\n" +
                this.headers.entrySet().stream().sorted().map(entry -> entry.getKey() + ": " + entry.getValue()).reduce((left, right) -> left + "\r\n" + right).orElse("") + "\r\n" +
                bodyToString();
    }

    private String bodyToString() {
        final StringBuilder stringBuilder = new StringBuilder();
        Arrays.stream(this.body).forEach(i -> stringBuilder.append((char) i));
        return stringBuilder.toString();
    }

}
