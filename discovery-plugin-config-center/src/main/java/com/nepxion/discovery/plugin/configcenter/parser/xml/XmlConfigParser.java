package com.nepxion.discovery.plugin.configcenter.parser.xml;

/**
 * <p>Title: Nepxion Discovery</p>
 * <p>Description: Nepxion Discovery</p>
 * <p>Copyright: Copyright (c) 2017-2050</p>
 * <p>Company: Nepxion</p>
 * @author Haojun Ren
 * @version 1.0
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nepxion.discovery.plugin.configcenter.constant.ConfigConstant;
import com.nepxion.discovery.plugin.configcenter.parser.xml.dom4j.Dom4JReader;
import com.nepxion.discovery.plugin.framework.config.PluginConfigParser;
import com.nepxion.discovery.plugin.framework.constant.PluginConstant;
import com.nepxion.discovery.plugin.framework.entity.CountFilterEntity;
import com.nepxion.discovery.plugin.framework.entity.DiscoveryEntity;
import com.nepxion.discovery.plugin.framework.entity.DiscoveryServiceEntity;
import com.nepxion.discovery.plugin.framework.entity.FilterHolderEntity;
import com.nepxion.discovery.plugin.framework.entity.FilterType;
import com.nepxion.discovery.plugin.framework.entity.HostFilterEntity;
import com.nepxion.discovery.plugin.framework.entity.RegisterEntity;
import com.nepxion.discovery.plugin.framework.entity.RuleEntity;
import com.nepxion.discovery.plugin.framework.entity.VersionFilterEntity;
import com.nepxion.discovery.plugin.framework.exception.PluginException;

public class XmlConfigParser implements PluginConfigParser {
    private static final Logger LOG = LoggerFactory.getLogger(XmlConfigParser.class);

    @Override
    public RuleEntity parse(String config) {
        if (StringUtils.isEmpty(config)) {
            throw new PluginException("Config is null or empty");
        }

        try {
            Document document = Dom4JReader.getDocument(config);

            Element rootElement = document.getRootElement();

            return parseRoot(config, rootElement);
        } catch (Exception e) {
            throw new PluginException(e.getMessage(), e);
        }
    }

    @SuppressWarnings("rawtypes")
    private RuleEntity parseRoot(String config, Element element) {
        LOG.info("Start to parse rule xml...");

        int registerElementCount = element.elements(ConfigConstant.REGISTER_ELEMENT_NAME).size();
        if (registerElementCount > 1) {
            throw new PluginException("Allow only one element[" + ConfigConstant.REGISTER_ELEMENT_NAME + "] to be configed");
        }

        int discoveryElementCount = element.elements(ConfigConstant.DISCOVERY_ELEMENT_NAME).size();
        if (discoveryElementCount > 1) {
            throw new PluginException("Allow only one element[" + ConfigConstant.DISCOVERY_ELEMENT_NAME + "] to be configed");
        }

        RegisterEntity registerEntity = null;
        DiscoveryEntity discoveryEntity = null;
        for (Iterator elementIterator = element.elementIterator(); elementIterator.hasNext();) {
            Object childElementObject = elementIterator.next();
            if (childElementObject instanceof Element) {
                Element childElement = (Element) childElementObject;

                if (StringUtils.equals(childElement.getName(), ConfigConstant.REGISTER_ELEMENT_NAME)) {
                    registerEntity = new RegisterEntity();
                    parseRegister(childElement, registerEntity);
                } else if (StringUtils.equals(childElement.getName(), ConfigConstant.DISCOVERY_ELEMENT_NAME)) {
                    discoveryEntity = new DiscoveryEntity();
                    parseDiscovery(childElement, discoveryEntity);
                }
            }
        }

        RuleEntity ruleEntity = new RuleEntity();
        ruleEntity.setRegisterEntity(registerEntity);
        ruleEntity.setDiscoveryEntity(discoveryEntity);
        ruleEntity.setContent(config);

        LOG.info("Rule entity=\n{}", ruleEntity);

        return ruleEntity;
    }

    @SuppressWarnings("rawtypes")
    private void parseRegister(Element element, RegisterEntity registerEntity) {
        for (Iterator elementIterator = element.elementIterator(); elementIterator.hasNext();) {
            Object childElementObject = elementIterator.next();
            if (childElementObject instanceof Element) {
                Element childElement = (Element) childElementObject;

                if (StringUtils.equals(childElement.getName(), ConfigConstant.BLACKLIST_ELEMENT_NAME)) {
                    parseHostFilter(childElement, ConfigConstant.BLACKLIST_ELEMENT_NAME, registerEntity);
                } else if (StringUtils.equals(childElement.getName(), ConfigConstant.WHITELIST_ELEMENT_NAME)) {
                    parseHostFilter(childElement, ConfigConstant.WHITELIST_ELEMENT_NAME, registerEntity);
                } else if (StringUtils.equals(childElement.getName(), ConfigConstant.COUNT_ELEMENT_NAME)) {
                    parseCountFilter(childElement, registerEntity);
                }
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private void parseDiscovery(Element element, DiscoveryEntity discoveryEntity) {
        for (Iterator elementIterator = element.elementIterator(); elementIterator.hasNext();) {
            Object childElementObject = elementIterator.next();
            if (childElementObject instanceof Element) {
                Element childElement = (Element) childElementObject;

                if (StringUtils.equals(childElement.getName(), ConfigConstant.BLACKLIST_ELEMENT_NAME)) {
                    parseHostFilter(childElement, ConfigConstant.BLACKLIST_ELEMENT_NAME, discoveryEntity);
                } else if (StringUtils.equals(childElement.getName(), ConfigConstant.WHITELIST_ELEMENT_NAME)) {
                    parseHostFilter(childElement, ConfigConstant.WHITELIST_ELEMENT_NAME, discoveryEntity);
                } else if (StringUtils.equals(childElement.getName(), ConfigConstant.VERSION_ELEMENT_NAME)) {
                    parseVersionFilter(childElement, discoveryEntity);
                }
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private void parseHostFilter(Element element, String filterTypeValue, FilterHolderEntity filterHolderEntity) {
        HostFilterEntity hostFilterEntity = filterHolderEntity.getHostFilterEntity();
        if (hostFilterEntity != null) {
            throw new PluginException("Allow only one filter element to be configed, [" + ConfigConstant.BLACKLIST_ELEMENT_NAME + "] or [" + ConfigConstant.WHITELIST_ELEMENT_NAME + "]");
        }

        hostFilterEntity = new HostFilterEntity();
        hostFilterEntity.setFilterType(FilterType.fromString(filterTypeValue));

        Attribute globalFilterAttribute = element.attribute(ConfigConstant.FILTER_VALUE_ATTRIBUTE_NAME);
        if (globalFilterAttribute != null) {
            String globalFilterValue = globalFilterAttribute.getData().toString().trim();
            List<String> globalFilterValueList = parseList(globalFilterValue);
            hostFilterEntity.setFilterValueList(globalFilterValueList);
        }

        Map<String, List<String>> filterMap = hostFilterEntity.getFilterMap();
        for (Iterator elementIterator = element.elementIterator(); elementIterator.hasNext();) {
            Object childElementObject = elementIterator.next();
            if (childElementObject instanceof Element) {
                Element childElement = (Element) childElementObject;

                if (StringUtils.equals(childElement.getName(), ConfigConstant.SERVICE_ELEMENT_NAME)) {
                    Attribute serviceNameAttribute = childElement.attribute(ConfigConstant.SERVICE_NAME_ATTRIBUTE_NAME);
                    if (serviceNameAttribute == null) {
                        throw new PluginException("Attribute[" + ConfigConstant.SERVICE_NAME_ATTRIBUTE_NAME + "] in element[" + childElement.getName() + "] is missing");
                    }
                    String serviceName = serviceNameAttribute.getData().toString().trim();

                    Attribute filterValueAttribute = childElement.attribute(ConfigConstant.FILTER_VALUE_ATTRIBUTE_NAME);
                    List<String> filterValueList = null;
                    if (filterValueAttribute != null) {
                        String filterValue = filterValueAttribute.getData().toString().trim();
                        filterValueList = parseList(filterValue);
                    }
                    filterMap.put(serviceName, filterValueList);
                }
            }
        }

        filterHolderEntity.setHostFilterEntity(hostFilterEntity);
    }

    @SuppressWarnings("rawtypes")
    private void parseCountFilter(Element element, RegisterEntity registerEntity) {
        CountFilterEntity countFilterEntity = registerEntity.getCountFilterEntity();
        if (countFilterEntity != null) {
            throw new PluginException("Allow only one element[" + ConfigConstant.COUNT_ELEMENT_NAME + "] to be configed");
        }

        countFilterEntity = new CountFilterEntity();

        Attribute globalFilterAttribute = element.attribute(ConfigConstant.FILTER_VALUE_ATTRIBUTE_NAME);
        if (globalFilterAttribute != null) {
            String globalFilterValue = globalFilterAttribute.getData().toString().trim();
            if (StringUtils.isNotEmpty(globalFilterValue)) {
                Integer globalValue = null;
                try {
                    globalValue = Integer.valueOf(globalFilterValue);
                } catch (NumberFormatException e) {
                    throw new PluginException("Attribute[" + ConfigConstant.FILTER_VALUE_ATTRIBUTE_NAME + "] value in element[" + element.getName() + "] is invalid, must be int type", e);
                }
                countFilterEntity.setFilterValue(globalValue);
            }
        }

        Map<String, Integer> filterMap = countFilterEntity.getFilterMap();
        for (Iterator elementIterator = element.elementIterator(); elementIterator.hasNext();) {
            Object childElementObject = elementIterator.next();
            if (childElementObject instanceof Element) {
                Element childElement = (Element) childElementObject;

                if (StringUtils.equals(childElement.getName(), ConfigConstant.SERVICE_ELEMENT_NAME)) {
                    Attribute serviceNameAttribute = childElement.attribute(ConfigConstant.SERVICE_NAME_ATTRIBUTE_NAME);
                    if (serviceNameAttribute == null) {
                        throw new PluginException("Attribute[" + ConfigConstant.SERVICE_NAME_ATTRIBUTE_NAME + "] in element[" + childElement.getName() + "] is missing");
                    }
                    String serviceName = serviceNameAttribute.getData().toString().trim();

                    Integer value = null;
                    Attribute filterValueAttribute = childElement.attribute(ConfigConstant.FILTER_VALUE_ATTRIBUTE_NAME);
                    if (filterValueAttribute != null) {
                        String filterValue = filterValueAttribute.getData().toString().trim();
                        if (StringUtils.isNotEmpty(filterValue)) {
                            try {
                                value = Integer.valueOf(filterValue);
                            } catch (NumberFormatException e) {
                                throw new PluginException("Attribute[" + ConfigConstant.FILTER_VALUE_ATTRIBUTE_NAME + "] value in element[" + childElement.getName() + "] is invalid, must be int type", e);
                            }
                        }
                    }

                    filterMap.put(serviceName, value);
                }
            }
        }

        registerEntity.setCountFilterEntity(countFilterEntity);
    }

    @SuppressWarnings("rawtypes")
    private void parseVersionFilter(Element element, DiscoveryEntity discoveryEntity) {
        VersionFilterEntity versionFilterEntity = discoveryEntity.getVersionFilterEntity();
        if (versionFilterEntity != null) {
            throw new PluginException("Allow only one element[" + ConfigConstant.VERSION_ELEMENT_NAME + "] to be configed");
        }

        versionFilterEntity = new VersionFilterEntity();

        Map<String, List<DiscoveryServiceEntity>> serviceEntityMap = versionFilterEntity.getServiceEntityMap();
        for (Iterator elementIterator = element.elementIterator(); elementIterator.hasNext();) {
            Object childElementObject = elementIterator.next();
            if (childElementObject instanceof Element) {
                Element childElement = (Element) childElementObject;

                if (StringUtils.equals(childElement.getName(), ConfigConstant.SERVICE_ELEMENT_NAME)) {
                    DiscoveryServiceEntity serviceEntity = new DiscoveryServiceEntity();

                    Attribute consumerServiceNameAttribute = childElement.attribute(ConfigConstant.CONSUMER_SERVICE_NAME_ATTRIBUTE_NAME);
                    if (consumerServiceNameAttribute == null) {
                        throw new PluginException("Attribute[" + ConfigConstant.CONSUMER_SERVICE_NAME_ATTRIBUTE_NAME + "] in element[" + childElement.getName() + "] is missing");
                    }
                    String consumerServiceName = consumerServiceNameAttribute.getData().toString().trim();
                    serviceEntity.setConsumerServiceName(consumerServiceName);

                    Attribute providerServiceNameAttribute = childElement.attribute(ConfigConstant.PROVIDER_SERVICE_NAME_ATTRIBUTE_NAME);
                    if (providerServiceNameAttribute == null) {
                        throw new PluginException("Attribute[" + ConfigConstant.PROVIDER_SERVICE_NAME_ATTRIBUTE_NAME + "] in element[" + childElement.getName() + "] is missing");
                    }
                    String providerServiceName = providerServiceNameAttribute.getData().toString().trim();
                    serviceEntity.setProviderServiceName(providerServiceName);

                    Attribute consumerVersionValueAttribute = childElement.attribute(ConfigConstant.CONSUMER_VERSION_VALUE_ATTRIBUTE_NAME);
                    if (consumerVersionValueAttribute != null) {
                        String consumerVersionValue = consumerVersionValueAttribute.getData().toString().trim();
                        List<String> consumerVersionValueList = parseList(consumerVersionValue);
                        serviceEntity.setConsumerVersionValueList(consumerVersionValueList);
                    }

                    Attribute providerVersionValueAttribute = childElement.attribute(ConfigConstant.PROVIDER_VERSION_VALUE_ATTRIBUTE_NAME);
                    if (providerVersionValueAttribute != null) {
                        String providerVersionValue = providerVersionValueAttribute.getData().toString().trim();
                        List<String> providerVersionValueList = parseList(providerVersionValue);
                        serviceEntity.setProviderVersionValueList(providerVersionValueList);
                    }

                    List<DiscoveryServiceEntity> serviceEntityList = serviceEntityMap.get(consumerServiceName);
                    if (serviceEntityList == null) {
                        serviceEntityList = new ArrayList<DiscoveryServiceEntity>();
                        serviceEntityMap.put(consumerServiceName, serviceEntityList);
                    }

                    serviceEntityList.add(serviceEntity);
                }
            }
        }

        discoveryEntity.setVersionFilterEntity(versionFilterEntity);
    }

    private List<String> parseList(String value) {
        if (StringUtils.isEmpty(value)) {
            return null;
        }

        String[] valueArray = StringUtils.split(value, PluginConstant.SEPARATE);

        return Arrays.asList(valueArray);
    }
}