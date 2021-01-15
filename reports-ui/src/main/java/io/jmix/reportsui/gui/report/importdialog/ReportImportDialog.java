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

package io.jmix.reportsui.gui.report.importdialog;

import io.jmix.core.Messages;
import io.jmix.reports.app.service.ReportService;
import io.jmix.reports.entity.ReportImportOption;
import io.jmix.reports.entity.ReportImportResult;
import io.jmix.ui.Notifications;
import io.jmix.ui.component.*;
import io.jmix.ui.download.DownloadFormat;
import io.jmix.ui.screen.LookupComponent;
import io.jmix.ui.screen.*;
import io.jmix.ui.upload.TemporaryStorage;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.util.EnumSet;
import java.util.UUID;

@UiController("report_ReportImport.dialog")
@UiDescriptor("report-import-dialog.xml")
@LookupComponent("fileUpload")
public class ReportImportDialog extends StandardEditor {

    @Autowired
    protected FileStorageUploadField fileUpload;
    @Autowired
    protected Label<String> fileName;
    @Autowired
    protected CheckBox importRoles;
    @Autowired
    protected TemporaryStorage temporaryStorage;
    @Autowired
    protected ReportService reportService;
    @Autowired
    protected HBoxLayout dropZone;
    @Autowired
    protected Messages messages;
    @Autowired
    protected Notifications notifications;

    @Subscribe
    protected void onInit(InitEvent event) {
        fileUpload.addFileUploadSucceedListener(e -> {
            fileName.setValue(fileUpload.getFileName());
        });
        importRoles.setValue(Boolean.TRUE);

        dropZone.setVisible(false);
    }

    protected void importReport() {
        try {
            UUID fileID = fileUpload.getFileId();
            File file = temporaryStorage.getFile(fileID);
            byte[] bytes = FileUtils.readFileToByteArray(file);
            temporaryStorage.deleteFile(fileID);
            ReportImportResult result = reportService.importReportsWithResult(bytes, getImportOptions());

            notifications.create(Notifications.NotificationType.HUMANIZED)
                    .withCaption(messages.formatMessage(getClass(), "importResult", result.getCreatedReports().size(), result.getUpdatedReports().size()))
                    .show();
        } catch (Exception e) {
            notifications.create(Notifications.NotificationType.ERROR)
                    .withCaption(messages.getMessage(getClass(), "reportException.unableToImportReport"))
                    .withDescription(e.toString())
                    .show();
        }
    }

    protected EnumSet<ReportImportOption> getImportOptions() {
        if (BooleanUtils.isNotTrue(importRoles.getValue())) {
            return EnumSet.of(ReportImportOption.DO_NOT_IMPORT_ROLES);
        }
        return null;
    }

    @Override
    protected void validateAdditionalRules(ValidationErrors errors) {
        if (fileUpload.getFileId() == null) {
            errors.add(messages.getMessage(getClass(), "reportException.noFile"));
            return;
        }
        String extension = FilenameUtils.getExtension(fileUpload.getFileName());
        if (!StringUtils.equalsIgnoreCase(extension, DownloadFormat.ZIP.getFileExt())) {
            errors.add(messages.formatMessage(getClass(), "reportException.wrongFileType", extension));
        }

        super.validateAdditionalRules(errors);
    }
}
