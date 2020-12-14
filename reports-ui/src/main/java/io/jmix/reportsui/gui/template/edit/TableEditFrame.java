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

package io.jmix.reportsui.gui.template.edit;

import io.jmix.core.Messages;
import io.jmix.core.Metadata;
import io.jmix.core.Sort;
import io.jmix.ui.Notifications;
import io.jmix.ui.action.list.RemoveAction;
import io.jmix.ui.component.Table;
import io.jmix.core.metamodel.model.MetaClass;
import io.jmix.core.metamodel.model.MetaPropertyPath;
import io.jmix.reports.entity.ReportOutputType;
import io.jmix.reports.entity.ReportTemplate;
import io.jmix.reports.entity.table.TemplateTableBand;
import io.jmix.reports.entity.table.TemplateTableColumn;
import io.jmix.reports.entity.table.TemplateTableDescription;
import io.jmix.ui.action.AbstractAction;
import io.jmix.ui.action.ListAction;
import io.jmix.ui.component.BoxLayout;
import io.jmix.ui.component.Component;
import io.jmix.ui.model.CollectionContainer;
import io.jmix.ui.model.InstanceContainer;
import io.jmix.ui.screen.Subscribe;
import io.jmix.ui.screen.UiController;
import io.jmix.ui.screen.UiDescriptor;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@UiController("report_TableEdit.fragment")
@UiDescriptor("table-edit-frame.xml")
public class TableEditFrame extends DescriptionEditFrame {

    public static final int UP = 1;
    public static final int DOWN = -1;
    public static final String POSITION = "position";

    protected ReportTemplate reportTemplate;
    protected TemplateTableDescription description;

    @Autowired
    protected Metadata metadata;

    @Autowired
    protected Table<TemplateTableBand> bandsTable;

    @Autowired
    protected Table<TemplateTableColumn> columnsTable;

    @Autowired
    protected CollectionContainer<TemplateTableBand> tableBandsDc;

    @Autowired
    protected InstanceContainer<TemplateTableDescription> templateTableDc;

    @Autowired
    protected CollectionContainer<TemplateTableColumn> tableColumnsDc;

    @Autowired
    protected Messages messages;

    @Autowired
    protected Notifications notifications;

    public ReportTemplate getReportTemplate() {
        return reportTemplate;
    }

    @Subscribe
    protected void onInit(InitEvent event) {
        initButtonBandTable();
        initButtonColumnTable();
    }

    protected void sortParametersByPosition(CollectionContainer collectionContainer) {
        collectionContainer.getSorter().sort(Sort.by(Sort.Direction.ASC, POSITION));
    }

    private void initButtonBandTable() {
        bandsTable.addAction(new AbstractAction("create") {
            @Override
            public String getCaption() {
                return "";
            }

            @Override
            public void actionPerform(Component component) {
                TemplateTableBand templateTableBand = metadata.create(TemplateTableBand.class);
                templateTableBand.setPosition(tableBandsDc.getItems().size());

                tableBandsDc.getItems().add(templateTableBand);
                tableBandsDc.commit();
            }
        });

        bandsTable.addAction(new RemoveAction(bandsTable, false, "remove") {
            @Override
            public String getCaption() {
                return "";
            }

            @Override
            protected void afterRemove(Set selected) {
                TemplateTableBand deletedColumn = (TemplateTableBand) selected.iterator().next();
                int deletedPosition = deletedColumn.getPosition();

                for (TemplateTableBand templateTableBand : tableBandsDc.getItems()) {
                    if (templateTableBand.getPosition() > deletedPosition) {
                        int currentPosition = templateTableBand.getPosition();
                        templateTableBand.setPosition(currentPosition - 1);
                    }
                }
            }
        });

        bandsTable.addAction(new ListAction("up") {
            @Override
            public void actionPerform(Component component) {
                changeOrderBandsOfIndexes(UP);
            }

            @Override
            protected boolean isApplicable() {
                TemplateTableBand item = bandsTable.getSingleSelected();
                if (item == null) {
                    return false;
                }
                return item.getPosition() > 0;
            }
        });

        bandsTable.addAction(new ListAction("down") {
            @Override
            public void actionPerform(Component component) {
                changeOrderBandsOfIndexes(DOWN);
            }

            @Override
            protected boolean isApplicable() {
                TemplateTableBand item = bandsTable.getSingleSelected();
                if (item == null) {
                    return false;
                }
                return item.getPosition() < (tableBandsDc.getItems().size() - 1) && super.isApplicable();
            }
        });
    }


    private void initButtonColumnTable() {
        columnsTable.addAction(new AbstractAction("create") {
            @Override
            public String getCaption() {
                return "";
            }

            @Override
            public void actionPerform(Component component) {
                TemplateTableBand selectBand = bandsTable.getSingleSelected();

                if (selectBand != null) {
                    TemplateTableColumn item = metadata.create(TemplateTableColumn.class);
                    item.setPosition(tableColumnsDc.getItems().size());

                    tableColumnsDc.getItems().add(item);
                    tableColumnsDc.commit();
                } else {
                    notifications.create(Notifications.NotificationType.HUMANIZED)
                            .withCaption(messages.getMessage("template.bandRequired"))
                            .show();
                }
            }
        });

        columnsTable.addAction(new RemoveAction(columnsTable, false, "remove") {
            @Override
            public String getCaption() {
                return "";
            }

            @Override
            protected void afterRemove(Set selected) {
                TemplateTableColumn deletedColumn = (TemplateTableColumn) selected.iterator().next();
                int deletedPosition = deletedColumn.getPosition();

                for (TemplateTableColumn templateTableColumn : tableColumnsDc.getItems()) {
                    if (templateTableColumn.getPosition() > deletedPosition) {
                        int currentPosition = templateTableColumn.getPosition();
                        templateTableColumn.setPosition(currentPosition - 1);
                    }
                }
            }
        });

        columnsTable.addAction(new ListAction("up") {
            @Override
            public void actionPerform(Component component) {
                changeOrderColumnsOfIndexes(UP);
            }

            @Override
            protected boolean isApplicable() {
                TemplateTableColumn item = columnsTable.getSingleSelected();
                if (item == null) {
                    return false;
                }
                return item.getPosition() > 0 && super.isApplicable();
            }
        });

        columnsTable.addAction(new ListAction("down") {
            @Override
            public void actionPerform(Component component) {
                changeOrderColumnsOfIndexes(DOWN);
            }

            @Override
            protected boolean isApplicable() {
                TemplateTableColumn item = columnsTable.getSingleSelected();
                if (item == null) {
                    return false;
                }
                return item.getPosition() < (tableColumnsDc.getItems().size() - 1) && super.isApplicable();
            }
        });
    }


    private void changeOrderColumnsOfIndexes(int order) {
        TemplateTableColumn currentColumn = columnsTable.getSingleSelected();
        int currentPosition = currentColumn.getPosition();

        for (TemplateTableColumn templateTableColumn : tableColumnsDc.getItems()) {
            if (templateTableColumn.getPosition() == (currentPosition - order)) {
                templateTableColumn.setPosition(templateTableColumn.getPosition() + order);
            }
            currentColumn.setPosition(currentPosition - order);
        }
        sortParametersByPosition(tableColumnsDc);
    }

    private void changeOrderBandsOfIndexes(int order) {
        TemplateTableBand currentBand = bandsTable.getSingleSelected();
        int currentPosition = currentBand.getPosition();

        for (TemplateTableBand templateTableBand : tableBandsDc.getItems()) {
            if (templateTableBand.getPosition() == (currentPosition - order)) {
                templateTableBand.setPosition(templateTableBand.getPosition() + order);
            }
            currentBand.setPosition(currentPosition - order);
        }
        sortParametersByPosition(tableBandsDc);
    }


    protected TemplateTableDescription createDefaultTemplateTableDescription() {
        description = metadata.create(TemplateTableDescription.class);
        return description;
    }


    public void setItem(ReportTemplate reportTemplate) {
        this.reportTemplate = reportTemplate;

        if (reportTemplate.getTemplateTableDescription() == null) {
            templateTableDc.setItem(createDefaultTemplateTableDescription());
        } else {
            templateTableDc.setItem(reportTemplate.getTemplateTableDescription());
        }

    }

    @Override
    public boolean applyChanges() {
        reportTemplate.setTemplateTableDescription(templateTableDc.getItem());

        for (TemplateTableBand band : tableBandsDc.getItems()) {
            if (band.getBandName() == null) {
                notifications.create(Notifications.NotificationType.TRAY)
                        .withCaption(messages.getMessage("template.bandTableOrColumnTableRequired"))
                        .show();
                return false;
            }

            for (TemplateTableColumn column : band.getColumns()) {
                if (column.getKey() == null || column.getCaption() == null) {
                    notifications.create(Notifications.NotificationType.TRAY)
                            .withCaption(messages.getMessage("template.bandTableOrColumnTableRequired"))
                            .show();
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean isApplicable(ReportOutputType reportOutputType) {
        return reportOutputType == ReportOutputType.TABLE;
    }

    @Override
    protected void initPreviewContent(BoxLayout previewBox) {
    }

    @Override
    public boolean isSupportPreview() {
        return false;
    }
}
