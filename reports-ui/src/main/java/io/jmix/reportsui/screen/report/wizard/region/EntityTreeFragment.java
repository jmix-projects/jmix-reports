package io.jmix.reportsui.screen.report.wizard.region;

import io.jmix.core.Messages;
import io.jmix.reports.entity.wizard.EntityTreeNode;
import io.jmix.ui.Notifications;
import io.jmix.ui.action.Action;
import io.jmix.ui.component.Tree;
import io.jmix.ui.model.CollectionContainer;
import io.jmix.ui.screen.*;
import org.springframework.beans.factory.annotation.Autowired;

@UiController("report_EntityTree.fragment")
@UiDescriptor("entity-tree-frame.xml")
public class EntityTreeFragment extends ScreenFragment {

    @Autowired
    private Tree<EntityTreeNode> entityTree;

    @Autowired
    protected Notifications notifications;

    @Autowired
    protected Messages messages;

    @Autowired
    private CollectionContainer<EntityTreeNode> reportEntityTreeNodeDc;

    @Subscribe(target = Target.PARENT_CONTROLLER)
    public void onBeforeShow(Screen.BeforeShowEvent event) {
        RegionEditor regionEditor = (RegionEditor) getHostScreen();
//        if (!reportEntityTreeNodeDc.getItems().isEmpty()) {
//            entityTree.collapseTree();
//            //todo
//            //entityTree.expand(rootNode.getId());
//        } else {
//            notifications.create(Notifications.NotificationType.HUMANIZED)
//                    .withCaption(messages.getMessage("valueNotFound"))
//                    .show();
//        }
        EntityTreeNode entityTreeNode = regionEditor.getEditedEntity().getRegionPropertiesRootNode();

        reportEntityTreeNodeDc.getMutableItems().add(entityTreeNode);
        reportEntityTreeNodeDc.getMutableItems().addAll(entityTreeNode.getChildren());
        entityTree.expand(entityTreeNode);
    }

}
