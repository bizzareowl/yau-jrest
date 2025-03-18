package org.owl.services.yaujrest.http;

import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

public record HttpRequest(Method method, URI uri, Version version, Map<String, String> headers, byte[] body) {

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof HttpRequest(Method methodOther, URI uriOther, Version versionOther, Map<String, String> headersOther, byte[] bodyOther))) return false;
        return method == methodOther && Objects.equals(uri, uriOther) && Objects.equals(version, versionOther) && Objects.equals(headers, headersOther) && Objects.deepEquals(body, bodyOther);
    }

    @Override
    public int hashCode() {
        return Objects.hash(method, uri, version, headers, Arrays.hashCode(body));
    }

    @Override
    public String toString() {
        return "HttpRequest{" +
                "method=" + method +
                ", uri=" + uri +
                ", version=" + version +
                ", headers=" + headers +
                ", body=" + Arrays.toString(body) +
                '}';
    }
}
