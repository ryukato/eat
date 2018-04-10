package com.nhnent.eat.common.Config;

import com.google.gson.Gson;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Config {
    private Common common = new Common();
    private Display display = new Display();
    private Server server = new Server();
    private Packet packet = new Packet();
    private Scenario scenario = new Scenario();
    private CustomScenarioAPI customScenarioAPI = new CustomScenarioAPI();
    private QaService qaService = new QaService();
    private JMXConfig jmxConfig = new JMXConfig();


    private static Config instance = null;

    private static String configFilePath;

    public static void setConfigRootPath(String configRootPath) {
        configFilePath = configRootPath;
    }
    /**
     * Get singleton instance
     *
     * @return singleton instance
     */

    public static Config obj() {
        final Logger logger = LoggerFactory.getLogger("EAT.Config");
        if (instance == null) {
            try {
                System.out.println(loadConfiguration(configFilePath));
                instance = new Gson().fromJson(loadConfiguration(configFilePath), Config.class);
            } catch (IOException e) {
                logger.error(ExceptionUtils.getStackTrace(e));
            }
        }
        return instance;
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
        final Logger logger = LoggerFactory.getLogger("EAT.Config");
        Path configPath = Paths.get(configResourcePath);

        if (Files.exists(configPath) && Files.isReadable(configPath)) {
            return configPath;
        } else {
            logger.warn("Config file not exist or not readable => {}, so try to find config resource under classpath", configFilePath);
            try {
                return Paths.get(Config.class.getClassLoader().getResource("config.json").toURI());
            } catch (URISyntaxException e) {
                return null;
            }
        }
    }

    public static <T> T obj(Class<T> configClass, Object instancePara) {
        final Logger logger = LoggerFactory.getLogger("EAT.Config");
        if (instancePara == null) {

            Gson gson = new Gson();
            InputStream inputStream = null;
            if (configFilePath != null) {
                try {
                    logger.info("configRootPath => ", configFilePath);
                    inputStream = new FileInputStream(configFilePath);
                } catch (FileNotFoundException e) {
                    logger.error(ExceptionUtils.getStackTrace(e));
                }
            } else {
                inputStream = Config.class.getClassLoader().getResourceAsStream("config.json");
            }

            StringBuffer sb = new StringBuffer();
            byte[] b = new byte[4096];
            try {
                for (int n; (n = inputStream.read(b)) != -1; ) {
                    sb.append(new String(b, 0, n));
                }
            } catch (IOException e) {
                logger.error(ExceptionUtils.getStackTrace(e));
            }
            String configJson = sb.toString();
            instancePara = gson.fromJson(configJson, configClass);
        }
        instance = (Config)instancePara;
        return (T) instance;
    }

    /**
     * Default Constructor
     */
    public Config() {

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
     * Set Common group config item.
     *
     * @param common Common group config item
     */
    public void setCommon(Common common) {
        this.common = common;
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
     * Set Display Group config item
     *
     * @param display Display Group config item
     */
    public void setDisplay(Display display) {
        this.display = display;
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
     * Set Server group config item
     *
     * @param server Server group config item
     */
    public void setServer(Server server) {
        this.server = server;
    }

    /**
     * Get Scenario group config item
     *
     * @return Scenario group config item
     */
    public Scenario getScenario() {
        return scenario;
    }

    /**
     * Set Scenario group config item
     *
     * @param scenario Scenario group config item
     */
    public void setScenario(Scenario scenario) {
        this.scenario = scenario;
    }

    public CustomScenarioAPI getCustomScenarioAPI() {
        return customScenarioAPI;
    }

    public void setCustomScenarioAPI(CustomScenarioAPI customScenarioAPI) {
        this.customScenarioAPI = customScenarioAPI;
    }

    public QaService getQaService() {
        return qaService;
    }

    public void setQaService(QaService qaService) {
        this.qaService = qaService;
    }


    public JMXConfig getJmxConfig() {
        return jmxConfig;
    }

    public void setJmxConfig(JMXConfig config) {
        this.jmxConfig = config;
    }

    public Packet getPacket() {
        return packet;
    }

    public void setPacket(Packet packet) {
        this.packet = packet;
    }
}
