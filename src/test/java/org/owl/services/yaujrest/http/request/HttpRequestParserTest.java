package org.owl.services.yaujrest.http.request;

import org.junit.jupiter.api.Test;
import org.owl.services.yaujrest.http.Method;
import org.owl.services.yaujrest.http.Version;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class HttpRequestParserTest {

    @Test
    public void parseRegularHttpRequestWithoutBodyTest() {
        final String httpRequestString = """
                GET /path/to/resource?param1=1&param2=2 HTTP/1.1\r
                Host: localhost\r
                \r
                """;

        final HttpRequest httpRequest = new HttpRequestParser().parse(new ByteArrayInputStream(httpRequestString.getBytes()));

        assertEquals(Method.GET, httpRequest.getMethod());
        assertEquals(URI.create("/path/to/resource?param1=1&param2=2"), httpRequest.getUri());
        assertEquals(Version.HTTP_1_1, httpRequest.getVersion());
        assertEquals(Map.of("Host", "localhost"), httpRequest.getHeaders());
        assertNull(httpRequest.getBody());

    }

    @Test
    public void parseRegularHttpRequestWithBodyTest() {
        final String httpRequestString = """
                POST /path/to/resource?param1=1&param2=2 HTTP/1.1\r
                Host: www.example.com\r
                \r
                {
                    "param3": 3,
                    "param4": 4
                }""";

        final HttpRequest httpRequest = new HttpRequestParser().parse(new ByteArrayInputStream(httpRequestString.getBytes()));

        assertEquals(Method.POST, httpRequest.getMethod());
        assertEquals(URI.create("/path/to/resource?param1=1&param2=2"), httpRequest.getUri());
        assertEquals(Version.HTTP_1_1, httpRequest.getVersion());
        assertEquals(Map.of("Host", "www.example.com"), httpRequest.getHeaders());
        assertArrayEquals("{\n    \"param3\": 3,\n    \"param4\": 4\n}".chars().toArray(), httpRequest.getBody());

    }

}
