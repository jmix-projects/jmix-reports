package io.jmix.reportsui.gui.report.edit.tabs;

import io.jmix.core.EntityStates;
import io.jmix.core.Messages;
import io.jmix.core.Metadata;
import io.jmix.core.Sort;
import io.jmix.reports.entity.*;
import io.jmix.reportsui.gui.definition.edit.BandDefinitionEditor;
import io.jmix.security.constraint.PolicyStore;
import io.jmix.security.constraint.SecureOperations;
import io.jmix.ui.Notifications;
import io.jmix.ui.action.Action;
import io.jmix.ui.component.Button;
import io.jmix.ui.component.FileUploadField;
import io.jmix.ui.component.Tree;
import io.jmix.ui.model.CollectionContainer;
import io.jmix.ui.model.CollectionPropertyContainer;
import io.jmix.ui.model.InstanceContainer;
import io.jmix.ui.model.InstanceLoader;
import io.jmix.ui.screen.*;
import org.springframework.beans.factory.annotation.Autowired;

import javax.inject.Named;
import java.util.*;

@UiController("report_ReportEditGeneral.fragment")
@UiDescriptor("general.xml")
public class GeneralFragment extends ScreenFragment {

    @Named("serviceTree")
    protected Tree<BandDefinition> bandTree;

    @Autowired
    protected InstanceLoader<Report> reportDl;

    @Autowired
    protected InstanceContainer<Report> reportDc;

    @Autowired
    protected CollectionContainer<BandDefinition> bandsDc;

    @Autowired
    private CollectionContainer<BandDefinition> availableParentBandsDc;

    @Autowired
    protected Metadata metadata;

    @Autowired
    protected SecureOperations secureOperations;

    @Autowired
    protected PolicyStore policyStore;

    @Autowired
    protected CollectionPropertyContainer<DataSet> dataSetsDc;

    @Autowired
    protected EntityStates entityStates;

    @Autowired
    protected Notifications notifications;

    @Autowired
    private FileUploadField invisibleFileUpload;

    @Autowired
    protected Messages messages;

    @Autowired
    private BandDefinitionEditor bandEditor;

    @Autowired
    protected Button up;

    @Autowired
    protected Button down;

    @Subscribe
    public void onInit(InitEvent event) {
        invisibleFileUpload.addFileUploadSucceedListener(invisibleUpload -> {
            final ReportTemplate defaultTemplate = reportDc.getItem().getDefaultTemplate();
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
                            .withCaption(messages.getMessage(getClass(), "notification.fileIsNotAllowedForSpecificTypes"))
                            .show();
                }
            } else {
                notifications.create(Notifications.NotificationType.HUMANIZED)
                        .withCaption(messages.getMessage(getClass(), "notification.defaultTemplateIsEmpty"))
                        .show();
            }
        });

        bandsDc.addItemChangeListener(e -> {
            //bandEditor.setBandDefinition(e.getItem());
            bandEditor.setEnabled(e.getItem() != null);
            availableParentBandsDc.getMutableItems().clear();
            if (e.getItem() != null) {
                for (BandDefinition bandDefinition : bandsDc.getItems()) {
                    if (!isChildOrEqual(e.getItem(), bandDefinition) ||
                            Objects.equals(e.getItem().getParentBandDefinition(), bandDefinition)) {
                        availableParentBandsDc.getMutableItems().add(bandDefinition);
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
    }

    protected void sortBandDefinitionsTableByPosition() {
        bandsDc.getSorter().sort(Sort.by(Sort.Direction.ASC, "position"));
    }

    protected void refreshMoveButtonEnabled() {
        up.setEnabled(isUpButtonEnabled());
        down.setEnabled(isDownButtonEnabled());
    }

    @Subscribe("serviceTree.create")
    public void onServiceTreeCreate(Action.ActionPerformedEvent event) {
        BandDefinition parentDefinition = bandsDc.getItem();
        Report report = reportDc.getItem();
        // Use root band as parent if no items selected
        if (parentDefinition == null) {
            parentDefinition = report.getRootBandDefinition();
        }
        if (parentDefinition.getChildrenBandDefinitions() == null) {
            parentDefinition.setChildrenBandDefinitions(new ArrayList<>());
        }


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

        bandsDc.getMutableItems().add(newBandDefinition);

        bandTree.expandTree();
        bandTree.setSelected(newBandDefinition);//let's try and see if it increases usability

        bandTree.focus();
    }

    @Install(to = "serviceTree.create", subject = "enabledRule")
    private boolean serviceTreeCreateEnabledRule() {
        return isUpdatePermitted();
    }

    @Subscribe("serviceTree.remove")
    public void onServiceTreeRemove(Action.ActionPerformedEvent event) {
        Set<BandDefinition> selected = bandTree.getSelected();
        removeChildrenCascade(selected);
        for (Object object : selected) {
            BandDefinition definition = (BandDefinition) object;
            if (definition.getParentBandDefinition() != null) {
                orderBandDefinitions(((BandDefinition) object).getParentBandDefinition());
            }
        }
        bandTree.focus();
    }

    @Install(to = "serviceTree.remove", subject = "enabledRule")
    private boolean serviceTreeRemoveEnabledRule() {
        Object selectedItem = bandTree.getSingleSelected();
        if (selectedItem != null) {
            return !Objects.equals(reportDc.getItem().getRootBandDefinition(), selectedItem);
        }

        return false;
    }

    @Subscribe("serviceTree")
    public void onServiceTreeSelection(Tree.SelectionEvent<BandDefinition> event) {
        refreshMoveButtonEnabled();
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
                bandsDc.setItem(definition);
                for (DataSet dataSet : new ArrayList<>(definition.getDataSets())) {
                    if (entityStates.isNew(dataSet)) {
                        dataSetsDc.getMutableItems().remove(dataSet);
                    }
                }
            }
            bandsDc.getMutableItems().remove(definition);
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

    @Subscribe("up")
    public void onUpClick(Button.ClickEvent event) {
        BandDefinition definition = bandTree.getSingleSelected();
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

                sortBandDefinitionsTableByPosition();
                refreshMoveButtonEnabled();
            }
        }
    }

    protected boolean isUpButtonEnabled() {
        if (bandTree != null) {
            BandDefinition selectedItem = bandTree.getSingleSelected();
            return selectedItem != null && selectedItem.getPosition() > 0 && isUpdatePermitted();
        }
        return false;
    }

    @Subscribe("down")
    public void onDownClick(Button.ClickEvent event) {
        BandDefinition definition = bandTree.getSingleSelected();
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

                sortBandDefinitionsTableByPosition();
                refreshMoveButtonEnabled();
            }
        }
    }


    protected boolean isDownButtonEnabled() {
        if (bandTree != null) {
            BandDefinition bandDefinition = bandTree.getSingleSelected();
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

    protected boolean isUpdatePermitted() {
        return secureOperations.isEntityUpdatePermitted(metadata.getClass(Report.class), policyStore);
    }

    protected boolean isTemplateWithoutFile(ReportTemplate template) {
        return template.getOutputType() == JmixReportOutputType.chart ||
                template.getOutputType() == JmixReportOutputType.table ||
                template.getOutputType() == JmixReportOutputType.pivot;
    }

    protected boolean isChildOrEqual(BandDefinition definition, BandDefinition child) {
        if (definition.equals(child)) {
            return true;
        } else if (child != null) {
            return isChildOrEqual(definition, child.getParentBandDefinition());
        } else {
            return false;
        }
    }
}
