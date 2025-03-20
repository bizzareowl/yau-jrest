package org.owl.services.yaujrest.controller;

import java.util.HashMap;
import java.util.Map;

/**
 * Контейнер для хранения обработчиков запросов
 */
public class ControllerContainer {

    /**
     *
     */
    private final Map<String, Controller> lookupMap;

    /**
     *
     * @param lookupMap
     */
    public ControllerContainer(final Map<String, Controller> lookupMap) {
        this.lookupMap = lookupMap;
    }

    /**
     *
     */
    public static final class ControllerContainerBuilder {

        private final Map<String, Controller> lookupMap = new HashMap<>();

        private ControllerContainerBuilder() { }

        public ControllerContainerBuilder addController(final Controller controller) {
            this.lookupMap.put(controller.getPath(), controller);
            return this;
        }

        public ControllerContainer build() {
            return new ControllerContainer(lookupMap);
        }

    }

    /**
     *
     * @return
     */
    public static ControllerContainerBuilder builder() {
        return new ControllerContainerBuilder();
    }

    /**
     *
     * @param path
     * @return
     */
    public Controller getByPath(final String path) {
        return this.lookupMap.getOrDefault(path, null);
    }
}
