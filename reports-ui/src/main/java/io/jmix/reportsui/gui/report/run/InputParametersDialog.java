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
package io.jmix.reportsui.gui.report.run;

import io.jmix.core.Messages;
import io.jmix.core.common.util.Preconditions;
import io.jmix.reports.entity.Report;
import io.jmix.reports.entity.ReportInputParameter;
import io.jmix.reports.entity.ReportTemplate;
import io.jmix.reports.exception.ReportParametersValidationException;
import io.jmix.reportsui.gui.ReportGuiManager;
import io.jmix.reportsui.gui.ReportParameterValidator;
import io.jmix.ui.Notifications;
import io.jmix.ui.UiProperties;
import io.jmix.ui.component.Button;
import io.jmix.ui.component.ValidationErrors;
import io.jmix.ui.screen.*;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.Map;

@DialogMode(width = "AUTO", forceDialog = true)
@UiController("report_InputParameters.dialog")
@UiDescriptor("input-parameters.xml")
public class InputParametersDialog extends Screen {
    public static final String INPUT_PARAMETER = "inputParameter";
    public static final String REPORT_PARAMETER = "report";

    protected String templateCode;

    protected String outputFileName;

    protected boolean bulkPrint;

    protected Report report;

    protected ReportInputParameter inputParameter;

    protected Map<String, Object> parameters;

    protected Collection selectedEntities;

    @Autowired
    protected ReportGuiManager reportGuiManager;

    @Autowired
    protected Button printReportBtn;

    @Autowired
    protected InputParametersFragment inputParametersFrame;

    @Autowired
    protected ReportParameterValidator reportParameterValidator;

    @Autowired
    protected Notifications notifications;

    @Autowired
    protected Messages messages;

    @Autowired
    protected ScreenValidation screenValidation;

    @Autowired
    protected UiProperties properties;

    public void setTemplateCode(String templateCode) {
        this.templateCode = templateCode;
    }

    public void setOutputFileName(String outputFileName) {
        this.outputFileName = outputFileName;
    }

    public void setBulkPrint(boolean bulkPrint) {
        this.bulkPrint = bulkPrint;
    }

    public void setReport(Report report) {
        this.report = report;
    }

    public void setInputParameter(ReportInputParameter inputParameter) {
        this.inputParameter = inputParameter;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    @Subscribe
    protected void onInit(InitEvent event) {
        if (bulkPrint) {
            Preconditions.checkNotNullArgument(inputParameter, String.format("%s is null for bulk print", INPUT_PARAMETER));
            //noinspection unchecked
            selectedEntities = (Collection) parameters.get(inputParameter.getAlias());
        }
        //todo
//        Action printReportAction = printReportBtn.getAction();
//        printReportAction.setShortcut(properties.getCommitShortcut());
    }

    @Subscribe
    protected void onBeforeShow(BeforeShowEvent event) {
        inputParametersFrame.initTemplateAndOutputSelect();
    }

    @Subscribe("printReportBtn")
    public void printReport(Button.ClickEvent event) {
        if (inputParametersFrame.getReport() != null) {
            ValidationErrors validationErrors = screenValidation.validateUiComponents(getWindow());
            if (validationErrors.isEmpty()) {
                ReportTemplate template = inputParametersFrame.getReportTemplate();
                if (template != null) {
                    templateCode = template.getCode();
                }
                Report report = inputParametersFrame.getReport();
                Map<String, Object> parameters = inputParametersFrame.collectParameters();
                if (bulkPrint) {
                    reportGuiManager.bulkPrint(report, templateCode, inputParametersFrame.getOutputType(), inputParameter.getAlias(), selectedEntities, this, parameters);
                } else {
                    reportGuiManager.printReport(report, parameters, templateCode, outputFileName, inputParametersFrame.getOutputType(), this);
                }
            } else {
                screenValidation.showValidationErrors(this, validationErrors);
            }
        }
    }

//    @Override
//    public boolean validateAll() {
//        return super.validateAll() && crossValidateParameters();
//    }

    protected boolean crossValidateParameters() {
        boolean isValid = true;
        if (BooleanUtils.isTrue(inputParametersFrame.getReport().getValidationOn())) {
            try {
                reportParameterValidator.crossValidateParameters(inputParametersFrame.getReport(),
                        inputParametersFrame.collectParameters());
            } catch (ReportParametersValidationException e) {

                notifications.create(Notifications.NotificationType.WARNING)
                        .withCaption(messages.getMessage("validationFail.caption"))
                        .withDescription(e.getMessage())
                        .show();
                isValid = false;
            }
        }

        return isValid;
    }

    @Subscribe("cancelBtn")
    public void cancel() {
        close(StandardOutcome.CLOSE);
    }
}