package com.nhnent.eat.common.Config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The instance of this class is immutable but not singleton.
 * So be careful not creating many instances containing same configuration.
 */
public class Config {
    private Common common;
    private Display display;
    private Server server;
    private Packet packet;
    private Scenario scenario;
    private CustomScenarioAPI customScenarioAPI;
    private QaService qaService;
    private JMXConfig jmxConfig;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String configFilePath;

        public Builder from(String configFilePath) {
            this.configFilePath = configFilePath;
            return this;
        }

        public Config create() {
            try {
                return new Gson().fromJson(loadConfiguration(configFilePath), Config.class);
            } catch (IOException e) {
              throw new RuntimeException("Fail to create Config instance with path: " + this.configFilePath + "\ncause: " + e.getMessage());
            }
        }

        public Config create(String configFilePath) {
            this.from(configFilePath);
            return this.create();
        }

        public <T extends Config> T create(Class<T> configClass, T t) {
            if (t == null) {
                try {
                    String configJson = loadConfiguration(configFilePath);
                    return new Gson().fromJson(configJson, configClass);
                } catch (IOException e) {
                    throw new RuntimeException("Fail to create Config instance with path: " + this.configFilePath + "\ncause: " + e.getMessage());
                }
            }
            return t;

        }
    }

    public static String loadConfiguration(String configResourcePath) throws IOException {
        Path configPath = findConfigResourcePath(configResourcePath);
        return Optional.ofNullable(configPath)
            .map(path -> readLinesFrom(path).collect(Collectors.joining(System.lineSeparator()))
            ).orElseThrow( () -> new RuntimeException("Failed to load config resource" + configResourcePath));
    }

    private static Stream<String> readLinesFrom(Path path) {
        try {
            return Files.lines(path);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read config resource", e);
        }
    }

    private static Path findConfigResourcePath(String configResourcePath) {
        return Optional.ofNullable(configResourcePath)
            .filter(s -> s != null)
            .map(s -> Paths.get(configResourcePath))
            .filter(path -> Files.exists(path) && Files.isReadable(path))
            .orElse(getDefaultResourcePath());

    }

    private static Path getDefaultResourcePath() {
        try {
            return Paths.get(Config.class.getClassLoader().getResource("config.json").toURI());
        } catch (URISyntaxException e) {
            return null;
        }
    }

    /**
     * Default Constructor, but create instance with Builder
     */
    protected Config() {

    }

    /**
     * Get Common group config item.
     *
     * @return Common group config item
     */
    public Common getCommon() {
        return common;
    }

    /**
     * Get Display Group config item.
     *
     * @return Display Group config item
     */
    public Display getDisplay() {
        return display;
    }

    /**
     * Get Server group config item.
     *
     * @return Server group config item
     */
    public Server getServer() {
        return server;
    }

    /**
     * Get Scenario group config item
     *
     * @return Scenario group config item
     */
    public Scenario getScenario() {
        return scenario;
    }

    public CustomScenarioAPI getCustomScenarioAPI() {
        return customScenarioAPI;
    }

    public QaService getQaService() {
        return qaService;
    }

    public JMXConfig getJmxConfig() {
        return jmxConfig;
    }

    public Packet getPacket() {
        return packet;
    }

    @Override
    public String toString() {
        return "Config{" +
            "common=" + common +
            ", display=" + display +
            ", server=" + server +
            ", packet=" + packet +
            ", scenario=" + scenario +
            ", customScenarioAPI=" + customScenarioAPI +
            ", qaService=" + qaService +
            ", jmxConfig=" + jmxConfig +
            '}';
    }

    public String toJsonString() {
        return new GsonBuilder().setPrettyPrinting().create().toJson(this);
    }
}
