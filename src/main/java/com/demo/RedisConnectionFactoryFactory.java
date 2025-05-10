package com.demo;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.ReadFrom;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.data.redis.connection.RedisConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStaticMasterReplicaConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class RedisConnectionFactoryFactory {

  static final ClientResources clientResources =
      DefaultClientResources.builder()
          // assuming that every machine has at least 2vCores given to it.
          .computationThreadPoolSize(Math.max(4, Runtime.getRuntime().availableProcessors() * 2))
          .ioThreadPoolSize(Math.max(4, Runtime.getRuntime().availableProcessors() * 2))
          .build();

  @Data
  static class RedisHostConfiguration {
    private String host;
    private int port;
  }

  static final String hosts =
      System.getProperty("redis.hosts", "localhost:6379,localhost:6479,localhost:6579");

  private static List<RedisHostConfiguration> parseRedisHostConfigurations() {

    List<RedisHostConfiguration> configurations = new ArrayList<>();
    String[] hostEntries = hosts.split(",");
    for (String entry : hostEntries) {
      String[] parts = entry.split(":");
      if (parts.length == 2) {
        RedisHostConfiguration config = new RedisHostConfiguration();
        config.setHost(parts[0]);
        config.setPort(Integer.parseInt(parts[1]));
        configurations.add(config);
      }
    }
    log.info("redis.hosts: {}", hosts);
    return configurations;
  }

  public static RedisConnectionFactory createConnFactory() {
    final RedisConfiguration rConfig = createRedisConfiguration();
    return createConnectionFactory(rConfig);
  }

  private static RedisConfiguration createRedisConfiguration() {
    List<RedisHostConfiguration> configs = parseRedisHostConfigurations();

    RedisHostConfiguration host = configs.get(0);
    RedisStaticMasterReplicaConfiguration redisStandaloneConfiguration =
        new RedisStaticMasterReplicaConfiguration(host.getHost(), host.getPort());
    redisStandaloneConfiguration.setDatabase(0);
    final List<RedisHostConfiguration> serverHosts = new ArrayList<>(configs);
    serverHosts.removeIf(host::equals);

    serverHosts.forEach(
        server -> redisStandaloneConfiguration.addNode(server.getHost(), server.getPort()));
    return redisStandaloneConfiguration;
  }

  private static LettuceConnectionFactory createConnectionFactory(RedisConfiguration rConfig) {
    final LettucePoolingClientConfiguration.LettucePoolingClientConfigurationBuilder builder =
        LettucePoolingClientConfiguration.builder();
    if (rConfig instanceof RedisConfiguration.StaticMasterReplicaConfiguration) {
      builder.readFrom(ReadFrom.MASTER);
    }
    final LettuceClientConfiguration config =
        builder
            .poolConfig(getRedisPoolConfig())
            .clientOptions(
                ClientOptions.builder()
                    .autoReconnect(true)
                    .cancelCommandsOnReconnectFailure(true)
                    .socketOptions(
                        SocketOptions.builder().connectTimeout(Duration.ofSeconds(1)).build())
                    .timeoutOptions(TimeoutOptions.enabled(Duration.ofSeconds(3)))
                    .build())
            .commandTimeout(Duration.ofSeconds(3))
            .clientResources(clientResources)
            .clientName("test-app-bug")

            .build();
    final LettuceConnectionFactory factory = new LettuceConnectionFactory(rConfig, config);
    factory.setShareNativeConnection(true);
    factory.afterPropertiesSet();
    return factory;
  }

  private static GenericObjectPoolConfig getRedisPoolConfig() {
    GenericObjectPoolConfig poolConfig = new RedisConnectionPoolConfig();
    poolConfig.setMaxIdle(5);
    poolConfig.setMinIdle(1);
    poolConfig.setMaxTotal(100);
    poolConfig.setBlockWhenExhausted(true);
    poolConfig.setMaxWait(Duration.ofMillis(500));
    return poolConfig;
  }

  static class RedisConnectionPoolConfig<T> extends GenericObjectPoolConfig<T> {
    public RedisConnectionPoolConfig() {
      // defaults to make your life with connection pool easier :)
      setTestWhileIdle(true);
      setMinEvictableIdleTime(Duration.ofMinutes(1));
      setTimeBetweenEvictionRuns(Duration.ofSeconds(30));
      setNumTestsPerEvictionRun(10);
    }
  }
}
