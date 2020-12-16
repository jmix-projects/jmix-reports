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
package io.jmix.reportsui.gui.valueformat.edit;

import io.jmix.core.Messages;
import io.jmix.core.Metadata;
import io.jmix.core.MetadataTools;
import io.jmix.core.common.util.ParamsMap;
import io.jmix.reports.entity.ReportValueFormat;
import io.jmix.reportsui.gui.definition.edit.scripteditordialog.ScriptEditorDialog;
import io.jmix.security.constraint.PolicyStore;
import io.jmix.security.constraint.SecureOperations;
import io.jmix.ui.Dialogs;
import io.jmix.ui.Notifications;
import io.jmix.ui.ScreenBuilders;
import io.jmix.ui.UiComponents;
import io.jmix.ui.component.*;
import io.jmix.ui.model.InstanceContainer;
import io.jmix.ui.screen.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

import static io.jmix.ui.component.Window.COMMIT_ACTION_ID;

@UiController("format-edit")
@UiDescriptor("format-edit.xml")
public class ValueFormatEditor extends StandardEditor<ReportValueFormat> {

    public static final String RETURN_VALUE = "return value";

    protected String[] defaultFormats = new String[]{
            "#,##0",
            "##,##0",
            "#,##0.###",
            "#,##0.##",
            "dd/MM/yyyy HH:mm",
            "${image:WxH}",
            "${bitmap:WxH}",
            "${imageFileId:WxH}",
            "${html}",
            "class:"
    };

    protected ComboBox<String> formatField = null;

    @Autowired
    protected Form formatFields;

    @Autowired
    protected CheckBox groovyCheckBox;

    @Autowired
    protected LinkButton groovyFullScreenLinkButton;

    @Autowired
    protected VBoxLayout groovyVBox;

    @Autowired
    protected SourceCodeEditor groovyCodeEditor;

    @Autowired
    protected UiComponents componentsFactory;

    @Autowired
    protected InstanceContainer valuesFormatsDs;

    @Autowired
    protected Metadata metadata;

    @Autowired
    protected SecureOperations secureOperations;

    @Autowired
    protected PolicyStore policyStore;

    @Autowired
    protected Notifications notifications;

    @Autowired
    protected Dialogs dialogs;

    @Autowired
    protected Messages messages;

    @Autowired
    protected MetadataTools metadataTools;

    @Autowired
    protected ScreenBuilders screenBuilders;

    protected SourceCodeEditor.Mode groovyScriptFieldMode = SourceCodeEditor.Mode.Groovy;

    @Subscribe
    protected void onInit(InitEvent event) {
        //TODO dialog options
//        getDialogOptions().setWidthAuto();

        // Add default format strings to combobox
        //todo
//        formatFields.addCustomField("formatString", new FieldGroup.CustomFieldGenerator() {
//            @Override
//            public Component generateField(Datasource datasource, String propertyId) {
//                formatField = componentsFactory.create(ComboBox.class);
//                Map<String, String> options = new HashMap<>();
//                for (String format : defaultFormats) {
//                    options.put(format, format);
//                }
//
//                formatField.setDatasource(datasource, propertyId);
//                formatField.setOptionsMap(options);
//                formatField.setNewOptionAllowed(true);
//                formatField.setNewOptionHandler(caption -> {
//                    addFormatItem(caption);
//                    formatField.setValue(caption);
//                });
//                formatField.setEditable(security.isEntityOpPermitted(ReportValueFormat.class, EntityOp.UPDATE));
//                return formatField;
//            }
//        });

        groovyCheckBox.setContextHelpIconClickHandler(e -> dialogs.createMessageDialog()
                .withCaption(messages.getMessage("valuesFormats.groovyScript"))
                .withMessage(messages.getMessage("valuesFormats.groovyScriptHelpText"))
                .withContentMode(ContentMode.HTML)
                .withModal(false)
                .withWidth("700px")
                .show());

        groovyCheckBox.addValueChangeListener(booleanValueChangeEvent -> {
            Boolean visible = booleanValueChangeEvent.getValue();
            Boolean prevVisible = booleanValueChangeEvent.getPrevValue();

            Boolean userOriginated = booleanValueChangeEvent.isUserOriginated();

            if (isClickTrueGroovyScript(visible, prevVisible, userOriginated)) {
                groovyCodeEditor.setValue(RETURN_VALUE);
            }
            if (Boolean.FALSE.equals(visible)) {
                formatField.clear();
            }

            groovyVBox.setVisible(Boolean.TRUE.equals(visible));
            formatField.setVisible(Boolean.FALSE.equals(visible));
        });

        //noinspection unchecked
//        valuesFormatsDs.addItemPropertyChangeListener(e ->
//                ((DatasourceImplementation) valuesFormatsDs).modified(e.getItem()));
    }

    protected boolean isClickTrueGroovyScript(Boolean visible, Boolean prevVisible, Boolean userOriginated) {
        return Boolean.TRUE.equals(userOriginated) && Boolean.TRUE.equals(visible) && Boolean.FALSE.equals(prevVisible);
    }

    protected void addFormatItem(String caption) {
        //noinspection unchecked
//        Map<String, String> optionsMap =
//                new HashMap<>((Map<? extends String, ? extends String>) formatField.getOptionsMap());
//        optionsMap.put(caption, caption);
//
//        formatField.setOptionsMap(optionsMap);
    }

    @Subscribe
    protected void onAfterInit(AfterInitEvent event) {
        String value = formatField.getValue();
        if (value != null) {
//            if (!formatField.getOptions().containsValue(value) && Boolean.FALSE.equals(groovyCheckBox.isChecked())) {
//                addFormatItem(value);
//            }
            formatField.setValue(value);
        }
    }

    //todo
//    @Override
//    public void setItem(Entity item) {
//        Entity newItem = valuesFormatsDs.getDataSupplier().newInstance(valuesFormatsDs.getMetaClass());
//        metadataTools.copy(item, newItem);
//        ((ReportValueFormat) newItem).setId((UUID) Id.of(item).getValue());
//        super.setItem(newItem);
//    }

    public void showGroovyEditorDialog() {
        ScriptEditorDialog editorDialog = (ScriptEditorDialog) screenBuilders.screen(this)
                .withScreenId("scriptEditorDialog")
                .withOpenMode(OpenMode.DIALOG)
                .withOptions(new MapScreenOptions(ParamsMap.of(
                        "mode", groovyScriptFieldMode,
                        "suggester", groovyCodeEditor.getSuggester(),
                        "scriptValue", groovyCodeEditor.getValue(),
                        "helpVisible", groovyCodeEditor.isVisible(),
                        "helpMsgKey", "valuesFormats.groovyScriptHelpText"
                )))
                .build();
        editorDialog.addAfterCloseListener(actionId -> {
            StandardCloseAction closeAction = (StandardCloseAction) actionId.getCloseAction();
            if (COMMIT_ACTION_ID.equals(closeAction.getActionId())) {
                groovyCodeEditor.setValue(editorDialog.getValue());
            }
        });
        editorDialog.show();
    }
}