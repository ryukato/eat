package com.nhnent.eat.communication;

import com.nhnent.eat.common.Config.Config;
import com.nhnent.eat.communication.jmx.JMXClient;
import org.junit.Test;

/**
 * Created by NHNEnt on 2017-03-20.
 */
public class JMXClientTest {
    @Test
    public void connect() throws Exception {
        Config config = Config.builder().create();
        JMXClient jmxClient = new JMXClient(config);
        jmxClient.disconnect();
    }

}