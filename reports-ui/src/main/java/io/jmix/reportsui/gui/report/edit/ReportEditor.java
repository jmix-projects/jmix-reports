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
package io.jmix.reportsui.gui.report.edit;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.haulmont.cuba.gui.data.impl.CollectionPropertyDatasourceImpl;
import com.haulmont.cuba.gui.data.impl.DatasourceImpl;
import com.haulmont.cuba.gui.data.impl.HierarchicalPropertyDatasourceImpl;
import com.haulmont.cuba.gui.upload.FileUploadingAPI;
import com.haulmont.yarg.structure.BandOrientation;
import io.jmix.core.*;
import io.jmix.core.common.util.ParamsMap;
import io.jmix.reports.ReportPrintHelper;
import io.jmix.reports.app.service.ReportService;
import io.jmix.reports.entity.*;
import io.jmix.reportsui.gui.definition.edit.BandDefinitionEditor;
import io.jmix.security.constraint.PolicyStore;
import io.jmix.security.constraint.SecureOperations;
import io.jmix.ui.*;
import io.jmix.ui.action.AbstractAction;
import io.jmix.ui.action.ItemTrackingAction;
import io.jmix.ui.action.ListAction;
import io.jmix.ui.action.list.CreateAction;
import io.jmix.ui.action.list.EditAction;
import io.jmix.ui.action.list.RemoveAction;
import io.jmix.ui.component.*;
import io.jmix.ui.component.data.meta.ContainerDataUnit;
import io.jmix.ui.download.Downloader;
import io.jmix.ui.model.CollectionContainer;
import io.jmix.ui.model.CollectionLoader;
import io.jmix.ui.model.InstanceContainer;
import io.jmix.ui.screen.*;
import io.jmix.ui.sys.ScreensHelper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import javax.inject.Named;
import java.util.*;
import java.util.stream.Collectors;

@UiController("report_Report.edit")
@UiDescriptor("report-edit.xml")
public class ReportEditor extends StandardEditor<Report> {

    @Named("generalFrame.propertiesFieldGroup")
    protected Form propertiesFieldGroup;

    @Named("generalFrame.bandEditor")
    protected BandDefinitionEditor bandEditor;

    @Named("securityFrame.screenIdLookup")
    protected ComboBox<String> screenIdLookup;

    @Named("securityFrame.screenTable")
    protected Table<ReportScreen> screenTable;

    @Named("templatesFrame.templatesTable")
    protected Table<ReportTemplate> templatesTable;

    @Named("localesFrame.localeTextField")
    protected TextArea localesTextField;

    @Named("run")
    protected Button run;

    @Named("generalFrame.createBandDefinition")
    protected Button createBandDefinitionButton;

    @Named("generalFrame.removeBandDefinition")
    protected Button removeBandDefinitionButton;

    @Named("generalFrame.up")
    protected Button bandUpButton;

    @Named("generalFrame.down")
    protected Button bandDownButton;

    @Named("securityFrame.addReportScreenBtn")
    protected Button addReportScreenBtn;

    @Named("securityFrame.addRoleBtn")
    protected Button addRoleBtn;

    @Named("securityFrame.rolesTable")
    //TODO roles table
    protected Table rolesTable;

    @Named("parametersFrame.inputParametersTable")
    protected Table<ReportInputParameter> parametersTable;

    @Named("formatsFrame.valuesFormatsTable")
    protected Table<ReportValueFormat> formatsTable;

    @Named("parametersFrame.up")
    protected Button paramUpButton;

    @Named("parametersFrame.down")
    protected Button paramDownButton;

    @Named("generalFrame.serviceTree")
    protected Tree<BandDefinition> bandTree;

    @Named("generalFrame.invisibleFileUpload")
    protected FileUploadField invisibleFileUpload;

    @Named("generalFrame.reportFields")
    protected HBoxLayout reportFields;

    @Named("parametersFrame.validationScriptGroupBox")
    protected GroupBoxLayout validationScriptGroupBox;

    @Named("parametersFrame.validationScriptCodeEditor")
    protected SourceCodeEditor validationScriptCodeEditor;

    @Autowired
    protected WindowConfig windowConfig;

    @Autowired
    protected InstanceContainer<Report> reportDc;

    @Autowired
    protected CollectionContainer<ReportGroup> groupsDc;

    @Autowired
    protected CollectionLoader groupsDl;

    @Autowired
    protected CollectionContainer<ReportInputParameter> parametersDc;

    @Autowired
    protected CollectionContainer<ReportScreen> reportScreensDc;

    //TODO roles ds
    @Autowired
    protected CollectionContainer rolesDc;

    //TODO roles ds
    @Autowired
    protected CollectionContainer lookupRolesDc;

    @Autowired
    protected CollectionContainer<DataSet> dataSetsDc;

    @Autowired
    protected CollectionContainer<BandDefinition> treeDc;

    @Autowired
    protected CollectionContainer<ReportTemplate> templatesDc;

    @Autowired
    protected UiComponents uiComponents;

    @Autowired
    protected FileUploadingAPI fileUpload;

    @Autowired
    protected ReportService reportService;

    @Autowired
    protected CollectionContainer<BandDefinition> bandsDc;

    @Autowired
    protected CollectionContainer<BandDefinition> availableParentBandsDc;

    @Autowired
    protected ScreensHelper screensHelper;

    @Autowired
    protected Metadata metadata;

    @Autowired
    protected MetadataTools metadataTools;

    @Autowired
    protected SecureOperations secureOperations;

    @Autowired
    protected PolicyStore policyStore;

    @Autowired
    protected Messages messages;

    @Autowired
    protected UiProperties uiProperties;

    @Autowired
    protected CoreProperties coreProperties;

    @Autowired
    protected Notifications notifications;

    @Autowired
    protected Dialogs dialogs;

    @Autowired
    protected EntityStates entityStates;

    @Autowired
    protected ScreenBuilders screenBuilders;

    @Autowired
    protected Screens screens;

    @Autowired
    protected Downloader downloader;

    @Autowired
    protected Actions actions;

    @Subscribe
    protected void initNewItem(InitEntityEvent<Report> event) {
        Report report = event.getEntity();
        report.setReportType(ReportType.SIMPLE);

        BandDefinition rootDefinition = metadata.create(BandDefinition.class);
        rootDefinition.setName("Root");
        rootDefinition.setPosition(0);
        report.setBands(new HashSet<>());
        report.getBands().add(rootDefinition);

        rootDefinition.setReport(report);

        groupsDl.load();
        Collection<ReportGroup> reportGroups = groupsDc.getItems();
        if (!reportGroups.isEmpty()) {
            ReportGroup reportGroup = reportGroups.iterator().next();
            report.setGroup(groupsDc.getItem(report));
        }
    }

    @Subscribe
    protected void onBeforeShow(BeforeShowEvent event) {
        if (!StringUtils.isEmpty(getEditedEntity().getName())) {
            getWindow().setCaption(messages.formatMessage(getClass(), "reportEditor.format", getEditedEntity().getName()));
        }
    }

    @Subscribe
    protected void onAfterInit(AfterInitEvent event) {

        ((CollectionPropertyDatasourceImpl) treeDc).setModified(false);
        ((DatasourceImpl) reportDc).setModified(false);

        //todo
        //bandTree.getDatasource().refresh();
        bandTree.expandTree();
        bandTree.setSelected(reportDc.getItem().getRootBandDefinition());

        bandEditor.setBandDefinition(bandTree.getSingleSelected());
        if (bandTree.getSingleSelected() == null) {
            bandEditor.setEnabled(false);
        }

        setupDropZoneForTemplate();

        initValidationScriptGroupBoxCaption();
    }

    @Subscribe
    public void init(InitEvent event) {
        initGeneral();
        initTemplates();
        initParameters();
        initRoles();
        initScreens();
        initValuesFormats();
        initHelpButtons();
    }

    protected void initParameters() {
        CreateAction createAction = (CreateAction) actions.create(CreateAction.ID);
        createAction.setOpenMode(OpenMode.DIALOG);
        parametersTable.addAction(createAction);

//        parametersTable.addAction(
//                new CreateAction(parametersTable, OpenType.DIALOG) {
//                    @Override
//                    public Map<String, Object> getInitialValues() {
//                        Map<String, Object> params = new HashMap<>();
//                        params.put("position", parametersDs.getItemIds().size());
//                        params.put("report", getEditedEntity());
//                        return params;
//                    }
//
//                    @Override
//                    public void actionPerform(Component component) {
//                        orderParameters();
//                        super.actionPerform(component);
//                    }
//                }
//        );

        RemoveAction removeAction = (RemoveAction) actions.create(RemoveAction.ID);
        removeAction.setAfterActionPerformedHandler(event -> orderParameters());
        parametersTable.addAction(removeAction);

        EditAction editAction = (EditAction) actions.create(EditAction.ID);
        editAction.setOpenMode(OpenMode.DIALOG);
        parametersTable.addAction(editAction);

        paramUpButton.setAction(new ListAction("generalFrame.up") {
            @Override
            public void actionPerform(Component component) {
                ReportInputParameter parameter = (ReportInputParameter) target.getSingleSelected();
                if (parameter != null) {
                    List<ReportInputParameter> inputParameters = getEditedEntity().getInputParameters();
                    int index = parameter.getPosition();
                    if (index > 0) {
                        ReportInputParameter previousParameter = null;
                        for (ReportInputParameter _param : inputParameters) {
                            if (_param.getPosition() == index - 1) {
                                previousParameter = _param;
                                break;
                            }
                        }
                        if (previousParameter != null) {
                            parameter.setPosition(previousParameter.getPosition());
                            previousParameter.setPosition(index);

                            sortParametersByPosition();
                        }
                    }
                }
            }

            @Override
            protected boolean isApplicable() {
                if (target != null) {
                    ReportInputParameter item = (ReportInputParameter) target.getSingleSelected();
                    if (item != null && parametersDc.getItem() == item) {
                        return item.getPosition() > 0 && isUpdatePermitted();
                    }
                }

                return false;
            }
        });

        paramDownButton.setAction(new ListAction("generalFrame.down") {
            @Override
            public void actionPerform(Component component) {
                ReportInputParameter parameter = (ReportInputParameter) target.getSingleSelected();
                if (parameter != null) {
                    List<ReportInputParameter> inputParameters = getEditedEntity().getInputParameters();
                    int index = parameter.getPosition();
                    if (index < parametersDc.getItems().size() - 1) {
                        ReportInputParameter nextParameter = null;
                        for (ReportInputParameter _param : inputParameters) {
                            if (_param.getPosition() == index + 1) {
                                nextParameter = _param;
                                break;
                            }
                        }
                        if (nextParameter != null) {
                            parameter.setPosition(nextParameter.getPosition());
                            nextParameter.setPosition(index);

                            sortParametersByPosition();
                        }
                    }
                }
            }

            @Override
            protected boolean isApplicable() {
                if (target != null) {
                    ReportInputParameter item = (ReportInputParameter) target.getSingleSelected();
                    if (item != null && parametersDc.getItem() == item) {
                        return item.getPosition() < parametersDc.getItems().size() - 1 && isUpdatePermitted();
                    }
                }

                return false;
            }
        });

        parametersTable.addAction(paramUpButton.getAction());
        parametersTable.addAction(paramDownButton.getAction());

        parametersDc.addItemPropertyChangeListener(e -> {
            if ("position".equals(e.getProperty())) {
                //todo
                //((DatasourceImplementation) parametersDc).modified(e.getItem());
            }
        });
    }

    protected boolean isUpdatePermitted() {
        return secureOperations.isEntityUpdatePermitted(metadata.getClass(Report.class), policyStore);
    }

    protected void sortParametersByPosition() {
        parametersDc.getSorter().sort(Sort.by(Sort.Direction.ASC, "position"));
    }

    protected void initValuesFormats() {
        CreateAction formatCreateAction = (CreateAction) actions.create(CreateAction.ID);
        formatCreateAction.setTarget(formatsTable);
        formatCreateAction.setOpenMode(OpenMode.DIALOG);
//        formatCreateAction.setInitialValuesSupplier(() ->
//                ParamsMap.of("report", getItem())
//        );
        formatsTable.addAction(formatCreateAction);

        RemoveAction removeAction = (RemoveAction) actions.create(RemoveAction.ID);
        removeAction.setTarget(formatsTable);
        formatsTable.addAction(removeAction);
        //todo autocommit
//        formatsTable.addAction(new RemoveAction(formatsTable, false));

        EditAction editAction = (EditAction) actions.create(EditAction.ID);
        editAction.setOpenMode(OpenMode.DIALOG);
        editAction.setTarget(formatsTable);
        formatsTable.addAction(editAction);
    }

    protected void initRoles() {
        //todo
//        rolesTable.addAction(new ExcludeAction(rolesTable, false, true) {
//            @Override
//            public boolean isEnabled() {
//                return super.isEnabled() && isUpdatePermitted();
//            }
//        });

        addRoleBtn.setAction(new AbstractAction("actions.Add") {
            @Override
            public void actionPerform(Component component) {
                if (lookupRolesDc.getItem() != null && !rolesDc.containsItem(Id.of(lookupRolesDc.getItem()).getValue())) {
                    rolesDc.getItems().add(lookupRolesDc.getItem());
                }
            }

            @Override
            public boolean isEnabled() {
                return super.isEnabled() && isUpdatePermitted();
            }
        });
    }

    protected void initHelpButtons() {
        localesTextField.setContextHelpIconClickHandler(e ->
                dialogs.createMessageDialog()
                        .withCaption(messages.getMessage("localeText"))
                        .withMessage(messages.getMessage("report.localeTextHelp"))
                        .withModal(false)
                        .withWidth("600px")
                        .show());
        validationScriptCodeEditor.setContextHelpIconClickHandler(e ->
                dialogs.createMessageDialog()
                        .withCaption(messages.getMessage("validationScript"))
                        .withMessage(messages.getMessage("crossFieldValidationScriptHelp"))
                        .withModal(false)
                        .withWidth("600px")
                        .show());
    }

    protected void initScreens() {
        RemoveAction removeAction = (RemoveAction) actions.create(RemoveAction.ID);
        removeAction.setTarget(screenTable);
        screenTable.addAction(removeAction);
        //todo autocommit
//        screenTable.addAction(new RemoveAction(screenTable, false));
        List<WindowInfo> windowInfoCollection = new ArrayList<>(windowConfig.getWindows());
        // sort by screenId
        screensHelper.sortWindowInfos(windowInfoCollection);

        Map<String, String> screens = new LinkedHashMap<>();
        for (WindowInfo windowInfo : windowInfoCollection) {
            String id = windowInfo.getId();
            String menuId = "menu-config." + id;
            String localeMsg = messages.getMessage(menuId);
            String title = menuId.equals(localeMsg) ? id : id + " ( " + localeMsg + " )";
            screens.put(title, id);
        }
        screenIdLookup.setOptionsMap(screens);

        addReportScreenBtn.setAction(new AbstractAction("actions.Add") {
            @Override
            public void actionPerform(Component component) {
                if (screenIdLookup.getValue() != null) {
                    String screenId = screenIdLookup.getValue();

                    boolean exists = false;
                    for (ReportScreen item : reportScreensDc.getItems()) {
                        if (screenId.equalsIgnoreCase(item.getScreenId())) {
                            exists = true;
                            break;
                        }
                    }

                    if (!exists) {
                        ReportScreen reportScreen = metadata.create(ReportScreen.class);
                        reportScreen.setReport(getEditedEntity());
                        reportScreen.setScreenId(screenId);
                        reportScreensDc.getItems().add(reportScreen);
                    }
                }
            }

            @Override
            public boolean isEnabled() {
                return super.isEnabled() && isUpdatePermitted();
            }
        });
    }

    private boolean isChildOrEqual(BandDefinition definition, BandDefinition child) {
        if (definition.equals(child)) {
            return true;
        } else if (child != null) {
            return isChildOrEqual(definition, child.getParentBandDefinition());
        } else {
            return false;
        }
    }

    protected void initGeneral() {
        invisibleFileUpload.addFileUploadSucceedListener(invisibleUpload -> {
            final ReportTemplate defaultTemplate = getEditedEntity().getDefaultTemplate();
            if (defaultTemplate != null) {
                if (!isTemplateWithoutFile(defaultTemplate)) {
                    //todo
//                    File file = fileUpload.getFile(invisibleFileUpload.getFileName());
//                    try {
//                        byte[] data = FileUtils.readFileToByteArray(file);
//                        defaultTemplate.setContent(data);
//                        defaultTemplate.setName(invisibleFileUpload.getFileName());
//                        templatesDc.modifyItem(defaultTemplate);
//                    } catch (IOException e) {
//                        throw new RuntimeException(String.format(
//                                "An error occurred while uploading file for template [%s]",
//                                defaultTemplate.getCode()));
//                    }
                } else {
                    notifications.create(Notifications.NotificationType.HUMANIZED)
                            .withCaption(messages.getMessage("notification.fileIsNotAllowedForSpecificTypes"))
                            .show();
                }
            } else {
                notifications.create(Notifications.NotificationType.HUMANIZED)
                        .withCaption(messages.getMessage("notification.defaultTemplateIsEmpty"))
                        .show();
            }
        });

        treeDc.addItemChangeListener(e -> {
            bandEditor.setBandDefinition(e.getItem());
            bandEditor.setEnabled(e.getItem() != null);
            //todo
            //availableParentBandsDc.clear();
            if (e.getItem() != null) {
                for (BandDefinition bandDefinition : bandsDc.getItems()) {
                    if (!isChildOrEqual(e.getItem(), bandDefinition) ||
                            Objects.equals(e.getItem().getParentBandDefinition(), bandDefinition)) {
                        //todo
                        //availableParentBandsDc.getItems(bandDefinition);
                    }
                }
            }
        });

        bandEditor.getBandDefinitionDs().addItemPropertyChangeListener(e -> {
            if ("parentBandDefinition".equals(e.getProperty())) {
                BandDefinition previousParent = (BandDefinition) e.getPrevValue();
                BandDefinition parent = (BandDefinition) e.getValue();

                if (e.getValue() == e.getItem()) {
                    e.getItem().setParentBandDefinition(previousParent);
                } else {
                    //todo
                    //treeDc.refresh();
                    previousParent.getChildrenBandDefinitions().remove(e.getItem());
                    parent.getChildrenBandDefinitions().add(e.getItem());
                }

                if (e.getPrevValue() != null) {
                    orderBandDefinitions(previousParent);
                }

                if (e.getValue() != null) {
                    orderBandDefinitions(parent);
                }
            }
            //todo
            //treeDc.modifyItem(e.getItem());
        });


//        propertiesFieldGroup.add("defaultTemplate", new FieldGroup.CustomFieldGenerator() {
//            @Override
//            public Component generateField(Datasource datasource, String propertyId) {
//                EntityComboBox entityComboBox = uiComponents.create(EntityComboBox.class);
//
////                entityComboBox.setOptionsDatasource(templatesDc);
////                entityComboBox.setDatasource(datasource, propertyId);
//
//                entityComboBox.addAction(new AbstractAction("download") {
//
//                    @Override
//                    public String getDescription() {
//                        return messages.getMessage("description.downloadTemplate");
//                    }
//
//                    @Override
//                    public String getCaption() {
//                        return null;
//                    }
//
//                    @Override
//                    public String getIcon() {
//                        return "icons/reports-template-download.png";
//                    }
//
//                    @Override
//                    public void actionPerform(Component component) {
//                        ReportTemplate defaultTemplate = getEditedEntity().getDefaultTemplate();
//                        if (defaultTemplate != null) {
//                            if (defaultTemplate.isCustom()) {
//                                notifications.create(Notifications.NotificationType.HUMANIZED)
//                                        .withCaption(messages.getMessage("unableToSaveTemplateWhichDefinedWithClass"))
//                                        .show();
//                            } else if (isTemplateWithoutFile(defaultTemplate)) {
//                                notifications.create(Notifications.NotificationType.HUMANIZED)
//                                        .withCaption(messages.getMessage("notification.fileIsNotAllowedForSpecificTypes"))
//                                        .show();
//                            } else {
//                                byte[] reportTemplate = defaultTemplate.getContent();
//                                downloader.download(new ByteArrayDataProvider(reportTemplate, uiProperties.getSaveExportedByteArrayDataThresholdBytes(), coreProperties.getTempDir()),
//                                        defaultTemplate.getName(), DownloadFormat.getByExtension(defaultTemplate.getExt()));
//                            }
//                        } else {
//                            notifications.create(Notifications.NotificationType.HUMANIZED)
//                                    .withCaption(messages.getMessage("notification.defaultTemplateIsEmpty"))
//                                    .show();
//                        }
//
//                        entityComboBox.focus();
//                    }
//                });
//
//                entityComboBox.addAction(new AbstractAction("upload") {
//                    @Override
//                    public String getDescription() {
//                        return messages.getMessage("description.uploadTemplate");
//                    }
//
//                    @Override
//                    public String getCaption() {
//                        return null;
//                    }
//
//                    @Override
//                    public String getIcon() {
//                        return "icons/reports-template-upload.png";
//                    }
//
//                    @Override
//                    public void actionPerform(Component component) {
//                        final ReportTemplate defaultTemplate = getEditedEntity().getDefaultTemplate();
//                        if (defaultTemplate != null) {
//                            if (!isTemplateWithoutFile(defaultTemplate)) {
//                                FileUploadDialog fileUploadDialog = (FileUploadDialog) screens.create("fileUploadDialog", OpenMode.DIALOG);
//                                fileUploadDialog.addAfterCloseListener(event -> {
//                                    StandardCloseAction standardCloseAction = (StandardCloseAction) event.getCloseAction();
//                                    if (Window.COMMIT_ACTION_ID.equals(standardCloseAction.getActionId())) {
//                                        //todo
////                                        File file = fileUpload.getFile(fileUploadDialog.getFileId());
////                                        try {
////                                            byte[] data = FileUtils.readFileToByteArray(file);
////                                            defaultTemplate.setContent(data);
////                                            defaultTemplate.setName(fileUploadDialog.getFileName());
////                                            //todo
////                                            //templatesDc.modifyItem(defaultTemplate);
////                                        } catch (IOException e) {
////                                            throw new RuntimeException(String.format(
////                                                    "An error occurred while uploading file for template [%s]",
////                                                    defaultTemplate.getCode()));
////                                        }
//                                    }
//                                    entityComboBox.focus();
//                                });
//                                fileUploadDialog.show();
//
//                            } else {
//                                notifications.create(Notifications.NotificationType.HUMANIZED)
//                                        .withCaption(messages.getMessage("notification.fileIsNotAllowedForSpecificTypes"))
//                                        .show();
//                            }
//                        } else {
//                            notifications.create(Notifications.NotificationType.HUMANIZED)
//                                    .withCaption(messages.getMessage("notification.defaultTemplateIsEmpty"))
//                                    .show();
//                        }
//                    }
//
//                    @Override
//                    public boolean isEnabled() {
//                        return super.isEnabled() && isUpdatePermitted();
//                    }
//                });
//
//                entityComboBox.addAction(new AbstractAction("create") {
//
//                    @Override
//                    public String getDescription() {
//                        return messages.getMessage("description.createTemplate");
//                    }
//
//                    @Override
//                    public String getIcon() {
//                        return "icons/plus-btn.png";
//                    }
//
//                    @Override
//                    public void actionPerform(Component component) {
//                        ReportTemplate template = metadata.create(ReportTemplate.class);
//                        template.setReport(getEditedEntity());
//
//                        StandardEditor editor = (StandardEditor) screenBuilders.editor(entityComboBox)
//                                .withScreenId("report_ReportTemplate.edit")
//                                .withContainer(templatesDc)
//                                .editEntity(template)
//                                .build();
//                        editor.addAfterCloseListener(e -> {
//                            StandardCloseAction standardCloseAction = (StandardCloseAction) e.getCloseAction();
//                            if (Window.COMMIT_ACTION_ID.equals(standardCloseAction.getActionId())) {
//                                ReportTemplate item = (ReportTemplate) editor.getEditedEntity();
//                                templatesDc.getItems().add(item);
//                                getEditedEntity().setDefaultTemplate(item);
//                                //Workaround to disable button after default template setting
//                                Action defaultTemplate = templatesTable.getActionNN("defaultTemplate");
//                                defaultTemplate.refreshState();
//                            }
//                            entityComboBox.focus();
//                        });
//
//                        editor.show();
//                    }
//
//                    @Override
//                    public boolean isEnabled() {
//                        return super.isEnabled() && isUpdatePermitted();
//                    }
//                });
//
//                entityComboBox.addAction(new AbstractAction("edit") {
//                    @Override
//                    public String getDescription() {
//                        return messages.getMessage("description.editTemplate");
//                    }
//
//                    @Override
//                    public String getIcon() {
//                        return "icons/reports-template-view.png";
//                    }
//
//                    @Override
//                    public void actionPerform(Component component) {
//                        ReportTemplate defaultTemplate = getEditedEntity().getDefaultTemplate();
//                        if (defaultTemplate != null) {
//                            StandardEditor editor = (StandardEditor) screenBuilders.editor(entityComboBox)
//                                    .withScreenId("report_ReportTemplate.edit")
//                                    .withOpenMode(OpenMode.DIALOG)
//                                    .withContainer(templatesDc)
//                                    .build();
//                            editor.addAfterCloseListener(e -> {
//                                StandardCloseAction standardCloseAction = (StandardCloseAction) e.getCloseAction();
//                                if (Window.COMMIT_ACTION_ID.equals(standardCloseAction.getActionId())) {
//                                    ReportTemplate item = (ReportTemplate) editor.getEditedEntity();
//                                    getEditedEntity().setDefaultTemplate(item);
//                                    //todo
//                                    //templatesDc.modifyItem(item);
//                                }
//                                entityComboBox.focus();
//                            });
//                            editor.show();
//                        } else {
//                            notifications.create(Notifications.NotificationType.HUMANIZED)
//                                    .withCaption(messages.getMessage("notification.defaultTemplateIsEmpty"))
//                                    .show();
//                        }
//                    }
//
//                    @Override
//                    public boolean isEnabled() {
//                        return super.isEnabled() && isUpdatePermitted();
//                    }
//                });
//
//                entityComboBox.addValueChangeListener(event -> {
//                    setupDropZoneForTemplate();
//                });
//
//                entityComboBox.setEditable(isUpdatePermitted());
//
//                return entityComboBox;
//            }
//        });

        ((HierarchicalPropertyDatasourceImpl) treeDc).setSortPropertyName("position");

        createBandDefinitionButton.setAction(new AbstractAction("create") {
            @Override
            public String getDescription() {
                return messages.getMessage("description.createBand");
            }

            @Override
            public String getCaption() {
                return "";
            }

            @Override
            public void actionPerform(Component component) {
                BandDefinition parentDefinition = treeDc.getItem();
                Report report = getEditedEntity();
                // Use root band as parent if no items selected
                if (parentDefinition == null) {
                    parentDefinition = report.getRootBandDefinition();
                }
                if (parentDefinition.getChildrenBandDefinitions() == null) {
                    parentDefinition.setChildrenBandDefinitions(new ArrayList<>());
                }

                //
                orderBandDefinitions(parentDefinition);

                BandDefinition newBandDefinition = metadata.create(BandDefinition.class);
                newBandDefinition.setName("newBand" + (parentDefinition.getChildrenBandDefinitions().size() + 1));
                newBandDefinition.setOrientation(Orientation.HORIZONTAL);
                newBandDefinition.setParentBandDefinition(parentDefinition);
                if (parentDefinition.getChildrenBandDefinitions() != null) {
                    newBandDefinition.setPosition(parentDefinition.getChildrenBandDefinitions().size());
                } else {
                    newBandDefinition.setPosition(0);
                }
                newBandDefinition.setReport(report);
                parentDefinition.getChildrenBandDefinitions().add(newBandDefinition);

                treeDc.getItems().add(newBandDefinition);

                //todo
                //treeDc.refresh();
                bandTree.expandTree();
                bandTree.setSelected(newBandDefinition);//let's try and see if it increases usability

                bandTree.focus();
            }

            @Override
            public boolean isEnabled() {
                return super.isEnabled() && isUpdatePermitted();
            }
        });

        //removeBandDefinitionButton.setAction(new RemoveAction((ListComponent) bandTree, false, "generalFrame.removeBandDefinition") {
        removeBandDefinitionButton.setAction(new RemoveAction("generalFrame.removeBandDefinition") {
            @Override
            public String getDescription() {
                return messages.getMessage("description.removeBand");
            }

            @Override
            public String getCaption() {
                return "";
            }

            @Override
            protected boolean isApplicable() {
                if (target != null) {
                    Object selectedItem = target.getSingleSelected();
                    if (selectedItem != null) {
                        return !Objects.equals(getEditedEntity().getRootBandDefinition(), selectedItem);
                    }
                }

                return false;
            }

            protected void doRemove(Set selected, boolean autocommit) {
                if (selected != null) {
                    removeChildrenCascade(selected);
                    for (Object object : selected) {
                        BandDefinition definition = (BandDefinition) object;
                        if (definition.getParentBandDefinition() != null) {
                            orderBandDefinitions(((BandDefinition) object).getParentBandDefinition());
                        }
                    }
                }
                bandTree.focus();
            }

            private void removeChildrenCascade(Collection selected) {
                for (Object o : selected) {
                    BandDefinition definition = (BandDefinition) o;
                    BandDefinition parentDefinition = definition.getParentBandDefinition();
                    if (parentDefinition != null) {
                        definition.getParentBandDefinition().getChildrenBandDefinitions().remove(definition);
                    }

                    if (definition.getChildrenBandDefinitions() != null) {
                        removeChildrenCascade(new ArrayList<>(definition.getChildrenBandDefinitions()));
                    }

                    if (definition.getDataSets() != null) {
                        treeDc.setItem(definition);
                        for (DataSet dataSet : new ArrayList<>(definition.getDataSets())) {
                            if (entityStates.isNew(dataSet)) {
                                //todo
                                //dataSetsDc.removeItem(dataSet);
                            }
                        }
                    }
                    //todo
                    //treeDc.removeItem(definition);
                }
            }
        });

        bandUpButton.setAction(new ListAction("generalFrame.up") {
            @Override
            public String getDescription() {
                return messages.getMessage("description.moveUp");
            }

            @Override
            public String getCaption() {
                return "";
            }

            @Override
            public void actionPerform(Component component) {
                BandDefinition definition = (BandDefinition) target.getSingleSelected();
                if (definition != null && definition.getParentBandDefinition() != null) {
                    BandDefinition parentDefinition = definition.getParentBandDefinition();
                    List<BandDefinition> definitionsList = parentDefinition.getChildrenBandDefinitions();
                    int index = definitionsList.indexOf(definition);
                    if (index > 0) {
                        BandDefinition previousDefinition = definitionsList.get(index - 1);
                        definition.setPosition(definition.getPosition() - 1);
                        previousDefinition.setPosition(previousDefinition.getPosition() + 1);

                        definitionsList.set(index, previousDefinition);
                        definitionsList.set(index - 1, definition);

                        //todo
                        //treeDc.refresh();
                    }
                }
            }

            @Override
            protected boolean isApplicable() {
                if (target != null) {
                    BandDefinition selectedItem = (BandDefinition) target.getSingleSelected();
                    return selectedItem != null && selectedItem.getPosition() > 0 && isUpdatePermitted();
                }

                return false;
            }
        });

        bandDownButton.setAction(new ListAction("generalFrame.down") {
            @Override
            public String getDescription() {
                return messages.getMessage("description.moveDown");
            }

            @Override
            public String getCaption() {
                return "";
            }

            @Override
            public void actionPerform(Component component) {
                BandDefinition definition = (BandDefinition) target.getSingleSelected();
                if (definition != null && definition.getParentBandDefinition() != null) {
                    BandDefinition parentDefinition = definition.getParentBandDefinition();
                    List<BandDefinition> definitionsList = parentDefinition.getChildrenBandDefinitions();
                    int index = definitionsList.indexOf(definition);
                    if (index < definitionsList.size() - 1) {
                        BandDefinition nextDefinition = definitionsList.get(index + 1);
                        definition.setPosition(definition.getPosition() + 1);
                        nextDefinition.setPosition(nextDefinition.getPosition() - 1);

                        definitionsList.set(index, nextDefinition);
                        definitionsList.set(index + 1, definition);

                        //todo
                        //treeDc.refresh();
                    }
                }
            }

            @Override
            protected boolean isApplicable() {
                if (target != null) {
                    BandDefinition bandDefinition = (BandDefinition) target.getSingleSelected();
                    if (bandDefinition != null) {
                        BandDefinition parent = bandDefinition.getParentBandDefinition();
                        return parent != null &&
                                parent.getChildrenBandDefinitions() != null &&
                                bandDefinition.getPosition() < parent.getChildrenBandDefinitions().size() - 1
                                && isUpdatePermitted();
                    }
                }
                return false;
            }
        });

        bandTree.addAction(createBandDefinitionButton.getAction());
        bandTree.addAction(removeBandDefinitionButton.getAction());
        bandTree.addAction(bandUpButton.getAction());
        bandTree.addAction(bandDownButton.getAction());

        run.setAction(new AbstractAction("button.run") {
            @Override
            public void actionPerform(Component component) {
//                if (validateAll()) {
//                    getEditedEntity().setIsTmp(true);
//                    Map<String, Object> params = ParamsMap.of("report", getEditedEntity());
//
//                    Screen screen = screens.create("report_inputParameters", OpenMode.DIALOG, new MapScreenOptions(params));
//                    screen.addAfterCloseListener(e -> bandTree.focus());
//                    screen.show();
//                }
            }
        });
    }

    protected void setupDropZoneForTemplate() {
        final ReportTemplate defaultTemplate = getEditedEntity().getDefaultTemplate();
        if (defaultTemplate != null) {
            invisibleFileUpload.setDropZone(new UploadField.DropZone(reportFields));
        } else {
            invisibleFileUpload.setDropZone(null);
        }
    }
//
//    @Override
//    public boolean validateAll() {
//        return super.validateAll() && validateInputOutputFormats();
//    }

    protected boolean validateInputOutputFormats() {
        ReportTemplate template = getEditedEntity().getDefaultTemplate();
        if (template != null && !template.isCustom()
                && template.getReportOutputType() != ReportOutputType.CHART
                && template.getReportOutputType() != ReportOutputType.TABLE
                && template.getReportOutputType() != ReportOutputType.PIVOT_TABLE) {
            String inputType = template.getExt();
            if (!ReportPrintHelper.getInputOutputTypesMapping().containsKey(inputType) ||
                    !ReportPrintHelper.getInputOutputTypesMapping().get(inputType).contains(template.getReportOutputType())) {
                notifications.create(Notifications.NotificationType.TRAY)
                        .withCaption(messages.getMessage("inputOutputTypesError"))
                        .show();
                return false;
            }
        }
        return true;
    }

    protected void initTemplates() {
        CreateAction templateCreateAction = (CreateAction) actions.create(CreateAction.ID);
        templateCreateAction.setOpenMode(OpenMode.DIALOG);
        templateCreateAction.setInitializer(e ->
                ParamsMap.of("report", getEditedEntity())
        );
        templateCreateAction.setAfterCommitHandler(entity -> {
            ReportTemplate reportTemplate = (ReportTemplate) entity;
            ReportTemplate defaultTemplate = getEditedEntity().getDefaultTemplate();
            if (defaultTemplate == null) {
                getEditedEntity().setDefaultTemplate(reportTemplate);
            }
        });
        templatesTable.addAction(templateCreateAction);


        EditAction editAction = (EditAction) actions.create(EditAction.ID);
        editAction.setOpenMode(OpenMode.DIALOG);
        editAction.setAfterCommitHandler(event -> {
            ReportTemplate reportTemplate = (ReportTemplate) event;
            ReportTemplate defaultTemplate = getEditedEntity().getDefaultTemplate();
            if (defaultTemplate != null && defaultTemplate.equals(reportTemplate)) {
                getEditedEntity().setDefaultTemplate(reportTemplate);
            }
        });
        templatesTable.addAction(editAction);


        RemoveAction removeAction = (RemoveAction) actions.create(RemoveAction.ID);
        removeAction.setAfterActionPerformedHandler(event -> {
            Set selected = (Set) event;

            Report report = getEditedEntity();
            ReportTemplate defaultTemplate = report.getDefaultTemplate();
            if (defaultTemplate != null && selected.contains(defaultTemplate)) {
                ReportTemplate newDefaultTemplate = null;

                if (templatesDc.getItems().size() == 1) {
                    newDefaultTemplate = templatesDc.getItems().iterator().next();
                }

                report.setDefaultTemplate(newDefaultTemplate);
            }
        });
        templatesTable.addAction(removeAction);

        templatesTable.addAction(new ListAction("defaultTemplate") {
            @Override
            public String getCaption() {
                return messages.getMessage("report.defaultTemplate");
            }

            @Override
            public void actionPerform(Component component) {
                ReportTemplate template = (ReportTemplate) target.getSingleSelected();
                if (template != null) {
                    getEditedEntity().setDefaultTemplate(template);
                }

                refreshState();

                templatesTable.focus();
            }

            @Override
            protected boolean isApplicable() {
                if (target != null) {
                    Object selectedItem = target.getSingleSelected();
                    if (selectedItem != null) {
                        return !Objects.equals(getEditedEntity().getDefaultTemplate(), selectedItem);
                    }
                }

                return false;
            }

            @Override
            public boolean isEnabled() {
                return super.isEnabled() && isUpdatePermitted();
            }
        });
        templatesTable.addAction(new ItemTrackingAction("copy") {
            @Override
            public void actionPerform(Component component) {
                ReportTemplate template = (ReportTemplate) target.getSingleSelected();
                if (template != null) {

                    ReportTemplate copy = metadataTools.copy(template);
                    copy.setId(UuidProvider.createUuid());

                    String copyNamingPattern = messages.getMessage("template.copyNamingPattern");
                    String copyCode = String.format(copyNamingPattern, StringUtils.isEmpty(copy.getCode()) ? StringUtils.EMPTY : copy.getCode());

                    CollectionContainer<Object> container = ((ContainerDataUnit) target.getItems()).getContainer();

                    List<String> codes = container.getItems().stream()
                            .map(o -> ((ReportTemplate) o).getCode())
                            .filter(o -> !StringUtils.isEmpty(o))
                            .collect(Collectors.toList());
                    if (codes.contains(copyCode)) {
                        String code = copyCode;
                        int i = 0;
                        while ((codes.contains(code))) {
                            i += 1;
                            code = copyCode + " " + i;
                        }
                        copyCode = code;
                    }
                    copy.setCode(copyCode);

                    container.getItems().add(copy);
                }
            }

            @Override
            public boolean isEnabled() {
                return super.isEnabled() && isUpdatePermitted();
            }
        });
    }

    protected void orderParameters() {
        if (getEditedEntity().getInputParameters() == null) {
            getEditedEntity().setInputParameters(new ArrayList<>());
        }

        for (int i = 0; i < getEditedEntity().getInputParameters().size(); i++) {
            getEditedEntity().getInputParameters().get(i).setPosition(i);
        }
    }

    protected void orderBandDefinitions(BandDefinition parent) {
        if (parent.getChildrenBandDefinitions() != null) {
            List<BandDefinition> childrenBandDefinitions = parent.getChildrenBandDefinitions();
            for (int i = 0, childrenBandDefinitionsSize = childrenBandDefinitions.size(); i < childrenBandDefinitionsSize; i++) {
                BandDefinition bandDefinition = childrenBandDefinitions.get(i);
                bandDefinition.setPosition(i);
            }
        }
    }

    @Subscribe
    protected void onBeforeCommit(BeforeCommitChangesEvent event) {
        addCommitListeners();

        if (entityStates.isNew(getEditedEntity())) {
            ((CollectionPropertyDatasourceImpl) treeDc).setModified(true);
        }
    }

    protected void addCommitListeners() {
        String xml = reportService.convertToString(getEditedEntity());
        getEditedEntity().setXml(xml);

//        reportDc.getDsContext().addBeforeCommitListener(context -> {
//            context.getCommitInstances()
//                    .removeIf(entity ->
//                            !(entity instanceof Report || entity instanceof ReportTemplate)
//                    );
//        });
    }

    //    @Override
    protected void postValidate(ValidationErrors errors) {
        if (getEditedEntity().getRootBand() == null) {
            errors.add(messages.getMessage("error.rootBandNull"));
        }

        if (CollectionUtils.isNotEmpty(getEditedEntity().getRootBandDefinition().getChildrenBandDefinitions())) {
            Multimap<String, BandDefinition> names = ArrayListMultimap.create();
            names.put(getEditedEntity().getRootBand().getName(), getEditedEntity().getRootBandDefinition());

            for (BandDefinition band : getEditedEntity().getRootBandDefinition().getChildrenBandDefinitions()) {
                validateBand(errors, band, names);
            }

            checkForNameDuplication(errors, names);
        }
    }

    protected void checkForNameDuplication(ValidationErrors errors, Multimap<String, BandDefinition> names) {
        for (String name : names.keySet()) {
            Collection<BandDefinition> bandDefinitionsWithsSameNames = names.get(name);
            if (bandDefinitionsWithsSameNames != null && bandDefinitionsWithsSameNames.size() > 1) {
                errors.add(messages.formatMessage("error.bandNamesDuplicated", name));
            }
        }
    }

    protected void validateBand(ValidationErrors errors, BandDefinition band, Multimap<String, BandDefinition> names) {
        names.put(band.getName(), band);

        if (StringUtils.isBlank(band.getName())) {
            errors.add(messages.getMessage("error.bandNameNull"));
        }

        if (band.getBandOrientation() == BandOrientation.UNDEFINED) {
            errors.add(messages.formatMessage("error.bandOrientationNull", band.getName()));
        }

        if (CollectionUtils.isNotEmpty(band.getDataSets())) {
            for (DataSet dataSet : band.getDataSets()) {
                if (StringUtils.isBlank(dataSet.getName())) {
                    errors.add(messages.getMessage("error.dataSetNameNull"));
                }

                if (dataSet.getType() == null) {
                    errors.add(messages.formatMessage("error.dataSetTypeNull", dataSet.getName()));
                }

                if (dataSet.getType() == DataSetType.GROOVY
                        || dataSet.getType() == DataSetType.SQL
                        || dataSet.getType() == DataSetType.JPQL) {
                    if (StringUtils.isBlank(dataSet.getScript())) {
                        errors.add(messages.formatMessage("error.dataSetScriptNull", dataSet.getName()));
                    }
                } else if (dataSet.getType() == DataSetType.JSON) {
                    if (StringUtils.isBlank(dataSet.getJsonSourceText()) && dataSet.getJsonSourceType() != JsonSourceType.PARAMETER) {
                        errors.add(messages.formatMessage("error.jsonDataSetScriptNull", dataSet.getName()));
                    }
                }
            }
        }

        if (CollectionUtils.isNotEmpty(band.getChildrenBandDefinitions())) {
            for (BandDefinition child : band.getChildrenBandDefinitions()) {
                validateBand(errors, child, names);
            }
        }
    }

    protected void initValidationScriptGroupBoxCaption() {
        setValidationScriptGroupBoxCaption(reportDc.getItem().getValidationOn());

        reportDc.addItemPropertyChangeListener(e -> {
            boolean validationOnChanged = e.getProperty().equalsIgnoreCase("validationOn");

            if (validationOnChanged) {
                setValidationScriptGroupBoxCaption(e.getItem().getValidationOn());
            }
        });
    }

    protected void setValidationScriptGroupBoxCaption(Boolean onOffFlag) {
        if (BooleanUtils.isTrue(onOffFlag)) {
            validationScriptGroupBox.setCaption(messages.getMessage("report.validationScriptOn"));
        } else {
            validationScriptGroupBox.setCaption(messages.getMessage("report.validationScriptOff"));
        }
    }

    protected boolean isTemplateWithoutFile(ReportTemplate template) {
        return template.getOutputType() == CubaReportOutputType.chart ||
                template.getOutputType() == CubaReportOutputType.table ||
                template.getOutputType() == CubaReportOutputType.pivot;


    }
}