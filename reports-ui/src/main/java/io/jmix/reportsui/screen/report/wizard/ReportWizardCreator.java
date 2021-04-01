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
package io.jmix.reportsui.screen.report.wizard;

import io.jmix.core.*;
import io.jmix.core.metamodel.model.MetaClass;
import io.jmix.reports.app.EntityTree;
import io.jmix.reports.app.service.ReportsWizard;
import io.jmix.reports.entity.Report;
import io.jmix.reports.entity.ReportGroup;
import io.jmix.reports.entity.wizard.ReportData;
import io.jmix.reports.entity.wizard.ReportRegion;
import io.jmix.reports.entity.wizard.ReportTypeGenerate;
import io.jmix.reports.entity.wizard.TemplateFileType;
import io.jmix.reports.exception.TemplateGenerationException;
import io.jmix.reports.exception.ValidationException;
import io.jmix.reportsui.screen.ReportGuiManager;
import io.jmix.reportsui.screen.report.wizard.step.*;
import io.jmix.ui.Dialogs;
import io.jmix.ui.Notifications;
import io.jmix.ui.ScreenBuilders;
import io.jmix.ui.UiComponents;
import io.jmix.ui.action.Action;
import io.jmix.ui.action.DialogAction;
import io.jmix.ui.component.*;
import io.jmix.ui.model.CollectionChangeType;
import io.jmix.ui.model.CollectionContainer;
import io.jmix.ui.model.InstanceContainer;
import io.jmix.ui.screen.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@UiController("report_ReportWizardCreator")
@UiDescriptor("report-wizard.xml")
public class ReportWizardCreator extends Screen implements MainWizardScreen<Screen> {

    @Autowired
    protected InstanceContainer<ReportData> reportDataDc;

    @Autowired
    protected CollectionContainer<ReportRegion> reportRegionsDc;

    @Autowired
    protected CollectionContainer<ReportGroup> groupsDc;

    @Autowired
    protected Button nextBtn;

    @Autowired
    protected Button backBtn;

    @Autowired
    protected Button saveBtn;

    @Autowired
    protected Label<String> tipLabel;
    @Autowired
    protected BoxLayout editAreaVbox;
    @Autowired
    protected ButtonsPanel navBtnsPanel;
    @Autowired
    protected GroupBoxLayout editAreaGroupBox;
    @Autowired
    protected Dialogs dialogs;
    @Autowired
    protected ScreenBuilders screenBuilders;
    @Autowired
    protected DataManager dataManager;

//    @Named("detailsStep.mainFields")
//    protected Form mainFields;

    protected RadioButtonGroup reportTypeRadioButtonGroup;//this and following are set during creation
    protected ComboBox<TemplateFileType> templateFileFormat;
    protected TextField<String> reportName;

//    @Named("saveStep.outputFileFormat")
//    protected ComboBox<ReportOutputType> outputFileFormat;
//    @Named("saveStep.outputFileName")
//    protected TextField<String> outputFileName;
//    @Named("saveStep.downloadTemplateFile")
//    protected Button downloadTemplateFile;
//    @Named("saveStep.diagramTypeLabel")
//    protected Label<String> diagramTypeLabel;
//    @Named("saveStep.diagramType")
//    protected ComboBox<ChartType> diagramType;
//    @Named("saveStep.chartPreviewBox")
//    protected BoxLayout chartPreviewBox;

    @Autowired
    protected ExtendedEntities extendedEntities;
    @Autowired
    protected Metadata metadata;
    @Autowired
    protected MetadataTools metadataTools;
    @Autowired
    protected MessageTools messageTools;
    @Autowired
    protected UiComponents uiComponents;
    @Autowired
    protected ReportsWizard reportWizardService;
    @Autowired
    protected ReportGuiManager reportGuiManager;
    @Autowired
    protected Messages messages;
    @Autowired
    protected Notifications notifications;

    @Autowired
    protected ApplicationContext applicationContext;

    @Autowired
    protected DetailsStepFragment detailsFragment;

    @Autowired
    protected RegionsStepFragment regionsStepFragment;

    @Autowired
    protected SaveStepFragment saveStepFragment;

    @Autowired
    protected StepFrameManager stepFrameManager;

    protected byte[] lastGeneratedTemplate;

    protected String query;
    protected String dataStore;
    protected List<ReportData.Parameter> queryParameters;

    @Subscribe
    @SuppressWarnings("unchecked")
    protected void onInit(InitEvent event) {
        reportDataDc.setItem(metadata.create(ReportData.class));

        stepFrameManager.setMainWizardFrame(this);
        stepFrameManager.setStepFragments(getStepFragments());

        stepFrameManager.showCurrentFrame();
        tipLabel.setValue(messages.getMessage(getClass(), "enterMainParameters"));
    }

    @Subscribe(id = "reportRegionsDc", target = Target.DATA_CONTAINER)
    public void onReportRegionsDcCollectionChange(CollectionContainer.CollectionChangeEvent<ReportRegion> event) {
        if (event.getChangeType().equals(CollectionChangeType.ADD_ITEMS)) {
            //regionsTable.setSelected((Collection) event.getChanges());
        }
    }

    @Subscribe(id = "reportRegionsDc", target = Target.DATA_CONTAINER)
    public void onReportRegionsDcItemChange(InstanceContainer.ItemChangeEvent<ReportRegion> event) {
//        if (regionsTable.getSingleSelected() != null) {
//            moveDownBtn.setEnabled(true);
//            moveUpBtn.setEnabled(true);
//            removeBtn.setEnabled(true);
//        }
    }

    @Subscribe("nextBtn")
    public void onNextBtnClick(Button.ClickEvent event) {
        MetaClass metaClass = metadata.findClass(getItem().getEntityName());

        if (metaClass == null) {
            notifications.create(Notifications.NotificationType.TRAY)
                    .withCaption(messages.getMessage(getClass(), "fillEntityMsg"))
                    .show();
            return;
        }

        if (detailsFragment.isNeedUpdateEntityModel()) {
            EntityTree entityTree = reportWizardService.buildEntityTree(metaClass);

            regionsStepFragment.setEntityTreeHasSimpleAttrs(entityTree.getEntityTreeStructureInfo().isEntityTreeHasSimpleAttrs());
            regionsStepFragment.setEntityTreeHasCollections(entityTree.getEntityTreeStructureInfo().isEntityTreeRootHasCollections());

            entityTree.getEntityTreeRootNode().getLocalizedName();
            getItem().setEntityTreeRootNode(entityTree.getEntityTreeRootNode());
            detailsFragment.setNeedUpdateEntityModel(false);
        }
        stepFrameManager.nextFragment();
        refreshFrameVisible();
    }

    @Subscribe("backBtn")
    public void onBackBtnClick(Button.ClickEvent event) {
        stepFrameManager.prevFragment();
        refreshFrameVisible();
    }

    protected void refreshFrameVisible() {
        if (detailsFragment.getFragment().isVisible()) {
            tipLabel.setValue(messages.getMessage(getClass(), "enterMainParameters"));
            editAreaGroupBox.setVisible(true);
        } else if (regionsStepFragment.getFragment().isVisible()) {
            tipLabel.setValue(messages.getMessage(getClass(), "addPropertiesAndTableAreas"));
            editAreaGroupBox.setVisible(false);
        } else if (saveStepFragment.getFragment().isVisible()) {
            tipLabel.setValue(messages.getMessage(getClass(), "finishPrepareReport"));
            editAreaGroupBox.setVisible(true);
        }
    }

    protected List<StepFragment> getStepFragments() {
        return Arrays.asList(detailsFragment, regionsStepFragment, saveStepFragment);
    }

    protected String generateOutputFileName(String fileExtension) {
        if (StringUtils.isBlank(reportName.getValue())) {
            MetaClass entityMetaClass = getItem().getEntity();
            return entityMetaClass != null ?
                    messages.formatMessage("downloadOutputFileNamePattern", messageTools.getEntityCaption(entityMetaClass), fileExtension) :
                    "";
        } else {
            return reportName.getValue() + "." + fileExtension;
        }
    }


    @Override
    public Button getForwardBtn() {
        return nextBtn;
    }

    @Override
    public void removeBtns() {
        navBtnsPanel.remove(nextBtn);
        navBtnsPanel.remove(backBtn);
        navBtnsPanel.remove(saveBtn);
    }

    @Override
    public void addForwardBtn() {
        navBtnsPanel.add(nextBtn);
    }

    @Override
    public void addBackwardBtn() {
        navBtnsPanel.add(backBtn);
    }

    @Override
    public void addSaveBtn() {
        navBtnsPanel.add(saveBtn);
    }

    @Override
    public Button getBackwardBtn() {
        return backBtn;
    }

    @Override
    public Screen getMainWizardFrame() {
        return this;
    }

    @Subscribe("save")
    public void onSave(Action.ActionPerformedEvent event) {
        try {
            //wizard.outputFileName.validate();
        } catch (ValidationException e) {
            notifications.create(Notifications.NotificationType.TRAY)
                    .withCaption(messages.getMessage("validationFail.caption"))
                    .withDescription(e.getMessage())
                    .show();
            return;
        }
        if (reportDataDc.getItem().getReportRegions().isEmpty()) {
            dialogs.createOptionDialog()
                    .withCaption(messages.getMessage("dialogs.Confirmation"))
                    .withMessage(messages.getMessage("confirmSaveWithoutRegions"))
                    .withActions(
                            new DialogAction(DialogAction.Type.OK).withHandler(handle ->
                                    convertToReportAndForceCloseWizard()
                            ),
                            new DialogAction(DialogAction.Type.NO)
                    ).show();
        } else {
            convertToReportAndForceCloseWizard();
        }
    }

    private void convertToReportAndForceCloseWizard() {
        Report r = buildReport(false);
        //todo
//                    if (r != null) {
//                        wizard.close(Window.COMMIT_ACTION_ID); //true is ok cause it is a save btn
//                    }
    }

    public Report buildReport(boolean temporary) {
        ReportData reportData = reportDataDc.getItem();
        reportData.setName(reportName.getValue());
        reportData.setTemplateFileName(generateTemplateFileName(templateFileFormat.getValue().toString().toLowerCase()));
//        if (outputFileFormat.getValue() == null) {
//            reportData.setOutputFileType(ReportOutputType.fromId(templateFileFormat.getValue().getId()));
//        } else {
//            //lets generate output report in same format as the template
//            reportData.setOutputFileType(outputFileFormat.getValue());
//        }
        reportData.getReportTypeGenerate((ReportTypeGenerate) reportTypeRadioButtonGroup.getValue());
        //groupsDc.refresh();
        if (!groupsDc.getItems().isEmpty()) {
            UUID id = groupsDc.getItems().iterator().next().getId();
            reportData.setGroup(groupsDc.getItem(id));
        }

//        be sure that reportData.name and reportData.outputFileFormat is not null before generation of template
        try {
            byte[] templateByteArray = reportWizardService.generateTemplate(reportData, templateFileFormat.getValue());
            reportData.setTemplateContent(templateByteArray);
        } catch (TemplateGenerationException e) {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption(messages.getMessage("templateGenerationException"))
                    .show();
            return null;
        }
        reportData.setTemplateFileType(templateFileFormat.getValue());
        //reportData.setOutputNamePattern(outputFileName.getValue());

        if (query != null) {
            reportData.setQuery(query);
            reportData.setQueryParameters(queryParameters);

            MetaClass entityMetaClass = reportDataDc.getItem().getEntity();
            String storeName = entityMetaClass.getStore().getName();

            if (!Stores.isMain(storeName)) {
                reportData.setDataStore(storeName);
            }
        }

        Report report = reportWizardService.toReport(reportData, temporary);
        reportData.setGeneratedReport(report);
        return null;
    }

    protected String generateTemplateFileName(String fileExtension) {
        MetaClass entityMetaClass = reportDataDc.getItem().getEntity();
        return entityMetaClass != null ?
                messages.formatMessage("downloadTemplateFileNamePattern", reportName.getValue(), fileExtension) :
                "";
    }

    public ReportData getItem() {
        return reportDataDc.getItem();
    }
}