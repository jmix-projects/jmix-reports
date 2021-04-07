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

import io.jmix.reports.entity.wizard.ReportData;
import io.jmix.reports.entity.wizard.ReportTypeGenerate;
import io.jmix.ui.component.HasValue;
import io.jmix.ui.component.SourceCodeEditor;
import io.jmix.ui.model.InstanceContainer;
import io.jmix.ui.screen.Subscribe;
import io.jmix.ui.screen.UiController;
import io.jmix.ui.screen.UiDescriptor;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

@UiController("report_QueryStep.fragment")
@UiDescriptor("query-step-fragment.xml")
public class QueryStepFragment extends StepFragment {

    @Autowired
    private InstanceContainer<ReportData> reportDataDc;

    @Autowired
    private SourceCodeEditor reportQueryCodeEditor;

    @Subscribe
    public void onInit(InitEvent event) {
        initQueryReportSourceCode();
    }

    @Subscribe("reportQueryCodeEditor")
    public void onReportQueryCodeEditorValueChange(HasValue.ValueChangeEvent<String> event) {
        reportDataDc.getItem().setQuery(event.getValue());
    }

    protected void initQueryReportSourceCode() {
        reportQueryCodeEditor.setHighlightActiveLine(false);
        reportQueryCodeEditor.setShowGutter(false);
        reportQueryCodeEditor.setMode(SourceCodeEditor.Mode.SQL);
    }

    @Override
    public String getCaption() {
        return messages.getMessage(getClass(), "reportQueryCaption");
    }

    @Override
    public String getDescription() {
        return messages.getMessage(getClass(), "enterQuery");
    }

    @Override
    public void beforeShow() {
        String entityName = reportDataDc.getItem().getEntityName();

        String query = String.format("select e from %s e", entityName);
        reportQueryCodeEditor.setValue(query);
    }
}