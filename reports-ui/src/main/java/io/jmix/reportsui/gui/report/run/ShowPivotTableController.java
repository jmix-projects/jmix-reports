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

import com.haulmont.yarg.reporting.ReportOutputDocument;
import io.jmix.core.LoadContext;
import io.jmix.core.Messages;
import io.jmix.core.common.util.ParamsMap;
import io.jmix.core.entity.KeyValueEntity;
import io.jmix.core.impl.StandardSerialization;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.reports.entity.PivotTableData;
import io.jmix.reports.entity.Report;
import io.jmix.reports.entity.ReportOutputType;
import io.jmix.reports.entity.ReportTemplate;
import io.jmix.reportsui.gui.ReportGuiManager;
import io.jmix.ui.Fragments;
import io.jmix.ui.UiComponents;
import io.jmix.ui.WindowParam;
import io.jmix.ui.component.*;
import io.jmix.ui.screen.*;
import io.jmix.ui.theme.ThemeConstants;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;


@UiController("report_ShowPivotTableController")
@UiDescriptor("show-pivot-table.xml")
public class ShowPivotTableController extends StandardLookup {
    public static final String PIVOT_TABLE_SCREEN_ID = "chart$pivotTable";

    public static final String REPORT_PARAMETER = "report";
    public static final String PIVOT_TABLE_DATA_PARAMETER = "pivotTableData";
    public static final String TEMPLATE_CODE_PARAMETER = "templateCode";
    public static final String PARAMS_PARAMETER = "reportParams";

    @Autowired
    protected ReportGuiManager reportGuiManager;

    @Autowired
    protected UiComponents uiComponents;

    @Autowired
    protected ThemeConstants themeConstants;

    @Autowired
    protected GroupBoxLayout reportBox;

    @Autowired
    protected GroupBoxLayout reportParamsBox;

    @Autowired
    protected BoxLayout parametersFrameHolder;

    @Autowired
    protected ComboBox<Report> reportLookup;

    @Autowired
    protected HBoxLayout reportSelectorBox;

    @Autowired
    protected StandardSerialization serialization;

    @Autowired
    protected Messages messages;

    @Autowired
    protected Fragments fragments;

    @Autowired
    protected ScreenValidation screenValidation;

    @Autowired
    protected CurrentAuthentication currentAuthentication;

    @WindowParam(name = REPORT_PARAMETER)
    protected Report report;

    @WindowParam(name = PARAMS_PARAMETER)
    protected Map<String, Object> params;

    @WindowParam(name = TEMPLATE_CODE_PARAMETER)
    protected String templateCode;

    @WindowParam(name = PIVOT_TABLE_DATA_PARAMETER)
    protected byte[] pivotTableData;

    protected InputParametersFrame inputParametersFrame;


    @Install(to = "reportsDl", target = Target.DATA_LOADER)
    protected List<Report> reportsDlLoadDelegate(LoadContext<Report> loadContext) {
        return reportGuiManager.getAvailableReports(null, currentAuthentication.getUser(), null);
    }


    @Subscribe
    protected void onInit(InitEvent event) {
        //TODO dialog options
//        getDialogOptions()
//                .setWidth(themeConstants.get("cuba.gui.report.ShowPivotTable.width"))
//                .setHeight(themeConstants.get("cuba.gui.report.ShowPivotTable.height"))
//                .setResizable(true)
//                .center();

        if (report != null) {
            reportSelectorBox.setVisible(false);
            if (pivotTableData != null) {
                PivotTableData result = (PivotTableData) serialization.deserialize(pivotTableData);
                initFrames(result.getPivotTableJson(), result.getValues(), params);
            }
        } else {
            showStubText();
        }

        reportLookup.addValueChangeListener(e -> {
            report = (Report) e.getValue();
            initFrames(null, null, null);
        });

    }

    protected void initFrames(String pivotTableJson, List<KeyValueEntity> values, Map<String, Object> reportParameters) {
        openPivotTable(pivotTableJson, values);
        openReportParameters(reportParameters);
    }

    protected void openReportParameters(Map<String, Object> reportParameters) {
        parametersFrameHolder.removeAll();
        if (report != null) {
            Map<String, Object> params = ParamsMap.of(
                    InputParametersFrame.REPORT_PARAMETER, report,
                    InputParametersFrame.PARAMETERS_PARAMETER, reportParameters
            );

            inputParametersFrame = (InputParametersFrame) fragments.create(this,
                    "report_inputParametersFrame",
                    new MapScreenOptions(params))
                    .init();
            parametersFrameHolder.add(inputParametersFrame.getFragment());

            reportParamsBox.setVisible(true);
        } else {
            reportParamsBox.setVisible(false);
        }
    }

    @Subscribe("printReportBtn")
    public void printReport() {
        if (inputParametersFrame != null && inputParametersFrame.getReport() != null) {
            ValidationErrors validationErrors = screenValidation.validateUiComponents(getWindow());
            if (validationErrors.isEmpty()) {
                Map<String, Object> parameters = inputParametersFrame.collectParameters();
                Report report = inputParametersFrame.getReport();

                if (templateCode == null) {
                    templateCode = report.getTemplates().stream()
                            .filter(template -> template.getReportOutputType() == ReportOutputType.PIVOT_TABLE)
                            .findFirst()
                            .map(ReportTemplate::getCode).orElse(null);
                }

                ReportOutputDocument document = reportGuiManager.getReportResult(report, parameters, templateCode);
                PivotTableData result = (PivotTableData) serialization.deserialize(document.getContent());
                openPivotTable(result.getPivotTableJson(), result.getValues());
            } else {
                screenValidation.showValidationErrors(this, validationErrors);
            }
        }
    }

    protected void openPivotTable(String pivotTableJson, List<KeyValueEntity> values) {
        reportBox.removeAll();
        if (pivotTableJson != null) {
            Map<String, Object> screenParams = ParamsMap.of(
                    "pivotTableJson", pivotTableJson,
                    "values", values);

            Fragment fragment = fragments.create(this, PIVOT_TABLE_SCREEN_ID, new MapScreenOptions(screenParams))
                    .init()
                    .getFragment();

            reportBox.add(fragment);
        }
        showStubText();
    }

    protected void showStubText() {
        if (reportBox.getOwnComponents().isEmpty()) {
            Label label = uiComponents.create(Label.class);
            label.setValue(messages.getMessage("showPivotTable.caption"));
            label.setAlignment(Component.Alignment.MIDDLE_CENTER);
            label.setStyleName("h1");
            reportBox.add(label);
        }
    }
}
