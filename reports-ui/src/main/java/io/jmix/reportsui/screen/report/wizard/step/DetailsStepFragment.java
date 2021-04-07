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

import io.jmix.core.ExtendedEntities;
import io.jmix.core.MessageTools;
import io.jmix.core.Metadata;
import io.jmix.core.MetadataTools;
import io.jmix.core.metamodel.model.MetaClass;
import io.jmix.reports.app.service.ReportsWizard;
import io.jmix.reports.entity.wizard.ReportData;
import io.jmix.reports.entity.wizard.ReportTypeGenerate;
import io.jmix.reports.entity.wizard.TemplateFileType;
import io.jmix.reportsui.screen.report.run.ShowChartLookup;
import io.jmix.ui.Dialogs;
import io.jmix.ui.Notifications;
import io.jmix.ui.WindowConfig;
import io.jmix.ui.action.DialogAction;
import io.jmix.ui.component.*;
import io.jmix.ui.model.InstanceContainer;
import io.jmix.ui.screen.Subscribe;
import io.jmix.ui.screen.UiController;
import io.jmix.ui.screen.UiDescriptor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

@UiController("report_DetailsStep.fragment")
@UiDescriptor("details-step-fragment.xml")
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
    protected ComboBox<TemplateFileType> templateFileFormat;

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
    }

    protected void initEntityLookupField() {
        entity.setOptionsMap(getAvailableEntities());
    }

    @Subscribe("reportName")
    public void onReportNameTextChange(TextInputField.TextChangeEvent event) {
        reportDataDc.getItem().setName(event.getText());
    }

    @Subscribe("reportTypeGenerate")
    public void onReportTypeGenerateValueChange(HasValue.ValueChangeEvent<ReportTypeGenerate> event) {
        dialogs.createOptionDialog()
                .withCaption(messages.getMessage("dialogs.Confirmation"))
                .withMessage(messages.getMessage(getClass(), "regionsClearConfirm"))
                .withActions(
                        new DialogAction(DialogAction.Type.OK).withHandler(e -> {
                            ReportTypeGenerate reportTypeGenerate = event.getValue();

                            ReportData reportData = reportDataDc.getItem();
                            reportData.setReportTypeGenerate(reportTypeGenerate);
                            reportData.getReportRegions().clear();

                            clearQuery();
                        }),
                        new DialogAction(DialogAction.Type.CANCEL))
                .show();
    }

    @Subscribe("entity")
    public void onEntityValueChange(HasValue.ValueChangeEvent<MetaClass> event) {
        ReportData reportData = reportDataDc.getItem();
        reportData.getReportRegions().clear();
        needUpdateEntityModel = true;

        MetaClass metaClass = event.getValue();
        reportData.setEntityName(metaClass.getName());

        setGeneratedReportName(event.getPrevValue(), metaClass);

        clearQuery();
    }

    @Subscribe("templateFileFormat")
    public void onTemplateFileFormatValueChange(HasValue.ValueChangeEvent<TemplateFileType> event) {
        reportDataDc.getItem().setTemplateFileType(event.getValue());
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
                                .withCaption(messages.getMessage(getClass(), "reportNameChanged"))
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
    public String getDescription() {
        return messages.getMessage(getClass(), "enterMainParameters");
    }

    protected void clearQuery() {
        ReportData reportData = reportDataDc.getItem();
        reportData.setQuery(null);
        reportData.setQueryParameters(null);
    }
}