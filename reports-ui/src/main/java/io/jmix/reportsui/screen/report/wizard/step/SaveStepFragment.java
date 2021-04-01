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

import io.jmix.core.CoreProperties;
import io.jmix.core.Messages;
import io.jmix.reports.app.service.ReportsWizard;
import io.jmix.reports.entity.ReportOutputType;
import io.jmix.reports.entity.wizard.ReportData;
import io.jmix.reports.entity.wizard.TemplateFileType;
import io.jmix.reports.exception.TemplateGenerationException;
import io.jmix.reportsui.screen.report.wizard.OutputFormatTools;
import io.jmix.ui.Dialogs;
import io.jmix.ui.Fragments;
import io.jmix.ui.Notifications;
import io.jmix.ui.UiProperties;
import io.jmix.ui.component.*;
import io.jmix.ui.download.ByteArrayDataProvider;
import io.jmix.ui.download.DownloadFormat;
import io.jmix.ui.download.Downloader;
import io.jmix.ui.model.InstanceContainer;
import io.jmix.ui.screen.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

@UiController("report_Save.fragment")
@UiDescriptor("save-fragment.xml")
public class SaveStepFragment extends StepFragment {

    @Autowired
    private InstanceContainer<ReportData> reportDataDc;

    @Autowired
    protected UiProperties uiProperties;

    @Autowired
    protected CoreProperties coreProperties;

    @Autowired
    protected Notifications notifications;

    @Autowired
    protected Dialogs dialogs;

    @Autowired
    protected Messages messages;

    @Autowired
    protected Fragments fragments;

    @Autowired
    protected Downloader downloader;

    @Autowired
    protected ReportsWizard reportWizardService;

    @Autowired
    private ComboBox<ReportOutputType> outputFileFormat;

    @Autowired
    private TextField outputFileName;

    @Autowired
    protected OutputFormatTools outputFormatTools;

    @Autowired
    private Button downloadTemplateFile;


    @Subscribe
    public void onInit(InitEvent event) {
        //beforeShowFrameHandler = new BeforeShowSaveStepFrameHandler();

        //beforeHideFrameHandler = new BeforeHideSaveStepFrameHandler();
    }

    @Subscribe("outputFileFormat")
    public void onOutputFileFormatValueChange(HasValue.ValueChangeEvent event) {
        event.getValue();
    }


    @Install(to = "outputFileFormat", subject = "contextHelpIconClickHandler")
    private void outputFileFormatContextHelpIconClickHandler(HasContextHelp.ContextHelpIconClickEvent contextHelpIconClickEvent) {
        dialogs.createMessageDialog()
                .withCaption(messages.getMessage("template.namePatternText"))
                .withMessage(messages.getMessage("template.namePatternTextHelp"))
                .withModal(false)
                .withWidth("560px")
                .show();
    }

    protected void setCorrectReportOutputType() {
        ReportOutputType outputFileFormatPrevValue = outputFileFormat.getValue();
        outputFileFormat.setValue(null);
        Map<String, ReportOutputType> optionsMap = outputFormatTools.getOutputAvailableFormats(reportDataDc.getItem().getTemplateFileType());
        outputFileFormat.setOptionsMap(optionsMap);

        if (outputFileFormatPrevValue != null) {
            if (optionsMap.containsKey(outputFileFormatPrevValue.toString())) {
                outputFileFormat.setValue(outputFileFormatPrevValue);
            }
        }
        if (outputFileFormat.getValue() == null) {
            if (optionsMap.size() > 1) {
                outputFileFormat.setValue(optionsMap.get(reportDataDc.getItem().getTemplateFileType().toString()));
            } else if (optionsMap.size() == 1) {
                outputFileFormat.setValue(optionsMap.values().iterator().next());
            }
        }
    }

    @Subscribe(id = "reportDataDc", target = Target.DATA_CONTAINER)
    public void onReportDataDcItemPropertyChange(InstanceContainer.ItemPropertyChangeEvent<ReportData> event) {
        if(event.getProperty().equals("entity")) {
            setCorrectReportOutputType();
        }
    }

    @Override
    public String getCaption() {
        return messages.getMessage(getClass(), "saveReport");
    }

    @Override
    public boolean isLast() {
        return true;
    }

    @Override
    public boolean isFirst() {
        return false;
    }

//    protected class BeforeShowSaveStepFrameHandler implements BeforeShowStepFrameHandler {
//        @Override
//        public void beforeShowFrame() {
//            initSaveAction();
//            initDownloadAction();
//
//            if (StringUtils.isEmpty(wizard.outputFileName.getValue())) {
//                Object value = wizard.templateFileFormat.getValue();
//                wizard.outputFileName.setValue(wizard.generateOutputFileName(value.toString().toLowerCase()));
//            }
//            wizard.setCorrectReportOutputType();
//
//            initChartPreview();
//        }

//        protected void initChartPreview() {
//            if (wizard.outputFileFormat.getValue() == ReportOutputType.CHART) {
//                wizard.chartPreviewBox.setVisible(true);
//                wizard.diagramTypeLabel.setVisible(true);
//                wizard.diagramType.setVisible(true);
//
//                showChart();
//
////TODO dialog options
////                wizard.getDialogOptions()
////                        .setHeight(wizard.wizardHeight + 400).setHeightUnit(SizeUnit.PIXELS)
////                        .center();
//
//                wizard.diagramType.setRequired(true);
//                wizard.diagramType.setOptionsList(Arrays.asList(ChartType.values()));
//                wizard.diagramType.setValue(ChartType.SERIAL);
//
//                wizard.diagramType.addValueChangeListener(e -> {
//                    wizard.getItem().setChartType((ChartType) e.getValue());
//                    wizard.chartPreviewBox.removeAll();
//                    showChart();
//                });
//            } else {
//                wizard.chartPreviewBox.setVisible(false);
//                wizard.diagramTypeLabel.setVisible(false);
//                wizard.diagramType.setVisible(false);
//            }
//        }

    @Subscribe("downloadTemplateFile")
    public void onDownloadTemplateFileClick(Button.ClickEvent event) {
        ReportData reportData = reportDataDc.getItem();
        try {
            //reportData.setName(wizard.reportName.getValue().toString());
            TemplateFileType templateFileType = reportData.getTemplateFileType();
            byte[] newTemplate = reportWizardService.generateTemplate(reportData, templateFileType);
            downloader.download(new ByteArrayDataProvider(
                            newTemplate,
                            uiProperties.getSaveExportedByteArrayDataThresholdBytes(),
                            coreProperties.getTempDir()),
                    downloadTemplateFile.getCaption(),
                    DownloadFormat.getByExtension(templateFileType.toString().toLowerCase()));
        } catch (TemplateGenerationException e) {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption(messages.getMessage(getClass(), "templateGenerationException"))
                    .show();
        }
    }

//
//        protected void showChart() {
//            byte[] content = wizard.buildReport(true).getDefaultTemplate().getContent();
//            String chartDescriptionJson = new String(content, StandardCharsets.UTF_8);
//            AbstractChartDescription chartDescription = AbstractChartDescription.fromJsonString(chartDescriptionJson);
//            RandomChartDataGenerator randomChartDataGenerator = new RandomChartDataGenerator();
//            List<Map<String, Object>> randomChartData = randomChartDataGenerator.generateRandomChartData(chartDescription);
//            ChartToJsonConverter chartToJsonConverter = new ChartToJsonConverter();
//            String chartJson = null;
//            if (chartDescription instanceof PieChartDescription) {
//                chartJson = chartToJsonConverter.convertPieChart((PieChartDescription) chartDescription, randomChartData);
//            } else if (chartDescription instanceof SerialChartDescription) {
//                chartJson = chartToJsonConverter.convertSerialChart((SerialChartDescription) chartDescription, randomChartData);
//            }
//
//            //todo
////            wizard.openFrame(wizard.chartPreviewBox, ShowChartController.JSON_CHART_SCREEN_ID,
////                    ParamsMap.of(ShowChartController.CHART_JSON_PARAMETER, chartJson));
//        }
//    }
}
