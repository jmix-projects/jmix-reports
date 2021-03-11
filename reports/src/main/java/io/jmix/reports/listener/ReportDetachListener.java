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
package io.jmix.reports.listener;

import io.jmix.core.EntityStates;
import io.jmix.core.event.EntitySavingEvent;
import io.jmix.data.listener.BeforeDetachEntityListener;
import io.jmix.reports.Reports;
import io.jmix.reports.entity.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component("report_ReportDetachListener")
public class ReportDetachListener implements BeforeDetachEntityListener<Report> {

    @Autowired
    protected Reports reports;

    @Autowired
    protected EntityStates entityStates;

    @EventListener
    public void onReportSaving(EntitySavingEvent<Report> event) {
        Report report = event.getEntity();

        updateInputParamIdx(report);
        updateRoleIdx(report);
        updateScreenIdx(report);
    }

    private void updateInputParamIdx(Report report) {
        if (CollectionUtils.isNotEmpty(report.getInputParameters())) {
            report.setInputEntityTypesIdx(report.getInputParameters()
                    .stream()
                    .map(ReportInputParameter::getEntityMetaClass)
                    .collect(Collectors.joining(",")));
        } else {
            report.setInputEntityTypesIdx(null);
        }
    }

    private void updateRoleIdx(Report report) {
        if (CollectionUtils.isNotEmpty(report.getReportRoles())) {
            report.setRolesIdx(report.getReportRoles()
                    .stream()
                    .map(ReportRole::getRoleName)
                    .collect(Collectors.joining(",")));
        } else {
            report.setRolesIdx(null);
        }
    }

    private void updateScreenIdx(Report report) {
        if (CollectionUtils.isNotEmpty(report.getReportScreens())) {
            report.setScreensIdx(report.getReportScreens()
                    .stream()
                    .map(ReportScreen::getScreenId)
                    .collect(Collectors.joining(",")));
        } else {
            report.setScreensIdx(null);
        }
    }

    @Override
    public void onBeforeDetach(Report entity) {
        if (entityStates.isLoaded(entity, "xml") && StringUtils.isNotBlank(entity.getXml())) {
            Report reportFromXml = reports.convertToReport(entity.getXml());
            entity.setBands(reportFromXml.getBands());
            entity.setInputParameters(reportFromXml.getInputParameters());
            entity.setReportScreens(reportFromXml.getReportScreens());
            entity.setReportRoles(reportFromXml.getReportRoles());
            entity.setValuesFormats(reportFromXml.getValuesFormats());
            entity.setValidationOn(reportFromXml.getValidationOn());
            entity.setValidationScript(reportFromXml.getValidationScript());

            setRelevantReferencesToReport(entity);
            sortRootChildrenBands(entity);
        }
    }

    protected void sortRootChildrenBands(Report entity) {
        if (entity.getRootBandDefinition() != null
                && CollectionUtils.isNotEmpty(entity.getRootBandDefinition().getChildrenBandDefinitions())) {
            List<BandDefinition> bandDefinitions = new ArrayList<>(entity.getRootBandDefinition().getChildrenBandDefinitions());
            Collections.sort(bandDefinitions, Comparator.comparing(BandDefinition::getPosition));
            entity.getRootBandDefinition().setChildrenBandDefinitions(bandDefinitions);
        }
    }

    protected void setRelevantReferencesToReport(Report entity) {
        for (ReportValueFormat reportValueFormat : entity.getValuesFormats()) {
            reportValueFormat.setReport(entity);
        }

        for (BandDefinition bandDefinition : entity.getBands()) {
            bandDefinition.setReport(entity);
        }

        for (ReportInputParameter reportInputParameter : entity.getInputParameters()) {
            reportInputParameter.setReport(entity);
        }

        for (ReportScreen reportScreen : entity.getReportScreens()) {
            reportScreen.setReport(entity);
        }
    }
}