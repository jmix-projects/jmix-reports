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

package io.jmix.reports.runner;

import io.jmix.reports.entity.Report;
import io.jmix.reports.entity.ReportOutputType;
import io.jmix.reports.entity.ReportTemplate;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Class stores the information required for report running. The instance of the class may be created using the
 * constructor or using the {@link ReportRunContextBuilders} bean.
 *
 * @see ReportRunContextBuilder
 * @see ReportRunContextBuilders
 */
public class ReportRunContext {
    protected Report report;
    protected ReportTemplate reportTemplate;
    protected ReportOutputType outputType;
    protected Map<String, Object> params = new HashMap<>();
    protected String outputNamePattern;

    public Report getReport() {
        return report;
    }

    public ReportRunContext setReport(Report report) {
        this.report = report;
        return this;
    }

    @Nullable
    public ReportTemplate getReportTemplate() {
        return reportTemplate;
    }

    public ReportRunContext setReportTemplate(ReportTemplate reportTemplate) {
        this.reportTemplate = reportTemplate;
        return this;
    }

    @Nullable
    public ReportOutputType getOutputType() {
        return outputType;
    }

    public ReportRunContext setOutputType(ReportOutputType outputType) {
        this.outputType = outputType;
        return this;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public ReportRunContext setParams(Map<String, Object> params) {
        this.params = params;
        return this;
    }

    @Nullable
    public String getOutputNamePattern() {
        return outputNamePattern;
    }

    public ReportRunContext setOutputNamePattern(@Nullable String outputNamePattern) {
        this.outputNamePattern = outputNamePattern;
        return this;
    }

}
