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

import com.haulmont.yarg.reporting.ReportOutputDocument;
import io.jmix.reports.entity.Report;

/**
 * Interface is used for running reports
 */
public interface ReportRunner {

    /**
     * Runs the report based on the information from the {@link ReportRunContext}. The run context may be created
     * manually using the constructor or using the {@link ReportRunContextBuilder} builder.
     *
     * @param context the object that contains all information required to run the report
     * @return report execution result
     */
    ReportOutputDocument runReport(ReportRunContext context);

    ReportRunContextBuilder createRunContextBuilderByReportCode(String reportCode);

    ReportRunContextBuilder createRunContextBuilderByReportEntity(Report report);
}
