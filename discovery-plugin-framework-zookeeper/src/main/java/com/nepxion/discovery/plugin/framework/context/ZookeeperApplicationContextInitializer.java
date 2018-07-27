package com.nepxion.discovery.plugin.framework.context;

/**
 * <p>Title: Nepxion Discovery</p>
 * <p>Description: Nepxion Discovery</p>
 * <p>Copyright: Copyright (c) 2017-2050</p>
 * <p>Company: Nepxion</p>
 * @author Haojun Ren
 * @version 1.0
 */

import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.cloud.zookeeper.discovery.ZookeeperDiscoveryProperties;
import org.springframework.cloud.zookeeper.serviceregistry.ZookeeperServiceRegistry;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

import com.nepxion.discovery.plugin.framework.constant.PluginConstant;
import com.nepxion.discovery.plugin.framework.constant.ZookeeperConstant;
import com.nepxion.discovery.plugin.framework.decorator.ZookeeperServiceRegistryDecorator;

public class ZookeeperApplicationContextInitializer extends PluginApplicationContextInitializer {
    @Override
    protected Object afterInitialization(ConfigurableApplicationContext applicationContext, Object bean, String beanName) throws BeansException {
        if (bean instanceof ZookeeperServiceRegistry) {
            ZookeeperServiceRegistry zookeeperServiceRegistry = (ZookeeperServiceRegistry) bean;

            return new ZookeeperServiceRegistryDecorator(zookeeperServiceRegistry, applicationContext);
        } else if (bean instanceof ZookeeperDiscoveryProperties) {
            ConfigurableEnvironment environment = applicationContext.getEnvironment();

            ZookeeperDiscoveryProperties zookeeperDiscoveryProperties = (ZookeeperDiscoveryProperties) bean;
            zookeeperDiscoveryProperties.setPreferIpAddress(true);

            Map<String, String> metadata = zookeeperDiscoveryProperties.getMetadata();
            metadata.put(PluginConstant.SPRING_APPLICATION_DISCOVERY_PLUGIN, ZookeeperConstant.DISCOVERY_PLUGIN);
            metadata.put(PluginConstant.SPRING_APPLICATION_REGISTER_CONTROL_ENABLED, PluginContextAware.isRegisterControlEnabled(environment).toString());
            metadata.put(PluginConstant.SPRING_APPLICATION_DISCOVERY_CONTROL_ENABLED, PluginContextAware.isDiscoveryControlEnabled(environment).toString());
            metadata.put(PluginConstant.SPRING_APPLICATION_CONFIG_REST_CONTROL_ENABLED, PluginContextAware.isConfigRestControlEnabled(environment).toString());
            metadata.put(PluginConstant.SPRING_APPLICATION_GROUP_KEY, PluginContextAware.getGroupKey(environment));

            return bean;
        } else {
            return bean;
        }
    }
}