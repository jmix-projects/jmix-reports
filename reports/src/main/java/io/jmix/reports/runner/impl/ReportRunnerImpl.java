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

package io.jmix.reports.runner.impl;

import com.haulmont.yarg.exception.OpenOfficeException;
import com.haulmont.yarg.exception.ReportingInterruptedException;
import com.haulmont.yarg.formatters.impl.doc.connector.NoFreePortsException;
import com.haulmont.yarg.reporting.ReportOutputDocument;
import com.haulmont.yarg.reporting.ReportingAPI;
import com.haulmont.yarg.reporting.RunParams;
import io.jmix.reports.PrototypesLoader;
import io.jmix.reports.app.ParameterPrototype;
import io.jmix.reports.entity.Report;
import io.jmix.reports.entity.ReportTemplate;
import io.jmix.reports.exception.*;
import io.jmix.reports.libintegration.CustomFormatter;
import io.jmix.reports.runner.ReportRunContext;
import io.jmix.reports.runner.ReportRunContextBuilder;
import io.jmix.reports.runner.ReportRunner;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.*;

@Component("report_ReportRunner")
public class ReportRunnerImpl implements ReportRunner {

    @Autowired
    protected PrototypesLoader prototypesLoader;

    @Autowired
    protected ReportingAPI reportingAPI;

    @Autowired
    private ObjectProvider<ReportRunContextBuilder> reportRunContextObjectProvider;

    @Override
    public ReportOutputDocument runReport(ReportRunContext context) {
        Report report = context.getReport();
        ReportTemplate template = context.getReportTemplate();
        io.jmix.reports.entity.ReportOutputType outputType = context.getOutputType();
        Map<String, Object> params = context.getParams();
        String outputNamePattern = context.getOutputNamePattern();

        StopWatch stopWatch = null;
        MDC.put("user", SecurityContextHolder.getContext().getAuthentication().getName());
        //TODO web context name
//        MDC.put("webContextName", globalConfig.getWebContextName());
        //TODO executions
//        executions.startExecution(report.getId().toString(), "Reporting");
        try {
            //TODO Slf4JStopWatch
//            stopWatch = new Slf4JStopWatch("Reporting#" + report.getName());
            List<String> prototypes = new LinkedList<>();
            for (Map.Entry<String, Object> param : params.entrySet()) {
                if (param.getValue() instanceof ParameterPrototype)
                    prototypes.add(param.getKey());
            }
            Map<String, Object> resultParams = new HashMap<>(params);

            for (String paramName : prototypes) {
                ParameterPrototype prototype = (ParameterPrototype) params.get(paramName);
                List data = loadDataForParameterPrototype(prototype);
                resultParams.put(paramName, data);
            }

            if (template.isCustom()) {
                CustomFormatter customFormatter = new CustomFormatter(report, template);
                template.setCustomReport(customFormatter);
            }

            com.haulmont.yarg.structure.ReportOutputType resultOutputType = (outputType != null) ? outputType.getOutputType() : template.getOutputType();

            return reportingAPI.runReport(new RunParams(report).template(template).params(resultParams).output(resultOutputType).outputNamePattern(outputNamePattern));
        } catch (NoFreePortsException nfe) {
            throw new NoOpenOfficeFreePortsException(nfe.getMessage());
        } catch (OpenOfficeException ooe) {
            throw new FailedToConnectToOpenOfficeException(ooe.getMessage());
        } catch (com.haulmont.yarg.exception.UnsupportedFormatException fe) {
            throw new UnsupportedFormatException(fe.getMessage());
        } catch (com.haulmont.yarg.exception.ValidationException ve) {
            throw new ValidationException(ve.getMessage());
        } catch (ReportingInterruptedException ie) {
            throw new ReportCanceledException(String.format("Report is canceled. %s", ie.getMessage()));
        } catch (com.haulmont.yarg.exception.ReportingException re) {
            Throwable rootCause = ExceptionUtils.getRootCause(re);
            //TODO cancelled exception
//            if (rootCause instanceof ResourceCanceledException) {
//                throw new ReportCanceledException(String.format("Report is canceled. %s", rootCause.getMessage()));
//            }
            //noinspection unchecked
            List<Throwable> list = ExceptionUtils.getThrowableList(re);
            StringBuilder sb = new StringBuilder();
            for (Iterator<Throwable> it = list.iterator(); it.hasNext(); ) {
                //noinspection ThrowableResultOfMethodCallIgnored
                sb.append(it.next().getMessage());
                if (it.hasNext())
                    sb.append("\n");
            }

            throw new ReportingException(sb.toString());
        } finally {
            //TODO executions
//            executions.endExecution();
            MDC.remove("user");
            MDC.remove("webContextName");
            if (stopWatch != null) {
                stopWatch.stop();
            }
        }
    }

    protected List loadDataForParameterPrototype(ParameterPrototype prototype) {
        return prototypesLoader.loadData(prototype);
    }

    @Override
    public ReportRunContextBuilder createRunContextBuilderByReportCode(String reportCode) {
        return reportRunContextObjectProvider.getObject(reportCode);
    }

    @Override
    public ReportRunContextBuilder createRunContextBuilderByReportEntity(Report report) {
        return reportRunContextObjectProvider.getObject(report);
    }
}
