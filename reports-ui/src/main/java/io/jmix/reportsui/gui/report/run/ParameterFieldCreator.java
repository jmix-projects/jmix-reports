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

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.haulmont.chile.core.datatypes.DatatypeRegistry;
import com.haulmont.cuba.core.global.Scripting;
import com.haulmont.cuba.gui.WindowParams;
import com.haulmont.cuba.gui.components.PickerField;
import io.jmix.core.*;
import io.jmix.core.metamodel.model.MetaClass;
import io.jmix.reports.app.service.ReportService;
import io.jmix.reports.entity.ParameterType;
import io.jmix.reports.entity.ReportInputParameter;
import io.jmix.reports.entity.ReportType;
import io.jmix.ui.Actions;
import io.jmix.ui.UiComponents;
import io.jmix.ui.action.entitypicker.LookupAction;
import io.jmix.ui.component.*;
import io.jmix.ui.component.data.options.ContainerOptions;
import io.jmix.ui.component.validators.DoubleValidator;
import io.jmix.ui.model.CollectionContainer;
import io.jmix.ui.model.CollectionLoader;
import io.jmix.ui.model.DataComponents;
import io.jmix.ui.screen.ScreenFragment;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

@org.springframework.stereotype.Component("report_ParameterFieldCreator")
public class ParameterFieldCreator {

    public static final String COMMON_LOOKUP_SCREEN_ID = "commonLookup";

    @Autowired
    protected UiComponents uiComponents;

    @Autowired
    protected Messages messages;

    @Autowired
    protected Metadata metadata;

    @Autowired
    protected Scripting scripting;

    @Autowired
    protected ReportService reportService;

    @Autowired
    protected QueryTransformerFactory queryTransformerFactory;

    @Autowired
    protected DatatypeRegistry datatypeRegistry;

    @Autowired
    protected DataComponents factory;

    @Autowired
    protected FetchPlans fetchPlans;

    @Autowired
    protected Actions actions;

    protected ScreenFragment frame;

    protected Map<ParameterType, FieldCreator> fieldCreationMapping = new ImmutableMap.Builder<ParameterType, FieldCreator>()
            .put(ParameterType.BOOLEAN, new CheckBoxCreator())
            .put(ParameterType.DATE, new DateFieldCreator())
            .put(ParameterType.ENTITY, new SingleFieldCreator())
            .put(ParameterType.ENUMERATION, new EnumFieldCreator())
            .put(ParameterType.TEXT, new TextFieldCreator())
            .put(ParameterType.NUMERIC, new NumericFieldCreator())
            .put(ParameterType.ENTITY_LIST, new MultiFieldCreator())
            .put(ParameterType.DATETIME, new DateTimeFieldCreator())
            .put(ParameterType.TIME, new TimeFieldCreator())
            .build();

    public ParameterFieldCreator(ScreenFragment frame) {
        this.frame = frame;
    }

    public Label createLabel(ReportInputParameter parameter, Field field) {
        Label label = uiComponents.create(Label.class);
        label.setAlignment(field instanceof TokenList ? Component.Alignment.TOP_LEFT : Component.Alignment.MIDDLE_LEFT);
        label.setWidth(Component.AUTO_SIZE);
        label.setValue(parameter.getLocName());
        return label;
    }

    public Field createField(ReportInputParameter parameter) {
        Field field = fieldCreationMapping.get(parameter.getType()).createField(parameter);
        field.setRequiredMessage(messages.formatMessage(this.getClass(), "error.paramIsRequiredButEmpty", parameter.getLocName()));

        field.setId("param_" + parameter.getAlias());
        field.setWidth("100%");
        //field.setFrame(frame);
//        field.setFrame(frame.getWrappedFrame());
        field.setEditable(true);

        field.setRequired(parameter.getRequired());
        return field;
    }

    protected void setCurrentDateAsNow(ReportInputParameter parameter, Field dateField) {
        Date now = reportService.currentDateOrTime(parameter.getType());
        dateField.setValue(now);
        parameter.setDefaultValue(reportService.convertToString(now.getClass(), now));
    }

    protected interface FieldCreator {
        Field createField(ReportInputParameter parameter);
    }

    protected class DateFieldCreator implements FieldCreator {
        @Override
        public Field createField(ReportInputParameter parameter) {
            DateField dateField = uiComponents.create(DateField.class);
            dateField.setResolution(DateField.Resolution.DAY);
            dateField.setDateFormat(messages.getMessage("dateFormat"));
            if (BooleanUtils.isTrue(parameter.getDefaultDateIsCurrent())) {
                setCurrentDateAsNow(parameter, dateField);
            }
            return dateField;
        }
    }

    protected class DateTimeFieldCreator implements FieldCreator {
        @Override
        public Field createField(ReportInputParameter parameter) {
            DateField dateField = uiComponents.create(DateField.class);
            dateField.setResolution(DateField.Resolution.MIN);
            dateField.setDateFormat(messages.getMessage("dateTimeFormat"));
            if (BooleanUtils.isTrue(parameter.getDefaultDateIsCurrent())) {
                setCurrentDateAsNow(parameter, dateField);
            }
            return dateField;
        }
    }

    protected class TimeFieldCreator implements FieldCreator {

        @Override
        public Field createField(ReportInputParameter parameter) {
            Field timeField = uiComponents.create(TimeField.class);
            if (BooleanUtils.isTrue(parameter.getDefaultDateIsCurrent())) {
                setCurrentDateAsNow(parameter, timeField);
            }
            return uiComponents.create(TimeField.class);
        }
    }

    protected class CheckBoxCreator implements FieldCreator {

        @Override
        public Field createField(ReportInputParameter parameter) {
            CheckBox checkBox = uiComponents.create(CheckBox.class);
            checkBox.setAlignment(Component.Alignment.MIDDLE_LEFT);
            return checkBox;
        }
    }

    protected class TextFieldCreator implements FieldCreator {

        @Override
        public Field createField(ReportInputParameter parameter) {
            return uiComponents.create(TextField.class);
        }
    }

    protected class NumericFieldCreator implements FieldCreator {

        @Override
        public Field createField(ReportInputParameter parameter) {
            TextField textField = uiComponents.create(TextField.class);
            textField.addValidator(new DoubleValidator());
            textField.setDatatype(datatypeRegistry.get(Double.class));
            return textField;
        }
    }

    protected class EnumFieldCreator implements FieldCreator {

        @Override
        public Field createField(ReportInputParameter parameter) {
            ComboBox lookupField = uiComponents.create(ComboBox.class);
            String enumClassName = parameter.getEnumerationClass();
            if (StringUtils.isNotBlank(enumClassName)) {
                Class enumClass = scripting.loadClass(enumClassName);

                if (enumClass != null) {
                    Object[] constants = enumClass.getEnumConstants();
                    List<Object> optionsList = new ArrayList<>();
                    Collections.addAll(optionsList, constants);

                    lookupField.setOptionsList(optionsList);
                    if (optionsList.size() < 10) {
                        lookupField.setTextInputAllowed(false);
                    }
                }
            }
            return lookupField;
        }
    }

    protected class SingleFieldCreator implements FieldCreator {
        @Override
        public Field createField(ReportInputParameter parameter) {
            boolean isLookup = Boolean.TRUE.equals(parameter.getLookup());
            EntityPicker field;
            MetaClass entityMetaClass = metadata.getClass(parameter.getEntityMetaClass());

            if (isLookup) {
                field = uiComponents.create(EntityComboBox.class);

                FetchPlan fetchPlan = fetchPlans.builder(entityMetaClass.getJavaClass())
                        .addFetchPlan(FetchPlan.BASE)
                        .build();

                CollectionContainer collectionContainer = factory.createCollectionContainer(entityMetaClass.getJavaClass());
                collectionContainer.setFetchPlan(fetchPlan);

                CollectionLoader collectionLoader = factory.createCollectionLoader();
                collectionLoader.setContainer(collectionContainer);

//                CollectionDatasource ds = DsBuilder.create()
//                        .setViewName(View.MINIMAL)
//                        .setMetaClass(entityMetaClass)
//                        .buildCollectionDatasource();
//                ds.setRefreshOnComponentValueChange(true);

                String whereClause = parameter.getLookupWhere();
                String joinClause = parameter.getLookupJoin();
                if (!Strings.isNullOrEmpty(whereClause)) {
                    String query = String.format("select e from %s e", entityMetaClass.getName());
                    QueryTransformer queryTransformer = queryTransformerFactory.transformer(query);
                    queryTransformer.addWhere(whereClause);
                    if (!Strings.isNullOrEmpty(joinClause)) {
                        queryTransformer.addJoin(joinClause);
                    }
                    query = queryTransformer.getResult();
                    collectionLoader.setQuery(query);
                }
                collectionLoader.load();
//                ((DatasourceImplementation) ds).initialized();
                ((EntityComboBox) field).setOptionsContainer(collectionContainer);
            } else {
                field = uiComponents.create(EntityPicker.class);
            }
            field.setMetaClass(entityMetaClass);

            LookupAction lookupAction = (LookupAction) actions.create(LookupAction.ID);

            LookupAction pickerLookupAction = lookupAction;
            field.addAction(pickerLookupAction);
            //field.addClearAction();

            String parameterScreen = parameter.getScreen();

            if (StringUtils.isNotEmpty(parameterScreen)) {
                pickerLookupAction.setLookupScreen(parameterScreen);
                pickerLookupAction.setLookupScreenParams(Collections.emptyMap());
            } else {
                pickerLookupAction.setLookupScreen(COMMON_LOOKUP_SCREEN_ID);

                Map<String, Object> params = new HashMap<>();
                //TODO class parameter
//                params.put(CLASS_PARAMETER, entityMetaClass);

                if (parameter.getReport().getReportType() == ReportType.SIMPLE) {
                    WindowParams.MULTI_SELECT.set(params, false);
                }

                pickerLookupAction.setLookupScreenParams(params);
            }

            return field;
        }
    }

    protected class MultiFieldCreator implements FieldCreator {

        @Override
        public Field createField(final ReportInputParameter parameter) {
            TokenList tokenList = uiComponents.create(TokenList.class);
            MetaClass entityMetaClass = metadata.getClass(parameter.getEntityMetaClass());

            CollectionContainer collectionContainer = factory.createCollectionContainer(entityMetaClass.getJavaClass());
            FetchPlan fetchPlan = fetchPlans.builder(entityMetaClass.getJavaClass())
                    .addFetchPlan(FetchPlan.LOCAL)
                    .build();

            collectionContainer.setFetchPlan(fetchPlan);

            factory.createCollectionLoader()
                    .setContainer(collectionContainer);


//            DsBuilder builder = DsBuilder.create(frame.getDsContext());
//            CollectionDatasource cds = builder
//                    .setRefreshMode(CollectionDatasource.RefreshMode.NEVER)
//                    .setId("entities_" + parameter.getAlias())
//                    .setMetaClass(entityMetaClass)
//                    .setViewName(View.LOCAL)
//                    .setAllowCommit(false)
//                    .buildCollectionDatasource();
//
//            cds.refresh();


            tokenList.setOptions(new ContainerOptions(collectionContainer));
            tokenList.setEditable(true);
            tokenList.setLookup(true);
            tokenList.setHeight("120px");

            String screen = parameter.getScreen();

            if (StringUtils.isNotEmpty(screen)) {
                tokenList.setLookupScreen(screen);
                tokenList.setLookupScreenParams(Collections.emptyMap());
            } else {
                tokenList.setLookupScreen("commonLookup");
                //TODO class parameter
//                tokenList.setLookupScreenParams(ParamsMap.of(CLASS_PARAMETER, entityMetaClass));
            }

            tokenList.setAddButtonCaption(messages.getMessage(TokenList.class, "actions.Select"));
            tokenList.setInline(true);
            tokenList.setSimple(true);

            return tokenList;
        }
    }
}