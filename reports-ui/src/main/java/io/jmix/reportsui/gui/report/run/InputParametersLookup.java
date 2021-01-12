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
import io.jmix.reports.entity.Report;
import io.jmix.reports.entity.ReportInputParameter;
import io.jmix.reports.entity.ReportTemplate;
import io.jmix.reports.exception.ReportParametersValidationException;
import io.jmix.reportsui.gui.ReportGuiManager;
import io.jmix.reportsui.gui.ReportParameterValidator;
import io.jmix.ui.Notifications;
import io.jmix.ui.action.Action;
import io.jmix.ui.component.Button;
import io.jmix.ui.component.ValidationErrors;
import io.jmix.ui.component.Window;
import io.jmix.ui.screen.*;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.Map;

@UiController("report_InputParameters.lookup")
@UiDescriptor("input-parameters.xml")
public class InputParametersLookup extends StandardLookup {
    public static final String TEMPLATE_CODE_PARAMETER = "templateCode";
    public static final String OUTPUT_FILE_NAME_PARAMETER = "outputFileName";
    public static final String INPUT_PARAMETER = "inputParameter";
    public static final String BULK_PRINT = "bulkPrint";
    public static final String REPORT_PARAMETER = "report";

    protected String templateCode;

    protected String outputFileName;

    protected boolean bulkPrint;

    protected Report report;

    protected ReportInputParameter inputParameter;

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

    @Subscribe
    protected void onInit(InitEvent event) {
        //noinspection unchecked
        //todo params
//        templateCode = (String) params.get(TEMPLATE_CODE_PARAMETER);
//        outputFileName = (String) params.get(OUTPUT_FILE_NAME_PARAMETER);
//        bulkPrint = BooleanUtils.isTrue((Boolean) params.get(BULK_PRINT));
//        inputParameter = (ReportInputParameter) params.get(INPUT_PARAMETER);
//
//        if (bulkPrint) {
//            Preconditions.checkNotNullArgument(inputParameter, String.format("%s is null for bulk print", INPUT_PARAMETER));
//            //noinspection unchecked
//            Map<String, Object> parameters = (Map<String, Object>) params.get(PARAMETERS_PARAMETER);
//            selectedEntities = (Collection) parameters.get(inputParameter.getAlias());
//        }
//
//        report = (Report) params.get(REPORT_PARAMETER);

        Action printReportAction = printReportBtn.getAction();
//TODO Commit shortcut
//        String commitShortcut = clientConfig.getCommitShortcut();
//        printReportAction.setShortcut(commitShortcut);
        //addAction(printReportAction);
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
        close(new StandardCloseAction(Window.CLOSE_ACTION_ID));
    }
}