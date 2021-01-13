package io.jmix.reportsui.gui.report.edit.tabs;

import io.jmix.core.Messages;
import io.jmix.core.Metadata;
import io.jmix.core.Sort;
import io.jmix.reports.entity.Report;
import io.jmix.reports.entity.ReportInputParameter;
import io.jmix.security.constraint.PolicyStore;
import io.jmix.security.constraint.SecureOperations;
import io.jmix.ui.Dialogs;
import io.jmix.ui.RemoveOperation;
import io.jmix.ui.action.ListAction;
import io.jmix.ui.component.*;
import io.jmix.ui.model.CollectionPropertyContainer;
import io.jmix.ui.model.InstanceContainer;
import io.jmix.ui.screen.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

@DialogMode(forceDialog = true)
@UiController("report_ReportEditParameters.fragment")
@UiDescriptor("parameters.xml")
public class ParametersFragment extends ScreenFragment {

    @Autowired
    protected InstanceContainer<Report> reportDc;

    @Autowired
    protected CollectionPropertyContainer<ReportInputParameter> parametersDc;

    @Autowired
    protected Table<ReportInputParameter> inputParametersTable;

    @Autowired
    protected Button upButton;

    @Autowired
    protected Button downButton;

    @Autowired
    protected Metadata metadata;

    @Autowired
    protected SecureOperations secureOperations;

    @Autowired
    protected PolicyStore policyStore;

    @Autowired
    protected Dialogs dialogs;

    @Autowired
    protected Messages messages;

    @Subscribe
    public void onInit(InitEvent event) {
        upButton.setAction(new ListAction("generalFragment.up") {
            @Override
            public void actionPerform(Component component) {
                ReportInputParameter parameter = (ReportInputParameter) target.getSingleSelected();
                if (parameter != null) {
                    List<ReportInputParameter> inputParameters = reportDc.getItem().getInputParameters();
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

        downButton.setAction(new ListAction("generalFragment.down") {
            @Override
            public void actionPerform(Component component) {
                ReportInputParameter parameter = (ReportInputParameter) target.getSingleSelected();
                if (parameter != null) {
                    List<ReportInputParameter> inputParameters = reportDc.getItem().getInputParameters();
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

        inputParametersTable.addAction(upButton.getAction());
        inputParametersTable.addAction(downButton.getAction());

        parametersDc.addItemPropertyChangeListener(e -> {
            if ("position".equals(e.getProperty())) {
                //todo
                //((DatasourceImplementation) parametersDc).modified(e.getItem());
            }
        });
    }

    @Install(to = "inputParametersTable.create", subject = "initializer")
    protected void inputParametersTableCreateInitializer(ReportInputParameter reportInputParameter) {
        reportInputParameter.setReport(reportDc.getItem());
        reportInputParameter.setPosition(parametersDc.getItems().size());
    }

    @Install(to = "inputParametersTable.create", subject = "afterCommitHandler")
    protected void inputParametersTableCreateAfterCommitHandler(ReportInputParameter reportInputParameter) {

    }

    @Install(to = "inputParametersTable.remove", subject = "afterActionPerformedHandler")
    protected void inputParametersTableRemoveAfterActionPerformedHandler(RemoveOperation.AfterActionPerformedEvent<ReportInputParameter> afterActionPerformedEvent) {
        orderParameters();
    }

    @Install(to = "validationScriptCodeEditor", subject = "contextHelpIconClickHandler")
    private void validationScriptCodeEditorContextHelpIconClickHandler(HasContextHelp.ContextHelpIconClickEvent contextHelpIconClickEvent) {
        dialogs.createMessageDialog()
                .withCaption(messages.getMessage(getClass(), "parameters.validationScript"))
                .withMessage(messages.getMessage(getClass(), "parameters.crossFieldValidationScriptHelp"))
                .withContentMode(ContentMode.HTML)
                .withModal(false)
                .withWidth("600px")
                .show();
    }

    protected void orderParameters() {
        Report report = reportDc.getItem();
        if (report.getInputParameters() == null) {
            report.setInputParameters(new ArrayList<>());
        }

        for (int i = 0; i < report.getInputParameters().size(); i++) {
            report.getInputParameters().get(i).setPosition(i);
        }
    }

    protected boolean isUpdatePermitted() {
        return secureOperations.isEntityUpdatePermitted(metadata.getClass(Report.class), policyStore);
    }

    protected void sortParametersByPosition() {
        parametersDc.getSorter().sort(Sort.by(Sort.Direction.ASC, "position"));
    }
}
