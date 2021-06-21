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

package io.jmix.reportsui.runner;

import io.jmix.reports.entity.Report;
import io.jmix.reports.entity.ReportOutputType;
import io.jmix.reports.entity.ReportTemplate;
import io.jmix.reports.runner.ReportRunContextBuilder;
import io.jmix.ui.screen.FrameOwner;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Class is used for creating instances of {@link UiReportRunContext}. It allows to create run contexts using various
 * additional criteria, e.g. run context may be created either by {@link Report} entity or by report code.
 * <p/>
 * The following parameters can be specified to create {@link UiReportRunContext}:
 * <ul>
 *     <li>{@link Report} entity or report code</li>
 *     <li>{@link ReportTemplate} entity or template code: if none of these fields is set, the default template is used.</li>
 *     <li>Output type</li>
 *     <li>Output name pattern</li>
 *     <li>Input parameters</li>
 *     <li>Screen: screen or screen fragment from which the report runs</li>
 *     <li>Show a dialog to input the report parameters (defined by {@link ShowParametersDialogMode})</li>
 *     <li>Run a report synchronously or in the background (defined by {@link RunInBackgroundMode})</li>
 * </ul>
 * <br/>
 * Use the {@link UiReportRunner} bean to obtain an instance of the {@link UiReportRunContextBuilder}.
 */
@Component("report_UiReportRunContextBuilder")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class UiReportRunContextBuilder {
    @Autowired
    protected ObjectProvider<ReportRunContextBuilder> reportRunContextBuilders;

    private ReportRunContextBuilder reportRunContextBuilder;
    private FrameOwner screen;
    private RunInBackgroundMode runInBackgroundMode;
    private ShowParametersDialogMode showParametersDialogMode;

    private UiReportRunner uiReportRunner;

    public void setUiReportRunner(UiReportRunner uiReportRunner) {
        this.uiReportRunner = uiReportRunner;
    }

    public UiReportRunContextBuilder init(Report report) {
        this.reportRunContextBuilder = reportRunContextBuilders.getObject(report);
        return this;
    }

    public UiReportRunContextBuilder init(String reportCode) {
        this.reportRunContextBuilder = reportRunContextBuilders.getObject(reportCode);
        return this;
    }

    /**
     * Sets a map with input parameters.
     *
     * @param params input parameters
     * @return current instance of builder
     */
    public UiReportRunContextBuilder withParams(Map<String, Object> params) {
        this.reportRunContextBuilder.withParams(params);
        return this;
    }

    /**
     * Adds an input parameter to the parameter map.
     *
     * @param alias parameter alias
     * @param value parameter value
     * @return current instance of builder
     */
    public UiReportRunContextBuilder addParam(String alias, Object value) {
        this.reportRunContextBuilder.addParam(alias, value);
        return this;
    }

    /**
     * Sets a code of template that will be used to run a report.
     *
     * @param templateCode template code
     * @return current instance of builder
     */
    public UiReportRunContextBuilder withTemplateCode(String templateCode) {
        this.reportRunContextBuilder.withTemplateCode(templateCode);
        return this;
    }

    /**
     * Sets a template that will be used to run a report.
     *
     * @param template report template
     * @return current instance of builder
     */
    public UiReportRunContextBuilder withTemplate(ReportTemplate template) {
        this.reportRunContextBuilder.withTemplate(template);
        return this;
    }

    /**
     * Sets a type of output document.
     *
     * @param outputType type of output document.
     * @return current instance of builder
     */
    public UiReportRunContextBuilder withOutputType(ReportOutputType outputType) {
        this.reportRunContextBuilder.withOutputType(outputType);
        return this;
    }

    /**
     * Sets a name of output document.
     *
     * @param outputNamePattern name of an output document
     * @return current instance of builder
     */
    public UiReportRunContextBuilder withOutputNamePattern(@Nullable String outputNamePattern) {
        this.reportRunContextBuilder.withOutputNamePattern(outputNamePattern);
        return this;
    }

    /**
     * Sets a mode to run the report in the background.
     *
     * @param mode mode to run the report in the background.
     * @return current instance of builder
     */
    public UiReportRunContextBuilder runInBackground(RunInBackgroundMode mode) {
        this.runInBackgroundMode = mode;
        return this;
    }

    /**
     * Sets a mode to run the report in the background and screen.
     *
     * @param mode mode to run the report in the background
     * @param screen screen or screen fragment from which the report runs
     *
     * @return current instance of builder
     */
    public UiReportRunContextBuilder runInBackground(RunInBackgroundMode mode, FrameOwner screen) {
        this.runInBackgroundMode = mode;
        this.screen = screen;
        return this;
    }

    /**
     * Sets a mode to show a dialog to input the report parameter before report run.
     *
     * @param mode mode to show a dialog to input the report parameter before report run
     * @return current instance of builder
     */
    public UiReportRunContextBuilder showParametersDialogMode(ShowParametersDialogMode mode) {
        this.showParametersDialogMode = mode;
        return this;
    }

    /**
     * Sets a screen or screen fragment from which the report runs.
     *
     * @param screen screen or screen fragment from which the report runs
     * @return current instance of builder
     */
    public UiReportRunContextBuilder withScreen(FrameOwner screen) {
        this.screen = screen;
        return this;
    }

    /**
     * Creates an instance of {@link UiReportRunContext} based on the parameters specified for the builder.
     *
     * @return run context
     */
    public UiReportRunContext build() {
        return new UiReportRunContext()
                .setReportRunContext(this.reportRunContextBuilder.build())
                .setScreen(this.screen)
                .setRunInBackgroundMode(this.runInBackgroundMode)
                .setShowParametersDialogMode(this.showParametersDialogMode);
    }

    /**
     * Builds a {@link UiReportRunContext} instance, runs a report using this run context and shows the result.
     */
    public void runAndShow() {
        uiReportRunner.runAndShow(build());
    }
}
