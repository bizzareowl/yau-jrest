package org.owl.services.yaujrest.http;

/**
 * Объект-значение для представления версии HTTP-протокола
 * @param major мажорная версия
 * @param minor минорная версия
 */
public record Version(int major, int minor) {

    @Override
    public String toString() {
        return "HTTP/" + major + "." + minor;
    }

}
