package org.owl.services.yaujrest.http;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

/**
 * Объект значение представляющий HTTP-ответ
 * @param version версия HTTP-протокола
 * @param statusCode код ответа
 * @param reason пояснительная фраза ответа
 * @param headers HTTP-заголовки ответа
 * @param body тело ответа
 */
public record HttpResponse(Version version, int statusCode, String reason, Map<String, String> headers, byte[] body) {

    /**
     * Сериализует HTTP-ответ в массив байтов {@code byte[]}
     * @return представление HTTP-ответа в виде {@code byte[]}
     */
    public byte[] serialize() {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(version.toString()).append(' ').append(statusCode).append(' ');
        if (Objects.nonNull(reason)) {
            stringBuilder.append(reason);
        }
        stringBuilder.append("\r\n");

        if (Objects.nonNull(this.headers)) {
            headers.forEach((key, value) -> stringBuilder.append(key).append(": ").append(value).append("\r\n"));
        }
        stringBuilder.append("\r\n").append(new String(body, StandardCharsets.US_ASCII));

        return stringBuilder.toString().getBytes(StandardCharsets.US_ASCII);
    }

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
