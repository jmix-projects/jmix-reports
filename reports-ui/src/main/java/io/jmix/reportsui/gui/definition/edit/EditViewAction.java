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

package io.jmix.reportsui.gui.definition.edit;

import io.jmix.core.FetchPlan;
import io.jmix.core.FetchPlanProperty;
import io.jmix.core.FetchPlans;
import io.jmix.core.Messages;
import io.jmix.core.metamodel.model.MetaClass;
import io.jmix.core.metamodel.model.MetaProperty;
import io.jmix.reports.app.EntityTree;
import io.jmix.reports.entity.DataSet;
import io.jmix.reports.entity.DataSetType;
import io.jmix.reports.entity.wizard.ReportRegion;
import io.jmix.reportsui.gui.report.wizard.region.RegionEditor;
import io.jmix.ui.Notifications;
import io.jmix.ui.ScreenBuilders;
import io.jmix.ui.action.AbstractAction;
import io.jmix.ui.component.Component;
import io.jmix.ui.component.Window;
import io.jmix.ui.screen.OpenMode;
import io.jmix.ui.screen.Screen;
import io.jmix.ui.screen.StandardCloseAction;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@org.springframework.stereotype.Component("report_EditViewAction")
@Scope("prototype")
public class EditViewAction extends AbstractAction {

    @Autowired
    protected ScreenBuilders screenBuilders;

    @Autowired
    protected Notifications notifications;

    @Autowired
    protected FetchPlans fetchPlans;

    @Autowired
    protected Messages messages;

    protected BandDefinitionEditor bandDefinitionEditor;

    public EditViewAction(BandDefinitionEditor bandDefinitionEditor) {
        super("editView");
        this.bandDefinitionEditor = bandDefinitionEditor;
    }

    @Override
    public void actionPerform(Component component) {
        if (bandDefinitionEditor.dataSets.getSingleSelected() != null) {
            final DataSet dataSet = bandDefinitionEditor.dataSets.getSingleSelected();
            if (DataSetType.SINGLE == dataSet.getType() || DataSetType.MULTI == dataSet.getType()) {
                MetaClass forEntityTreeModelMetaClass = findMetaClassByAlias(dataSet);
                if (forEntityTreeModelMetaClass != null) {

                    final EntityTree entityTree = bandDefinitionEditor.reportWizardService.buildEntityTree(forEntityTreeModelMetaClass);
                    ReportRegion reportRegion = dataSetToReportRegion(dataSet, entityTree);

                    if (reportRegion != null) {
                        if (reportRegion.getRegionPropertiesRootNode() == null) {
                            notifications.create(Notifications.NotificationType.TRAY)
                                    .withCaption(messages.getMessage("dataSet.entityAliasInvalid"))
                                    .withDescription(getNameForEntityParameter(dataSet))
                                    .show();
                            //without that root node region editor form will not initialized correctly and became empty. just return
                            return;
                        } else {
                            //Open editor and convert saved in editor ReportRegion item to View
                            Map<String, Object> editorParams = new HashMap<>();
                            editorParams.put("asViewEditor", Boolean.TRUE);
                            editorParams.put("rootEntity", reportRegion.getRegionPropertiesRootNode());
                            editorParams.put("scalarOnly", Boolean.TRUE);
                            editorParams.put("updateDisabled", !bandDefinitionEditor.isUpdatePermitted());

                            Screen screen = screenBuilders.editor(ReportRegion.class, bandDefinitionEditor.getHostController())
                                    .editEntity(reportRegion)
                                    .withScreenClass(RegionEditor.class)
                                    .withOpenMode(OpenMode.DIALOG)
                                    .build();
                            screen.addAfterCloseListener(afterCloseEvent -> {
                                if (Window.COMMIT_ACTION_ID.equals(((StandardCloseAction) afterCloseEvent.getCloseAction()).getActionId())) {
                                    dataSet.setFetchPlan(reportRegionToView(entityTree, reportRegion));
                                }
                            });
                            screen.show();
                        }
                    }
                }
            }
        }
    }

    //Detect metaclass by an alias and parameter
    protected MetaClass findMetaClassByAlias(DataSet dataSet) {
        String dataSetAlias = getNameForEntityParameter(dataSet);
        if (dataSetAlias == null) {
            notifications.create(Notifications.NotificationType.TRAY)
                    .withCaption(messages.getMessage("dataSet.entityAliasNull"))
                    .show();
            return null;
        }
        MetaClass byAliasMetaClass = bandDefinitionEditor.reportService.findMetaClassByDataSetEntityAlias(dataSetAlias, dataSet.getType(),
                bandDefinitionEditor.bandsDc.getItem().getReport().getInputParameters());

        //Lets return some value
        if (byAliasMetaClass == null) {
            //Can`t determine parameter and its metaClass by alias
            notifications.create(Notifications.NotificationType.TRAY)
                    .withCaption(messages.formatMessage("dataSet.entityAliasInvalid", dataSetAlias))
                    .show();
            return null;
            //when byAliasMetaClass is null we return also null
        } else {
            //Detect metaclass by current view for comparison
            MetaClass viewMetaClass = null;
            if (dataSet.getFetchPlan() != null) {
                viewMetaClass = bandDefinitionEditor.metadata.getClass(dataSet.getFetchPlan().getEntityClass());
            }
            if (viewMetaClass != null && !byAliasMetaClass.getName().equals(viewMetaClass.getName())) {
                notifications.create(Notifications.NotificationType.TRAY)
                        .withCaption(messages.formatMessage("dataSet.entityWasChanged", byAliasMetaClass.getName()))
                        .show();
            }
            return byAliasMetaClass;
        }
    }

    protected ReportRegion dataSetToReportRegion(DataSet dataSet, EntityTree entityTree) {
        boolean isTabulatedRegion;
        FetchPlan view = null;
        String collectionPropertyName;
        switch (dataSet.getType()) {
            case SINGLE:
                isTabulatedRegion = false;
                view = dataSet.getFetchPlan();
                collectionPropertyName = null;
                break;
            case MULTI:
                isTabulatedRegion = true;
                collectionPropertyName = StringUtils.substringAfter(dataSet.getListEntitiesParamName(), "#");
                if (StringUtils.isBlank(collectionPropertyName) && dataSet.getListEntitiesParamName().contains("#")) {
                    notifications.create(Notifications.NotificationType.TRAY)
                            .withCaption(messages.formatMessage("dataSet.entityAliasInvalid", getNameForEntityParameter(dataSet)))
                            .show();
                    return null;
                }
                if (StringUtils.isNotBlank(collectionPropertyName)) {

                    if (dataSet.getFetchPlan() != null) {
                        view = findSubViewByCollectionPropertyName(dataSet.getFetchPlan(), collectionPropertyName);

                    }
                    if (view == null) {
                        //View was never created for current dataset.
                        //We must to create minimal view that contains collection property for ability of creating ReportRegion.regionPropertiesRootNode later
                        MetaClass metaClass = entityTree.getEntityTreeRootNode().getWrappedMetaClass();
                        MetaProperty metaProperty = metaClass.getProperty(collectionPropertyName);
                        if (metaProperty != null && metaProperty.getDomain() != null && metaProperty.getRange().getCardinality().isMany()) {
                            view = fetchPlans.builder(metaProperty.getDomain().getJavaClass()).build();
                        } else {
                            notifications.create(Notifications.NotificationType.TRAY)
                                    .withCaption(messages.formatMessage("dataSet.cantFindCollectionProperty",
                                            collectionPropertyName, metaClass.getName()))
                                    .show();
                            return null;
                        }
                    }
                } else {
                    view = dataSet.getFetchPlan();
                }
                break;
            default:
                return null;
        }
        return bandDefinitionEditor.reportWizardService.createReportRegionByView(entityTree, isTabulatedRegion,
                view, collectionPropertyName);
    }

    protected FetchPlan reportRegionToView(EntityTree entityTree, ReportRegion reportRegion) {
        return bandDefinitionEditor.reportWizardService.createViewByReportRegions(entityTree.getEntityTreeRootNode(), Collections.singletonList(reportRegion));
    }

    public FetchPlan findSubViewByCollectionPropertyName(FetchPlan view, final String propertyName) {
        if (view == null) {
            return null;
        }
        for (FetchPlanProperty viewProperty : view.getProperties()) {
            if (propertyName.equals(viewProperty.getName())) {
                if (viewProperty.getFetchMode() != null) {
                    return viewProperty.getFetchPlan();
                }
            }

            if (viewProperty.getFetchMode() != null) {
                FetchPlan foundedView = findSubViewByCollectionPropertyName(viewProperty.getFetchPlan(), propertyName);
                if (foundedView != null) {
                    return foundedView;
                }
            }
        }
        return null;
    }

    protected String getNameForEntityParameter(DataSet dataSet) {
        String dataSetAlias = null;
        switch (dataSet.getType()) {
            case SINGLE:
                dataSetAlias = dataSet.getEntityParamName();
                break;
            case MULTI:
                dataSetAlias = dataSet.getListEntitiesParamName();
                break;
        }
        return dataSetAlias;
    }
}
