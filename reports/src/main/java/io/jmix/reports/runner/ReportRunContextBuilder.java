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

import com.google.common.base.Strings;
import io.jmix.core.DataManager;
import io.jmix.core.EntityStates;
import io.jmix.core.Id;
import io.jmix.reports.entity.Report;
import io.jmix.reports.entity.ReportOutputType;
import io.jmix.reports.entity.ReportTemplate;
import io.jmix.reports.exception.ReportingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Class is used for creating instances of {@link ReportRunContext}. It allows to create run contexts using various
 * additional criteria, e.g. run context may be created either by {@link Report} entity or by report code.
 * <p>
 * Use the {@link ReportRunContextBuilders} bean to obtain an instance of the {@link ReportRunContextBuilder}.
 */
@Component("report_ReportRunContextBuilder")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ReportRunContextBuilder {

    private static final String REPORT_RUN_FETCH_PLAN = "report.run";

    private Report report;
    private String reportCode;
    private Map<String, Object> params = new HashMap<>();
    private String templateCode;
    private ReportTemplate template;
    private ReportOutputType outputType;
    private String outputNamePattern;

    private static final Logger log = LoggerFactory.getLogger(ReportRunContextBuilder.class);

    @Autowired
    private DataManager dataManager;

    @Autowired
    private EntityStates entityStates;

    public ReportRunContextBuilder(Report report) {
        this.report = report;
    }

    public ReportRunContextBuilder(String reportCode) {
        this.reportCode = reportCode;
    }

    public ReportRunContextBuilder withParams(Map<String, Object> params) {
        this.params = params;
        return this;
    }

    public ReportRunContextBuilder addParam(String name, Object value) {
        params.put(name, value);
        return this;
    }

    public ReportRunContextBuilder withTemplateCode(String templateCode) {
        this.templateCode = templateCode;
        return this;
    }

    public ReportRunContextBuilder withTemplate(ReportTemplate template) {
        this.template = template;
        return this;
    }

    public ReportRunContextBuilder withOutputType(ReportOutputType outputType) {
        this.outputType = outputType;
        return this;
    }

    public ReportRunContextBuilder withOutputNamePattern(String outputNamePattern) {
        this.outputNamePattern = outputNamePattern;
        return this;
    }

    public ReportRunContext build() {
        Report report = getReportToUse();
        ReportTemplate reportTemplate = getReportTemplateToUse(report);
        return new ReportRunContext()
                .setReport(report)
                .setReportTemplate(reportTemplate)
                .setOutputNamePattern(this.outputNamePattern)
                .setOutputType(this.outputType)
                .setParams(this.params);
    }

    private Optional<Report> loadReportByCode(String reportCode) {
        return dataManager.load(Report.class)
                .query("e.code = :code")
                .parameter("code", reportCode)
                .fetchPlan(REPORT_RUN_FETCH_PLAN)
                .optional();
    }

    private Report getReportToUse() {
        if (this.report != null) {
            if (!entityStates.isLoadedWithFetchPlan(this.report, "report.run")) {
                return dataManager.load(Id.of(report))
                        .fetchPlan("report.run")
                        .one();
            } else {
                return this.report;
            }
        }
        if (!Strings.isNullOrEmpty(reportCode)) {
            Optional<Report> reportOpt = loadReportByCode(this.reportCode);
            if (reportOpt.isPresent()) {
                return reportOpt.get();
            }
        }
        log.error("Cannot evaluate report to run. report param: {}, reportCode param: {}", report, reportCode);
        throw new IllegalStateException("Cannot evaluate a report to run");
    }

    private ReportTemplate getReportTemplateToUse(Report report) {
        if (this.template != null) {
            return template;
        }
        if (!Strings.isNullOrEmpty(templateCode)) {
            ReportTemplate templateByCode = report.getTemplateByCode(templateCode);
            if (templateByCode == null) {
                throw new RuntimeException(String.format("Cannot find report template with code %s in report %s", templateCode, report.getCode()));
            }
            return templateByCode;
        }
        ReportTemplate defaultTemplate = report.getDefaultTemplate();
        if (defaultTemplate == null)
            throw new ReportingException(String.format("No default template specified for report [%s]", report.getName()));
        return defaultTemplate;
    }


}
