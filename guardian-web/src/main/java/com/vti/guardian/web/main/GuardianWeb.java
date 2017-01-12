package com.vti.guardian.web.main;

import javax.annotation.Resource;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@SpringBootApplication
@ComponentScan(basePackages = "com.vti.guardian.web")
public class GuardianWeb extends WebMvcConfigurerAdapter implements EmbeddedServletContainerCustomizer {

	@Resource
	private Environment environment;

	@Override
	public void customize(ConfigurableEmbeddedServletContainer container) {
		container.setPort(Integer.valueOf(environment.getProperty("sever.port")));
	}

	@Bean(name = "zkClient")
	public CuratorFramework getCurator() {
		RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, Integer.MAX_VALUE);
		CuratorFramework zkClient = CuratorFrameworkFactory.newClient(environment.getProperty("zkurl"), 1000,
				100 * 1000, retryPolicy);
		zkClient.start();
		return zkClient;
	}

	public static void main(String[] args) {
		SpringApplication.run(GuardianWeb.class, args);
	}
}
