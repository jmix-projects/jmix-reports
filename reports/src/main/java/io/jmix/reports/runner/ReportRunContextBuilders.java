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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Class is used for creating instances of {@link ReportRunContextBuilder}
 * <p>
 * Usage example:
 * <pre>
 * reportRunContextBuilders.createByReportCode("order-report")
 *  .withOutputType(ReportOutputType.PDF)
 *  .addParam("order", order)
 *  .build();
 * </pre>
 */
@Component("report_ReportRunContextBuilders")
public class ReportRunContextBuilders {

    @Autowired
    private ObjectProvider<ReportRunContextBuilder> reportRunContextObjectProvider;

    public ReportRunContextBuilder createByReportCode(String reportCode) {
        return reportRunContextObjectProvider.getObject(reportCode);
    }

    public ReportRunContextBuilder createByReportEntity(Report report) {
        return reportRunContextObjectProvider.getObject(report);
    }
}
