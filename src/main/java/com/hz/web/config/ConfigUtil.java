package com.hz.web.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;

import java.util.LinkedHashMap;
import java.util.Map;

public class ConfigUtil implements EnvironmentPostProcessor {

    private static Binder binder;

    private static ConfigurableEnvironment environment;

//    public static String getString(String key) {
//        return environment.getProperty(key, String.class, "");
//    }
//
//    public static <T> T bindProperties(String prefix, Class<T> clazz) {
//        BindResult<T> result = ConfigUtil.binder.bind(prefix, clazz);
//        return result.isBound() ? result.get() : null;
//    }

    /**
     * 通过 META-INF/spring.factories，触发该方法的执行，进行环境变量的加载
     */
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        for (PropertySource<?> propertySource : environment.getPropertySources()) {
            if (propertySource.getName().equals("refreshArgs")) {
                return;
            }
        }
        ConfigUtil.environment = environment;
        ConfigUtil.binder = Binder.get(environment);
    }

    public static Map<String, String> getPrefixProperties(String prefix) {
        Map<String, String> map = new LinkedHashMap<>();
        String normalizedPrefix = prefix.endsWith(".") ? prefix : prefix + ".";

        for (PropertySource<?> propertySource : environment.getPropertySources()) {
            if (propertySource instanceof EnumerablePropertySource) {
                for (String name : ((EnumerablePropertySource<?>) propertySource).getPropertyNames()) {
                    if (name.startsWith(normalizedPrefix)) {
                        String key = name.substring(normalizedPrefix.length());
                        String value = environment.getProperty(name);
                        map.put(key, value);
                    }
                }
            }
        }
        return map;
    }
}
