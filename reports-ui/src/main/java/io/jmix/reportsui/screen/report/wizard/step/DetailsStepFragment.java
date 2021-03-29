/*
 * Copyright 2021 Haulmont.
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

package io.jmix.reportsui.screen.report.wizard.step;

import com.google.common.collect.ImmutableMap;
import io.jmix.core.*;
import io.jmix.core.metamodel.model.MetaClass;
import io.jmix.reports.app.service.ReportsWizard;
import io.jmix.reports.entity.ReportOutputType;
import io.jmix.reports.entity.wizard.ReportData;
import io.jmix.reports.entity.wizard.ReportTypeGenerate;
import io.jmix.reports.entity.wizard.TemplateFileType;
import io.jmix.ui.Dialogs;
import io.jmix.ui.action.DialogAction;
import io.jmix.ui.component.ComboBox;
import io.jmix.ui.component.HasValue;
import io.jmix.ui.component.RadioButtonGroup;
import io.jmix.ui.model.InstanceContainer;
import io.jmix.ui.screen.Subscribe;
import io.jmix.ui.screen.UiController;
import io.jmix.ui.screen.UiDescriptor;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

@UiController("report_DetailsStep.fragment")
@UiDescriptor("first-details-fragment.xml")
public class DetailsStepFragment extends StepFragment {

    @Autowired
    private InstanceContainer<ReportData> reportDataDc;

    @Autowired
    protected Messages messages;

    @Autowired
    protected Dialogs dialogs;

    @Autowired
    protected MessageTools messageTools;

    @Autowired
    protected ComboBox<MetaClass> entity;

    @Autowired
    protected ComboBox templateFileFormat;

    @Autowired
    protected Metadata metadata;

    @Autowired
    protected ExtendedEntities extendedEntities;

    @Autowired
    protected RadioButtonGroup<ReportTypeGenerate> reportTypeGenerate;

    @Autowired
    protected MetadataTools metadataTools;

    @Autowired
    protected ReportsWizard reportsWizard;

    protected boolean needUpdateEntityModel = false;

    public boolean isNeedUpdateEntityModel() {
        return needUpdateEntityModel;
    }

    public void setNeedUpdateEntityModel(boolean needUpdateEntityModel) {
        this.needUpdateEntityModel = needUpdateEntityModel;
    }

    @Subscribe
    public void onInit(InitEvent event) {
        initAvailableFormats();

        initReportTypeOptionGroup();
        initTemplateFormatLookupField();
        initEntityLookupField();

        //entity.addValueChangeListener(new ChangeReportNameListener());
    }

    protected void initEntityLookupField() {
        entity.setOptionsMap(getAvailableEntities());
        entity.addValueChangeListener(e -> {
            reportDataDc.getItem().getReportRegions().clear();
            //regionsTable.refresh(); //for web6
            needUpdateEntityModel = true;
            entity.setValue(e.getValue());

            //clearQueryAndFilter();
        });
//        entity.addValueChangeListener(new ClearRegionListener(
//                new DialogActionWithChangedValue(DialogAction.Type.YES) {
//                    @Override
//                    public void actionPerform(Component component) {
//                        reportDataDc.getItem().getReportRegions().clear();
//                        //regionsTable.refresh(); //for web6
//                        needUpdateEntityModel = true;
//                        entity.setValue((MetaClass) newValue);
//
//                        clearQueryAndFilter();
//                    }
//                }));
    }

    @Subscribe("entity")
    public void onEntityValueChange(HasValue.ValueChangeEvent event) {
        MetaClass metaClass = (MetaClass) event.getValue();
        reportDataDc.getItem().setEntityName(metaClass.getName());

    }

    protected void initTemplateFormatLookupField() {
        templateFileFormat.setOptionsMap(getAvailableTemplateFormats());
        templateFileFormat.setTextInputAllowed(false);
        templateFileFormat.setValue(TemplateFileType.DOCX);
    }

    protected void initReportTypeOptionGroup() {
        reportTypeGenerate.setOptionsMap(getListedReportOptionsMap());
        reportTypeGenerate.setValue(ReportTypeGenerate.SINGLE_ENTITY);
//        reportTypeGenerate.addValueChangeListener(new ClearRegionListener(
//                new DialogActionWithChangedValue(Type.YES) {
//                    @Override
//                    public void actionPerform(Component component) {
//                        wizard.getItem().getReportRegions().clear();
//                        wizard.regionsTable.refresh(); //for web6
//                        wizard.reportTypeOptionGroup.setValue(newValue);
//                    }
//                }));
    }

    protected Map<String, ReportTypeGenerate> getListedReportOptionsMap() {
        Map<String, ReportTypeGenerate> result = new LinkedHashMap<>(3);
        result.put(messages.getMessage("singleEntityReport"), ReportTypeGenerate.SINGLE_ENTITY);
        result.put(messages.getMessage("listOfEntitiesReport"), ReportTypeGenerate.LIST_OF_ENTITIES);
        result.put(messages.getMessage("listOfEntitiesReportWithQuery"), ReportTypeGenerate.LIST_OF_ENTITIES_WITH_QUERY);
        return result;
    }

    protected Map<String, TemplateFileType> getAvailableTemplateFormats() {
        Map<String, TemplateFileType> result = new LinkedHashMap<>(4);
        result.put(messages.getMessage(TemplateFileType.XLSX), TemplateFileType.XLSX);
        result.put(messages.getMessage(TemplateFileType.DOCX), TemplateFileType.DOCX);
        result.put(messages.getMessage(TemplateFileType.HTML), TemplateFileType.HTML);
        result.put(messages.getMessage(TemplateFileType.CSV), TemplateFileType.CSV);
        result.put(messages.getMessage(TemplateFileType.TABLE), TemplateFileType.TABLE);

        //todo char addon
//        WindowConfig windowConfig = AppBeans.get(WindowConfig.NAME);
//        if (windowConfig.hasWindow(ShowChartController.JSON_CHART_SCREEN_ID)) {
//            result.put(messages.getMessage(TemplateFileType.CHART), TemplateFileType.CHART);
//        }
        return result;
    }

    protected Map<String, MetaClass> getAvailableEntities() {
        Map<String, MetaClass> result = new TreeMap<>(String::compareTo);
        // todo need to be fixed later - ReferenceToEntity is not persistent but returned in 'metadataTools.getAllPersistentMetaClasses'
        Collection<MetaClass> classes = metadataTools.getAllJpaEntityMetaClasses();
        for (MetaClass metaClass : classes) {
            MetaClass effectiveMetaClass = extendedEntities.getEffectiveMetaClass(metaClass);
            if (!reportsWizard.isEntityAllowedForReportWizard(effectiveMetaClass)) {
                continue;
            }
            result.put(messageTools.getEntityCaption(effectiveMetaClass) + " (" + effectiveMetaClass.getName() + ")", effectiveMetaClass);
        }
        return result;
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

    @Override
    public boolean isLast() {
        return false;
    }

    @Override
    public boolean isFirst() {
        return true;
    }

//    public DetailsStepFragment(ReportWizardCreator wizard) {
//        super(wizard, "reportDetails", "detailsStep");
//
//        isFirst = true;
////        initFrameHandler = new InitDetailsStepFrameHandler();
////        beforeShowFrameHandler = new BeforeShowDetailsStepFrameHandler();
//    }

//    protected class InitDetailsStepFrameHandler implements InitStepFrameHandler {
//        @Override
//        public void initFrame() {
//            initReportTypeOptionGroup();
//            initTemplateFormatLookupField();
//            initEntityLookupField();
//
//            wizard.entity.addValueChangeListener(new ChangeReportNameListener());
//        }
//
//        protected void initEntityLookupField() {
//            wizard.entity.setOptionsMap(getAvailableEntities());
//            wizard.entity.addValueChangeListener(new ClearRegionListener(
//                    new DialogActionWithChangedValue(DialogAction.Type.YES) {
//                        @Override
//                        public void actionPerform(Component component) {
//                            wizard.getItem().getReportRegions().clear();
////                            wizard.regionsTable.refresh(); //for web6
//                            wizard.needUpdateEntityModel = true;
//                            wizard.entity.setValue((MetaClass) newValue);
//
//                            clearQueryAndFilter();
//                        }
//                    }));
//        }
//
//        protected void initTemplateFormatLookupField() {
//            wizard.templateFileFormat.setOptionsMap(getAvailableTemplateFormats());
//            wizard.templateFileFormat.setTextInputAllowed(false);
//            wizard.templateFileFormat.setValue(TemplateFileType.DOCX);
//        }
//
//        protected void initReportTypeOptionGroup() {
//            wizard.reportTypeRadioButtonGroup.setOptionsMap(getListedReportOptionsMap());
//            wizard.reportTypeRadioButtonGroup.setValue(ReportType.SINGLE_ENTITY);
//            wizard.reportTypeRadioButtonGroup.addValueChangeListener(new ClearRegionListener(
//                    new DialogActionWithChangedValue(DialogAction.Type.YES) {
//                        @Override
//                        public void actionPerform(Component component) {
//                            wizard.getItem().getReportRegions().clear();
////                            wizard.regionsTable.refresh(); //for web6
//                            wizard.reportTypeRadioButtonGroup.setValue(newValue);
//                        }
//                    }));
//        }
//
//        protected Map<String, Object> getListedReportOptionsMap() {
//            Map<String, Object> result = new LinkedHashMap<>(3);
//            result.put(messages.getMessage("singleEntityReport"), ReportType.SINGLE_ENTITY);
//            result.put(messages.getMessage("listOfEntitiesReport"), ReportType.LIST_OF_ENTITIES);
//            result.put(messages.getMessage("listOfEntitiesReportWithQuery"), ReportType.LIST_OF_ENTITIES_WITH_QUERY);
//            return result;
//        }
//
//        protected Map<String, TemplateFileType> getAvailableTemplateFormats() {
//            Messages messages = wizard.messages;
//            Map<String, TemplateFileType> result = new LinkedHashMap<>(4);
//            result.put(messages.getMessage(TemplateFileType.XLSX), TemplateFileType.XLSX);
//            result.put(messages.getMessage(TemplateFileType.DOCX), TemplateFileType.DOCX);
//            result.put(messages.getMessage(TemplateFileType.HTML), TemplateFileType.HTML);
//            result.put(messages.getMessage(TemplateFileType.CSV), TemplateFileType.CSV);
//            result.put(messages.getMessage(TemplateFileType.TABLE), TemplateFileType.TABLE);
////            WindowConfig windowConfig = AppBeans.get(WindowConfig.NAME);
////            if (windowConfig.hasWindow(ShowChartController.JSON_CHART_SCREEN_ID)) {
////                result.put(messages.getMessage(TemplateFileType.CHART), TemplateFileType.CHART);
////            }
//            return result;
//        }
//
//        protected Map<String, MetaClass> getAvailableEntities() {
//            Map<String, MetaClass> result = new TreeMap<>(String::compareTo);
//            Collection<MetaClass> classes = wizard.metadataTools.getAllJpaEntityMetaClasses();
//            for (MetaClass metaClass : classes) {
//                MetaClass effectiveMetaClass = wizard.extendedEntities.getEffectiveMetaClass(metaClass);
//                if (!wizard.reportWizardService.isEntityAllowedForReportWizard(effectiveMetaClass)) {
//                    continue;
//                }
//                result.put(wizard.messageTools.getEntityCaption(effectiveMetaClass) + " (" + effectiveMetaClass.getName() + ")", effectiveMetaClass);
//            }
//            return result;
//        }
//    }

//    @Override
//    public List<String> validateFrame() {
//        ArrayList<String> errors = new ArrayList<>(super.validateFrame());
//        if (wizard.reportTypeRadioButtonGroup.getValue() == ReportType.LIST_OF_ENTITIES_WITH_QUERY && wizard.query == null) {
//            errors.add(messages.getMessage("fillReportQuery"));
//        }
//
//        return errors;
//    }

//    protected class ChangeReportNameListener implements Consumer<HasValue.ValueChangeEvent<MetaClass>> {
//
//        public ChangeReportNameListener() {
//        }
//
//        @Override
//        public void accept(HasValue.ValueChangeEvent e) {
//            setGeneratedReportName((MetaClass) e.getPrevValue(), (MetaClass) e.getValue());
//            wizard.outputFileName.setValue("");
//        }
//
//        protected void setGeneratedReportName(MetaClass prevValue, MetaClass value) {
//            String oldReportName = wizard.reportName.getValue();
//            MessageTools messageTools = wizard.messageTools;
//            if (StringUtils.isBlank(oldReportName)) {
//                String newText = messages.formatMessage("reportNamePattern", messageTools.getEntityCaption(value));
//                wizard.reportName.setValue(newText);
//            } else {
//                if (prevValue != null) {
//                    //if old text contains MetaClass name substring, just replace it
//                    String prevEntityCaption = messageTools.getEntityCaption(prevValue);
//                    if (StringUtils.contains(oldReportName, prevEntityCaption)) {
//
//                        String newText = oldReportName;
//                        int index = oldReportName.lastIndexOf(prevEntityCaption);
//                        if (index > -1) {
//                            newText = StringUtils.substring(oldReportName, 0, index)
//                                    + messageTools.getEntityCaption(value)
//                                    + StringUtils.substring(oldReportName, index + prevEntityCaption.length(), oldReportName.length());
//                        }
//
//                        wizard.reportName.setValue(newText);
//                        if (!oldReportName.equals(messages.formatMessage("reportNamePattern", prevEntityCaption))) {
//                            //if user changed auto generated report name and we have changed it, we show message to him
//                            wizard.notifications.create(Notifications.NotificationType.TRAY)
//                                    .withCaption(messages.getMessage("reportNameChanged"))
//                                    .show();
//                        }
//                    }
//                }
//            }
//        }
//    }

    //    protected class DialogActionWithChangedValue extends DialogAction {
//        protected Object newValue;
//
//        public DialogActionWithChangedValue(Type type) {
//            super(type);
//        }
//
//        public DialogActionWithChangedValue setValue(Object value) {
//            this.newValue = value;
//            return this;
//        }
//    }
//
//    protected class ClearRegionListener implements Consumer<HasValue.ValueChangeEvent<MetaClass>> {
//        protected DialogActionWithChangedValue okAction;
//
//        public ClearRegionListener(DialogActionWithChangedValue okAction) {
//            this.okAction = okAction;
//        }
//
//        @Override
//        public void accept(HasValue.ValueChangeEvent e) {
//            if (!reportDataDc.getItem().getReportRegions().isEmpty()) {
//                dialogs.createOptionDialog()
//                        .withCaption(messages.getMessage("dialogs.Confirmation"))
//                        .withMessage(messages.getMessage("regionsClearConfirm"))
//                        .withActions(
//                                okAction.setValue(e.getValue()),
//                                new DialogAction(DialogAction.Type.NO, Action.Status.PRIMARY)
//                        )
//                        .show();
//            } else {
//                needUpdateEntityModel = true;
//                clearQueryAndFilter();
//            }
//        }
//    }

//    protected void clearQueryAndFilter() {
//        query = null;
//        queryParameters = null;
//        //filter = null;
////        filterEntity = null;
//        //conditionsTree = null;
//    }

//    protected class BeforeShowDetailsStepFrameHandler implements BeforeShowStepFrameHandler {
//        @Override
//        public void beforeShowFrame() {
//            //TODO dialog options
////            wizard.getDialogOptions()
////                    .setHeight(wizard.wizardHeight).setHeightUnit(SizeUnit.PIXELS)
////                    .center();
//        }
//    }
}