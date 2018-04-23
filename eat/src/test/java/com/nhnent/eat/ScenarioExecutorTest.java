package com.nhnent.eat;

import com.nhnent.eat.common.Config.Config;
import com.nhnent.eat.communication.jmx.JMXClient;
import com.nhnent.eat.entity.ScenarioUnit;
import com.nhnent.eat.handler.ScenarioContext;
import com.nhnent.eat.handler.ScenarioExecutor;
import com.nhnent.eat.handler.ScenarioLoader;
import org.junit.Test;

import java.util.List;

/**
 * Created by NHNEnt on 2016-08-09.
 */
public class ScenarioExecutorTest {
    @Test
    public void execute() throws Exception {
        Config config = Config.builder().create();
        ScenarioContext scenarioContext = ScenarioContext.builder().userId("test").jmxClient(new JMXClient(config)).build();
        ScenarioExecutor scenExec = new ScenarioExecutor(scenarioContext);

        List<ScenarioUnit> listScenPck;

        ScenarioLoader scenLoader = new ScenarioLoader(config);
        String scenPath = config.getScenario().getScenarioPath();
        String scenFile = scenPath + "\\chat_test.scn";
        System.out.println(scenPath);
        listScenPck = scenLoader.loadScenario(scenFile, "");

        scenExec.runScenario(listScenPck);

        listScenPck.clear();
    }

}