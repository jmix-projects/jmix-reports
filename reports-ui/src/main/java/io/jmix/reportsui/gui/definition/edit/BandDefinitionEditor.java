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
package io.jmix.reportsui.gui.definition.edit;

import io.jmix.core.*;
import io.jmix.core.common.util.ParamsMap;
import io.jmix.core.metamodel.model.MetaClass;
import io.jmix.core.security.EntityOp;
import io.jmix.reports.app.service.ReportService;
import io.jmix.reports.app.service.ReportWizardService;
import io.jmix.reports.entity.*;
import io.jmix.reports.util.DataSetFactory;
import io.jmix.reportsui.gui.ReportingClientConfig;
import io.jmix.reportsui.gui.definition.edit.crosstab.CrossTabTableDecorator;
import io.jmix.reportsui.gui.definition.edit.scripteditordialog.ScriptEditorDialog;
import io.jmix.security.constraint.PolicyStore;
import io.jmix.security.constraint.SecureOperations;
import io.jmix.ui.Actions;
import io.jmix.ui.Dialogs;
import io.jmix.ui.ScreenBuilders;
import io.jmix.ui.action.AbstractAction;
import io.jmix.ui.action.Action;
import io.jmix.ui.action.list.CreateAction;
import io.jmix.ui.action.list.RemoveAction;
import io.jmix.ui.component.*;
import io.jmix.ui.component.autocomplete.AutoCompleteSupport;
import io.jmix.ui.component.autocomplete.JpqlSuggestionFactory;
import io.jmix.ui.component.autocomplete.Suggester;
import io.jmix.ui.component.autocomplete.Suggestion;
import io.jmix.ui.model.CollectionContainer;
import io.jmix.ui.model.InstanceContainer;
import io.jmix.ui.screen.*;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Nullable;
import javax.inject.Named;
import java.util.*;

@UiController("report_BandDefinitionEditor.fragment")
@UiDescriptor("definition-edit.xml")
public class BandDefinitionEditor extends ScreenFragment implements Suggester {

    @Autowired
    protected InstanceContainer<BandDefinition> bandsDc;
    @Autowired
    protected CollectionContainer<DataSet> dataSetDc;
    @Autowired
    protected InstanceContainer<Report> reportDc;
    @Autowired
    protected CollectionContainer<ReportInputParameter> parametersDc;
    @Autowired
    protected Table<DataSet> dataSets;
    @Named("text")
    protected SourceCodeEditor dataSetScriptField;
    @Autowired
    protected SourceCodeEditor jsonGroovyCodeEditor;
    @Autowired
    protected BoxLayout textBox;
    @Autowired
    protected Label entitiesParamLabel;
    @Autowired
    protected Label entityParamLabel;
    @Autowired
    protected GridLayout commonEntityGrid;
    @Autowired
    protected ComboBox jsonSourceTypeField;
    @Autowired
    protected VBoxLayout jsonDataSetTypeVBox;
    @Autowired
    protected Label jsonPathQueryLabel;
    @Autowired
    protected VBoxLayout jsonSourceGroovyCodeVBox;
    @Autowired
    protected VBoxLayout jsonSourceURLVBox;
    @Autowired
    protected VBoxLayout jsonSourceParameterCodeVBox;
    @Autowired
    protected HBoxLayout textParamsBox;
    @Autowired
    protected Label viewNameLabel;
    @Autowired
    protected ComboBox orientation;
    @Autowired
    protected ComboBox parentBand;
    @Autowired
    protected TextField name;
    @Autowired
    protected ComboBox viewNameLookup;
    @Autowired
    protected ComboBox entitiesParamLookup;
    @Autowired
    protected ComboBox entityParamLookup;
    @Autowired
    protected ComboBox dataStore;
    @Autowired
    protected CheckBox processTemplate;
    @Autowired
    protected CheckBox useExistingViewCheckbox;
    @Autowired
    protected Button viewEditButton;
    @Autowired
    protected Label buttonEmptyElement;
    @Autowired
    protected Label checkboxEmptyElement;
    @Autowired
    protected Label spacer;
    @Autowired
    protected Metadata metadata;
    @Autowired
    protected ReportService reportService;
    @Autowired
    protected ReportWizardService reportWizardService;
    @Autowired
    protected BoxLayout editPane;
    @Autowired
    protected DataSetFactory dataSetFactory;
    @Autowired
    protected CrossTabTableDecorator tabOrientationTableDecorator;
    @Autowired
    protected FetchPlanRepository fetchPlanRepository;
    @Autowired
    protected SecureOperations secureOperations;
    @Autowired
    protected PolicyStore policyStore;
    @Autowired
    protected TextArea jsonPathQueryTextAreaField;
    @Autowired
    protected JpqlSuggestionFactory jpqlSuggestionFactory;
    @Autowired
    protected Stores stores;
    @Autowired
    protected ReportingClientConfig reportingClientConfig;
    @Autowired
    protected Messages messages;
    @Autowired
    protected Dialogs dialogs;
    @Autowired
    protected ScreenBuilders screenBuilders;
    @Autowired
    protected Actions actions;

    protected SourceCodeEditor.Mode dataSetScriptFieldMode = SourceCodeEditor.Mode.Text;

    @Subscribe("jsonSourceGroovyCodeLinkBtn")
    public void showJsonScriptEditorDialog() {
        ScriptEditorDialog editorDialog = (ScriptEditorDialog) screenBuilders.screen(this)
                .withScreenId("scriptEditorDialog")
                .withOpenMode(OpenMode.DIALOG)
                .withOptions(new MapScreenOptions(ParamsMap.of(
                        "caption", getScriptEditorDialogCaption(),
                        "scriptValue", jsonGroovyCodeEditor.getValue(),
                        "helpHandler", jsonGroovyCodeEditor.getContextHelpIconClickHandler()
                ))).build();

        editorDialog.addAfterCloseListener(actionId -> {
            if (Window.COMMIT_ACTION_ID.equals(((StandardCloseAction)actionId.getCloseAction()).getActionId())) {
                jsonGroovyCodeEditor.setValue(editorDialog.getValue());
            }
        });

        editorDialog.show();
    }

    protected String getScriptEditorDialogCaption() {
        ReportGroup group = reportDc.getItem().getGroup();
        String report = reportDc.getItem().getName();

        if (ObjectUtils.isNotEmpty(group) && ObjectUtils.isNotEmpty(report)) {
            return messages.formatMessage(getClass(), "scriptEditorDialog.captionFormat", report, bandsDc.getItem().getName());
        }
        return null;
    }

    @Subscribe("dataSetTextLinkBtn")
    public void showDataSetScriptEditorDialog() {
        ScriptEditorDialog editorDialog = (ScriptEditorDialog) screenBuilders.screen(this)
                .withScreenId("scriptEditorDialog")
                .withOpenMode(OpenMode.DIALOG)
                .withOptions(new MapScreenOptions(ParamsMap.of(
                        "caption", getScriptEditorDialogCaption(),
                        "mode", dataSetScriptFieldMode,
                        "suggester", dataSetScriptField.getSuggester(),
                        "scriptValue", dataSetScriptField.getValue(),
                        "helpHandler", dataSetScriptField.getContextHelpIconClickHandler()
                ))).build();

        editorDialog.addAfterCloseListener(actionId -> {
            if (Window.COMMIT_ACTION_ID.equals(((StandardCloseAction)actionId.getCloseAction()).getActionId())) {
                dataSetScriptField.setValue(editorDialog.getValue());
            }
        });

        editorDialog.show();
    }

    public void setBandDefinition(BandDefinition bandDefinition) {
        bandsDc.setItem(bandDefinition);
        name.setEditable((bandDefinition == null || bandDefinition.getParent() != null)
                && isUpdatePermitted());
    }

    public InstanceContainer<BandDefinition> getBandDefinitionDs() {
        return bandsDc;
    }


//    @Override
    public void setEnabled(boolean enabled) {
        //Desktop Component containers doesn't apply disable flags for child components
        for (Component component : getFragment().getComponents()) {
            component.setEnabled(enabled);
        }
    }

    @Override
    public List<Suggestion> getSuggestions(AutoCompleteSupport source, String text, int cursorPosition) {
        if (StringUtils.isBlank(text)) {
            return Collections.emptyList();
        }
        int queryPosition = cursorPosition - 1;

        return jpqlSuggestionFactory.requestHint(text, queryPosition, source, cursorPosition);
    }

    @Subscribe
    protected void onInit(InitEvent event) {
        initDataSetListeners();

        initBandDefinitionsListeners();

        initParametersListeners();

        initActions();

        initDataStoreField();

        initSourceCodeOptions();

        initHelpButtons();
    }

    protected void initHelpButtons() {
        jsonGroovyCodeEditor.setContextHelpIconClickHandler(e ->
                dialogs.createMessageDialog()
                        .withCaption(messages.getMessage("dataSet.text"))
                        .withMessage(messages.getMessage("dataSet.jsonSourceGroovyCodeHelp"))
                        .withModal(false)
                        .withWidth("700px")
                        .show());
        jsonPathQueryTextAreaField.setContextHelpIconClickHandler(e ->
                dialogs.createMessageDialog()
                        .withCaption(messages.getMessage("dataSet.text"))
                        .withMessage(messages.getMessage("dataSet.jsonPathQueryHelp"))
                        .withModal(false)
                        .withWidth("700px")
                        .show());
    }

    protected void initSourceCodeOptions() {
        boolean enableTabSymbolInDataSetEditor = reportingClientConfig.getEnableTabSymbolInDataSetEditor();
        jsonGroovyCodeEditor.setHandleTabKey(enableTabSymbolInDataSetEditor);
        dataSetScriptField.setHandleTabKey(enableTabSymbolInDataSetEditor);
    }

    protected void initJsonDataSetOptions(DataSet dataSet) {
        jsonDataSetTypeVBox.removeAll();
        jsonDataSetTypeVBox.add(jsonSourceTypeField);
        jsonDataSetTypeVBox.add(jsonPathQueryLabel);
        jsonDataSetTypeVBox.add(jsonPathQueryTextAreaField);

        if (dataSet.getJsonSourceType() == null) {
            dataSet.setJsonSourceType(JsonSourceType.GROOVY_SCRIPT);
        }

        switch (dataSet.getJsonSourceType()) {
            case GROOVY_SCRIPT:
                jsonDataSetTypeVBox.add(jsonSourceGroovyCodeVBox);
                jsonDataSetTypeVBox.expand(jsonSourceGroovyCodeVBox);
                break;
            case URL:
                jsonDataSetTypeVBox.add(jsonSourceURLVBox);
                jsonDataSetTypeVBox.expand(jsonSourceURLVBox);
                break;
            case PARAMETER:
                jsonDataSetTypeVBox.add(jsonSourceParameterCodeVBox);
                jsonDataSetTypeVBox.add(spacer);
                jsonDataSetTypeVBox.expand(spacer);
                break;
        }
    }

    protected void initDataStoreField() {
        Map<String, Object> all = new HashMap<>();
        all.put(messages.getMessage("dataSet.dataStoreMain"), Stores.MAIN);
        for (String additional : stores.getAdditional()) {
            all.put(additional, additional);
        }
        dataStore.setOptionsMap(all);
    }

    protected void initActions() {
        RemoveAction removeAction = (RemoveAction) actions.create(RemoveAction.class)
                .withCaption("")
                .withDescription(messages.getMessage("description.removeDataSet"));
        dataSets.addAction(removeAction);


        CreateAction createAction = (CreateAction) actions.create(CreateAction.class)
                .withCaption("")
                .withDescription(messages.getMessage("description.createDataSet"))
                .withHandler(handle -> {
                    BandDefinition selectedBand = bandsDc.getItem();
                    if (selectedBand != null) {
                        DataSet dataset = dataSetFactory.createEmptyDataSet(selectedBand);
                        selectedBand.getDataSets().add(dataset);
                        dataSetDc.getMutableItems().add(dataset);
                        dataSetDc.setItem(dataset);
                        dataSets.setSelected(dataset);
                    }
                });
        createAction.setEnabled(createAction.isEnabled() && isUpdatePermitted());
        dataSets.addAction(createAction);

        //todo
        Action editDataSetViewAction = new EditViewAction(this);
        viewEditButton.setAction(editDataSetViewAction);

        viewNameLookup.setOptionsMap(new HashMap<>());

        //todo
//        entitiesParamLookup.setNewOptionAllowed(true);
//        entityParamLookup.setNewOptionAllowed(true);
//        viewNameLookup.setNewOptionAllowed(true);
        entitiesParamLookup.setNewOptionHandler(LinkedWithPropertyNewOptionHandler.handler(dataSetDc, "listEntitiesParamName"));
        entityParamLookup.setNewOptionHandler(LinkedWithPropertyNewOptionHandler.handler(dataSetDc, "entityParamName"));
        viewNameLookup.setNewOptionHandler(LinkedWithPropertyNewOptionHandler.handler(dataSetDc, "viewName"));
    }

    protected void initParametersListeners() {
        parametersDc.addCollectionChangeListener(e -> {
            Map<String, Object> paramAliases = new HashMap<>();

            for (ReportInputParameter item : e.getSource().getItems()) {
                paramAliases.put(item.getName(), item.getAlias());
            }
            entitiesParamLookup.setOptionsMap(paramAliases);
            entityParamLookup.setOptionsMap(paramAliases);
        });
    }

    protected void initBandDefinitionsListeners() {
        bandsDc.addItemChangeListener(e -> {
            updateRequiredIndicators(e.getItem());
            selectFirstDataSet();
        });
        bandsDc.addItemPropertyChangeListener(e -> {
            if ("name".equals(e.getProperty()) && StringUtils.isBlank((String) e.getValue())) {
                e.getItem().setName("*");
            }
        });
    }

    protected void initDataSetListeners() {
        tabOrientationTableDecorator.decorate(dataSets, bandsDc);

        dataSetDc.addItemChangeListener(e -> {
            if (e.getItem() != null) {
                applyVisibilityRules(e.getItem());

                if (e.getItem().getType() == DataSetType.SINGLE) {
                    refreshViewNames(findParameterByAlias(e.getItem().getEntityParamName()));
                } else if (e.getItem().getType() == DataSetType.MULTI) {
                    refreshViewNames(findParameterByAlias(e.getItem().getListEntitiesParamName()));
                }

                dataSetScriptField.resetEditHistory();
            } else {
                hideAllDataSetEditComponents();
            }
        });

        dataSetDc.addItemPropertyChangeListener(e -> {
            applyVisibilityRules(e.getItem());
            if ("entityParamName".equals(e.getProperty()) || "listEntitiesParamName".equals(e.getProperty())) {
                ReportInputParameter linkedParameter = findParameterByAlias(String.valueOf(e.getValue()));
                refreshViewNames(linkedParameter);
            }

            if ("processTemplate".equals(e.getProperty()) && e.getItem() != null) {
                applyVisibilityRulesForType(e.getItem());
            }

//            @SuppressWarnings("unchecked")
//            DatasourceImplementation<DataSet> implementation = (DatasourceImplementation<DataSet>) dataSetDc;
//            implementation.modified(e.getItem());
        });

        dataSetScriptField.resetEditHistory();

        hideAllDataSetEditComponents();
    }

    protected boolean isUpdatePermitted() {
        return secureOperations.isEntityUpdatePermitted(metadata.getClass(Report.class), policyStore);
    }

    protected void updateRequiredIndicators(BandDefinition item) {
        boolean required = !(item == null || reportDc.getItem().getRootBandDefinition().equals(item));
        parentBand.setRequired(required);
        orientation.setRequired(required);
        name.setRequired(item != null);
    }

    @Nullable
    protected ReportInputParameter findParameterByAlias(String alias) {
        for (ReportInputParameter reportInputParameter : parametersDc.getItems()) {
            if (reportInputParameter.getAlias().equals(alias)) {
                return reportInputParameter;
            }
        }
        return null;
    }

    protected void refreshViewNames(@Nullable ReportInputParameter reportInputParameter) {
        if (reportInputParameter != null) {
            if (StringUtils.isNotBlank(reportInputParameter.getEntityMetaClass())) {
                MetaClass parameterMetaClass = metadata.getClass(reportInputParameter.getEntityMetaClass());
                Collection<String> viewNames = fetchPlanRepository.getFetchPlanNames(parameterMetaClass);
                Map<String, Object> views = new HashMap<>();
                for (String viewName : viewNames) {
                    views.put(viewName, viewName);
                }
                views.put(FetchPlan.LOCAL, FetchPlan.LOCAL);
                views.put(FetchPlan.INSTANCE_NAME, FetchPlan.INSTANCE_NAME);
                viewNameLookup.setOptionsMap(views);
                return;
            }
        }

        viewNameLookup.setOptionsMap(new HashMap<>());
    }

    protected void applyVisibilityRules(DataSet item) {
        applyVisibilityRulesForType(item);
        if (item.getType() == DataSetType.SINGLE || item.getType() == DataSetType.MULTI) {
            applyVisibilityRulesForEntityType(item);
        }
    }

    protected void applyVisibilityRulesForType(DataSet dataSet) {
        hideAllDataSetEditComponents();

        if (dataSet.getType() != null) {
            switch (dataSet.getType()) {
                case SQL:
                case JPQL:
                    textParamsBox.add(dataStore);
                    textBox.add(processTemplate);
                case GROOVY:
                    editPane.add(textBox);
                    break;
                case SINGLE:
                    editPane.add(commonEntityGrid);
                    setCommonEntityGridVisiblity(true, false);
                    editPane.add(spacer);
                    editPane.expand(spacer);
                    break;
                case MULTI:
                    editPane.add(commonEntityGrid);
                    setCommonEntityGridVisiblity(false, true);
                    editPane.add(spacer);
                    editPane.expand(spacer);
                    break;
                case JSON:
                    initJsonDataSetOptions(dataSet);
                    editPane.add(jsonDataSetTypeVBox);
                    break;
            }

            switch (dataSet.getType()) {
                case SQL:
                    dataSetScriptFieldMode = SourceCodeEditor.Mode.SQL;
                    dataSetScriptField.setMode(SourceCodeEditor.Mode.SQL);
                    dataSetScriptField.setSuggester(null);
                    dataSetScriptField.setContextHelpIconClickHandler(null);
                    break;

                case GROOVY:
                    dataSetScriptFieldMode = SourceCodeEditor.Mode.Groovy;
                    dataSetScriptField.setSuggester(null);
                    dataSetScriptField.setMode(SourceCodeEditor.Mode.Groovy);
                    dataSetScriptField.setContextHelpIconClickHandler(e ->
                            dialogs.createMessageDialog()
                                    .withCaption(messages.getMessage("dataSet.text"))
                                    .withMessage(messages.getMessage("dataSet.textHelp"))
                                    .withModal(false)
                                    .withWidth("700px")
                                    .show());
                    break;

                case JPQL:
                    dataSetScriptFieldMode = SourceCodeEditor.Mode.Text;
                    dataSetScriptField.setSuggester(processTemplate.isChecked() ? null : this);
                    dataSetScriptField.setMode(SourceCodeEditor.Mode.Text);
                    dataSetScriptField.setContextHelpIconClickHandler(null);
                    break;

                default:
                    dataSetScriptFieldMode = SourceCodeEditor.Mode.Text;
                    dataSetScriptField.setSuggester(null);
                    dataSetScriptField.setMode(SourceCodeEditor.Mode.Text);
                    dataSetScriptField.setContextHelpIconClickHandler(null);
                    break;
            }
        }
    }

    protected void applyVisibilityRulesForEntityType(DataSet item) {
        commonEntityGrid.remove(viewNameLabel);
        commonEntityGrid.remove(viewNameLookup);
        commonEntityGrid.remove(viewEditButton);
        commonEntityGrid.remove(buttonEmptyElement);
        commonEntityGrid.remove(useExistingViewCheckbox);
        commonEntityGrid.remove(checkboxEmptyElement);

        if (Boolean.TRUE.equals(item.getUseExistingView())) {
            commonEntityGrid.add(viewNameLabel);
            commonEntityGrid.add(viewNameLookup);
        } else {
            commonEntityGrid.add(viewEditButton);
            commonEntityGrid.add(buttonEmptyElement);
        }

        commonEntityGrid.add(useExistingViewCheckbox);
        commonEntityGrid.add(checkboxEmptyElement);
    }

    protected void hideAllDataSetEditComponents() {
        // do not use setVisible(false) due to web legacy (Vaadin 6) layout problems #PL-3916
        textParamsBox.remove(dataStore);
        textBox.remove(processTemplate);
        editPane.remove(textBox);
        editPane.remove(commonEntityGrid);
        editPane.remove(jsonDataSetTypeVBox);
        editPane.remove(spacer);
    }

    protected void selectFirstDataSet() {
//        dataSetDc.refresh();
        if (!dataSetDc.getItems().isEmpty()) {
            DataSet item = dataSetDc.getItems().iterator().next();
            dataSets.setSelected(item);
        } else {
            dataSets.setSelected((DataSet) null);
        }
    }

    // This is a stub for using set in some DataSet change listener
    protected void setViewEditVisibility(DataSet dataSet) {
        if (isViewEditAllowed(dataSet)) {
            viewEditButton.setVisible(true);
        } else {
            viewEditButton.setVisible(false);
        }
    }

    protected boolean isViewEditAllowed(DataSet dataSet) {
        return true;
    }

    protected void setCommonEntityGridVisiblity(boolean visibleEntityGrid, boolean visibleEntitiesGrid) {
        entityParamLabel.setVisible(visibleEntityGrid);
        entityParamLookup.setVisible(visibleEntityGrid);
        entitiesParamLabel.setVisible(visibleEntitiesGrid);
        entitiesParamLookup.setVisible(visibleEntitiesGrid);
    }
}