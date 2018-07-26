package com.nepxion.discovery.console.desktop.entity;

/**
 * <p>Title: Nepxion Discovery</p>
 * <p>Description: Nepxion Discovery</p>
 * <p>Copyright: Copyright (c) 2017-2050</p>
 * <p>Company: Nepxion</p>
 * @author Haojun Ren
 * @version 1.0
 */

import java.io.Serializable;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.nepxion.discovery.console.desktop.constant.ConsoleConstant;

public class InstanceEntity implements Serializable {
    private static final long serialVersionUID = -3001191508072178378L;

    private String serviceId;
    private String version;
    private String dynamicVersion;
    private String host;
    private int port;
    private String rule;
    private String dynamicRule;
    private Map<String, String> metaData;

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDynamicVersion() {
        return dynamicVersion;
    }

    public void setDynamicVersion(String dynamicVersion) {
        this.dynamicVersion = dynamicVersion;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getRule() {
        return rule;
    }

    public void setRule(String rule) {
        this.rule = rule;
    }

    public String getDynamicRule() {
        return dynamicRule;
    }

    public void setDynamicRule(String dynamicRule) {
        this.dynamicRule = dynamicRule;
    }

    public Map<String, String> getMetaData() {
        return metaData;
    }

    public void setMetaData(Map<String, String> metaData) {
        this.metaData = metaData;
    }

    public String getFilter() {
        String filterKey = metaData.get(ConsoleConstant.SPRING_APPLICATION_GROUP_KEY);
        if (StringUtils.isEmpty(filterKey)) {
            return "";
        }

        String filter = metaData.get(filterKey);
        if (filter == null) {
            return "";
        }

        return filter;
    }

    public String getPlugin() {
        String plugin = metaData.get(ConsoleConstant.SPRING_APPLICATION_DISCOVERY_PLUGIN);
        if (plugin == null) {
            return "";
        }

        return plugin;
    }

    public boolean isDiscoveryControlEnabled() {
        String flag = metaData.get(ConsoleConstant.SPRING_APPLICATION_DISCOVERY_CONTROL_ENABLED);
        if (flag == null) {
            return true;
        }

        return Boolean.valueOf(flag);
    }

    public boolean isConfigRestControlEnabled() {
        String flag = metaData.get(ConsoleConstant.SPRING_APPLICATION_CONFIG_REST_CONTROL_ENABLED);
        if (flag == null) {
            return true;
        }

        return Boolean.valueOf(flag);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object object) {
        return EqualsBuilder.reflectionEquals(this, object);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}