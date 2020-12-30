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
package io.jmix.reportsui.gui.parameter.edit;

import io.jmix.core.MessageTools;
import io.jmix.core.Messages;
import io.jmix.core.Metadata;
import io.jmix.core.MetadataTools;
import io.jmix.core.metamodel.model.MetaClass;
import io.jmix.reports.ParameterClassResolver;
import io.jmix.reports.app.service.ReportService;
import io.jmix.reports.entity.ParameterType;
import io.jmix.reports.entity.PredefinedTransformation;
import io.jmix.reports.entity.ReportInputParameter;
import io.jmix.reportsui.gui.report.run.ParameterFieldCreator;
import io.jmix.security.constraint.PolicyStore;
import io.jmix.security.constraint.SecureOperations;
import io.jmix.ui.Dialogs;
import io.jmix.ui.component.*;
import io.jmix.ui.component.autocomplete.JpqlSuggestionFactory;
import io.jmix.ui.component.autocomplete.Suggestion;
import io.jmix.ui.model.InstanceContainer;
import io.jmix.ui.screen.*;
import io.jmix.ui.sys.ScreensHelper;
import io.jmix.ui.theme.ThemeConstants;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import javax.inject.Named;
import java.util.*;

@UiController("report_ReportInputParameter.edit")
@UiDescriptor("parameter-edit.xml")
@EditedEntityContainer("parameterDc")
public class ParameterEditor extends StandardEditor<ReportInputParameter> {
    protected final static String LOOKUP_SETTINGS_TAB_ID = "lookupSettingsTab";
    protected final static String WHERE = " where ";

    @Autowired
    protected Label<String> defaultValueLabel;
    @Autowired
    protected BoxLayout defaultValueBox;

    @Autowired
    protected ComboBox<String> screen;
    @Autowired
    protected ComboBox<String> enumeration;
    @Autowired
    protected ComboBox<ParameterType> type;
    @Autowired
    protected ComboBox<String> metaClass;
    @Autowired
    protected CheckBox lookup;
    @Autowired
    protected Label<String> lookupLabel;
    @Autowired
    protected SourceCodeEditor lookupWhere;
    @Autowired
    protected SourceCodeEditor lookupJoin;
    @Named("tabsheet.lookupSettingsTab")
    protected VBoxLayout lookupSettingsTab;
    @Autowired
    protected TabSheet tabsheet;
    @Autowired
    protected Label<String> enumerationLabel;

    @Autowired
    protected Label<String> screenLabel;

    @Autowired
    protected Label<String> metaClassLabel;

    @Autowired
    protected GridLayout predefinedTransformationBox;

    @Autowired
    protected CheckBox predefinedTransformation;

    @Autowired
    protected SourceCodeEditor transformationScript;

    @Autowired
    protected SourceCodeEditor validationScript;

    @Autowired
    protected Label<String> transformationScriptLabel;

    @Autowired
    protected ComboBox<PredefinedTransformation> wildcards;

    @Autowired
    protected Label<String> wildcardsLabel;

    @Autowired
    protected CheckBox defaultDateIsCurrentCheckBox;

    @Autowired
    protected Label<String> defaultDateIsCurrentLabel;

    @Autowired
    protected Label<String> requiredLabel;

    @Autowired
    protected CheckBox required;

    @Autowired
    protected Metadata metadata;

    @Autowired
    protected SecureOperations secureOperations;

    @Autowired
    protected PolicyStore policyStore;

    @Autowired
    protected ThemeConstants themeConstants;

    @Autowired
    protected ReportService reportService;

    @Autowired
    protected InstanceContainer<ReportInputParameter> parameterDc;

    @Autowired
    protected ScreensHelper screensHelper;

    @Autowired
    protected ParameterClassResolver parameterClassResolver;

    @Autowired
    protected TextArea localeTextField;

    @Autowired
    protected JpqlSuggestionFactory jpqlSuggestionFactory;

    @Autowired
    protected MetadataTools metadataTools;

    @Autowired
    protected Dialogs dialogs;

    @Autowired
    protected Messages messages;

    @Autowired
    protected MessageTools messageTools;

    protected ReportInputParameter parameter;

    @Autowired
    protected ParameterFieldCreator parameterFieldCreator;

    //todo
//    @Override
//    public void setItem(JmixEntity item) {
//        ReportInputParameter newParameter = (ReportInputParameter) metadata.create(parameterDs.getMetaClass());
//        metadataTools.copy(item, newParameter);
//        newParameter.setId((UUID) Id.of(item).getValue());
//        if (newParameter.getParameterClass() == null) {
//            newParameter.setParameterClass(parameterClassResolver.resolveClass(newParameter));
//        }
//
//        super.setItem(newParameter);
//        enableControlsByParamType(newParameter.getType());
//        initScreensLookup();
//        initTransformations();
//    }

    @Subscribe
    protected void onInit(Screen.InitEvent event) {
        type.setOptionsList(Arrays.asList(ParameterType.TEXT, ParameterType.NUMERIC, ParameterType.BOOLEAN, ParameterType.ENUMERATION,
                ParameterType.DATE, ParameterType.TIME, ParameterType.DATETIME, ParameterType.ENTITY, ParameterType.ENTITY_LIST));

        initMetaClassLookup();

        initEnumsLookup();

        initListeners();

        initHelpButtons();

        initCodeEditors();
    }

    protected void initHelpButtons() {
        localeTextField.setContextHelpIconClickHandler(e ->
                dialogs.createMessageDialog()
                        .withCaption(messages.getMessage("localeText"))
                        .withMessage(messages.getMessage("parameter.localeTextHelp"))
                        .withContentMode(ContentMode.HTML)
                        .withModal(false)
                        .withWidth("700px")
                        .show());

        transformationScript.setContextHelpIconClickHandler(e ->
                dialogs.createMessageDialog()
                        .withCaption(messages.getMessage("transformationScript"))
                        .withMessage(messages.getMessage("parameter.transformationScriptHelp"))
                        .withContentMode(ContentMode.HTML)
                        .withModal(false)
                        .withWidth("700px")
                        .show());

        validationScript.setContextHelpIconClickHandler(e ->
                dialogs.createMessageDialog()
                        .withCaption(messages.getMessage("validationScript"))
                        .withMessage(messages.getMessage("validationScriptHelp"))
                        .withContentMode(ContentMode.HTML)
                        .withModal(false)
                        .withWidth("700px")
                        .show());
    }

    //todo
//    @Override
//    public boolean commit() {
//        if (super.commit()) {
//            metadataTools.copy(getEditedEntity(), parameter);
//            return true;
//        }
//        return false;
//    }

    protected void initListeners() {
        type.addValueChangeListener(e ->
                enableControlsByParamType(e.getValue())
        );

        parameterDc.addItemPropertyChangeListener(e -> {
            boolean typeChanged = e.getProperty().equalsIgnoreCase("type");
            boolean classChanged = e.getProperty().equalsIgnoreCase("entityMetaClass")
                    || e.getProperty().equalsIgnoreCase("enumerationClass");
            boolean defaultDateIsCurrentChanged = e.getProperty().equalsIgnoreCase("defaultDateIsCurrent");
            ReportInputParameter parameter = getEditedEntity();
            if (typeChanged || classChanged) {
                parameter.setParameterClass(parameterClassResolver.resolveClass(parameter));

                if (typeChanged) {
                    parameter.setEntityMetaClass(null);
                    parameter.setEnumerationClass(null);
                }

                parameter.setDefaultValue(null);
                parameter.setScreen(null);

                initScreensLookup();

                initDefaultValueField();
            }

            if (defaultDateIsCurrentChanged) {
                initDefaultValueField();
                initCurrentDateTimeField();
            }

            //todo
            //((DatasourceImplementation<ReportInputParameter>) parameterDc).modified(e.getItem());
        });

        lookup.addValueChangeListener(e -> {
            if (Boolean.TRUE.equals(e.getValue())) {
                if (tabsheet.getTab(LOOKUP_SETTINGS_TAB_ID) == null) {
                    tabsheet.addTab(LOOKUP_SETTINGS_TAB_ID, lookupSettingsTab);
                }
            } else {
                if (tabsheet.getTab(LOOKUP_SETTINGS_TAB_ID) != null) {
                    tabsheet.removeTab(LOOKUP_SETTINGS_TAB_ID);
                }
            }
        });
    }

    protected void initCodeEditors() {
        lookupWhere.setSuggester((source, text, cursorPosition) -> requestHint(lookupWhere, cursorPosition));
        lookupWhere.setHeight(themeConstants.get("cuba.gui.customConditionFrame.whereField.height"));

        lookupJoin.setSuggester((source, text, cursorPosition) -> requestHint(lookupJoin, cursorPosition));
        lookupJoin.setHeight(themeConstants.get("cuba.gui.customConditionFrame.joinField.height"));
    }

    protected void initScreensLookup() {
        ReportInputParameter parameter = getEditedEntity();
        if (parameter.getType() == ParameterType.ENTITY || parameter.getType() == ParameterType.ENTITY_LIST) {
            Class clazz = parameterClassResolver.resolveClass(parameter);
            if (clazz != null) {
                Map<String, String> screensMap = screensHelper.getAvailableBrowserScreens(clazz);
                screen.setOptionsMap(screensMap);
            }
        }
    }

    protected void initEnumsLookup() {
        Map<String, String> enumsOptionsMap = new TreeMap<>();
        for (Class enumClass : metadataTools.getAllEnums()) {
            String enumLocalizedName = messages.getMessage(enumClass, enumClass.getSimpleName());
            enumsOptionsMap.put(enumLocalizedName + " (" + enumClass.getSimpleName() + ")", enumClass.getCanonicalName());
        }
        enumeration.setOptionsMap(enumsOptionsMap);
    }

    protected void initMetaClassLookup() {
        Map<String, String> metaClassesOptionsMap = new TreeMap<>();
        Collection<MetaClass> classes = metadata.getSession().getClasses();
        for (MetaClass clazz : classes) {
            if (!metadataTools.isSystemLevel(clazz)) {
                String caption = messageTools.getDetailedEntityCaption(clazz);
                metaClassesOptionsMap.put(caption, clazz.getName());
            }
        }
        metaClass.setOptionsMap(metaClassesOptionsMap);
    }


    @Subscribe
    protected void onBeforeCommit(BeforeCommitChangesEvent event) {
        if (!(getEditedEntity().getType() == ParameterType.ENTITY && Boolean.TRUE.equals(lookup.getValue()))) {
            lookupWhere.setValue(null);
            lookupJoin.setValue(null);
        }
    }

    protected void initDefaultValueField() {
        defaultValueLabel.setVisible(false);
        defaultValueBox.removeAll();
        ReportInputParameter parameter = getEditedEntity();
        if (canHaveDefaultValue()) {
            Field<Object> field;
            if (ParameterType.ENTITY.equals(parameter.getType()) && Boolean.TRUE.equals(parameter.getLookup())) {
                ReportInputParameter entityParam = metadata.create(ReportInputParameter.class);
                entityParam.setReport(parameter.getReport());
                entityParam.setType(parameter.getType());
                entityParam.setEntityMetaClass(parameter.getEntityMetaClass());
                entityParam.setScreen(parameter.getScreen());
                entityParam.setAlias(parameter.getAlias());
                entityParam.setRequired(parameter.getRequired());
                field = parameterFieldCreator.createField(entityParam);
            } else {
                field = parameterFieldCreator.createField(parameter);
            }

            field.addValueChangeListener(e -> {
                if (e.getValue() != null) {
                    parameter.setDefaultValue(reportService.convertToString(e.getValue().getClass(), e.getValue()));
                } else {
                    parameter.setDefaultValue(null);
                }
            });

            if (parameter.getParameterClass() != null) {
                field.setValue(reportService.convertFromString(parameter.getParameterClass(), parameter.getDefaultValue()));
            }
            field.setRequired(false);

            defaultValueBox.add(field);
            defaultValueLabel.setVisible(true);
        }
        defaultValueBox.setEnabled(secureOperations.isEntityUpdatePermitted(metadata.getClass(ReportInputParameter.class), policyStore));
    }

    protected void initCurrentDateTimeField() {
        boolean parameterDateOrTime = isParameterDateOrTime();
        defaultDateIsCurrentLabel.setVisible(parameterDateOrTime);
        defaultDateIsCurrentCheckBox.setVisible(parameterDateOrTime);
    }

    protected boolean canHaveDefaultValue() {
        ReportInputParameter parameter = getEditedEntity();
        if (parameter == null) {
            return false;
        }

        if (isParameterDateOrTime() && BooleanUtils.isTrue(parameter.getDefaultDateIsCurrent())) {
            return false;
        }

        ParameterType type = parameter.getType();
        return type != null
                && type != ParameterType.ENTITY_LIST
                && (type != ParameterType.ENTITY || StringUtils.isNotBlank(parameter.getEntityMetaClass()))
                && (type != ParameterType.ENUMERATION || StringUtils.isNotBlank(parameter.getEnumerationClass()));
    }

    protected void enableControlsByParamType(ParameterType type) {
        boolean isSingleEntity = type == ParameterType.ENTITY;
        boolean isEntity = isSingleEntity || type == ParameterType.ENTITY_LIST;
        boolean isEnum = type == ParameterType.ENUMERATION;
        boolean isText = type == ParameterType.TEXT;

        metaClass.setVisible(isEntity);
        metaClassLabel.setVisible(isEntity);

        lookup.setVisible(isSingleEntity);
        lookupLabel.setVisible(isSingleEntity);
        if (isSingleEntity && Boolean.TRUE.equals(lookup.getValue())) {
            if (tabsheet.getTab(LOOKUP_SETTINGS_TAB_ID) == null) {
                tabsheet.addTab(LOOKUP_SETTINGS_TAB_ID, lookupSettingsTab);
            }
        } else {
            if (tabsheet.getTab(LOOKUP_SETTINGS_TAB_ID) != null) {
                tabsheet.removeTab(LOOKUP_SETTINGS_TAB_ID);
            }
        }

        screen.setVisible(isEntity);
        screenLabel.setVisible(isEntity);

        enumeration.setVisible(isEnum);
        enumerationLabel.setVisible(isEnum);

        predefinedTransformationBox.setVisible(isText);

        initDefaultValueField();
        initCurrentDateTimeField();
    }

    protected void initTransformations() {
        ReportInputParameter parameter = getEditedEntity();
        predefinedTransformation.setValue(parameter.getPredefinedTransformation() != null);
        enableControlsByTransformationType(parameter.getPredefinedTransformation() != null);
        predefinedTransformation.addValueChangeListener(e -> {
            boolean hasPredefinedTransformation = e.getValue() != null && e.getValue();

            enableControlsByTransformationType(hasPredefinedTransformation);
            if (hasPredefinedTransformation) {
                parameter.setTransformationScript(null);
            } else {
                parameter.setPredefinedTransformation(null);
            }
        });
        predefinedTransformation.setEditable(secureOperations.isEntityUpdatePermitted(metadata.getClass(ReportInputParameter.class), policyStore));
    }

    protected void enableControlsByTransformationType(boolean hasPredefinedTransformation) {
        transformationScript.setVisible(!hasPredefinedTransformation);
        transformationScriptLabel.setVisible(!hasPredefinedTransformation);
        wildcards.setVisible(hasPredefinedTransformation);
        wildcardsLabel.setVisible(hasPredefinedTransformation);
    }

    protected boolean isParameterDateOrTime() {
        ReportInputParameter parameter = getEditedEntity();
        return Optional.ofNullable(parameter)
                .map(reportInputParameter ->
                        ParameterType.DATE.equals(parameter.getType()) ||
                                ParameterType.DATETIME.equals(parameter.getType()) ||
                                ParameterType.TIME.equals(parameter.getType()))
                .orElse(false);
    }

    protected List<Suggestion> requestHint(SourceCodeEditor sender, int senderCursorPosition) {
        String joinStr = lookupJoin.getValue();
        String whereStr = lookupWhere.getValue();

        // CAUTION: the magic entity name!  The length is three character to match "{E}" length in query
        String entityAlias = "a39";

        int queryPosition = -1;
        Class javaClassForEntity = getEditedEntity().getParameterClass();
        if (javaClassForEntity == null) {
            return new ArrayList<>();
        }

        String queryStart = String.format("select %s from %s %s ", entityAlias, metadata.getClass(javaClassForEntity), entityAlias);

        StringBuilder queryBuilder = new StringBuilder(queryStart);
        if (StringUtils.isNotEmpty(joinStr)) {
            if (sender == lookupJoin) {
                queryPosition = queryBuilder.length() + senderCursorPosition - 1;
            }
            if (!StringUtils.containsIgnoreCase(joinStr, "join") && !StringUtils.contains(joinStr, ",")) {
                queryBuilder.append("join ").append(joinStr);
                queryPosition += "join ".length();
            } else {
                queryBuilder.append(joinStr);
            }
        }
        if (StringUtils.isNotEmpty(whereStr)) {
            if (sender == lookupWhere) {
                queryPosition = queryBuilder.length() + WHERE.length() + senderCursorPosition - 1;
            }
            queryBuilder.append(WHERE).append(whereStr);
        }
        String query = queryBuilder.toString();
        query = query.replace("{E}", entityAlias);

        return jpqlSuggestionFactory.requestHint(query, queryPosition, sender.getAutoCompleteSupport(), senderCursorPosition);
    }
}
