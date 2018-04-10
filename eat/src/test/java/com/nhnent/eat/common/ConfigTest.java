package com.nhnent.eat.common;

import com.nhnent.eat.common.Config.Config;
import com.sun.source.tree.AssertTree;
import org.hamcrest.core.Is;
import org.hamcrest.core.IsNot;
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
    Config.setConfigRootPath(null);
  }


  @Test
  public void testLoadConfigurationWithGivenPath() throws Exception {
    String configContent = Config.loadConfiguration(configFilePath);
    Assert.assertThat("configuration content", configContent, IsNull.notNullValue());
  }

  @Test
  public void testLoadConfigurationWithClasspathResource() throws Exception {
    String configContent = Config.loadConfiguration("not_exists path");
    Assert.assertThat("configuration content", configContent, IsNull.notNullValue());
  }

  @Test
  public void testCreateConfigObjWithGivenConfigResourcePath() {
    Config.setConfigRootPath(configFilePath);
    Config config = Config.obj();
    System.out.println(config);
    Assert.assertThat("configuration is loaded successfully", config, IsNull.notNullValue());
  }
}
