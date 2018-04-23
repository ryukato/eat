package com.nhnent.eat.common;

import com.nhnent.eat.common.Config.Config;
import org.hamcrest.core.IsNull;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URL;

public class ConfigTest {
  private URL configResourceUrl;
  private String configFilePath;
  @Before
  public void setUp() throws Exception {
    this.configResourceUrl = ConfigTest.class
        .getClassLoader()
        .getResource("common/TestConfigResource.json");
    Assert.assertThat("load configuration resource", configResourceUrl, IsNull.notNullValue());
    this.configFilePath = new File(configResourceUrl.toURI()).getAbsolutePath();
  }

  @After
  public void tearDown() {
  }

  @Test
  public void testCreateConfigObjWithGivenConfigResourcePath() {

    Config config = Config.builder().from(configFilePath).create();
    System.out.println(config.toJsonString());
    Assert.assertThat("configuration is loaded successfully", config, IsNull.notNullValue());
  }

  @Test
  public void testCreateChildConfig() {
    SubConfig subConfig = Config.builder().from(configFilePath).create(SubConfig.class, null);
    System.out.println(subConfig.toJsonString());
    Assert.assertThat("configuration is loaded successfully", subConfig, IsNull.notNullValue());
  }

  public static class SubConfig extends Config {

  }
}
