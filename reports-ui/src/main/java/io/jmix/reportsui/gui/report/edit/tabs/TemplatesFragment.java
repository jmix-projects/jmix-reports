package io.jmix.reportsui.gui.report.edit.tabs;

import io.jmix.reports.entity.Report;
import io.jmix.reports.entity.ReportTemplate;
import io.jmix.ui.model.InstanceContainer;
import io.jmix.ui.screen.Install;
import io.jmix.ui.screen.ScreenFragment;
import io.jmix.ui.screen.UiController;
import io.jmix.ui.screen.UiDescriptor;
import org.springframework.beans.factory.annotation.Autowired;

@UiController("report_ReportEditTemplates.fragment")
@UiDescriptor("templates.xml")
public class TemplatesFragment extends ScreenFragment {

    @Autowired
    private InstanceContainer<Report> reportDc;

    @Install(to = "templatesTable.create", subject = "afterCommitHandler")
    private void templatesTableCreateAfterCommitHandler(ReportTemplate reportTemplate) {

    }

}
