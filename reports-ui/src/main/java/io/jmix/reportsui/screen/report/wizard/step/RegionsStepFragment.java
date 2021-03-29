/*
 * Copyright 2021 Haulmont.
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

package io.jmix.reportsui.screen.report.wizard.step;

import io.jmix.core.MessageTools;
import io.jmix.core.Messages;
import io.jmix.core.Metadata;
import io.jmix.core.MetadataTools;
import io.jmix.core.metamodel.model.MetaClass;
import io.jmix.reports.entity.Report;
import io.jmix.reports.entity.ReportType;
import io.jmix.reports.entity.wizard.*;
import io.jmix.reportsui.screen.ReportGuiManager;
import io.jmix.reportsui.screen.report.wizard.region.EntityTreeLookup;
import io.jmix.reportsui.screen.report.wizard.region.RegionEditor;
import io.jmix.reportsui.screen.report.wizard.step.StepFragment;
import io.jmix.ui.Dialogs;
import io.jmix.ui.Notifications;
import io.jmix.ui.ScreenBuilders;
import io.jmix.ui.UiComponents;
import io.jmix.ui.action.AbstractAction;
import io.jmix.ui.action.DialogAction;
import io.jmix.ui.component.*;
import io.jmix.ui.model.CollectionPropertyContainer;
import io.jmix.ui.model.InstanceContainer;
import io.jmix.ui.screen.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@UiController("report_Region.fragment")
@UiDescriptor("intermediate-regions-frame.xml")
public class RegionsStepFragment extends StepFragment {
    protected static final String ADD_TABULATED_REGION_ACTION_ID = "tabulatedRegion";
    protected static final String ADD_SIMPLE_REGION_ACTION_ID = "simpleRegion";


    @Autowired
    protected Button addRegionDisabledBtn;

    @Autowired
    protected Button addTabulatedRegionDisabledBtn;

    @Autowired
    protected Button addSimpleRegionBtn;

    @Autowired
    protected Button addTabulatedRegionBtn;

    @Autowired
    protected PopupButton addRegionPopupBtn;

    @Autowired
    protected Button moveUpBtn;

    @Autowired
    protected Button moveDownBtn;

    @Autowired
    protected Button removeBtn;

    @Autowired
    protected Table<ReportRegion> regionsTable;

    @Autowired
    protected BoxLayout buttonsBox;

    @Autowired
    protected Messages messages;

    @Autowired
    protected Metadata metadata;

    @Autowired
    protected Dialogs dialogs;

    @Autowired
    protected Notifications notifications;

    @Autowired
    protected ScreenBuilders screenBuilders;

    @Autowired
    protected UiComponents uiComponents;

    @Autowired
    protected MessageTools messageTools;

    @Autowired
    protected ReportGuiManager reportGuiManager;

    @Autowired
    private InstanceContainer<ReportData> reportDataDc;

    @Autowired
    private CollectionPropertyContainer<ReportRegion> reportRegionsDc;

    @Autowired
    protected Button runBtn;

    protected Report lastGeneratedTmpReport;

    protected boolean entityTreeHasSimpleAttrs;
    protected boolean entityTreeHasCollections;

    public void setEntityTreeHasCollections(boolean entityTreeHasCollections) {
        this.entityTreeHasCollections = entityTreeHasCollections;
    }

    public void setEntityTreeHasSimpleAttrs(boolean entityTreeHasSimpleAttrs) {
        this.entityTreeHasSimpleAttrs = entityTreeHasSimpleAttrs;
    }

    protected AddSimpleRegionAction addSimpleRegionAction;
    protected AddTabulatedRegionAction addTabulatedRegionAction;

    protected ReportTypeGenerate getReportTypeGenerate() {
        return reportDataDc.getItem().getReportTypeGenerate();
    }

    @Subscribe
    public void onInit(InitEvent event) {
        initFrameHandler = new InitRegionsStepFrameHandler();
    }

//    protected EditRegionAction editRegionAction;
//    protected RemoveRegionAction removeRegionAction;



//    public RegionsStepFragment(ReportWizardCreator wizard) {
        //super(wizard, "" /*wizard.getMessage("reportRegions")*/, "regionsStep");
//        initFrameHandler = new InitRegionsStepFrameHandler();
//
//        beforeShowFrameHandler = new BeforeShowRegionsStepFrameHandler();

//        beforeHideFrameHandler = new BeforeHideRegionsStepFrameHandler();
//    }

    @Override
    public boolean isLast() {
        return false;
    }

    @Override
    public boolean isFirst() {
        return false;
    }

    protected abstract class AddRegionAction extends AbstractAction {

        protected AddRegionAction(String id) {
            super(id);
        }

        protected ReportRegion createReportRegion(boolean tabulated) {
            ReportRegion reportRegion = metadata.create(ReportRegion.class);
            reportRegion.setReportData(reportDataDc.getItem());
            reportRegion.setIsTabulatedRegion(tabulated);
            reportRegion.setOrderNum((long) reportDataDc.getItem().getReportRegions().size() + 1L);
            return reportRegion;
        }

        protected void openTabulatedRegionEditor(final ReportRegion item) {
            if (ReportTypeGenerate.SINGLE_ENTITY == getReportTypeGenerate()) {
                openRegionEditorOnlyWithNestedCollections(item);

            } else {
                openRegionEditor(item);
            }
        }

        private void openRegionEditorOnlyWithNestedCollections(final ReportRegion item) {//show lookup for choosing parent collection for tabulated region
            final Map<String, Object> lookupParams = new HashMap<>();
            lookupParams.put("rootEntity", reportDataDc.getItem().getEntityTreeRootNode());
            lookupParams.put("collectionsOnly", Boolean.TRUE);
            lookupParams.put("persistentOnly", ReportTypeGenerate.LIST_OF_ENTITIES_WITH_QUERY == getReportTypeGenerate());

            EntityTreeLookup entityTreeLookup = (EntityTreeLookup) screenBuilders.lookup(EntityTreeNode.class, wizard)
                    .withScreenId("report_ReportEntityTree.lookup")
                    .withOpenMode(OpenMode.DIALOG)
                    .withOptions(new MapScreenOptions(lookupParams))
                    .withSelectHandler(items -> {
                        if (items.size() == 1) {
                            EntityTreeNode regionPropertiesRootNode = IterableUtils.get(items, 0);

                            Map<String, Object> editorParams = new HashMap<>();
                            editorParams.put("scalarOnly", Boolean.TRUE);
                            editorParams.put("persistentOnly", ReportTypeGenerate.LIST_OF_ENTITIES_WITH_QUERY == getReportTypeGenerate());
                            editorParams.put("rootEntity", regionPropertiesRootNode);
                            item.setRegionPropertiesRootNode(regionPropertiesRootNode);

                            RegionEditor regionEditor = screenBuilders.editor(ReportRegion.class, wizard)
                                    .withScreenClass(RegionEditor.class)
                                    .editEntity(item)
                                    .withOpenMode(OpenMode.DIALOG)
                                    .withContainer(reportRegionsDc)
                                    .withOptions(new MapScreenOptions(editorParams))
                                    .build();

                            regionEditor.addAfterCloseListener(new RegionEditorCloseListener());
                            regionEditor.show();
                        }
                    })
                    .build();

            entityTreeLookup.show();
        }

        protected void openRegionEditor(ReportRegion item) {
            item.setRegionPropertiesRootNode(reportDataDc.getItem().getEntityTreeRootNode());

            Map<String, Object> editorParams = new HashMap<>();
            editorParams.put("rootEntity", reportDataDc.getItem().getEntityTreeRootNode());
            editorParams.put("scalarOnly", Boolean.TRUE);
            editorParams.put("persistentOnly", ReportTypeGenerate.LIST_OF_ENTITIES_WITH_QUERY == getReportTypeGenerate());

            RegionEditor regionEditor = screenBuilders.editor(ReportRegion.class, getFragment().getFrameOwner())
                    .withScreenClass(RegionEditor.class)
                    .editEntity(item)
                    .withOpenMode(OpenMode.DIALOG)
                    .withContainer(reportRegionsDc)
                    .withOptions(new MapScreenOptions(editorParams))
                    .build();

//            regionEditor.setRootNode(reportDataDc.getItem().getEntityTreeRootNode());
//            regionEditor.setTabulated(item.getIsTabulatedRegion());


            regionEditor.addAfterCloseListener(new RegionEditorCloseListener());
            regionEditor.show();
        }

        protected class RegionEditorCloseListener implements Consumer<Screen.AfterCloseEvent> {
            @Override
            public void accept(Screen.AfterCloseEvent afterCloseEvent) {
                StandardCloseAction standardCloseAction = (StandardCloseAction) afterCloseEvent.getCloseAction();
                if (Window.COMMIT_ACTION_ID.equals(standardCloseAction.getActionId())) {
//                    wizard.regionsTable.refresh();
//                    wizard.setupButtonsVisibility();
                }
            }
        }
    }

    protected class AddSimpleRegionAction extends AddRegionAction {
        public AddSimpleRegionAction() {
            super(ADD_SIMPLE_REGION_ACTION_ID);
        }

        @Override
        public void actionPerform(Component component) {
            openRegionEditor(createReportRegion(false));
        }
    }

    protected class AddTabulatedRegionAction extends AddRegionAction {
        public AddTabulatedRegionAction() {
            super(ADD_TABULATED_REGION_ACTION_ID);
        }

        @Override
        public void actionPerform(Component component) {
            openTabulatedRegionEditor(createReportRegion(true));
        }
    }

    protected class ReportRegionTableColumnGenerator implements Table.ColumnGenerator<ReportRegion> {
        protected static final String WIDTH_PERCENT_100 = "100%";
        protected static final int MAX_ATTRS_BTN_CAPTION_WIDTH = 95;
        protected static final String BOLD_LABEL_STYLE = "semi-bold-label";

        private ReportRegion currentReportRegionGeneratedColumn;

        @Override
        public Component generateCell(ReportRegion entity) {
            currentReportRegionGeneratedColumn = entity;
            BoxLayout mainLayout = uiComponents.create(VBoxLayout.class);
            mainLayout.setWidth(WIDTH_PERCENT_100);
            mainLayout.add(createFirstTwoRowsLayout());
            mainLayout.add(createThirdRowAttrsLayout());
            return mainLayout;
        }

        private BoxLayout createFirstTwoRowsLayout() {
            BoxLayout firstTwoRowsLayout = uiComponents.create(HBoxLayout.class);
            BoxLayout expandedAttrsLayout = createExpandedAttrsLayout();
            firstTwoRowsLayout.setWidth(WIDTH_PERCENT_100);
            firstTwoRowsLayout.add(expandedAttrsLayout);
            firstTwoRowsLayout.add(createBtnsLayout());
            firstTwoRowsLayout.expand(expandedAttrsLayout);
            return firstTwoRowsLayout;
        }

        private BoxLayout createExpandedAttrsLayout() {
            BoxLayout expandedAttrsLayout = uiComponents.create(HBoxLayout.class);
            expandedAttrsLayout.setWidth(WIDTH_PERCENT_100);
            expandedAttrsLayout.add(createFirstRowAttrsLayout());
            expandedAttrsLayout.add(createSecondRowAttrsLayout());
            return expandedAttrsLayout;
        }

        private BoxLayout createFirstRowAttrsLayout() {
            BoxLayout firstRowAttrsLayout = uiComponents.create(HBoxLayout.class);
            firstRowAttrsLayout.setSpacing(true);
            Label regionLbl = uiComponents.create(Label.class);
            regionLbl.setStyleName(BOLD_LABEL_STYLE);
            regionLbl.setValue(messages.getMessage("region"));
            Label regionValueLbl = uiComponents.create(Label.class);
            regionValueLbl.setValue(currentReportRegionGeneratedColumn.getName());
            regionValueLbl.setWidth(WIDTH_PERCENT_100);
            firstRowAttrsLayout.add(regionLbl);
            firstRowAttrsLayout.add(regionValueLbl);
            return firstRowAttrsLayout;
        }

        private BoxLayout createSecondRowAttrsLayout() {
            BoxLayout secondRowAttrsLayout = uiComponents.create(HBoxLayout.class);
            secondRowAttrsLayout.setSpacing(true);
            Label entityLbl = uiComponents.create(Label.class);
            entityLbl.setStyleName(BOLD_LABEL_STYLE);
            entityLbl.setValue(messages.getMessage("entity"));
            Label entityValueLbl = uiComponents.create(Label.class);
            MetaClass wrapperMetaClass = currentReportRegionGeneratedColumn.getRegionPropertiesRootNode().getWrappedMetaClass();

            entityValueLbl.setValue(messageTools.getEntityCaption(wrapperMetaClass));
            entityValueLbl.setWidth(WIDTH_PERCENT_100);
            secondRowAttrsLayout.add(entityLbl);
            secondRowAttrsLayout.add(entityValueLbl);
            return secondRowAttrsLayout;
        }

        private BoxLayout createBtnsLayout() {
            BoxLayout btnsLayout = uiComponents.create(HBoxLayout.class);
            btnsLayout.setSpacing(true);
            btnsLayout.setStyleName("on-hover-visible-layout");
            return btnsLayout;
        }

        private BoxLayout createThirdRowAttrsLayout() {
            BoxLayout thirdRowAttrsLayout = uiComponents.create(HBoxLayout.class);
            thirdRowAttrsLayout.setSpacing(true);
            Label entityLbl = uiComponents.create(Label.class);
            entityLbl.setStyleName(BOLD_LABEL_STYLE);
            entityLbl.setValue(messages.getMessage("attributes"));
            Button editBtn = uiComponents.create(Button.class);
            editBtn.setCaption(generateAttrsBtnCaption());
            editBtn.setStyleName("link");
            editBtn.setWidth(WIDTH_PERCENT_100);
//            editBtn.setAction(editRegionAction);
            thirdRowAttrsLayout.add(entityLbl);
            thirdRowAttrsLayout.add(editBtn);
            return thirdRowAttrsLayout;
        }

        private String generateAttrsBtnCaption() {

            return StringUtils.abbreviate(StringUtils.join(
                    CollectionUtils.collect(currentReportRegionGeneratedColumn.getRegionProperties(),
                            RegionProperty::getHierarchicalLocalizedNameExceptRoot), ", "
                    ), MAX_ATTRS_BTN_CAPTION_WIDTH
            );
        }
    }



    protected class RemoveRegionAction extends AbstractAction {
        public RemoveRegionAction() {
            super("removeRegion");
        }

        @Override
        public void actionPerform(Component component) {
            if (regionsTable.getSingleSelected() != null) {
                dialogs.createOptionDialog()
                        .withCaption(messages.getMessage("dialogs.Confirmation"))
                        .withMessage(messages.formatMessage("deleteRegion", regionsTable.getSingleSelected().getName()))
                        .withActions(
                                new DialogAction(DialogAction.Type.YES).withHandler(e -> {
                                    reportRegionsDc.getMutableItems().remove(regionsTable.getSingleSelected());
                                    normalizeRegionPropertiesOrderNum();
                                    setupButtonsVisibility();
                                }),
                                new DialogAction(DialogAction.Type.NO).withPrimary(true)
                        ).show();
            }
        }

        @Override
        public String getCaption() {
            return "";
        }

        protected void normalizeRegionPropertiesOrderNum() {
            long normalizedIdx = 0;
            List<ReportRegion> allItems = new ArrayList<>(reportRegionsDc.getItems());
            for (ReportRegion item : allItems) {
                item.setOrderNum(++normalizedIdx); //first must to be 1
            }
        }
    }

//    protected class EditRegionAction extends AddRegionAction {
//        public EditRegionAction() {
//            super("removeRegion");
//        }
//
//        @Override
//        public void actionPerform(Component component) {
//            if (wizard.regionsTable.getSingleSelected() != null) {
//                Map<String, Object> editorParams = new HashMap<>();
//                editorParams.put("rootEntity", wizard.regionsTable.getSingleSelected().getRegionPropertiesRootNode());
//                editorParams.put("scalarOnly", Boolean.TRUE);
//                editorParams.put("persistentOnly", ReportType.LIST_OF_ENTITIES_WITH_QUERY == wizard.reportTypeRadioButtonGroup.getValue());
//
//                RegionEditor regionEditor = wizard.screenBuilders.editor(ReportRegion.class, wizard)
//                        .withScreenClass(RegionEditor.class)
//                        .editEntity(wizard.regionsTable.getSingleSelected())
//                        .withContainer(wizard.reportRegionsDc)
//                        .withOpenMode(OpenMode.DIALOG)
//                        .withOptions(new MapScreenOptions(editorParams))
//                        .build();
//                regionEditor.addAfterCloseListener(new RegionEditorCloseListener());
//                regionEditor.show();
//            }
//        }
//
//        @Override
//        public String getCaption() {
//            return "";
//        }
//    }

    protected class InitRegionsStepFrameHandler implements InitStepFrameHandler {
        @Override
        public void initFrame() {
            addSimpleRegionAction = new AddSimpleRegionAction();
            addTabulatedRegionAction = new AddTabulatedRegionAction();
            addSimpleRegionBtn.setAction(addSimpleRegionAction);
            addTabulatedRegionBtn.setAction(addTabulatedRegionAction);
            addRegionPopupBtn.addAction(addSimpleRegionAction);
            addRegionPopupBtn.addAction(addTabulatedRegionAction);
            regionsTable.addGeneratedColumn("regionsGeneratedColumn", new ReportRegionTableColumnGenerator());
//            editRegionAction = new EditRegionAction();

//            removeRegionAction = new RemoveRegionAction();

//            moveDownBtn.setAction(new OrderableItemMoveAction<>("downItem", Direction.DOWN, wizard.regionsTable));
//            moveUpBtn.setAction(new OrderableItemMoveAction<>("upItem", Direction.UP, wizard.regionsTable));
//            removeBtn.setAction(removeRegionAction);
        }
    }

    protected class BeforeShowRegionsStepFrameHandler implements BeforeShowStepFrameHandler {
        @Override
        public void beforeShowFrame() {
            setupButtonsVisibility();
            runBtn.setAction(new AbstractAction("runReport") {
                @Override
                public void actionPerform(Component component) {
                    if (reportDataDc.getItem().getReportRegions().isEmpty()) {
                        notifications.create(Notifications.NotificationType.TRAY)
                                .withCaption(messages.getMessage("addRegionsWarn"))
                                .show();
                        return;
                    }
//                    lastGeneratedTmpReport = wizard.buildReport(true);
//
//                    if (lastGeneratedTmpReport != null) {
//                        reportGuiManager.runReport(
//                                lastGeneratedTmpReport,
//                                wizard);
//                    }
                }
            });

            showAddRegion();
            //wizard.setCorrectReportOutputType();
            //TODO dialog options
//            wizard.getDialogOptions()
//                    .setHeight(wizard.wizardHeight).setHeightUnit(SizeUnit.PIXELS)
//                    .center();
        }
//
        private void showAddRegion() {
            if (reportRegionsDc.getItems().isEmpty()) {
                if (reportDataDc.getItem().getReportTypeGenerate().isList()) {
                    if (entityTreeHasSimpleAttrs) {
                        addTabulatedRegionAction.actionPerform(getFragment());
                    }
                } else {
                    if (entityTreeHasSimpleAttrs && entityTreeHasCollections) {
                        addSimpleRegionAction.actionPerform(getFragment());
                    } else if (entityTreeHasSimpleAttrs) {
                        addSimpleRegionAction.actionPerform(getFragment());
                    } else if (entityTreeHasCollections) {
                        addTabulatedRegionAction.actionPerform(getFragment());
                    }
                }
            }
        }
    }

        protected void setupButtonsVisibility() {
            buttonsBox.remove(addRegionDisabledBtn);
            buttonsBox.remove(addTabulatedRegionDisabledBtn);
            buttonsBox.remove(addSimpleRegionBtn);
            buttonsBox.remove(addTabulatedRegionBtn);
            buttonsBox.remove(addRegionPopupBtn);

            if (reportDataDc.getItem().getReportTypeGenerate().isList()) {
                MetaClass metaClass = metadata.getClass(reportDataDc.getItem().getName());
                Class javaClass = metaClass.getJavaClass();

//                tipLabel.setValue(messages.formatMessage("regionTabulatedMessage",
//                        messages.getMessage(javaClass, javaClass.getSimpleName())
//                ));

                if (entityTreeHasSimpleAttrs && reportDataDc.getItem().getReportRegions().isEmpty()) {
                    buttonsBox.add(addTabulatedRegionBtn);
                } else {
                    buttonsBox.add(addTabulatedRegionDisabledBtn);
                }
            } else {
                //tipLabel.setValue(messages.getMessage("addPropertiesAndTableAreas"));

                if (entityTreeHasSimpleAttrs && entityTreeHasCollections) {
                    buttonsBox.add(addRegionPopupBtn);
                } else if (entityTreeHasSimpleAttrs) {
                    buttonsBox.add(addSimpleRegionBtn);
                } else if (entityTreeHasCollections) {
                    buttonsBox.add(addTabulatedRegionBtn);
                } else {
                    buttonsBox.add(addRegionDisabledBtn);
                }
            }

            if (regionsTable.getSingleSelected() != null) {
                moveDownBtn.setEnabled(true);
                moveUpBtn.setEnabled(true);
                removeBtn.setEnabled(true);
            } else {
                moveDownBtn.setEnabled(false);
                moveUpBtn.setEnabled(false);
                removeBtn.setEnabled(false);
            }
        }

    protected class BeforeHideRegionsStepFrameHandler implements BeforeHideStepFrameHandler {
        @Override
        public void beforeHideFrame() {
        }
    }
}