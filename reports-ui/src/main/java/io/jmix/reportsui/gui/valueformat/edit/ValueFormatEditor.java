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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static io.jmix.ui.component.Window.COMMIT_ACTION_ID;

@DialogMode(width = "AUTO", forceDialog = true)
@UiController("report_ReportValueFormat.edit")
@UiDescriptor("format-edit.xml")
@EditedEntityContainer("valuesFormatsDc")
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

    @Autowired
    protected ComboBox<String> formatComboBox;

    @Autowired
    protected Form formatForm;

    @Autowired
    protected CheckBox groovyCheckBox;

    @Autowired
    protected LinkButton groovyFullScreenLinkButton;

    @Autowired
    protected VBoxLayout groovyVBox;

    @Autowired
    protected SourceCodeEditor groovyCodeEditor;

    @Autowired
    protected InstanceContainer<ReportValueFormat> valuesFormatsDc;

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

    @Autowired
    protected UiComponents uiComponents;

    protected SourceCodeEditor.Mode groovyScriptFieldMode = SourceCodeEditor.Mode.Groovy;

    @Subscribe
    protected void onInit(InitEvent event) {
        // Add default format strings to comboBox
        initFormatComboBox();
        groovyCheckBox.addValueChangeListener(booleanValueChangeEvent -> {
            Boolean visible = booleanValueChangeEvent.getValue();
            Boolean prevVisible = booleanValueChangeEvent.getPrevValue();

            Boolean userOriginated = booleanValueChangeEvent.isUserOriginated();

            if (isClickTrueGroovyScript(visible, prevVisible, userOriginated)) {
                groovyCodeEditor.setValue(RETURN_VALUE);
            }
            if (Boolean.FALSE.equals(visible)) {
                formatComboBox.clear();
            }

            groovyVBox.setVisible(Boolean.TRUE.equals(visible));
            formatComboBox.setVisible(Boolean.FALSE.equals(visible));
        });
    }

    @Install(to = "groovyCheckBox", subject = "contextHelpIconClickHandler")
    private void groovyCheckBoxContextHelpIconClickHandler(HasContextHelp.ContextHelpIconClickEvent contextHelpIconClickEvent) {
        dialogs.createMessageDialog()
                .withCaption(messages.getMessage(getClass(), "valuesFormats.groovyScript"))
                .withMessage(messages.getMessage(getClass(), "valuesFormats.groovyScriptHelpText"))
                .withContentMode(ContentMode.HTML)
                .withModal(false)
                .withWidth("700px")
                .show();
    }

    @Subscribe("groovyCheckBox")
    public void onGroovyCheckBoxValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        Boolean visible = event.getValue();
        Boolean prevVisible = event.getPrevValue();

        Boolean userOriginated = event.isUserOriginated();

        if (isClickTrueGroovyScript(visible, prevVisible, userOriginated)) {
            groovyCodeEditor.setValue(RETURN_VALUE);
        }
        if (Boolean.FALSE.equals(visible)) {
            formatComboBox.clear();
        }

        groovyVBox.setVisible(Boolean.TRUE.equals(visible));
        formatComboBox.setVisible(Boolean.FALSE.equals(visible));
    }

    protected void initFormatComboBox() {
        formatComboBox.setOptionsList(Arrays.asList(defaultFormats));

        formatComboBox.setNewOptionHandler(caption -> {
            addFormatItem(caption);
            formatComboBox.setValue(caption);
        });

        formatComboBox.setEditable(secureOperations.isEntityUpdatePermitted(metadata.getClass(ReportValueFormat.class), policyStore));
    }

    protected boolean isClickTrueGroovyScript(Boolean visible, Boolean prevVisible, Boolean userOriginated) {
        return Boolean.TRUE.equals(userOriginated) && Boolean.TRUE.equals(visible) && Boolean.FALSE.equals(prevVisible);
    }

    protected void addFormatItem(String caption) {
        //noinspection unchecked
        List<String> optionsList = formatComboBox.getOptions().getOptions()
                .collect(Collectors.toList());
        optionsList.add(caption);

        formatComboBox.setOptionsList(optionsList);
    }

    @Subscribe
    protected void onAfterInit(AfterInitEvent event) {
        String value = formatComboBox.getValue();
        if (value != null) {
            List<String> optionsList = formatComboBox.getOptions().getOptions()
                    .collect(Collectors.toList());

            if (!optionsList.contains(value) && Boolean.FALSE.equals(groovyCheckBox.isChecked())) {
                addFormatItem(value);
            }
            formatComboBox.setValue(value);
        }
    }

    @Subscribe
    public void onBeforeCommitChanges(BeforeCommitChangesEvent event) {
        getEditedEntity().setFormatString(formatComboBox.getValue());
    }

    @Subscribe("groovyFullScreenLinkButton")
    public void showGroovyEditorDialog(Button.ClickEvent event) {
        ScriptEditorDialog editorDialog = (ScriptEditorDialog) screenBuilders.screen(this)
                .withScreenId("report_Editor.dialog")
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