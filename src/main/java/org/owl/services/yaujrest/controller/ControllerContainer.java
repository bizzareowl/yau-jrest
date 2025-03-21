package org.owl.services.yaujrest.controller;

import java.util.HashMap;
import java.util.Map;

/**
 * Контейнер для хранения обработчиков запросов
 */
public class ControllerContainer {

    private final Map<String, Controller> lookupMap;

    private ControllerContainer(final Map<String, Controller> lookupMap) {
        this.lookupMap = lookupMap;
    }

    /**
     * Класс используемый для создания контейнера обработчиков
     */
    public static final class ControllerContainerBuilder {

        private final Map<String, Controller> lookupMap = new HashMap<>();

        private ControllerContainerBuilder() { }

        /**
         * Добавляет обработчик в контейнер
         * @param controller реализация обработчика
         * @return текущий объект-строитель
         */
        public ControllerContainerBuilder addController(final Controller controller) {
            this.lookupMap.put(controller.getPath(), controller);
            return this;
        }

        /**
         * Создает экземпляр контейнера обработчиков
         * @return контейнер обработчиков
         */
        public ControllerContainer build() {
            return new ControllerContainer(lookupMap);
        }

    }

    /**
     * Возвращает объект-строитель контейнера обработчиков
     * @return объект-строитель контейнера обработчиков
     */
    public static ControllerContainerBuilder builder() {
        return new ControllerContainerBuilder();
    }

    /**
     * Возвращает обработчик согласно его "прослушиваемому" относительному URI
     * @param path относительный URI
     * @return обработчик "прослушивающий" переданный относительны URI
     */
    public Controller getByPath(final String path) {
        return this.lookupMap.getOrDefault(path, null);
    }
}
