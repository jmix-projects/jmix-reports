package io.jmix.reportsui.gui.report.edit.tabs;

import io.jmix.core.Messages;
import io.jmix.core.Metadata;
import io.jmix.core.MetadataTools;
import io.jmix.core.UuidProvider;
import io.jmix.reports.entity.Report;
import io.jmix.reports.entity.ReportTemplate;
import io.jmix.security.constraint.PolicyStore;
import io.jmix.security.constraint.SecureOperations;
import io.jmix.ui.Actions;
import io.jmix.ui.RemoveOperation;
import io.jmix.ui.action.ItemTrackingAction;
import io.jmix.ui.action.ListAction;
import io.jmix.ui.component.Component;
import io.jmix.ui.component.Table;
import io.jmix.ui.component.data.meta.ContainerDataUnit;
import io.jmix.ui.model.CollectionContainer;
import io.jmix.ui.model.CollectionPropertyContainer;
import io.jmix.ui.model.InstanceContainer;
import io.jmix.ui.screen.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@UiController("report_ReportEditTemplates.fragment")
@UiDescriptor("templates.xml")
public class TemplatesFragment extends ScreenFragment {

    @Autowired
    protected InstanceContainer<Report> reportDc;

    @Autowired
    protected CollectionPropertyContainer<ReportTemplate> templatesDc;

    @Autowired
    protected Table<ReportTemplate> templatesTable;

    @Autowired
    protected Messages messages;

    @Autowired
    protected MetadataTools metadataTools;

    @Autowired
    protected Metadata metadata;

    @Autowired
    protected SecureOperations secureOperations;

    @Autowired
    protected PolicyStore policyStore;

    @Install(to = "templatesTable.create", subject = "afterCommitHandler")
    protected void templatesTableCreateAfterCommitHandler(ReportTemplate reportTemplate) {
        Report report = reportDc.getItem();
        ReportTemplate defaultTemplate = reportDc.getItem().getDefaultTemplate();
        if (defaultTemplate == null) {
            report.setDefaultTemplate(reportTemplate);
        }
    }

    @Install(to = "templatesTable.create", subject = "initializer")
    protected void templatesTableCreateInitializer(ReportTemplate reportTemplate) {
        reportTemplate.setReport(reportDc.getItem());
    }

    @Install(to = "templatesTable.remove", subject = "afterActionPerformedHandler")
    protected void templatesTableRemoveAfterActionPerformedHandler(RemoveOperation.AfterActionPerformedEvent<ReportTemplate> event) {
        List<ReportTemplate> selected = event.getItems();

        Report report = reportDc.getItem();
        ReportTemplate defaultTemplate = report.getDefaultTemplate();
        if (defaultTemplate != null && selected.contains(defaultTemplate)) {
            ReportTemplate newDefaultTemplate = null;

            if (templatesDc.getItems().size() == 1) {
                newDefaultTemplate = templatesDc.getItems().iterator().next();
            }

            report.setDefaultTemplate(newDefaultTemplate);
        }
    }

    @Subscribe
    public void onInit(InitEvent event) {

        templatesTable.addAction(new ListAction("defaultTemplate") {
            @Override
            public String getCaption() {
                return messages.getMessage(getClass(), "report.defaultTemplate");
            }

            @Override
            public void actionPerform(Component component) {
                ReportTemplate template = (ReportTemplate) target.getSingleSelected();
                if (template != null) {
                    reportDc.getItem().setDefaultTemplate(template);
                }

                refreshState();

                templatesTable.focus();
            }

            @Override
            protected boolean isApplicable() {
                if (target != null) {
                    Object selectedItem = target.getSingleSelected();
                    if (selectedItem != null) {
                        return !Objects.equals(reportDc.getItem().getDefaultTemplate(), selectedItem);
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

                    String copyNamingPattern = messages.getMessage(getClass(), "template.copyNamingPattern");
                    String copyCode = String.format(copyNamingPattern, StringUtils.isEmpty(copy.getCode())
                            ? StringUtils.EMPTY
                            : copy.getCode());

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

                    container.getMutableItems().add(copy);
                }
            }

            @Override
            public boolean isEnabled() {
                return super.isEnabled() && isUpdatePermitted();
            }
        });
    }

    protected boolean isUpdatePermitted() {
        return secureOperations.isEntityUpdatePermitted(metadata.getClass(Report.class), policyStore);
    }
}
