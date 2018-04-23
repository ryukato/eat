package com.nhnent.eat.handler;

import com.nhnent.eat.common.Config.Config;
import com.nhnent.eat.communication.communicator.IBaseCommunication;
import com.nhnent.eat.communication.jmx.JMXClient;

import java.util.Collections;
import java.util.List;

public final class ScenarioContext {
  private Config config;
  private String userId;
  private List<IBaseCommunication> listCommunication;
  private JMXClient jmxClient;

  private ScenarioContext(Builder builder) {
    this.config = builder.config;
    this.userId = builder.userId;
    this.listCommunication = builder.listCommunication;
    this.jmxClient = builder.jmxClient;
  }

  public Config getConfig() {
    return config;
  }

  public String getUserId() {
    return userId;
  }

  public JMXClient getJmxClient() {
    return jmxClient;
  }

  public List<IBaseCommunication> getListCommunication() {
    return listCommunication;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private Config config;
    private String userId;
    private List<IBaseCommunication> listCommunication = Collections.emptyList();
    private JMXClient jmxClient;

    public Builder() {
      this.config = Config.builder().create(); // create default config;
    }
    public Builder config(Config config) {
      this.config = config;
      return this;
    }

    public Builder userId(String userId) {
      this.userId = userId;
      return this;
    }

    public Builder jmxClient(JMXClient jmxClient) {
      this.jmxClient = jmxClient;
      return this;
    }

    public Builder listCommunication(List<IBaseCommunication> listCommunication) {
      this.listCommunication = listCommunication;
      return this;
    }

    public ScenarioContext build() {
      if (this.userId == null || this.userId.isEmpty()) {
        throw new RuntimeException("Invalid user id - empty userId is not allowed!");
      }

      if (this.jmxClient == null) {
        throw new RuntimeException("Invalid jmxClient - jmxClient has to be set");
      }
      return new ScenarioContext(this);
    }
  }

}
