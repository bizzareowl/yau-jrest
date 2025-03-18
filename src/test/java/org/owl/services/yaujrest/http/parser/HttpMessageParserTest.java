package org.owl.services.yaujrest.http.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import org.owl.services.yaujrest.http.HttpRequest;
import org.owl.services.yaujrest.http.HttpResponse;
import org.owl.services.yaujrest.http.Method;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class HttpMessageParserTest {

    @Test
    public void parseHttpRequestRegularHttpRequestWithoutBodyTest() {
        final String httpRequestString = """
                GET /path/to/resource?param1=1&param2=2 HTTP/1.1\r
                Host: localhost\r
                \r
                """;

        final HttpRequest httpRequest = new HttpMessageParser().parseHttpRequest(new ByteArrayInputStream(httpRequestString.getBytes()));

        assertEquals(Method.GET, httpRequest.method());
        assertEquals(URI.create("/path/to/resource?param1=1&param2=2"), httpRequest.uri());
        assertEquals(Map.of("Host", "localhost"), httpRequest.headers());
        assertNull(httpRequest.body());

    }

    @Test
    public void parseHttpRequestRegularHttpRequestWithBodyTest() {
        final String httpRequestString = """
                POST /path/to/resource?param1=1&param2=2 HTTP/1.1\r
                Host: www.example.com\r
                \r
                {
                    "param3": 3,
                    "param4": 4
                }""";

        final HttpRequest httpRequest = new HttpMessageParser().parseHttpRequest(new ByteArrayInputStream(httpRequestString.getBytes()));

        assertEquals(Method.POST, httpRequest.method());
        assertEquals(URI.create("/path/to/resource?param1=1&param2=2"), httpRequest.uri());
        assertEquals(Map.of("Host", "www.example.com"), httpRequest.headers());
        assertEquals("{\n    \"param3\": 3,\n    \"param4\": 4\n}", new String(httpRequest.body(), StandardCharsets.UTF_8));

    }

    @Test
    public void parseHttpResponseRegularHttpResponseWithoutBodyAndHeaders() {
        final String httpResponseString = """
                HTTP/1.1 200 OK\r
                \r
                """;

        final HttpResponse httpResponse = new HttpMessageParser().parseHttpResponse(new ByteArrayInputStream(httpResponseString.getBytes()));

        assertEquals(200, httpResponse.statusCode());
        assertEquals("OK", httpResponse.reason());
    }

    @Test
    public void parseHttpResponseRegularHttpResponseWithBodyAndHeaders() {
        final String httpResponseString = """
                HTTP/1.1 200 OK\r
                Content-Type: text/html;charset=utf-8\r
                \r
                <html><head></head><body><p>Hello World!</p></body></html>""";

        final HttpResponse httpResponse = new HttpMessageParser().parseHttpResponse(new ByteArrayInputStream(httpResponseString.getBytes()));

        assertEquals(200, httpResponse.statusCode());
        assertEquals("OK", httpResponse.reason());
        assertEquals("text/html;charset=utf-8", httpResponse.headers().get("Content-Type"));
        assertEquals("<html><head></head><body><p>Hello World!</p></body></html>", new String(httpResponse.body(), StandardCharsets.UTF_8));
    }

}
