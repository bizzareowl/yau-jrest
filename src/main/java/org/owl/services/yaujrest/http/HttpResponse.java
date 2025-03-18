package org.owl.services.yaujrest.http;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

public record HttpResponse(Version version, int statusCode, String reason, Map<String, String> headers, byte[] body) {

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof HttpResponse(Version versionOther, int statusCodeOther, String reasonOther, Map<String, String> headersOther, byte[] bodyOther))) return false;
        return statusCode == statusCodeOther && Objects.deepEquals(body, bodyOther) && Objects.equals(reason, reasonOther) && Objects.equals(version, versionOther) && Objects.equals(headers, headersOther);
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, statusCode, reason, headers, Arrays.hashCode(body));
    }

    @Override
    public String toString() {
        return "HttpResponse{" +
                "version=" + version +
                ", statusCode=" + statusCode +
                ", reason='" + reason + '\'' +
                ", headers=" + headers +
                ", body=" + Arrays.toString(body) +
                '}';
    }
}
