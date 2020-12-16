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

package io.jmix.reportsui.gui.report.run;

import com.haulmont.yarg.reporting.ReportOutputDocument;
import io.jmix.core.Entity;
import io.jmix.core.MessageTools;
import io.jmix.core.Metadata;
import io.jmix.core.MetadataTools;
import io.jmix.core.common.util.ParamsMap;
import io.jmix.core.entity.KeyValueEntity;
import io.jmix.core.impl.StandardSerialization;
import io.jmix.core.metamodel.datatype.impl.EnumClass;
import io.jmix.core.metamodel.model.MetaProperty;
import io.jmix.core.metamodel.model.MetaPropertyPath;
import io.jmix.reports.entity.CubaTableData;
import io.jmix.reports.entity.Report;
import io.jmix.reports.entity.ReportOutputType;
import io.jmix.reports.entity.ReportTemplate;
import io.jmix.reportsui.gui.ReportGuiManager;
import io.jmix.ui.Fragments;
import io.jmix.ui.UiComponents;
import io.jmix.ui.WindowParam;
import io.jmix.ui.component.*;
import io.jmix.ui.component.data.table.ContainerGroupTableItems;
import io.jmix.ui.model.CollectionContainer;
import io.jmix.ui.screen.*;
import io.jmix.ui.theme.ThemeConstants;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

@UiController("report_ShowReportTable")
@UiDescriptor("show-report-table.xml")
public class ShowReportTable extends StandardLookup {
    public static final String REPORT_PARAMETER = "report";
    public static final String TEMPLATE_CODE_PARAMETER = "templateCode";
    public static final String PARAMS_PARAMETER = "reportParams";
    public static final String TABLE_DATA_PARAMETER = "tableData";

    @Autowired
    protected GroupBoxLayout reportParamsBox;
    @Autowired
    protected ReportGuiManager reportGuiManager;
    @Autowired
    protected ThemeConstants themeConstants;
    @Autowired
    protected UiComponents uiComponents;
    @Autowired
    protected Metadata metadata;
    @Autowired
    protected MetadataTools metadataTools;
    @Autowired
    protected MessageTools messageTools;

    @Autowired
    protected ComboBox<Report> reportLookup;
    @Autowired
    protected Button printReportBtn;
    @Autowired
    protected BoxLayout parametersFrameHolder;
    @Autowired
    protected HBoxLayout reportSelectorBox;

    @Autowired
    protected VBoxLayout tablesVBoxLayout;

    @Autowired
    protected StandardSerialization serialization;

    @Autowired
    protected Fragments fragments;

    @Autowired
    protected ScreenValidation screenValidation;

    @WindowParam(name = REPORT_PARAMETER)
    protected Report report;
    @WindowParam(name = TEMPLATE_CODE_PARAMETER)
    protected String templateCode;
    @WindowParam(name = PARAMS_PARAMETER)
    protected Map<String, Object> reportParameters;

    protected InputParametersFrame inputParametersFrame;
//    protected DsContextImpl dsContext;

    @Subscribe
    protected void onInit(InitEvent event) {
        //dsContext = new DsContextImpl(getDsContext().getDataSupplier());

        //TODO dialog options
//        getDialogOptions()
//                .setWidth(themeConstants.get("cuba.gui.report.ShowReportTable.width"))
//                .setHeight(themeConstants.get("cuba.gui.report.ShowReportTable.height"))
//                .setResizable(true);

        if (report != null) {
            reportSelectorBox.setVisible(false);
//            CubaTableData dto = (CubaTableData) serialization.deserialize((byte[]) params.get(TABLE_DATA_PARAMETER));
//            drawTables(dto);
            openReportParameters(reportParameters);
        }
        reportLookup.addValueChangeListener(e -> {
            report = (Report) e.getValue();
            openReportParameters(null);
        });
    }

    private void openReportParameters(Map<String, Object> reportParameters) {
        parametersFrameHolder.removeAll();

        if (report != null) {
            Map<String, Object> params = ParamsMap.of(
                    InputParametersFrame.REPORT_PARAMETER, report,
                    InputParametersFrame.PARAMETERS_PARAMETER, reportParameters
            );

            inputParametersFrame = (InputParametersFrame) fragments.create(this,
                    "report_inputParametersFrame",
                    new MapScreenOptions(params))
                    .init();

            parametersFrameHolder.add(inputParametersFrame.getFragment());
            reportParamsBox.setVisible(true);
        } else {
            reportParamsBox.setVisible(false);
        }
    }

    @Subscribe("printReportBtn")
    public void printReport() {
        if (inputParametersFrame != null && inputParametersFrame.getReport() != null) {
            ValidationErrors validationErrors = screenValidation.validateUiComponents(getWindow());
            if (validationErrors.isEmpty()) {
                Map<String, Object> parameters = inputParametersFrame.collectParameters();
                Report report = inputParametersFrame.getReport();
                if (templateCode == null || templateCode.isEmpty())
                    templateCode = findTableCode(report);
                ReportOutputDocument reportResult = reportGuiManager.getReportResult(report, parameters, templateCode);
                CubaTableData dto = (CubaTableData) serialization.deserialize(reportResult.getContent());
                drawTables(dto);
            } else {
                screenValidation.showValidationErrors(this, validationErrors);
            }
        }
    }

    protected String findTableCode(Report report) {
        for (ReportTemplate reportTemplate : report.getTemplates()) {
            if (ReportOutputType.TABLE.equals(reportTemplate.getReportOutputType()))
                return reportTemplate.getCode();
        }
        return null;
    }

    protected void drawTables(CubaTableData dto) {
        Map<String, List<KeyValueEntity>> data = dto.getData();
        Map<String, Set<CubaTableData.ColumnInfo>> headerMap = dto.getHeaders();
        tablesVBoxLayout.removeAll();

        if (data == null || data.isEmpty())
            return;

        data.forEach((dataSetName, keyValueEntities) -> {
            if (keyValueEntities != null && !keyValueEntities.isEmpty()) {
//                CollectionContainer dataSource = createDataSource(dataSetName, keyValueEntities, headerMap);
                Table table = createTable(dataSetName, null, headerMap);

                GroupBoxLayout groupBox = uiComponents.create(GroupBoxLayout.class);
                groupBox.setCaption(dataSetName);
                groupBox.add(table);
                groupBox.expand(table);

                tablesVBoxLayout.add(groupBox);
                tablesVBoxLayout.expand(groupBox);
            }
        });
    }

//    protected CollectionContainer createDataSource(String dataSetName, List<KeyValueEntity> keyValueEntities, Map<String, Set<CubaTableData.ColumnInfo>> headerMap) {
//        DsBuilder dsBuilder = DsBuilder.create(getDsContext())
//                .setId(dataSetName + "Ds")
//                .setDataSupplier(getDsContext().getDataSupplier());
//        ValueGroupDatasourceImpl ds = dsBuilder.buildValuesGroupDatasource();
//        ds.setRefreshMode(CollectionDatasource.RefreshMode.NEVER);
//
//        Set<CubaTableData.ColumnInfo> headers = headerMap.get(dataSetName);
//        for (CubaTableData.ColumnInfo header : headers) {
//            Class javaClass = header.getColumnClass();
//            if (Entity.class.isAssignableFrom(javaClass) ||
//                    EnumClass.class.isAssignableFrom(javaClass) ||
//                    Datatypes.get(javaClass) != null) {
//                ds.addProperty(header.getKey(), javaClass);
//            }
//        }
//
//        dsContext.register(ds);
//        keyValueEntities.forEach(ds::includeItem);
//        return ds;
//    }

    protected Table createTable(String dataSetName, CollectionContainer dataSource, Map<String, Set<CubaTableData.ColumnInfo>> headerMap) {
        Table table = uiComponents.create(GroupTable.class);
        table.setId(dataSetName + "Table");

        Set<CubaTableData.ColumnInfo> headers = headerMap.get(dataSetName);

        createColumns(dataSource, table, headers);
        table.setItems(new ContainerGroupTableItems(dataSource));
        table.setWidth("100%");
        table.setMultiSelect(true);
        table.setColumnControlVisible(false);
        table.setColumnReorderingAllowed(false);

        //TODO excel action
//        ExcelAction excelAction = ExcelAction.create(table);
//        excelAction.setFileName(dataSetName);
//        Button excelButton = componentsFactory.createComponent(Button.class);
//        excelButton.setAction(excelAction);

        ButtonsPanel buttonsPanel = uiComponents.create(ButtonsPanel.class);
        table.setButtonsPanel(buttonsPanel);
//        table.addAction(excelAction);
//        buttonsPanel.add(excelButton);
        return table;
    }

    protected void createColumns(CollectionContainer dataSource, Table table, Set<CubaTableData.ColumnInfo> headers) {
        Collection<MetaPropertyPath> paths = metadataTools.getPropertyPaths(dataSource.getEntityMetaClass());
        for (MetaPropertyPath metaPropertyPath : paths) {
            MetaProperty property = metaPropertyPath.getMetaProperty();
            if (!property.getRange().getCardinality().isMany() && !metadataTools.isSystem(property)) {
                Table.Column column = new Table.Column(metaPropertyPath);

                String propertyName = property.getName();

                CubaTableData.ColumnInfo columnInfo = getColumnInfo(propertyName, headers);
                column.setCaption(columnInfo.getCaption());
                column.setType(metaPropertyPath.getRangeJavaClass());

                Element element = DocumentHelper.createElement("column");
                column.setXmlDescriptor(element);
                if (columnInfo.getPosition() == null) {
                    table.addColumn(column);
                } else {
                    table.addColumn(column, columnInfo.getPosition());
                }
            }
        }
    }

    private CubaTableData.ColumnInfo getColumnInfo(String headerKey, Set<CubaTableData.ColumnInfo> headers) {
        return headers.stream()
                .filter(header -> headerKey.equals(header.getKey()))
                .findFirst()
                .orElse(null);
    }
}
