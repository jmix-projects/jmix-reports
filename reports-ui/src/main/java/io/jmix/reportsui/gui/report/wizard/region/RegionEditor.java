/*
 * Copyright (c) 2008-2019 Haulmont.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.jmix.reportsui.gui.report.wizard.region;

import com.haulmont.cuba.core.global.Configuration;
import com.haulmont.cuba.gui.WindowParams;
import com.haulmont.cuba.gui.data.CollectionDatasource;
import com.haulmont.cuba.gui.data.Datasource;
import com.haulmont.cuba.gui.data.impl.AbstractTreeDatasource;
import io.jmix.core.Entity;
import io.jmix.core.Messages;
import io.jmix.core.Metadata;
import io.jmix.reports.entity.wizard.EntityTreeNode;
import io.jmix.reports.entity.wizard.RegionProperty;
import io.jmix.reports.entity.wizard.ReportRegion;
import io.jmix.reportsui.gui.components.actions.OrderableItemMoveAction;
import io.jmix.ui.Notifications;
import io.jmix.ui.action.AbstractAction;
import io.jmix.ui.action.Action;
import io.jmix.ui.action.ItemTrackingAction;
import io.jmix.ui.component.*;
import io.jmix.ui.screen.StandardEditor;
import io.jmix.ui.screen.Subscribe;
import io.jmix.ui.screen.UiController;
import io.jmix.ui.screen.UiDescriptor;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.core.CollectionUtils;

import javax.inject.Named;
import java.util.*;

@UiController("report_Region.edit")
@UiDescriptor("region-edit.xml")
public class RegionEditor extends StandardEditor<ReportRegion> {
    @Named("entityTreeFrame.reportEntityTreeNodeDs")
    protected AbstractTreeDatasource reportEntityTreeNodeDs;
    @Autowired
    protected CollectionDatasource<RegionProperty, UUID> reportRegionPropertiesTableDs;
    @Named("entityTreeFrame.entityTree")
    protected Tree<EntityTreeNode> entityTree;
    @Named("entityTreeFrame.reportPropertyName")
    protected TextField reportPropertyName;
    @Named("entityTreeFrame.reportPropertyNameSearchButton")
    protected Button reportPropertyNameSearchButton;
    @Autowired
    protected Datasource reportRegionDs;
    @Autowired
    protected Button addItem;
    @Autowired
    protected Button removeItem;
    @Autowired
    protected Button upItem;
    @Autowired
    protected Button downItem;
    @Autowired
    protected Table<RegionProperty> propertiesTable;
    @Autowired
    protected Label tipLabel;
    @Autowired
    protected Metadata metadata;
    @Autowired
    protected Configuration configuration;
    @Autowired
    protected Messages messages;
    @Autowired
    protected Notifications notifications;

    protected boolean isTabulated;//if true then user perform add tabulated region action
    protected int regionEditorWindowWidth = 950;
    protected boolean asViewEditor;
    protected EntityTreeNode rootNode;
    protected boolean updatePermission;

    @Subscribe
    protected void onInit(InitEvent event) {
        updatePermission = !Boolean.TRUE.equals(params.get("updateDisabled"));
        isTabulated = ((ReportRegion) WindowParams.ITEM.getEntity(params)).getIsTabulatedRegion();
        asViewEditor = BooleanUtils.isTrue((Boolean) params.get("asViewEditor"));
        params.put("component$reportPropertyName", reportPropertyName);
        reportEntityTreeNodeDs.refresh(params);
        //TODO add disallowing of classes selection in tree
        rootNode = (EntityTreeNode) params.get("rootEntity");
        if (!asViewEditor) {
            if (isTabulated) {
                setTabulatedRegionEditorCaption(((EntityTreeNode) (params.get("rootEntity"))).getName());
            } else {
                setSimpleRegionEditorCaption();
            }
        }
        String group = isTabulated
                ? "selectEntityPropertiesForTableArea"
                : "selectEntityProperties";
        tipLabel.setValue(messages.formatMessage(group, rootNode.getLocalizedName()));
        tipLabel.setHtmlEnabled(true);
        initComponents();
    }

    protected void initComponents() {
        initControlBtnsActions();
        addRegionPropertiesDsListener();
//TODO dialog options
//        getDialogOptions().setWidth(regionEditorWindowWidth).setWidthUnit(SizeUnit.PIXELS);
        if (asViewEditor) {
            initAsViewEditor();
        }
        entityTree.setSelectionMode(Tree.SelectionMode.MULTI);
        entityTree.expand(rootNode.getId());

        Action search = new AbstractAction("search"/*TODO client config, configuration.getConfig(ClientConfig.class).getFilterApplyShortcut()*/) {
            @Override
            public void actionPerform(Component component) {
                reportEntityTreeNodeDs.refresh();
                if (!reportEntityTreeNodeDs.getItemIds().isEmpty()) {
                    entityTree.collapseTree();
                    entityTree.expand(rootNode.getId());
                } else {
                    notifications.create(Notifications.NotificationType.HUMANIZED)
                            .withCaption(messages.getMessage("valueNotFound"))
                            .show();
                }
            }

            @Override
            public String getCaption() {
                return null;
            }
        };
        reportPropertyNameSearchButton.setAction(search);
    }

    protected void initAsViewEditor() {
        reportRegionDs.setAllowCommit(false);
        reportRegionPropertiesTableDs.setAllowCommit(false);
        if (isTabulated) {
            getWindow().setCaption(messages.getMessage("singleEntityDataSetViewEditor"));
        } else {
            getWindow().setCaption(messages.getMessage("multiEntityDataSetViewEditor"));
        }
    }

    protected boolean isUpdatePermitted() {
        return updatePermission;
    }

    protected void setTabulatedRegionEditorCaption(String collectionEntityName) {
        getWindow().setCaption(messages.getMessage("tabulatedRegionEditor"));
    }

    protected void setSimpleRegionEditorCaption() {
        getWindow().setCaption(messages.getMessage("simpleRegionEditor"));
    }

    protected void addRegionPropertiesDsListener() {
        reportRegionPropertiesTableDs.addItemChangeListener(e -> showOrHideSortBtns());
        reportRegionPropertiesTableDs.addCollectionChangeListener(e -> showOrHideSortBtns());
    }

    protected void showOrHideSortBtns() {
        if (propertiesTable.getSelected().size() == reportRegionPropertiesTableDs.getItems().size() ||
                propertiesTable.getSelected().size() == 0) {
            upItem.setEnabled(false);
            downItem.setEnabled(false);
        } else {
            upItem.setEnabled(isUpdatePermitted());
            downItem.setEnabled(isUpdatePermitted());
        }
    }

    protected void initControlBtnsActions() {
        Action addAction = new ItemTrackingAction(entityTree, "addItem") {
            @Override
            public void actionPerform(Component component) {
                @SuppressWarnings("unchecked")
                List<EntityTreeNode> nodesList = CollectionUtils.transform(
                        reportRegionPropertiesTableDs.getItems(), o -> ((RegionProperty) o).getEntityTreeNode());

                Set<EntityTreeNode> alreadyAddedNodes = new HashSet<>(nodesList);

                Set<EntityTreeNode> selectedItems = entityTree.getSelected();
                List<RegionProperty> addedItems = new ArrayList<>();
                boolean alreadyAdded = false;
                for (EntityTreeNode entityTreeNode : selectedItems) {
                    if (entityTreeNode.getWrappedMetaClass() != null) {
                        continue;
                    }
                    if (!alreadyAddedNodes.contains(entityTreeNode)) {
                        RegionProperty regionProperty = metadata.create(RegionProperty.class);
                        regionProperty.setEntityTreeNode(entityTreeNode);
                        regionProperty.setOrderNum((long) reportRegionPropertiesTableDs.getItemIds().size() + 1); //first element must be not zero cause later we do sorting by multiplying that values
                        reportRegionPropertiesTableDs.addItem(regionProperty);
                        addedItems.add(regionProperty);
                    } else {
                        alreadyAdded = true;
                    }
                }
                if (addedItems.isEmpty()) {
                    if (alreadyAdded) {
                        notifications.create(Notifications.NotificationType.TRAY)
                                .withCaption(messages.getMessage("elementsAlreadyAdded"))
                                .show();
                    } else if (selectedItems.size() != 0) {
                        notifications.create(Notifications.NotificationType.HUMANIZED)
                                .withCaption(messages.getMessage("selectPropertyFromEntity"))
                                .show();
                    } else {
                        notifications.create(Notifications.NotificationType.TRAY)
                                .withCaption(messages.getMessage("elementsWasNotAdded"))
                                .show();
                    }
                } else {
                    propertiesTable.setSelected(addedItems);
                }
            }

            @Override
            public String getCaption() {
                return "";
            }

            @Override
            public boolean isEnabled() {
                return super.isEnabled() && isUpdatePermitted();
            }
        };
        entityTree.addAction(addAction);
        addItem.setAction(addAction);

        Action removeAction = new ItemTrackingAction(propertiesTable, "removeItem") {
            @Override
            public void actionPerform(Component component) {
                for (Entity item : propertiesTable.getSelected()) {
                    reportRegionPropertiesTableDs.removeItem((RegionProperty) item);
                    normalizeRegionPropertiesOrderNum();
                }
            }

            @Override
            public String getCaption() {
                return "";
            }

            @Override
            public boolean isEnabled() {
                return super.isEnabled() && isUpdatePermitted();
            }
        };
        propertiesTable.addAction(removeAction);
        removeItem.setAction(removeAction);

        upItem.setAction(new OrderableItemMoveAction<Table<RegionProperty>, RegionProperty>("upItem", OrderableItemMoveAction.Direction.UP, propertiesTable) {
            @Override
            public boolean isEnabled() {
                return super.isEnabled() && isUpdatePermitted();
            }
        });
        downItem.setAction(new OrderableItemMoveAction<Table<RegionProperty>, RegionProperty>("downItem", OrderableItemMoveAction.Direction.DOWN, propertiesTable) {
            @Override
            public boolean isEnabled() {
                return super.isEnabled() && isUpdatePermitted();
            }
        });
    }

    protected void normalizeRegionPropertiesOrderNum() {
        long normalizedIdx = 0;
        List<RegionProperty> allItems = new ArrayList<>(reportRegionPropertiesTableDs.getItems());
        for (RegionProperty item : allItems) {
            item.setOrderNum(++normalizedIdx); //first must to be 1
        }
    }

    @Subscribe
    protected void onBeforeCommit(BeforeCommitChangesEvent event){
        if (reportRegionPropertiesTableDs.getItems().isEmpty()) {
            notifications.create(Notifications.NotificationType.TRAY)
                    .withCaption(messages.getMessage("selectAtLeastOneProp"))
                    .show();
            event.preventCommit();
        }
    }
}