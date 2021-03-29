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
import io.jmix.core.ExtendedEntities;
import io.jmix.core.MessageTools;
import io.jmix.core.Metadata;
import io.jmix.core.MetadataTools;
import io.jmix.core.metamodel.model.MetaClass;
import io.jmix.reports.app.service.ReportsWizard;
import io.jmix.reports.entity.ReportOutputType;
import io.jmix.reports.entity.ReportType;
import io.jmix.reports.entity.wizard.ReportData;
import io.jmix.reports.entity.wizard.ReportTypeGenerate;
import io.jmix.reports.entity.wizard.TemplateFileType;
import io.jmix.reportsui.screen.report.run.ShowChartLookup;
import io.jmix.ui.Dialogs;
import io.jmix.ui.Notifications;
import io.jmix.ui.WindowConfig;
import io.jmix.ui.component.*;
import io.jmix.ui.model.InstanceContainer;
import io.jmix.ui.screen.Subscribe;
import io.jmix.ui.screen.UiController;
import io.jmix.ui.screen.UiDescriptor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

@UiController("report_DetailsStep.fragment")
@UiDescriptor("details-fragment.xml")
public class DetailsStepFragment extends StepFragment {

    @Autowired
    private InstanceContainer<ReportData> reportDataDc;

    @Autowired
    protected Dialogs dialogs;

    @Autowired
    protected MessageTools messageTools;

    @Autowired
    protected ComboBox<MetaClass> entity;

    @Autowired
    private TextField<String> reportName;

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

    @Autowired
    protected Notifications notifications;

    @Autowired
    private SourceCodeEditor reportQueryCodeEditor;

    @Autowired
    protected WindowConfig windowConfig;

    protected boolean needUpdateEntityModel = false;

    public boolean isNeedUpdateEntityModel() {
        return needUpdateEntityModel;
    }

    public void setNeedUpdateEntityModel(boolean needUpdateEntityModel) {
        this.needUpdateEntityModel = needUpdateEntityModel;
    }

    @Subscribe
    public void onInit(InitEvent event) {
        initReportTypeOptionGroup();
        initTemplateFormatLookupField();
        initEntityLookupField();
        initQueryReportSourceCode();

        //entity.addValueChangeListener(new ChangeReportNameListener());
    }

    protected void initQueryReportSourceCode() {
        reportQueryCodeEditor.setHighlightActiveLine(false);
        reportQueryCodeEditor.setShowGutter(false);
        reportQueryCodeEditor.setMode(SourceCodeEditor.Mode.SQL);
    }

    protected void initEntityLookupField() {
        entity.setOptionsMap(getAvailableEntities());
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

    @Subscribe("reportTypeGenerate")
    public void onReportTypeGenerateValueChange(HasValue.ValueChangeEvent event) {
        ReportTypeGenerate currentType = (ReportTypeGenerate) event.getValue();

        reportQueryCodeEditor.setVisible(currentType.isList() && !currentType.isEntity());
    }

    @Subscribe("entity")
    public void onEntityValueChange(HasValue.ValueChangeEvent event) {
        ReportData reportData = reportDataDc.getItem();
        reportData.getReportRegions().clear();
        needUpdateEntityModel = true;

        MetaClass metaClass = (MetaClass) event.getValue();
        reportData.setEntityName(metaClass.getName());

        String query = String.format("select e from %s e", metaClass.getName());
        reportQueryCodeEditor.setValue(query);

        setGeneratedReportName((MetaClass) event.getPrevValue(), (MetaClass) event.getValue());
//            wizard.outputFileName.setValue("");
    }

    protected void setGeneratedReportName(MetaClass prevValue, MetaClass value) {
        String oldReportName = reportName.getValue();
        if (StringUtils.isBlank(oldReportName)) {
            String newText = messages.formatMessage(getClass(), "reportNamePattern", messageTools.getEntityCaption(value));
            reportName.setValue(newText);
        } else {
            if (prevValue != null) {
                //if old text contains MetaClass name substring, just replace it
                String prevEntityCaption = messageTools.getEntityCaption(prevValue);
                if (StringUtils.contains(oldReportName, prevEntityCaption)) {

                    String newText = oldReportName;
                    int index = oldReportName.lastIndexOf(prevEntityCaption);
                    if (index > -1) {
                        newText = StringUtils.substring(oldReportName, 0, index)
                                + messageTools.getEntityCaption(value)
                                + StringUtils.substring(oldReportName, index + prevEntityCaption.length(), oldReportName.length());
                    }

                    reportName.setValue(newText);
                    if (!oldReportName.equals(messages.formatMessage(getClass(), "reportNamePattern", prevEntityCaption))) {
                        //if user changed auto generated report name and we have changed it, we show message to him
                        notifications.create(Notifications.NotificationType.TRAY)
                                .withCaption(messages.getMessage(getClass(),"reportNameChanged"))
                                .show();
                    }
                }
            }
        }
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
        result.put(messages.getMessage(getClass(), "singleEntityReport"), ReportTypeGenerate.SINGLE_ENTITY);
        result.put(messages.getMessage(getClass(), "listOfEntitiesReport"), ReportTypeGenerate.LIST_OF_ENTITIES);
        result.put(messages.getMessage(getClass(), "listOfEntitiesReportWithQuery"), ReportTypeGenerate.LIST_OF_ENTITIES_WITH_QUERY);
        return result;
    }

    protected Map<String, TemplateFileType> getAvailableTemplateFormats() {
        Map<String, TemplateFileType> result = new LinkedHashMap<>(4);
        result.put(messages.getMessage(TemplateFileType.XLSX), TemplateFileType.XLSX);
        result.put(messages.getMessage(TemplateFileType.DOCX), TemplateFileType.DOCX);
        result.put(messages.getMessage(TemplateFileType.HTML), TemplateFileType.HTML);
        result.put(messages.getMessage(TemplateFileType.CSV), TemplateFileType.CSV);
        result.put(messages.getMessage(TemplateFileType.TABLE), TemplateFileType.TABLE);

        if (windowConfig.hasWindow(ShowChartLookup.JSON_CHART_SCREEN_ID)) {
            result.put(messages.getMessage(TemplateFileType.CHART), TemplateFileType.CHART);
        }
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

    @Override
    public String getCaption() {
        return messages.getMessage(getClass(), "reportDetails");
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
//    }

    @Override
    public List<String> validateFrame() {
        ArrayList<String> errors = new ArrayList<>(super.validateFrame());
        if (reportTypeGenerate.getValue() == ReportTypeGenerate.LIST_OF_ENTITIES_WITH_QUERY && reportQueryCodeEditor.getValue() == null) {
            errors.add(messages.getMessage("fillReportQuery"));
        }

        return errors;
    }

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