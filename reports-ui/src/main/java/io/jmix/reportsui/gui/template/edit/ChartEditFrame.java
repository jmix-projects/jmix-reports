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

import io.jmix.core.DataManager;
import io.jmix.core.Messages;
import io.jmix.core.Sort;
import io.jmix.core.common.util.ParamsMap;
import io.jmix.ui.Actions;
import io.jmix.ui.Dialogs;
import io.jmix.ui.Fragments;
import io.jmix.ui.Notifications;
import io.jmix.ui.action.list.CreateAction;
import io.jmix.ui.component.GroupBoxLayout;
import io.jmix.ui.component.SourceCodeEditor;
import io.jmix.ui.component.Table;
import io.jmix.ui.component.*;
import io.jmix.reports.entity.BandDefinition;
import io.jmix.reports.entity.ReportOutputType;
import io.jmix.reports.entity.ReportTemplate;
import io.jmix.reports.entity.charts.*;
import io.jmix.reportsui.gui.report.run.ShowChartLookup;
import io.jmix.reportsui.gui.template.edit.generator.RandomChartDataGenerator;
import io.jmix.ui.action.ItemTrackingAction;
import io.jmix.ui.component.validation.Validator;
import io.jmix.ui.model.CollectionContainer;
import io.jmix.ui.model.InstanceContainer;
import io.jmix.ui.screen.MapScreenOptions;
import io.jmix.ui.screen.Subscribe;
import io.jmix.ui.screen.UiController;
import io.jmix.ui.screen.UiDescriptor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

@UiController("report_ChartEdit.fragment")
@UiDescriptor("chart-edit-frame.xml")
public class ChartEditFrame extends DescriptionEditFrame {
    @Autowired
    protected InstanceContainer<PieChartDescription> pieChartDc;
    @Autowired
    protected InstanceContainer<SerialChartDescription> serialChartDc;
    //todo sort
    @Autowired
    protected CollectionContainer<ChartSeries> seriesDc;
    @Autowired
    protected ComboBox<ChartType> type;
    @Autowired
    protected Table<ChartSeries> seriesTable;
    @Autowired
    protected GroupBoxLayout seriesBox;
    @Autowired
    protected Form pieChartFieldGroup;
    @Autowired
    protected Form serialChartFieldGroup;
    @Autowired
    protected SourceCodeEditor serialJsonConfigEditor;
    @Autowired
    protected SourceCodeEditor pieJsonConfigEditor;
    @Autowired
    protected BeanFactory beanFactory;
    @Autowired
    protected Messages messages;
    @Autowired
    protected Notifications notifications;
    @Autowired
    protected Dialogs dialogs;
    @Autowired
    protected Actions actions;
    @Autowired
    protected DataManager dataManager;
    @Autowired
    protected Fragments fragments;

    @Subscribe
    @SuppressWarnings("IncorrectCreateEntity")
    protected void onInit(InitEvent event) {
        super.onInit(event);

        pieChartDc.setItem(new PieChartDescription());
        serialChartDc.setItem(new SerialChartDescription());
        type.setOptionsList(Arrays.asList(ChartType.values()));

        type.addValueChangeListener(e -> {
            pieChartFieldGroup.setVisible(ChartType.PIE == e.getValue());
            serialChartFieldGroup.setVisible(ChartType.SERIAL == e.getValue());
            seriesBox.setVisible(ChartType.SERIAL == e.getValue());
            serialJsonConfigEditor.setVisible(ChartType.SERIAL == e.getValue());
            pieJsonConfigEditor.setVisible(ChartType.PIE == e.getValue());
            showPreview();
        });

        pieChartFieldGroup.setVisible(false);
        serialChartFieldGroup.setVisible(false);
        seriesBox.setVisible(false);
        serialJsonConfigEditor.setVisible(false);
        pieJsonConfigEditor.setVisible(false);

        CreateAction createAction = (CreateAction) actions.create(CreateAction.ID);
        createAction.withHandler(handle -> {
            ChartSeries chartSeries = dataManager.create(ChartSeries.class);
            chartSeries.setOrder(seriesDc.getItems().size() + 1);
            seriesDc.getItems().add(chartSeries);
        });
        seriesTable.addAction(createAction);

        seriesTable.addAction(new ChartSeriesMoveAction(true));
        seriesTable.addAction(new ChartSeriesMoveAction(false));

        pieChartDc.addItemPropertyChangeListener(e -> showPreview());

        serialChartDc.addItemPropertyChangeListener(e -> showPreview());

        seriesDc.addItemPropertyChangeListener(e -> showPreview());
        seriesDc.addCollectionChangeListener(e -> {
            checkSeriesOrder();
            showPreview();
        });

        serialJsonConfigEditor.addValueChangeListener(this::codeEditorChangeListener);
        pieJsonConfigEditor.addValueChangeListener(this::codeEditorChangeListener);

//        Validator<String> validator = beanFactory.getBean(JsonConfigValidator.class, this);
//        serialJsonConfigEditor.addValidator(validator);
//        pieJsonConfigEditor.addValidator(validator);

        serialJsonConfigEditor.setContextHelpIconClickHandler(this::jsonEditorContextHelpIconClickHandler);
        pieJsonConfigEditor.setContextHelpIconClickHandler(this::jsonEditorContextHelpIconClickHandler);
    }

    protected void codeEditorChangeListener(HasValue.ValueChangeEvent<String> event) {
        if (ChartType.SERIAL == type.getValue() && serialJsonConfigEditor.isValid()) {
            serialChartDc.getItem().setCustomJsonConfig(event.getValue());
        } else if (ChartType.PIE == type.getValue() && pieJsonConfigEditor.isValid()) {
            pieChartDc.getItem().setCustomJsonConfig(event.getValue());
        }
    }

    protected void jsonEditorContextHelpIconClickHandler(HasContextHelp.ContextHelpIconClickEvent event) {
        dialogs.createMessageDialog()
                .withCaption(messages.getMessage("chartEdit.jsonConfig"))
                .withMessage(event.getSource().getContextHelpText())
                .show();
    }

    @Override
    public void setItem(ReportTemplate reportTemplate) {
        super.setItem(reportTemplate);
        setBands(reportTemplate.getReport().getBands());
        if (isApplicable(reportTemplate.getReportOutputType())) {
            setChartDescription(reportTemplate.getChartDescription());
            sortSeriesByOrder();
        }
    }

    @Override
    public boolean applyChanges() {
        if (validateChart()) {
            AbstractChartDescription chartDescription = getChartDescription();
            getReportTemplate().setChartDescription(chartDescription);
            return true;
        }
        return false;
    }

    @Override
    public boolean isApplicable(ReportOutputType reportOutputType) {
        return reportOutputType == ReportOutputType.CHART;
    }

    @Override
    public boolean isSupportPreview() {
        return true;
    }

    protected boolean validateChart() {
        AbstractChartDescription chartDescription = getChartDescription();
        if (chartDescription != null && chartDescription.getType() == ChartType.SERIAL) {
            List<ChartSeries> series = ((SerialChartDescription) chartDescription).getSeries();
            if (series == null || series.size() == 0) {
                notifications.create(Notifications.NotificationType.TRAY)
                        .withCaption(messages.getMessage("validationFail.caption"))
                        .withDescription(messages.getMessage("chartEdit.seriesEmptyMsg"))
                        .show();
                return false;
            }
            for (ChartSeries it : series) {
                if (it.getType() == null) {
                    notifications.create(Notifications.NotificationType.TRAY)
                            .withCaption(messages.getMessage("validationFail.caption"))
                            .withDescription(messages.getMessage("chartEdit.seriesTypeNullMsg"))
                            .show();
                    return false;
                }
                if (it.getValueField() == null) {
                    notifications.create(Notifications.NotificationType.TRAY)
                            .withCaption(messages.getMessage("validationFail.caption"))
                            .withDescription(messages.getMessage("chartEdit.seriesValueFieldNullMsg"))
                            .show();
                    return false;
                }
            }
        }
        return true;
    }

    protected void initPreviewContent(BoxLayout previewBox) {
        List<Map<String, Object>> data;
        String chartJson = null;
        if (ChartType.SERIAL == type.getValue()) {
            SerialChartDescription chartDescription = serialChartDc.getItem();
            data = new RandomChartDataGenerator().generateRandomChartData(chartDescription);
            ChartToJsonConverter chartToJsonConverter = beanFactory.getBean(ChartToJsonConverter.class);
            chartJson = chartToJsonConverter.convertSerialChart(chartDescription, data);
        } else if (ChartType.PIE == type.getValue()) {
            PieChartDescription chartDescription = pieChartDc.getItem();
            data = new RandomChartDataGenerator().generateRandomChartData(chartDescription);
            ChartToJsonConverter chartToJsonConverter = beanFactory.getBean(ChartToJsonConverter.class);
            chartJson = chartToJsonConverter.convertPieChart(chartDescription, data);
        }
        chartJson = chartJson == null ? "{}" : chartJson;

        Map<String, Object> parmas = ParamsMap.of(ShowChartLookup.CHART_JSON_PARAMETER, chartJson);

        Fragment fragment = fragments.create(this, ShowChartLookup.JSON_CHART_SCREEN_ID, new MapScreenOptions(parmas))
                .init()
                .getFragment();

        if (ChartType.SERIAL == type.getValue()) {
            fragment.setHeight("700px");
        } else if (ChartType.PIE == type.getValue()) {
            fragment.setHeight("350px");
        }
        previewBox.add(fragment);
    }

    @Nullable
    protected AbstractChartDescription getChartDescription() {
        if (ChartType.SERIAL == type.getValue()) {
            return serialChartDc.getItem();
        } else if (ChartType.PIE == type.getValue()) {
            return pieChartDc.getItem();
        }
        return null;
    }

    protected void setChartDescription(@Nullable AbstractChartDescription chartDescription) {
        if (chartDescription != null) {
            if (ChartType.SERIAL == chartDescription.getType()) {
                serialChartDc.setItem((SerialChartDescription) chartDescription);
                serialJsonConfigEditor.setValue(chartDescription.getCustomJsonConfig());
            } else if (ChartType.PIE == chartDescription.getType()) {
                pieChartDc.setItem((PieChartDescription) chartDescription);
                pieJsonConfigEditor.setValue(chartDescription.getCustomJsonConfig());
            }
            type.setValue(chartDescription.getType());
        }
    }

    protected void setBands(Collection<BandDefinition> bands) {
        List<String> bandNames = bands.stream()
                .filter(bandDefinition -> bandDefinition.getParentBandDefinition() != null)
                .map(BandDefinition::getName)
                .collect(Collectors.toList());
            // todo
//        ComboBox pieChartBandName = (ComboBox) pieChartFieldGroup.getComponentNN("pieBandName");
//        ComboBox serialChartBandName = (ComboBox) serialChartFieldGroup.getComponentNN("serialBandName");
//
//        pieChartBandName.setOptionsList(bandNames);
//        serialChartBandName.setOptionsList(bandNames);
    }

    protected void checkSeriesOrder() {
        Collection<ChartSeries> items = seriesDc.getItems();
        int i = 1;
        for (ChartSeries item : items) {
            if (!Objects.equals(i, item.getOrder())) {
                item.setOrder(i);
            }
            i += 1;
        }
    }

    protected class ChartSeriesMoveAction extends ItemTrackingAction {
        private final boolean up;

        ChartSeriesMoveAction(boolean up) {
            super(seriesTable, up ? "up" : "down");
            setCaption(messages.getMessage(up ? "generalFrame.up" : "generalFrame.down"));
            this.up = up;
        }

        @Override
        public void actionPerform(Component component) {
            ChartSeries selected = seriesTable.getSingleSelected();
            //noinspection ConstantConditions
            Integer currentOrder = selected.getOrder();
            Integer newOrder = up ? currentOrder - 1 : currentOrder + 1;

            Collection<ChartSeries> items = seriesDc.getItems();

            ChartSeries changing = IterableUtils.get(items, currentOrder - 1);
            ChartSeries neighbor = IterableUtils.get(items, newOrder - 1);
            changing.setOrder(newOrder);
            neighbor.setOrder(currentOrder);

            sortSeriesByOrder();
        }

        @Override
        public boolean isPermitted() {
            if (super.isPermitted()) {
                Set<ChartSeries> items = seriesTable.getSelected();
                if (!CollectionUtils.isEmpty(items) && items.size() == 1) {
                    Integer order = (IterableUtils.get(items, 0)).getOrder();
                    if (order != null) {
                        return up ? order > 1 : order < seriesDc.getItems().size();
                    }
                }
            }
            return false;
        }
    }

    protected void sortSeriesByOrder() {
        seriesDc.getSorter().sort(Sort.by(Sort.Direction.ASC, "order"));
    }
}
