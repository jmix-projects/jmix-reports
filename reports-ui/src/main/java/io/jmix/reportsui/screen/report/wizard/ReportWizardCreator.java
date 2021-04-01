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
import io.jmix.reports.entity.wizard.ReportData;
import io.jmix.reports.entity.wizard.ReportRegion;
import io.jmix.reports.entity.wizard.TemplateFileType;
import io.jmix.reportsui.screen.ReportGuiManager;
<<<<<<< HEAD
import io.jmix.reportsui.screen.report.wizard.step.*;
=======
import io.jmix.reportsui.screen.report.wizard.step.MainWizardFrame;
import io.jmix.reportsui.screen.report.wizard.step.StepFragment;
import io.jmix.reportsui.screen.report.wizard.step.StepFrameManager;
>>>>>>> origin/features/wizard
import io.jmix.ui.Dialogs;
import io.jmix.ui.Notifications;
import io.jmix.ui.ScreenBuilders;
import io.jmix.ui.UiComponents;
import io.jmix.ui.component.*;
import io.jmix.ui.model.CollectionChangeType;
import io.jmix.ui.model.CollectionContainer;
import io.jmix.ui.model.InstanceContainer;
import io.jmix.ui.screen.*;
<<<<<<< HEAD
=======
import io.jmix.ui.theme.ThemeConstants;
import org.apache.commons.lang3.StringUtils;
>>>>>>> origin/features/wizard
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.List;

@UiController("report_ReportWizardCreator")
@UiDescriptor("report-wizard.xml")
public class ReportWizardCreator extends Screen implements MainWizardFrame<Screen> {

    protected final List<String> stepFragments = Arrays.asList("detailsFragment", "regionsStep");

    @Autowired
    protected InstanceContainer<ReportData> reportDataDc;

    @Autowired
    protected CollectionContainer<ReportRegion> reportRegionsDc;
//    @Autowired
//    protected CollectionContainer<ReportGroup> groupsDs;

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

<<<<<<< HEAD
//    @Named("detailsStep.mainFields")
//    protected Form mainFields;
=======
    @Named("detailsStep.mainFields")
    protected Form mainFields;
>>>>>>> origin/features/wizard

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

<<<<<<< HEAD
    @Autowired
    protected DetailsStepFragment detailsFragment;

    @Autowired
    protected RegionsStepFragment regionsStepFragment;

    //protected StepFragment saveStepFragment;
=======
    protected StepFragment detailsStepFragment;
    protected StepFragment regionsStepFragment;
    protected StepFragment saveStepFragment;
>>>>>>> origin/features/wizard
    protected StepFrameManager stepFrameManager;

    protected byte[] lastGeneratedTemplate;

    protected String query;
    protected String dataStore;
    protected List<ReportData.Parameter> queryParameters;

    @Subscribe
    @SuppressWarnings("unchecked")
    protected void onInit(InitEvent event) {
        reportDataDc.setItem(metadata.create(ReportData.class));

        stepFrameManager = new StepFrameManager(this, getStepFrames());

        initMainFields();

<<<<<<< HEAD
//        stepFrameManager.showCurrentFrame();
        tipLabel.setValue(messages.getMessage("enterMainParameters"));
=======
        stepFrameManager.showCurrentFrame();
        tipLabel.setValue(messages.getMessage("enterMainParameters"));

        outputFileName.setContextHelpIconClickHandler(e ->
                dialogs.createMessageDialog()
                        .withCaption(messages.getMessage("template.namePatternText"))
                        .withMessage(messages.getMessage("template.namePatternTextHelp"))
                        .withModal(false)
                        .withWidth("560px")
                        .show());
>>>>>>> origin/features/wizard
    }

    @Subscribe(id = "reportRegionsDc", target = Target.DATA_CONTAINER)
    public void onReportRegionsDcCollectionChange(CollectionContainer.CollectionChangeEvent<ReportRegion> event) {
        if (event.getChangeType().equals(CollectionChangeType.ADD_ITEMS)) {
<<<<<<< HEAD
            //regionsTable.setSelected((Collection) event.getChanges());
=======
            regionsTable.setSelected((Collection) event.getChanges());
>>>>>>> origin/features/wizard
        }
    }

    @Subscribe(id = "reportRegionsDc", target = Target.DATA_CONTAINER)
    public void onReportRegionsDcItemChange(InstanceContainer.ItemChangeEvent<ReportRegion> event) {
<<<<<<< HEAD
//        if (regionsTable.getSingleSelected() != null) {
//            moveDownBtn.setEnabled(true);
//            moveUpBtn.setEnabled(true);
//            removeBtn.setEnabled(true);
//        }
    }

    @Subscribe("nextBtn")
    public void onNextBtnClick(Button.ClickEvent event) {
        MetaClass metaClass = metadata.getClass(reportDataDc.getItem().getEntityName());

        if (metaClass == null) {
            notifications.create(Notifications.NotificationType.TRAY)
                    .withCaption(messages.getMessage("fillEntityMsg"))
                    .show();
            return;
        }


        if (detailsFragment.isNeedUpdateEntityModel()) {
            EntityTree entityTree = reportWizardService.buildEntityTree(metaClass);

            regionsStepFragment.setEntityTreeHasSimpleAttrs(entityTree.getEntityTreeStructureInfo().isEntityTreeHasSimpleAttrs());
            regionsStepFragment.setEntityTreeHasCollections(entityTree.getEntityTreeStructureInfo().isEntityTreeRootHasCollections());

            entityTree.getEntityTreeRootNode().getLocalizedName();
            reportDataDc.getItem().setEntityTreeRootNode(entityTree.getEntityTreeRootNode());
            detailsFragment.setNeedUpdateEntityModel(false);
        }
        stepFrameManager.nextFrame();
        refreshFrameVisible();
    }

    @Subscribe("backBtn")
    public void onBackBtnClick(Button.ClickEvent event) {
        stepFrameManager.prevFrame();
        refreshFrameVisible();
=======
        if (regionsTable.getSingleSelected() != null) {
            moveDownBtn.setEnabled(true);
            moveUpBtn.setEnabled(true);
            removeBtn.setEnabled(true);
        }
    }

    protected void initMainButtons() {
        fwdBtn.setAction(new AbstractAction("fwd") {
            @Override
            public void actionPerform(Component component) {
                if (entity.getValue() == null) {
                    notifications.create(Notifications.NotificationType.TRAY)
                            .withCaption(messages.getMessage("fillEntityMsg"))
                            .show();
                    return;
                }

                if (needUpdateEntityModel) {
                    EntityTree entityTree = reportWizardService.buildEntityTree(entity.getValue());
                    entityTreeHasSimpleAttrs = entityTree.getEntityTreeStructureInfo().isEntityTreeHasSimpleAttrs();
                    entityTreeHasCollections = entityTree.getEntityTreeStructureInfo().isEntityTreeRootHasCollections();
                    entityTree.getEntityTreeRootNode().getLocalizedName();
                    getItem().setEntityTreeRootNode(entityTree.getEntityTreeRootNode());
                    needUpdateEntityModel = false;
                }
                stepFrameManager.nextFrame();
                refreshFrameVisible();
            }
        });
        bwdBtn.setAction(new AbstractAction("bwd") {
            @Override
            public void actionPerform(Component component) {
                stepFrameManager.prevFrame();
                refreshFrameVisible();
            }
        });
>>>>>>> origin/features/wizard
    }

    protected void initMainFields() {
        //todo
//        mainFields.addCustomField("entity", (datasource, propertyId) -> {
//            ComboBox comboBox = uiComponents.create(ComboBox.class);
//            //TODO request focus
////            lookupField.requestFocus();
//            entity = comboBox;
//            return comboBox;
//        });
//        mainFields.addCustomField("reportName", (datasource, propertyId) -> {
//            TextField textField = uiComponents.create(TextField.class);
//            textField.setMaxLength(255);
//            reportName = textField;
//            return textField;
//        });
//        mainFields.addCustomField("templateFileFormat", (datasource, propertyId) -> {
//            ComboBox comboBox = uiComponents.create(ComboBox.class);
//            templateFileFormat = comboBox;
//            return comboBox;
//        });
//        mainFields.addCustomField("reportType", (datasource, propertyId) -> {
//            RadioButtonGroup radioButtonGroup = uiComponents.create(RadioButtonGroup.class);
//            radioButtonGroup.setOrientation(OptionsGroup.Orientation.VERTICAL);
//            reportTypeRadioButtonGroup = radioButtonGroup;
//            return radioButtonGroup;
//        });
    }

    protected void refreshFrameVisible() {
<<<<<<< HEAD
        if (detailsFragment.getFragment().isVisible()) {
            tipLabel.setValue(messages.getMessage("enterMainParameters"));
            editAreaVbox.add(editAreaGroupBox);
            editAreaVbox.remove(regionsStepFragment.getFragment());
            //editAreaGroupBox.remove(saveStepFragment.getFragment());
            editAreaGroupBox.add(detailsFragment.getFragment());
        } else if (regionsStepFragment.getFragment().isVisible()) {
            tipLabel.setValue(messages.getMessage("addPropertiesAndTableAreas"));
            editAreaVbox.remove(editAreaGroupBox);
            editAreaVbox.add(regionsStepFragment.getFragment());
=======
        if (detailsStepFragment.getFrame().isVisible()) {
            tipLabel.setValue(messages.getMessage("enterMainParameters"));
            editAreaVbox.add(editAreaGroupBox);
            editAreaVbox.remove(regionsStepFragment.getFrame());
            editAreaGroupBox.remove(saveStepFragment.getFrame());
            editAreaGroupBox.add(detailsStepFragment.getFrame());
        } else if (regionsStepFragment.getFrame().isVisible()) {
            tipLabel.setValue(messages.getMessage("addPropertiesAndTableAreas"));
            editAreaVbox.remove(editAreaGroupBox);
            editAreaVbox.add(regionsStepFragment.getFrame());
        } else if (saveStepFragment.getFrame().isVisible()) {
            tipLabel.setValue(messages.getMessage("finishPrepareReport"));
            editAreaVbox.add(editAreaGroupBox);
            editAreaVbox.remove(regionsStepFragment.getFrame());
            editAreaGroupBox.add(saveStepFragment.getFrame());
            editAreaGroupBox.remove(detailsStepFragment.getFrame());
>>>>>>> origin/features/wizard
        }
//        else if (saveStepFragment.getFragment().isVisible()) {
//            tipLabel.setValue(messages.getMessage("finishPrepareReport"));
//            editAreaVbox.add(editAreaGroupBox);
//            editAreaVbox.remove(regionsStepFragment.getFragment());
//            editAreaGroupBox.add(saveStepFragment.getFragment());
//            editAreaGroupBox.remove(detailsFragment.getFragment());
//        }
    }

    protected List<StepFragment> getStepFrames() {
<<<<<<< HEAD
        return Arrays.asList(detailsFragment, regionsStepFragment /*saveStepFragment*/);
    }

//    protected String generateTemplateFileName(String fileExtension) {
//        if (entity.getValue() == null) {
//            return "";
//        }
//        return messages.formatMessage("downloadTemplateFileNamePattern", reportName.getValue(), fileExtension);
//    }
//
//    protected String generateOutputFileName(String fileExtension) {
//        if (StringUtils.isBlank(reportName.getValue())) {
//            if (entity.getValue() != null) {
//                return messages.formatMessage("downloadOutputFileNamePattern", messageTools.getEntityCaption(entity.getValue()), fileExtension);
//            } else {
//                return "";
//            }
//        } else {
//            return reportName.getValue() + "." + fileExtension;
//        }
//    }
=======
        detailsStepFragment = new DetailsStepFragment(this);
        regionsStepFragment = new RegionsStepFragment(this);
        saveStepFragment = new SaveStepFragment(this);
        return Arrays.asList(detailsStepFragment, regionsStepFragment, saveStepFragment);
    }

    protected String generateTemplateFileName(String fileExtension) {
        if (entity.getValue() == null) {
            return "";
        }
        return messages.formatMessage("downloadTemplateFileNamePattern", reportName.getValue(), fileExtension);
    }

    protected String generateOutputFileName(String fileExtension) {
        if (StringUtils.isBlank(reportName.getValue())) {
            if (entity.getValue() != null) {
                return messages.formatMessage("downloadOutputFileNamePattern", messageTools.getEntityCaption(entity.getValue()), fileExtension);
            } else {
                return "";
            }
        } else {
            return reportName.getValue() + "." + fileExtension;
        }
    }
>>>>>>> origin/features/wizard


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

<<<<<<< HEAD
=======
    protected void setupButtonsVisibility() {
        buttonsBox.remove(addRegionDisabledBtn);
        buttonsBox.remove(addTabulatedRegionDisabledBtn);
        buttonsBox.remove(addSimpleRegionBtn);
        buttonsBox.remove(addTabulatedRegionBtn);
        buttonsBox.remove(addRegionPopupBtn);
        if (((ReportData.ReportType) reportTypeRadioButtonGroup.getValue()).isList()) {
            tipLabel.setValue(messages.formatMessage("regionTabulatedMessage",
                    messages.getMessage(entity.getValue().getJavaClass(),
                            entity.getValue().getJavaClass().getSimpleName())
            ));
            if (entityTreeHasSimpleAttrs && getItem().getReportRegions().isEmpty()) {
                buttonsBox.add(addTabulatedRegionBtn);
            } else {
                buttonsBox.add(addTabulatedRegionDisabledBtn);
            }
        } else {
            tipLabel.setValue(messages.getMessage("addPropertiesAndTableAreas"));
            if (entityTreeHasSimpleAttrs && entityTreeHasCollections) {
                buttonsBox.add(addRegionPopupBtn);
            } else if (entityTreeHasSimpleAttrs) {
                buttonsBox.add(addSimpleRegionBtn);
            } else if (entityTreeHasCollections) {
                buttonsBox.add(addTabulatedRegionBtn);
            } else {
                buttonsBox.add(addRegionDisabledBtn);
            }
        }

        if (regionsTable.getSingleSelected() != null) {
            moveDownBtn.setEnabled(true);
            moveUpBtn.setEnabled(true);
            removeBtn.setEnabled(true);
        } else {
            moveDownBtn.setEnabled(false);
            moveUpBtn.setEnabled(false);
            removeBtn.setEnabled(false);
        }
    }
>>>>>>> origin/features/wizard

    protected Report buildReport(boolean temporary) {
//        ReportData reportData = ;
//        reportData.setName(reportName.getValue());
//        reportData.setTemplateFileName(generateTemplateFileName(templateFileFormat.getValue().toString().toLowerCase()));
//        if (outputFileFormat.getValue() == null) {
//            reportData.setOutputFileType(ReportOutputType.fromId(((TemplateFileType) templateFileFormat.getValue()).getId()));
//        } else {
//            //lets generate output report in same format as the template
//            reportData.setOutputFileType(outputFileFormat.getValue());
//        }
//        reportData.setReportType((ReportData.ReportType) reportTypeRadioButtonGroup.getValue());
        //groupsDs.refresh();
//        if (!groupsDs.getItems().isEmpty()) {
//            UUID id = groupsDs.getItems().iterator().next().getId();
//            reportData.setGroup(groupsDs.getItem(id));
//        }

        //be sure that reportData.name and reportData.outputFileFormat is not null before generation of template
//        try {
//            byte[] templateByteArray = reportWizardService.generateTemplate(reportData, templateFileFormat.getValue());
//            reportData.setTemplateContent(templateByteArray);
//        } catch (TemplateGenerationException e) {
//            notifications.create(Notifications.NotificationType.WARNING)
//                    .withCaption(messages.getMessage("templateGenerationException"))
//                    .show();
//            return null;
//        }
//        reportData.setTemplateFileType(templateFileFormat.getValue());
////        reportData.setOutputNamePattern(outputFileName.getValue());
//
//        if (query != null) {
//            reportData.setQuery(query);
//            reportData.setQueryParameters(queryParameters);
//            MetaClass entityMetaClass = entity.getValue();
//            String storeName = entityMetaClass.getStore().getName();
//            if (!Stores.isMain(storeName)) {
//                reportData.setDataStore(storeName);
//            }
//        }
//
//        Report report = reportWizardService.toReport(reportData, temporary);
//        reportData.setGeneratedReport(report);
        return null;
    }

<<<<<<< HEAD
//    protected void setCorrectReportOutputType() {
//        ReportOutputType outputFileFormatPrevValue = outputFileFormat.getValue();
//        outputFileFormat.setValue(null);
//        Map<String, ReportOutputType> optionsMap = refreshOutputAvailableFormats(templateFileFormat.getValue());
//        outputFileFormat.setOptionsMap(optionsMap);
//
//        if (outputFileFormatPrevValue != null) {
//            if (optionsMap.containsKey(outputFileFormatPrevValue.toString())) {
//                outputFileFormat.setValue(outputFileFormatPrevValue);
//            }
//        }
//        if (outputFileFormat.getValue() == null) {
//            if (optionsMap.size() > 1) {
//                outputFileFormat.setValue(optionsMap.get(templateFileFormat.getValue().toString()));
//            } else if (optionsMap.size() == 1) {
//                outputFileFormat.setValue(optionsMap.values().iterator().next());
//            }
//        }
//    }

//    public ReportData getItem() {
//        return reportDataDs.getItem();
//    }
=======
    protected void setCorrectReportOutputType() {
        ReportOutputType outputFileFormatPrevValue = outputFileFormat.getValue();
        outputFileFormat.setValue(null);
        Map<String, ReportOutputType> optionsMap = refreshOutputAvailableFormats(templateFileFormat.getValue());
        outputFileFormat.setOptionsMap(optionsMap);

        if (outputFileFormatPrevValue != null) {
            if (optionsMap.containsKey(outputFileFormatPrevValue.toString())) {
                outputFileFormat.setValue(outputFileFormatPrevValue);
            }
        }
        if (outputFileFormat.getValue() == null) {
            if (optionsMap.size() > 1) {
                outputFileFormat.setValue(optionsMap.get(templateFileFormat.getValue().toString()));
            } else if (optionsMap.size() == 1) {
                outputFileFormat.setValue(optionsMap.values().iterator().next());
            }
        }
    }

    protected Map<String, ReportOutputType> refreshOutputAvailableFormats(TemplateFileType templateFileType) {
        return availableOutputFormats.get(templateFileType);
    }

    protected Map<TemplateFileType, Map<String, ReportOutputType>> availableOutputFormats;

    private void initAvailableFormats() {
        availableOutputFormats = new ImmutableMap.Builder<TemplateFileType, Map<String, ReportOutputType>>()
                .put(TemplateFileType.DOCX, new ImmutableMap.Builder<String, ReportOutputType>()
                        .put(messages.getMessage(ReportOutputType.DOCX), ReportOutputType.DOCX)
                        .put(messages.getMessage(ReportOutputType.HTML), ReportOutputType.HTML)
                        .put(messages.getMessage(ReportOutputType.PDF), ReportOutputType.PDF)
                        .build())
                .put(TemplateFileType.XLSX, new ImmutableMap.Builder<String, ReportOutputType>()
                        .put(messages.getMessage(ReportOutputType.XLSX), ReportOutputType.XLSX)
                        .put(messages.getMessage(ReportOutputType.HTML), ReportOutputType.HTML)
                        .put(messages.getMessage(ReportOutputType.PDF), ReportOutputType.PDF)
                        .put(messages.getMessage(ReportOutputType.CSV), ReportOutputType.CSV)
                        .build())
                .put(TemplateFileType.HTML, new ImmutableMap.Builder<String, ReportOutputType>()
                        .put(messages.getMessage(ReportOutputType.HTML), ReportOutputType.HTML)
                        .put(messages.getMessage(ReportOutputType.PDF), ReportOutputType.PDF)
                        .build())
                .put(TemplateFileType.CHART, new ImmutableMap.Builder<String, ReportOutputType>()
                        .put(messages.getMessage(ReportOutputType.CHART), ReportOutputType.CHART)
                        .build())
                .put(TemplateFileType.CSV, new ImmutableMap.Builder<String, ReportOutputType>()
                        .put(messages.getMessage(ReportOutputType.CSV), ReportOutputType.CSV)
                        .build())
                .put(TemplateFileType.TABLE, new ImmutableMap.Builder<String, ReportOutputType>()
                        .put(messages.getMessage(ReportOutputType.TABLE), ReportOutputType.TABLE)
                        .build())
                .build();
    }

    public ReportData getItem() {
        return reportDataDs.getItem();
    }
>>>>>>> origin/features/wizard
}