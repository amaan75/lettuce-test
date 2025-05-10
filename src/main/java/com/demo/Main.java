package com.demo;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.demo.RedisConnectionFactoryFactory.createConnFactory;

@SpringBootApplication(scanBasePackageClasses = Main.class)
public class Main {
  public static void main(String[] args) {
    System.out.println("Hello, World!");
    SpringApplication.run(Main.class, args);
  }

  @Bean
  RedisTemplate<String, Object> redisTemplate() {
    RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
    redisTemplate.setConnectionFactory(redisConnectionFactory());
    return redisTemplate;
  }

  private RedisConnectionFactory redisConnectionFactory() {
    return createConnFactory();
  }

  @RestController
  @RequiredArgsConstructor
  static class TestController {
    private final TestRedisService testRedisService;

    @GetMapping("/test")
    public String test() {
      return "Hello from TestController!";
    }

    @GetMapping("/test-redis")
    public String testRedis() {
      Object testKey = testRedisService.getValue("testKey");
      if (testKey == null) {
        return "No value found for testKey!";
      }
      return testKey.toString();
    }

    @PostMapping("/test-redis")
    public String postRedis() {
      testRedisService.setValue("testKey", "Hello from Redis!");
      return testRedis();
    }
  }

  @RequiredArgsConstructor
  @Service
  static class TestRedisService {
    private final RedisTemplate<String, Object> redisTemplate;

    public void setValue(String key, Object value) {
      redisTemplate.opsForValue().set(key, value);
    }

    public Object getValue(String key) {
      return redisTemplate.opsForValue().get(key);
    }
  }
}
