package com.nepxion.discovery.console.desktop.workspace;

/**
 * <p>Title: Nepxion Discovery</p>
 * <p>Description: Nepxion Discovery</p>
 * <p>Copyright: Copyright (c) 2017-2050</p>
 * <p>Company: Nepxion</p>
 * @author Haojun Ren
 * @version 1.0
 */

import twaver.AlarmSeverity;
import twaver.BlinkingRule;
import twaver.Element;
import twaver.Generator;
import twaver.TWaverConst;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;

import org.apache.commons.lang3.StringUtils;

import com.nepxion.cots.twaver.element.TElement;
import com.nepxion.cots.twaver.element.TElementManager;
import com.nepxion.cots.twaver.element.TGroup;
import com.nepxion.cots.twaver.element.TGroupType;
import com.nepxion.cots.twaver.element.TNode;
import com.nepxion.cots.twaver.graph.TGraphBackground;
import com.nepxion.cots.twaver.graph.TGraphManager;
import com.nepxion.discovery.console.desktop.constant.ConsoleConstant;
import com.nepxion.discovery.console.desktop.controller.ServiceController;
import com.nepxion.discovery.console.desktop.entity.InstanceEntity;
import com.nepxion.discovery.console.desktop.entity.ResultEntity;
import com.nepxion.discovery.console.desktop.icon.ConsoleIconFactory;
import com.nepxion.discovery.console.desktop.locale.ConsoleLocale;
import com.nepxion.discovery.console.desktop.ui.UIFactory;
import com.nepxion.discovery.console.desktop.workspace.topology.AbstractTopology;
import com.nepxion.discovery.console.desktop.workspace.topology.LocationEntity;
import com.nepxion.discovery.console.desktop.workspace.topology.TopologyEntity;
import com.nepxion.discovery.console.desktop.workspace.topology.TopologyEntityType;
import com.nepxion.swing.action.JSecurityAction;
import com.nepxion.swing.button.ButtonManager;
import com.nepxion.swing.button.JClassicButton;
import com.nepxion.swing.button.JClassicMenuButton;
import com.nepxion.swing.combobox.JBasicComboBox;
import com.nepxion.swing.dialog.JExceptionDialog;
import com.nepxion.swing.dialog.JOptionDialog;
import com.nepxion.swing.handle.HandleManager;
import com.nepxion.swing.icon.IconFactory;
import com.nepxion.swing.label.JBasicLabel;
import com.nepxion.swing.layout.filed.FiledLayout;
import com.nepxion.swing.layout.table.TableLayout;
import com.nepxion.swing.locale.SwingLocale;
import com.nepxion.swing.menuitem.JBasicMenuItem;
import com.nepxion.swing.menuitem.JBasicRadioButtonMenuItem;
import com.nepxion.swing.optionpane.JBasicOptionPane;
import com.nepxion.swing.popupmenu.JBasicPopupMenu;
import com.nepxion.swing.scrollpane.JBasicScrollPane;
import com.nepxion.swing.tabbedpane.JBasicTabbedPane;
import com.nepxion.swing.textarea.JBasicTextArea;
import com.nepxion.swing.textfield.JBasicTextField;
import com.nepxion.swing.textfield.number.JNumberTextField;

public class ServiceTopology extends AbstractTopology {
    private static final long serialVersionUID = 1L;

    public static final String NO_FILTER = ConsoleLocale.getString("no_service_cluster_filter");

    private LocationEntity groupLocationEntity = new LocationEntity(120, 250, 280, 0);
    private LocationEntity nodeLocationEntity = new LocationEntity(0, 0, 120, 100);
    private TopologyEntity serviceGroupEntity = new TopologyEntity(TopologyEntityType.SERVICE, true, true);
    private TopologyEntity notServiceGroupEntity = new TopologyEntity(TopologyEntityType.MQ, true, true);
    private TopologyEntity serviceNodeEntity = new TopologyEntity(TopologyEntityType.SERVICE, true, false);
    private TopologyEntity notServiceNodeEntity = new TopologyEntity(TopologyEntityType.MQ, true, false);
    private Map<String, Point> groupLocationMap = new HashMap<String, Point>();

    private TGraphBackground background;
    private JBasicMenuItem executeGrayReleaseMenuItem;
    private JBasicMenuItem refreshGrayStateMenuItem;
    private JBasicMenuItem executeGrayRouterMenuItem;
    private JBasicRadioButtonMenuItem ruleToConfigCenterRadioButtonMenuItem;
    private JBasicRadioButtonMenuItem ruleToServiceRadioButtonMenuItem;
    private FilterPanel filterPanel;
    private GrayPanel grayPanel;
    private JBasicTextArea resultTextArea;
    private RouterTopology routerTopology;
    private LayoutDialog layoutDialog;

    private Map<String, List<InstanceEntity>> globalInstanceMap;
    private String globalFilter;

    public ServiceTopology() {
        initializeToolBar();
        initializeTopology();
    }

    @Override
    protected void initializePopupMenu() {
        super.initializePopupMenu();

        executeGrayReleaseMenuItem = new JBasicMenuItem(createExecuteGrayReleaseAction());
        refreshGrayStateMenuItem = new JBasicMenuItem(createRefreshGrayStateAction());
        executeGrayRouterMenuItem = new JBasicMenuItem(createExecuteGrayRouterAction());
        popupMenu.add(executeGrayReleaseMenuItem, 0);
        popupMenu.add(executeGrayRouterMenuItem, 1);
        popupMenu.add(refreshGrayStateMenuItem, 2);
    }

    @Override
    protected JBasicPopupMenu popupMenuGenerate() {
        super.popupMenuGenerate();

        TGroup group = TElementManager.getSelectedGroup(dataBox);

        TNode node = TElementManager.getSelectedNode(dataBox);
        executeGrayRouterMenuItem.setVisible(node != null && isPlugin(node));

        TElement element = TElementManager.getSelectedElement(dataBox);
        executeGrayReleaseMenuItem.setVisible(element != null && isPlugin(element));
        refreshGrayStateMenuItem.setVisible(element != null && isPlugin(element));

        if (group != null || node != null || element != null) {
            return popupMenu;
        }

        return null;
    }

    private void initializeToolBar() {
        ruleToConfigCenterRadioButtonMenuItem = new JBasicRadioButtonMenuItem(ConsoleLocale.getString("rule_control_mode_to_config_center"), ConsoleLocale.getString("rule_control_mode_to_config_center"), true);
        ruleToServiceRadioButtonMenuItem = new JBasicRadioButtonMenuItem(ConsoleLocale.getString("rule_control_mode_to_service"), ConsoleLocale.getString("rule_control_mode_to_service"));
        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(ruleToConfigCenterRadioButtonMenuItem);
        buttonGroup.add(ruleToServiceRadioButtonMenuItem);
        JBasicPopupMenu ruleControlPopupMenu = new JBasicPopupMenu();
        ruleControlPopupMenu.add(ruleToConfigCenterRadioButtonMenuItem);
        ruleControlPopupMenu.add(ruleToServiceRadioButtonMenuItem);
        JClassicMenuButton ruleControllMenubutton = new JClassicMenuButton(ConsoleLocale.getString("rule_control_mode"), ConsoleIconFactory.getSwingIcon("component/advanced_16.png"), ConsoleLocale.getString("rule_control_mode"));
        ruleControllMenubutton.setPopupMenu(ruleControlPopupMenu);

        JToolBar toolBar = getGraph().getToolbar();
        toolBar.addSeparator();
        toolBar.add(Box.createHorizontalStrut(5));
        toolBar.add(new JClassicButton(createShowTopologyAction()));
        toolBar.addSeparator();
        toolBar.add(new JClassicButton(createExecuteGrayReleaseAction()));
        toolBar.add(new JClassicButton(createExecuteGrayRouterAction()));
        toolBar.add(new JClassicButton(createRefreshGrayStateAction()));
        toolBar.add(ruleControllMenubutton);
        toolBar.addSeparator();
        toolBar.add(createConfigButton(true));

        ButtonManager.updateUI(toolBar);
    }

    private void initializeTopology() {
        background = graph.getGraphBackground();
        background.setTitle(ConsoleLocale.getString("title_service_cluster_gray_release"));
        graph.setBlinkingRule(new BlinkingRule() {
            public boolean isBodyBlinking(Element element) {
                return element.getAlarmState().getHighestNativeAlarmSeverity() != null || element.getClientProperty(TWaverConst.PROPERTYNAME_RENDER_COLOR) != null;
            }

            public boolean isOutlineBlinking(Element element) {
                return element.getAlarmState().getPropagateSeverity() != null || element.getClientProperty(TWaverConst.PROPERTYNAME_STATE_OUTLINE_COLOR) != null;
            }
        });
        graph.setElementStateOutlineColorGenerator(new Generator() {
            public Object generate(Object object) {
                return null;
            }
        });

        setGroupAutoExpand(true);
        setLinkAutoHide(true);
    }

    private String getFilter(TElement element) {
        return element.getClientProperty(ConsoleConstant.FILTER).toString();
    }

    private void setFilter(TElement element, String filter) {
        element.putClientProperty(ConsoleConstant.FILTER, filter);
    }

    private String getPlugin(TElement element) {
        return element.getClientProperty(ConsoleConstant.PLUGIN).toString();
    }

    private void setPlugin(TElement element, String plugin) {
        element.putClientProperty(ConsoleConstant.PLUGIN, plugin);
    }

    private boolean isPlugin(TElement element) {
        String plugin = getPlugin(element);

        return StringUtils.isNotEmpty(plugin);
    }

    private void addServices() {
        for (Map.Entry<String, List<InstanceEntity>> entry : globalInstanceMap.entrySet()) {
            String serviceId = entry.getKey();
            List<InstanceEntity> instances = entry.getValue();
            addService(globalFilter, serviceId, instances);
        }
    }

    private void addService(String filterId, String serviceId, List<InstanceEntity> instances) {
        String filter = getValidFilter(instances);
        String plugin = getValidPlugin(instances);

        if (!StringUtils.equals(filterId, NO_FILTER) && !StringUtils.equals(filterId, filter)) {
            return;
        }

        int count = groupLocationMap.size();
        String groupName = getGroupName(serviceId, instances.size(), filter);

        TGroup group = createGroup(groupName, StringUtils.isNotEmpty(plugin) ? serviceGroupEntity : notServiceGroupEntity, groupLocationEntity, count);
        group.setGroupType(TGroupType.ELLIPSE_GROUP_TYPE.getType());
        group.setUserObject(serviceId);
        setFilter(group, filter);
        setPlugin(group, plugin);

        addInstances(group, serviceId, instances);
    }

    private void addInstances(TGroup group, String serviceId, List<InstanceEntity> instances) {
        for (int i = 0; i < instances.size(); i++) {
            InstanceEntity instance = instances.get(i);
            String filter = instance.getFilter();
            String plugin = instance.getPlugin();
            String nodeName = getNodeName(instance);

            TNode node = createNode(nodeName, StringUtils.isNotEmpty(plugin) ? serviceNodeEntity : notServiceNodeEntity, nodeLocationEntity, i);
            node.setUserObject(instance);
            setFilter(node, filter);
            setPlugin(node, plugin);

            group.addChild(node);
        }

        updateGroup(group);

        groupLocationMap.put(serviceId, group.getLocation());

        dataBox.addElement(group);
        TElementManager.addGroupChildren(dataBox, group);
    }

    private String getValidFilter(List<InstanceEntity> instances) {
        // 服务注册发现中心，必须有一个规范，即在同一个服务集群下，必须所有服务的metadata格式一致，例如一个服务配了group，另一个服务没有配group
        // 只取有值的那个
        for (InstanceEntity instance : instances) {
            String filter = instance.getFilter();
            if (StringUtils.isNotEmpty(filter)) {
                return filter;
            }
        }

        return "";
    }

    private String getValidPlugin(List<InstanceEntity> instances) {
        for (InstanceEntity instance : instances) {
            String plugin = instance.getPlugin();
            if (StringUtils.isNotEmpty(plugin)) {
                return plugin;
            }
        }

        return "";
    }

    private Object[] filter(Map<String, List<InstanceEntity>> instanceMap) {
        List<String> filters = new ArrayList<String>();

        for (Map.Entry<String, List<InstanceEntity>> entry : instanceMap.entrySet()) {
            List<InstanceEntity> instances = entry.getValue();
            for (InstanceEntity instance : instances) {
                String filter = instance.getFilter();
                String plugin = instance.getPlugin();
                if (StringUtils.isNotEmpty(plugin) && !filters.contains(filter)) {
                    filters.add(filter);
                }
            }
        }

        if (filters.contains("")) {
            filters.remove("");
        }
        filters.add(NO_FILTER);

        return filters.toArray();
    }

    private Object[] filterServices(TNode node, Map<String, List<InstanceEntity>> instanceMap) {
        Set<String> services = instanceMap.keySet();
        List<String> filterServices = new ArrayList<String>();

        for (String service : services) {
            TGroup group = getGroup(service);
            // node.getParent() != group 表示自己不能路由自己
            if (group != null && isPlugin(group) && node.getParent() != group) {
                filterServices.add(service);
            }
        }

        return filterServices.toArray();
    }

    @SuppressWarnings("unchecked")
    private TGroup getGroup(String serviceId) {
        List<TElement> elements = dataBox.getAllElements();
        for (TElement element : elements) {
            if (element instanceof TGroup) {
                if (StringUtils.equals(element.getUserObject().toString(), serviceId)) {
                    return (TGroup) element;
                }
            }
        }

        return null;
    }

    private String getGroupName(String serviceId, int count, String filter) {
        return ButtonManager.getHtmlText(serviceId + " [" + count + "]" + (StringUtils.isNotEmpty(filter) ? "\n" + filter : ""));
    }

    private void updateGroup(TGroup group) {
        String name = getGroupName(group.getUserObject().toString(), group.childrenSize(), getFilter(group));

        group.setName(name);
    }

    private String getNodeName(InstanceEntity instance) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(instance.getHost()).append(":").append(instance.getPort());
        if (StringUtils.isNotEmpty(instance.getVersion())) {
            stringBuilder.append("\n[V").append(instance.getVersion());
            if (StringUtils.isNotEmpty(instance.getDynamicVersion())) {
                stringBuilder.append(" -> V").append(instance.getDynamicVersion());
            }
            stringBuilder.append("]");
        }

        return ButtonManager.getHtmlText(stringBuilder.toString());
    }

    private void updateNode(TNode node, InstanceEntity instance) {
        String name = getNodeName(instance);
        node.setName(name);
        if (StringUtils.isNotEmpty(instance.getDynamicRule())) {
            node.getAlarmState().clear();
            node.getAlarmState().addAcknowledgedAlarm(AlarmSeverity.WARNING);
        } else if (StringUtils.isNotEmpty(instance.getDynamicVersion())) {
            node.getAlarmState().clear();
            node.getAlarmState().addAcknowledgedAlarm(AlarmSeverity.MINOR);
        } else {
            node.getAlarmState().clear();
        }
    }

    @SuppressWarnings("unchecked")
    private void locateGroups() {
        List<TGroup> groups = TElementManager.getGroups(dataBox);
        for (TGroup group : groups) {
            String key = group.getUserObject().toString();
            if (groupLocationMap.containsKey(key)) {
                group.setLocation(groupLocationMap.get(key));
            }
        }
    }

    private void showTopology() {
        dataBox.clear();
        groupLocationMap.clear();

        addServices();
        locateGroups();

        TGraphManager.setGroupExpand(graph, isGroupAutoExpand());
        TGraphManager.setLinkVisible(graph, !isLinkAutoHide());
    }

    private void updateGrayState(TNode node) {
        InstanceEntity instance = (InstanceEntity) node.getUserObject();
        List<String> versions = ServiceController.getVersions(instance);
        List<String> rules = ServiceController.getRules(instance);
        instance.setVersion(versions.get(0));
        instance.setDynamicVersion(versions.get(1));
        instance.setRule(rules.get(0));
        instance.setDynamicRule(rules.get(1));

        updateNode(node, instance);
    }

    private boolean refreshGrayState(TNode node) {
        boolean hasException = false;

        TGroup group = (TGroup) node.getParent();

        try {
            updateGrayState(node);
        } catch (Exception e) {
            JExceptionDialog.traceException(HandleManager.getFrame(this), ConsoleLocale.getString("query_data_failure"), e);

            group.removeChild(node);
            dataBox.removeElement(node);

            hasException = true;
        }

        updateGroup(group);

        return hasException;
    }

    @SuppressWarnings("unchecked")
    private boolean refreshGrayState(TGroup group) {
        boolean hasException = false;

        List<TNode> nodes = group.getChildren();

        Iterator<TNode> iterator = nodes.iterator();
        while (iterator.hasNext()) {
            TNode node = iterator.next();

            try {
                updateGrayState(node);
            } catch (Exception e) {
                JExceptionDialog.traceException(HandleManager.getFrame(this), ConsoleLocale.getString("query_data_failure"), e);

                iterator.remove();
                dataBox.removeElement(node);

                hasException = true;
            }
        }

        updateGroup(group);

        return hasException;
    }

    private void showResult(Object result) {
        if (resultTextArea == null) {
            resultTextArea = new JBasicTextArea();
            resultTextArea.setLineWrap(true);
            resultTextArea.setPreferredSize(new Dimension(800, 600));
        }
        resultTextArea.setText(result.toString());

        JBasicOptionPane.showOptionDialog(HandleManager.getFrame(this), new JBasicScrollPane(resultTextArea), ConsoleLocale.getString("execute_result"), JBasicOptionPane.DEFAULT_OPTION, JBasicOptionPane.PLAIN_MESSAGE, ConsoleIconFactory.getSwingIcon("banner/edit.png"), new Object[] { SwingLocale.getString("close") }, null, true);
    }

    @Override
    public void showLayout() {
        if (layoutDialog == null) {
            layoutDialog = new LayoutDialog();
        }

        layoutDialog.setToUI();
        layoutDialog.setVisible(true);
        boolean confirmed = layoutDialog.isConfirmed();
        if (confirmed && !dataBox.isEmpty()) {
            showTopology();
        }
    }

    private JSecurityAction createShowTopologyAction() {
        JSecurityAction action = new JSecurityAction(ConsoleLocale.getString("show_topology"), ConsoleIconFactory.getSwingIcon("component/ui_16.png"), ConsoleLocale.getString("show_topology")) {
            private static final long serialVersionUID = 1L;

            public void execute(ActionEvent e) {
                Map<String, List<InstanceEntity>> instanceMap = null;
                try {
                    instanceMap = ServiceController.getInstanceMap();
                } catch (Exception ex) {
                    JExceptionDialog.traceException(HandleManager.getFrame(ServiceTopology.this), ConsoleLocale.getString("get_service_instances_failure"), ex);

                    return;
                }

                Object[] filters = filter(instanceMap);
                if (filterPanel == null) {
                    filterPanel = new FilterPanel();
                    filterPanel.setPreferredSize(new Dimension(320, 60));
                }
                filterPanel.setFilters(filters);

                int selectedValue = JBasicOptionPane.showOptionDialog(HandleManager.getFrame(ServiceTopology.this), filterPanel, ConsoleLocale.getString("service_cluster_filter"), JBasicOptionPane.DEFAULT_OPTION, JBasicOptionPane.PLAIN_MESSAGE, ConsoleIconFactory.getSwingIcon("banner/query.png"), new Object[] { SwingLocale.getString("yes"), SwingLocale.getString("no") }, null, true);
                if (selectedValue != 0) {
                    return;
                }

                globalInstanceMap = instanceMap;
                globalFilter = filterPanel.getFilter();

                String title = ConsoleLocale.getString("title_service_cluster_gray_release");
                if (!StringUtils.equals(globalFilter, NO_FILTER)) {
                    title += " [" + globalFilter + "]";
                }
                background.setTitle(title);

                showTopology();
            }
        };

        return action;
    }

    private JSecurityAction createExecuteGrayReleaseAction() {
        JSecurityAction action = new JSecurityAction(ConsoleLocale.getString("execute_gray_release"), ConsoleIconFactory.getSwingIcon("netbean/action_16.png"), ConsoleLocale.getString("execute_gray_release")) {
            private static final long serialVersionUID = 1L;

            public void execute(ActionEvent e) {
                TGroup group = TElementManager.getSelectedGroup(dataBox);
                TNode node = TElementManager.getSelectedNode(dataBox);
                if (group == null && node == null) {
                    JBasicOptionPane.showMessageDialog(HandleManager.getFrame(ServiceTopology.this), ConsoleLocale.getString("select_a_group_or_node"), SwingLocale.getString("warning"), JBasicOptionPane.WARNING_MESSAGE);

                    return;
                }

                if (group != null && !isPlugin(group)) {
                    JBasicOptionPane.showMessageDialog(HandleManager.getFrame(ServiceTopology.this), ConsoleLocale.getString("group_not_for_gray_release"), SwingLocale.getString("warning"), JBasicOptionPane.WARNING_MESSAGE);

                    return;
                }

                if (node != null && !isPlugin(node)) {
                    JBasicOptionPane.showMessageDialog(HandleManager.getFrame(ServiceTopology.this), ConsoleLocale.getString("node_not_for_gray_release"), SwingLocale.getString("warning"), JBasicOptionPane.WARNING_MESSAGE);

                    return;
                }

                boolean hasException = false;
                if (group != null) {
                    hasException = refreshGrayState(group);
                } else if (node != null) {
                    hasException = refreshGrayState(node);
                }

                if (hasException) {
                    return;
                }

                if (grayPanel == null) {
                    grayPanel = new GrayPanel();
                    grayPanel.setPreferredSize(new Dimension(1300, 800));
                }

                String description = null;
                if (group != null) {
                    grayPanel.setGray(group);

                    description = group.getUserObject().toString();
                } else if (node != null) {
                    grayPanel.setGray(node);

                    InstanceEntity instance = (InstanceEntity) node.getUserObject();

                    description = instance.getServiceId() + " [" + instance.getHost() + ":" + instance.getPort() + "]";
                }

                JBasicOptionPane.showOptionDialog(HandleManager.getFrame(ServiceTopology.this), grayPanel, ConsoleLocale.getString("execute_gray_release") + " - " + description, JBasicOptionPane.DEFAULT_OPTION, JBasicOptionPane.PLAIN_MESSAGE, ConsoleIconFactory.getSwingIcon("banner/navigator.png"), new Object[] { SwingLocale.getString("close") }, null, true);
            }
        };

        return action;
    }

    private JSecurityAction createExecuteGrayRouterAction() {
        JSecurityAction action = new JSecurityAction(ConsoleLocale.getString("execute_gray_router"), ConsoleIconFactory.getSwingIcon("netbean/close_path_16.png"), ConsoleLocale.getString("execute_gray_router")) {
            private static final long serialVersionUID = 1L;

            public void execute(ActionEvent e) {
                TNode node = TElementManager.getSelectedNode(dataBox);
                if (node == null) {
                    JBasicOptionPane.showMessageDialog(HandleManager.getFrame(ServiceTopology.this), ConsoleLocale.getString("select_a_node"), SwingLocale.getString("warning"), JBasicOptionPane.WARNING_MESSAGE);

                    return;
                }

                if (node != null && !isPlugin(node)) {
                    JBasicOptionPane.showMessageDialog(HandleManager.getFrame(ServiceTopology.this), ConsoleLocale.getString("node_not_for_gray_router"), SwingLocale.getString("warning"), JBasicOptionPane.WARNING_MESSAGE);

                    return;
                }

                InstanceEntity instance = (InstanceEntity) node.getUserObject();

                if (routerTopology == null) {
                    routerTopology = new RouterTopology();
                    // routerTopology.setPreferredSize(new Dimension(1300, 800));
                }

                Object[] filterServices = filterServices(node, globalInstanceMap);
                routerTopology.setServices(filterServices);
                routerTopology.setInstance(instance);

                String description = instance.getServiceId() + " [" + instance.getHost() + ":" + instance.getPort() + "]";

                JBasicOptionPane.showOptionDialog(HandleManager.getFrame(ServiceTopology.this), routerTopology, ConsoleLocale.getString("execute_gray_router") + " - " + description, JBasicOptionPane.DEFAULT_OPTION, JBasicOptionPane.PLAIN_MESSAGE, ConsoleIconFactory.getSwingIcon("banner/navigator.png"), new Object[] { SwingLocale.getString("close") }, null, true);
            }
        };

        return action;
    }

    private JSecurityAction createRefreshGrayStateAction() {
        JSecurityAction action = new JSecurityAction(ConsoleLocale.getString("refresh_gray_state"), ConsoleIconFactory.getSwingIcon("netbean/rotate_16.png"), ConsoleLocale.getString("refresh_gray_state")) {
            private static final long serialVersionUID = 1L;

            public void execute(ActionEvent e) {
                TGroup group = TElementManager.getSelectedGroup(dataBox);
                TNode node = TElementManager.getSelectedNode(dataBox);
                if (group == null && node == null) {
                    JBasicOptionPane.showMessageDialog(HandleManager.getFrame(ServiceTopology.this), ConsoleLocale.getString("select_a_group_or_node"), SwingLocale.getString("warning"), JBasicOptionPane.WARNING_MESSAGE);

                    return;
                }

                if (group != null && !isPlugin(group)) {
                    JBasicOptionPane.showMessageDialog(HandleManager.getFrame(ServiceTopology.this), ConsoleLocale.getString("group_not_for_refresh_gray_state"), SwingLocale.getString("warning"), JBasicOptionPane.WARNING_MESSAGE);

                    return;
                }

                if (node != null && !isPlugin(node)) {
                    JBasicOptionPane.showMessageDialog(HandleManager.getFrame(ServiceTopology.this), ConsoleLocale.getString("node_not_for_refresh_gray_state"), SwingLocale.getString("warning"), JBasicOptionPane.WARNING_MESSAGE);

                    return;
                }

                if (group != null) {
                    refreshGrayState(group);
                } else if (node != null) {
                    refreshGrayState(node);
                }
            }
        };

        return action;
    }

    private class FilterPanel extends JPanel {
        private static final long serialVersionUID = 1L;

        private JBasicComboBox filterComboBox;

        public FilterPanel() {
            filterComboBox = new JBasicComboBox();

            setLayout(new FiledLayout(FiledLayout.COLUMN, FiledLayout.FULL, 5));
            add(filterComboBox);
            add(new JLabel(NO_FILTER + " - " + ConsoleLocale.getString("description_no_service_cluster_filter")));
        }

        @SuppressWarnings("unchecked")
        public void setFilters(Object[] filters) {
            filterComboBox.setModel(new DefaultComboBoxModel<>(filters));
        }

        public String getFilter() {
            return filterComboBox.getSelectedItem().toString();
        }
    }

    private class GrayPanel extends JPanel {
        private static final long serialVersionUID = 1L;

        private JBasicTextField dynamicVersionTextField;
        private JPanel dynamicVersionPanel;
        private JBasicTextField localVersionTextField;
        private JPanel localVersionPanel;
        private JLabel versionInfoLabel;
        private JBasicTabbedPane versionTabbedPane;
        private JClassicButton updateVersionButton;
        private JClassicButton clearVersionButton;

        private JBasicTextArea dynamicRuleTextArea;
        private JPanel dynamicRulePanel;
        private JBasicTextArea localRuleTextArea;
        private JPanel localRulePanel;
        private JLabel ruleInfoLabel;
        private JBasicTabbedPane ruleTabbedPane;
        private JClassicButton updateRuleButton;
        private JClassicButton clearRuleButton;

        private TGroup group;
        private TNode node;

        public GrayPanel() {
            setLayout(new BorderLayout());
            add(createVersionPanel(), BorderLayout.NORTH);
            add(createRulePanel(), BorderLayout.CENTER);
        }

        private JPanel createVersionPanel() {
            dynamicVersionTextField = new JBasicTextField();
            dynamicVersionPanel = new JPanel();
            dynamicVersionPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
            dynamicVersionPanel.setLayout(new BorderLayout());
            dynamicVersionPanel.add(dynamicVersionTextField, BorderLayout.CENTER);

            localVersionTextField = new JBasicTextField();
            localVersionTextField.setEditable(false);
            localVersionPanel = new JPanel();
            localVersionPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
            localVersionPanel.setLayout(new BorderLayout());
            localVersionPanel.add(localVersionTextField, BorderLayout.CENTER);

            versionTabbedPane = new JBasicTabbedPane();
            versionTabbedPane.setPreferredSize(new Dimension(versionTabbedPane.getPreferredSize().width, 75));
            versionTabbedPane.addTab(ConsoleLocale.getString("label_dynamic_version"), dynamicVersionPanel, ConsoleLocale.getString("label_dynamic_version"));
            versionTabbedPane.addTab(ConsoleLocale.getString("label_local_version"), localVersionPanel, ConsoleLocale.getString("label_local_version"));

            updateVersionButton = new JClassicButton(createUpdateVersionAction());
            updateVersionButton.setPreferredSize(new Dimension(updateVersionButton.getPreferredSize().width, 30));

            clearVersionButton = new JClassicButton(createClearVersionAction());
            clearVersionButton.setPreferredSize(new Dimension(clearVersionButton.getPreferredSize().width, 30));

            JPanel toolBar = new JPanel();
            toolBar.setLayout(new BoxLayout(toolBar, BoxLayout.X_AXIS));
            toolBar.add(updateVersionButton);
            toolBar.add(clearVersionButton);
            ButtonManager.updateUI(toolBar);

            versionInfoLabel = new JLabel(ConsoleLocale.getString("description_gray_version"), IconFactory.getSwingIcon("question_message.png"), SwingConstants.LEADING);

            JPanel layoutPanel = new JPanel();
            layoutPanel.setLayout(new FiledLayout(FiledLayout.COLUMN, FiledLayout.FULL, 5));
            layoutPanel.add(versionInfoLabel);
            layoutPanel.add(toolBar);

            JPanel panel = new JPanel();
            panel.setBorder(UIFactory.createTitledBorder(ConsoleLocale.getString("title_gray_version_operation")));
            panel.setLayout(new BorderLayout());
            panel.add(versionTabbedPane, BorderLayout.CENTER);
            panel.add(layoutPanel, BorderLayout.SOUTH);

            return panel;
        }

        private JPanel createRulePanel() {
            dynamicRuleTextArea = new JBasicTextArea();
            dynamicRulePanel = new JPanel();
            dynamicRulePanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
            dynamicRulePanel.setLayout(new BorderLayout());
            dynamicRulePanel.add(new JBasicScrollPane(dynamicRuleTextArea), BorderLayout.CENTER);

            localRuleTextArea = new JBasicTextArea();
            localRuleTextArea.setEditable(false);
            localRulePanel = new JPanel();
            localRulePanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
            localRulePanel.setLayout(new BorderLayout());
            localRulePanel.add(new JBasicScrollPane(localRuleTextArea), BorderLayout.CENTER);

            ruleTabbedPane = new JBasicTabbedPane();
            ruleTabbedPane.addTab(ConsoleLocale.getString("label_dynamic_rule"), dynamicRulePanel, ConsoleLocale.getString("label_dynamic_rule"));
            ruleTabbedPane.addTab(ConsoleLocale.getString("label_local_rule"), localRulePanel, ConsoleLocale.getString("label_local_rule"));

            updateRuleButton = new JClassicButton(createUpdateRuleAction());
            updateRuleButton.setPreferredSize(new Dimension(updateRuleButton.getPreferredSize().width, 30));

            clearRuleButton = new JClassicButton(createClearRuleAction());
            updateRuleButton.setPreferredSize(new Dimension(clearRuleButton.getPreferredSize().width, 30));

            JPanel toolBar = new JPanel();
            toolBar.setLayout(new BoxLayout(toolBar, BoxLayout.X_AXIS));
            toolBar.add(updateRuleButton);
            toolBar.add(clearRuleButton);
            ButtonManager.updateUI(toolBar);

            ruleInfoLabel = new JLabel(ConsoleLocale.getString("description_gray_rule_to_config_center"), IconFactory.getSwingIcon("question_message.png"), SwingConstants.LEADING);

            JPanel layoutPanel = new JPanel();
            layoutPanel.setLayout(new FiledLayout(FiledLayout.COLUMN, FiledLayout.FULL, 5));
            layoutPanel.add(ruleInfoLabel);
            layoutPanel.add(toolBar);

            JPanel panel = new JPanel();
            panel.setBorder(UIFactory.createTitledBorder(ConsoleLocale.getString("title_gray_rule_operation")));
            panel.setLayout(new BorderLayout());
            panel.add(ruleTabbedPane, BorderLayout.CENTER);
            panel.add(layoutPanel, BorderLayout.SOUTH);

            return panel;
        }

        @SuppressWarnings("unchecked")
        public void setGray(TGroup group) {
            this.group = group;
            this.node = null;

            boolean versionControlEnabled = ruleToConfigCenterRadioButtonMenuItem.isSelected();
            boolean ruleControlEnabled = ruleToConfigCenterRadioButtonMenuItem.isSelected();
            if (!versionControlEnabled && !ruleControlEnabled) {
                for (Iterator<TNode> iterator = group.children(); iterator.hasNext();) {
                    TNode node = iterator.next();
                    InstanceEntity instance = (InstanceEntity) node.getUserObject();

                    boolean versionEnabled = instance.isDiscoveryControlEnabled();
                    if (versionEnabled) {
                        versionControlEnabled = true;
                    }
                    boolean ruleEnabled = instance.isDiscoveryControlEnabled() && instance.isConfigRestControlEnabled();
                    if (ruleEnabled) {
                        ruleControlEnabled = true;
                    }
                }
            }

            if (versionTabbedPane.getTabCount() == 2) {
                versionTabbedPane.remove(localVersionPanel);
            }
            if (ruleTabbedPane.getTabCount() == 2) {
                ruleTabbedPane.remove(localRulePanel);
            }

            dynamicVersionTextField.setText("");
            localVersionTextField.setText("");
            updateVersionButton.setText(ConsoleLocale.getString("button_batch_update_version"));
            clearVersionButton.setText(ConsoleLocale.getString("button_batch_clear_version"));
            updateVersionButton.setEnabled(versionControlEnabled);
            clearVersionButton.setEnabled(versionControlEnabled);

            dynamicRuleTextArea.setText("");
            localRuleTextArea.setText("");
            updateRuleButton.setText(ConsoleLocale.getString("button_batch_update_rule"));
            clearRuleButton.setText(ConsoleLocale.getString("button_batch_clear_rule"));
            updateRuleButton.setEnabled(ruleControlEnabled);
            clearRuleButton.setEnabled(ruleControlEnabled);

            String ruleInfo = null;
            if (ruleToConfigCenterRadioButtonMenuItem.isSelected()) {
                String filter = getFilter(group);
                String serviceId = group.getUserObject().toString();
                String config = ServiceController.remoteConfigView(filter, serviceId);
                dynamicRuleTextArea.setText(config);
                ruleInfo = ConsoleLocale.getString("description_gray_rule_to_config_center");
            } else {
                ruleInfo = ConsoleLocale.getString("description_gray_rule_to_service");
            }

            ruleInfoLabel.setText(ruleInfo);
        }

        public void setGray(TNode node) {
            this.group = null;
            this.node = node;
            InstanceEntity instance = (InstanceEntity) node.getUserObject();

            boolean versionControlEnabled = instance.isDiscoveryControlEnabled();
            boolean ruleControlEnabled = instance.isDiscoveryControlEnabled() && instance.isConfigRestControlEnabled() && !ruleToConfigCenterRadioButtonMenuItem.isSelected();

            if (versionTabbedPane.getTabCount() == 1) {
                versionTabbedPane.addTab(ConsoleLocale.getString("label_local_version"), localVersionPanel, ConsoleLocale.getString("label_local_version"));
            }
            if (ruleTabbedPane.getTabCount() == 1) {
                ruleTabbedPane.addTab(ConsoleLocale.getString("label_local_rule"), localRulePanel, ConsoleLocale.getString("label_local_rule"));
            }

            dynamicVersionTextField.setText(instance.getDynamicVersion());
            localVersionTextField.setText(instance.getVersion());
            updateVersionButton.setText(ConsoleLocale.getString("button_update_version"));
            clearVersionButton.setText(ConsoleLocale.getString("button_clear_version"));
            updateVersionButton.setEnabled(versionControlEnabled);
            clearVersionButton.setEnabled(versionControlEnabled);

            dynamicRuleTextArea.setText(instance.getDynamicRule());
            localRuleTextArea.setText(instance.getRule());
            updateRuleButton.setText(ConsoleLocale.getString("button_update_rule"));
            clearRuleButton.setText(ConsoleLocale.getString("button_clear_rule"));
            updateRuleButton.setEnabled(ruleControlEnabled);
            clearRuleButton.setEnabled(ruleControlEnabled);

            String ruleInfo = null;
            if (ruleToConfigCenterRadioButtonMenuItem.isSelected()) {
                ruleInfo = ConsoleLocale.getString("description_gray_rule_to_config_center");
            } else {
                ruleInfo = ConsoleLocale.getString("description_gray_rule_to_service");
            }

            ruleInfoLabel.setText(ruleInfo);
        }

        private JSecurityAction createUpdateVersionAction() {
            JSecurityAction action = new JSecurityAction(ConsoleLocale.getString("button_update_version"), ConsoleIconFactory.getSwingIcon("save.png"), ConsoleLocale.getString("button_update_version")) {
                private static final long serialVersionUID = 1L;

                public void execute(ActionEvent e) {
                    String dynamicVersion = dynamicVersionTextField.getText();
                    if (StringUtils.isEmpty(dynamicVersion)) {
                        JBasicOptionPane.showMessageDialog(HandleManager.getFrame(ServiceTopology.this), ConsoleLocale.getString("gray_version_not_null"), SwingLocale.getString("warning"), JBasicOptionPane.WARNING_MESSAGE);

                        return;
                    }

                    if (group != null) {
                        String serviceId = group.getUserObject().toString();
                        List<ResultEntity> results = null;
                        try {
                            results = ServiceController.versionUpdate(serviceId, dynamicVersion);
                        } catch (Exception ex) {
                            JExceptionDialog.traceException(HandleManager.getFrame(ServiceTopology.this), ConsoleLocale.getString("query_data_failure"), ex);

                            refreshGrayState(group);

                            return;
                        }

                        showResult(results);

                        refreshGrayState(group);
                    } else if (node != null) {
                        InstanceEntity instance = (InstanceEntity) node.getUserObject();

                        String result = null;
                        try {
                            result = ServiceController.versionUpdate(instance, dynamicVersion);
                        } catch (Exception ex) {
                            JExceptionDialog.traceException(HandleManager.getFrame(ServiceTopology.this), ConsoleLocale.getString("query_data_failure"), ex);

                            refreshGrayState(node);

                            return;
                        }

                        showResult(result);

                        refreshGrayState(node);
                    }
                }
            };

            return action;
        }

        private JSecurityAction createClearVersionAction() {
            JSecurityAction action = new JSecurityAction(ConsoleLocale.getString("button_clear_version"), ConsoleIconFactory.getSwingIcon("paint.png"), ConsoleLocale.getString("button_clear_version")) {
                private static final long serialVersionUID = 1L;

                public void execute(ActionEvent e) {
                    if (group != null) {
                        String serviceId = group.getUserObject().toString();
                        List<ResultEntity> results = null;
                        try {
                            results = ServiceController.versionClear(serviceId);
                        } catch (Exception ex) {
                            JExceptionDialog.traceException(HandleManager.getFrame(ServiceTopology.this), ConsoleLocale.getString("query_data_failure"), ex);

                            refreshGrayState(group);

                            return;
                        }

                        showResult(results);

                        refreshGrayState(group);
                    } else if (node != null) {
                        InstanceEntity instance = (InstanceEntity) node.getUserObject();

                        String result = null;
                        try {
                            result = ServiceController.versionClear(instance);
                        } catch (Exception ex) {
                            JExceptionDialog.traceException(HandleManager.getFrame(ServiceTopology.this), ConsoleLocale.getString("query_data_failure"), ex);

                            refreshGrayState(node);

                            return;
                        }

                        showResult(result);

                        refreshGrayState(node);
                    }
                }
            };

            return action;
        }

        private JSecurityAction createUpdateRuleAction() {
            JSecurityAction action = new JSecurityAction(ConsoleLocale.getString("button_update_rule"), ConsoleIconFactory.getSwingIcon("save.png"), ConsoleLocale.getString("button_update_rule")) {
                private static final long serialVersionUID = 1L;

                public void execute(ActionEvent e) {
                    String dynamicRule = dynamicRuleTextArea.getText();
                    if (StringUtils.isEmpty(dynamicRule)) {
                        JBasicOptionPane.showMessageDialog(HandleManager.getFrame(ServiceTopology.this), ConsoleLocale.getString("gray_rule_not_null"), SwingLocale.getString("warning"), JBasicOptionPane.WARNING_MESSAGE);

                        return;
                    }

                    if (group != null) {
                        String serviceId = group.getUserObject().toString();

                        if (ruleToConfigCenterRadioButtonMenuItem.isSelected()) {
                            String filter = getFilter(group);
                            String result = null;
                            try {
                                result = ServiceController.remoteConfigUpdate(filter, serviceId, dynamicRule);
                            } catch (Exception ex) {
                                JExceptionDialog.traceException(HandleManager.getFrame(ServiceTopology.this), ConsoleLocale.getString("query_data_failure"), ex);

                                refreshGrayState(group);

                                return;
                            }

                            showResult(result);
                        } else {
                            List<ResultEntity> results = null;
                            try {
                                results = ServiceController.configUpdate(serviceId, dynamicRule);
                            } catch (Exception ex) {
                                JExceptionDialog.traceException(HandleManager.getFrame(ServiceTopology.this), ConsoleLocale.getString("query_data_failure"), ex);

                                refreshGrayState(group);

                                return;
                            }

                            showResult(results);
                        }

                        refreshGrayState(group);
                    } else if (node != null) {
                        InstanceEntity instance = (InstanceEntity) node.getUserObject();

                        String result = null;
                        try {
                            result = ServiceController.configUpdate(instance, dynamicRule);
                        } catch (Exception ex) {
                            JExceptionDialog.traceException(HandleManager.getFrame(ServiceTopology.this), ConsoleLocale.getString("query_data_failure"), ex);

                            refreshGrayState(node);

                            return;
                        }

                        showResult(result);

                        refreshGrayState(node);
                    }
                }
            };

            return action;
        }

        private JSecurityAction createClearRuleAction() {
            JSecurityAction action = new JSecurityAction(ConsoleLocale.getString("button_clear_rule"), ConsoleIconFactory.getSwingIcon("paint.png"), ConsoleLocale.getString("button_clear_rule")) {
                private static final long serialVersionUID = 1L;

                public void execute(ActionEvent e) {
                    if (group != null) {
                        String serviceId = group.getUserObject().toString();
                        if (ruleToConfigCenterRadioButtonMenuItem.isSelected()) {
                            String filter = getFilter(group);
                            String result = null;
                            try {
                                result = ServiceController.remoteConfigClear(filter, serviceId);
                            } catch (Exception ex) {
                                JExceptionDialog.traceException(HandleManager.getFrame(ServiceTopology.this), ConsoleLocale.getString("query_data_failure"), ex);

                                refreshGrayState(group);

                                return;
                            }

                            showResult(result);
                        } else {
                            List<ResultEntity> results = null;
                            try {
                                results = ServiceController.configClear(serviceId);
                            } catch (Exception ex) {
                                JExceptionDialog.traceException(HandleManager.getFrame(ServiceTopology.this), ConsoleLocale.getString("query_data_failure"), ex);

                                refreshGrayState(group);

                                return;
                            }

                            showResult(results);
                        }

                        refreshGrayState(group);
                    } else if (node != null) {
                        InstanceEntity instance = (InstanceEntity) node.getUserObject();

                        String result = null;
                        try {
                            result = ServiceController.configClear(instance);
                        } catch (Exception ex) {
                            JExceptionDialog.traceException(HandleManager.getFrame(ServiceTopology.this), ConsoleLocale.getString("query_data_failure"), ex);

                            refreshGrayState(node);

                            return;
                        }

                        showResult(result);

                        refreshGrayState(node);
                    }
                }
            };

            return action;
        }
    }

    private class LayoutDialog extends JOptionDialog {
        private static final long serialVersionUID = 1L;

        private JNumberTextField groupStartXTextField;
        private JNumberTextField groupStartYTextField;
        private JNumberTextField groupHorizontalGapTextField;
        private JNumberTextField groupVerticalGapTextField;

        private JNumberTextField nodeStartXTextField;
        private JNumberTextField nodeStartYTextField;
        private JNumberTextField nodeHorizontalGapTextField;
        private JNumberTextField nodeVerticalGapTextField;

        public LayoutDialog() {
            super(HandleManager.getFrame(ServiceTopology.this), SwingLocale.getString("layout"), new Dimension(500, 330), true, false, true);

            groupStartXTextField = new JNumberTextField(4, 0, 0, 10000);
            groupStartYTextField = new JNumberTextField(4, 0, 0, 10000);
            groupHorizontalGapTextField = new JNumberTextField(4, 0, 0, 10000);
            groupVerticalGapTextField = new JNumberTextField(4, 0, 0, 10000);

            nodeStartXTextField = new JNumberTextField(4, 0, 0, 10000);
            nodeStartYTextField = new JNumberTextField(4, 0, 0, 10000);
            nodeHorizontalGapTextField = new JNumberTextField(4, 0, 0, 10000);
            nodeVerticalGapTextField = new JNumberTextField(4, 0, 0, 10000);

            double[][] size = {
                    { 100, TableLayout.FILL, 100, TableLayout.FILL },
                    { TableLayout.PREFERRED, TableLayout.PREFERRED }
            };

            TableLayout tableLayout = new TableLayout(size);
            tableLayout.setHGap(5);
            tableLayout.setVGap(5);

            JPanel groupPanel = new JPanel();
            groupPanel.setLayout(tableLayout);
            groupPanel.setBorder(UIFactory.createTitledBorder(ConsoleLocale.getString("group_layout")));
            groupPanel.add(new JBasicLabel(ConsoleLocale.getString("start_x")), "0, 0");
            groupPanel.add(groupStartXTextField, "1, 0");
            groupPanel.add(new JBasicLabel(ConsoleLocale.getString("start_y")), "2, 0");
            groupPanel.add(groupStartYTextField, "3, 0");
            groupPanel.add(new JBasicLabel(ConsoleLocale.getString("horizontal_gap")), "0, 1");
            groupPanel.add(groupHorizontalGapTextField, "1, 1");
            groupPanel.add(new JBasicLabel(ConsoleLocale.getString("vertical_gap")), "2, 1");
            groupPanel.add(groupVerticalGapTextField, "3, 1");

            JPanel nodePanel = new JPanel();
            nodePanel.setLayout(tableLayout);
            nodePanel.setBorder(UIFactory.createTitledBorder(ConsoleLocale.getString("node_layout")));
            nodePanel.add(new JBasicLabel(ConsoleLocale.getString("start_x")), "0, 0");
            nodePanel.add(nodeStartXTextField, "1, 0");
            nodePanel.add(new JBasicLabel(ConsoleLocale.getString("start_y")), "2, 0");
            nodePanel.add(nodeStartYTextField, "3, 0");
            nodePanel.add(new JBasicLabel(ConsoleLocale.getString("horizontal_gap")), "0, 1");
            nodePanel.add(nodeHorizontalGapTextField, "1, 1");
            nodePanel.add(new JBasicLabel(ConsoleLocale.getString("vertical_gap")), "2, 1");
            nodePanel.add(nodeVerticalGapTextField, "3, 1");

            JPanel panel = new JPanel();
            panel.setLayout(new FiledLayout(FiledLayout.COLUMN, FiledLayout.FULL, 5));
            panel.add(groupPanel);
            panel.add(nodePanel);

            setOption(YES_NO_OPTION);
            setIcon(IconFactory.getSwingIcon("banner/navigator.png"));
            setContent(panel);
        }

        @Override
        public boolean confirm() {
            return setFromUI();
        }

        @Override
        public boolean cancel() {
            return true;
        }

        public void setToUI() {
            groupStartXTextField.setText(groupLocationEntity.getStartX() + "");
            groupStartYTextField.setText(groupLocationEntity.getStartY() + "");
            groupHorizontalGapTextField.setText(groupLocationEntity.getHorizontalGap() + "");
            groupVerticalGapTextField.setText(groupLocationEntity.getVerticalGap() + "");

            nodeStartXTextField.setText(nodeLocationEntity.getStartX() + "");
            nodeStartYTextField.setText(nodeLocationEntity.getStartY() + "");
            nodeHorizontalGapTextField.setText(nodeLocationEntity.getHorizontalGap() + "");
            nodeVerticalGapTextField.setText(nodeLocationEntity.getVerticalGap() + "");
        }

        public boolean setFromUI() {
            int groupStartX = 0;
            int groupStartY = 0;
            int groupHorizontalGap = 0;
            int groupVerticalGap = 0;
            int nodeStartX = 0;
            int nodeStartY = 0;
            int nodeHorizontalGap = 0;
            int nodeVerticalGap = 0;

            try {
                groupStartX = Integer.parseInt(groupStartXTextField.getText());
                groupStartY = Integer.parseInt(groupStartYTextField.getText());
                groupHorizontalGap = Integer.parseInt(groupHorizontalGapTextField.getText());
                groupVerticalGap = Integer.parseInt(groupVerticalGapTextField.getText());

                nodeStartX = Integer.parseInt(nodeStartXTextField.getText());
                nodeStartY = Integer.parseInt(nodeStartYTextField.getText());
                nodeHorizontalGap = Integer.parseInt(nodeHorizontalGapTextField.getText());
                nodeVerticalGap = Integer.parseInt(nodeVerticalGapTextField.getText());
            } catch (NumberFormatException e) {
                return false;
            }

            groupLocationEntity.setStartX(groupStartX);
            groupLocationEntity.setStartY(groupStartY);
            groupLocationEntity.setHorizontalGap(groupHorizontalGap);
            groupLocationEntity.setVerticalGap(groupVerticalGap);

            nodeLocationEntity.setStartX(nodeStartX);
            nodeLocationEntity.setStartY(nodeStartY);
            nodeLocationEntity.setHorizontalGap(nodeHorizontalGap);
            nodeLocationEntity.setVerticalGap(nodeVerticalGap);

            return true;
        }
    }
}