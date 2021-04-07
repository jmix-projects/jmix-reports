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

package io.jmix.reportsui.screen.report.wizard.step;

import io.jmix.core.Messages;
import io.jmix.ui.Notifications;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component("report_StepFrameManager")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class StepFragmentManager {

    @Autowired
    protected Messages messages;

    @Autowired
    protected Notifications notifications;

    protected List<StepFragment> stepFragments;
    protected MainWizardScreen mainWizardScreen;
    protected int currentFrameIdx = 0;

    public void setStepFragments(List<StepFragment> stepFragments) {
        this.stepFragments = stepFragments;

        setWizardCaption();
    }

    public void setMainWizardFrame(MainWizardScreen mainWizardScreen) {
        this.mainWizardScreen = mainWizardScreen;
    }

    public void showCurrentFragment() {
        setWizardCaption();
        setWizardDescription();

        setNavigationButtonProps();
        getCurrentStepFragment().initFrame();
        getCurrentStepFragment().beforeShow();
        getCurrentStepFragment().getFragment().setVisible(true);
    }

    protected StepFragment getCurrentStepFragment() {
        return stepFragments.get(currentFrameIdx);
    }

    public void setWizardDescription() {
        mainWizardScreen.setDescription(getCurrentStepFragment().getDescription());
    }

    public void setWizardCaption() {
        mainWizardScreen.setCaption(messages.formatMessage(getClass(), "stepNo",
                getCurrentStepFragment().getCaption(),
                currentFrameIdx + 1,
                stepFragments.size())
        );
    }

    protected void setNavigationButtonProps() {
        if (currentFrameIdx <= 0) {
            mainWizardScreen.getForwardBtn().setVisible(true);
            mainWizardScreen.getBackwardBtn().setVisible(false);
            mainWizardScreen.getSaveBtn().setVisible(false);
        } else if (currentFrameIdx >= stepFragments.size() - 1) {
            mainWizardScreen.getForwardBtn().setVisible(false);
            mainWizardScreen.getBackwardBtn().setVisible(true);
            mainWizardScreen.getSaveBtn().setVisible(true);
        } else {
            mainWizardScreen.getBackwardBtn().setVisible(true);
            mainWizardScreen.getForwardBtn().setVisible(true);
            mainWizardScreen.getSaveBtn().setVisible(false);
        }
    }

    public boolean prevFragment() {
        if (currentFrameIdx == 0) {
            throw new ArrayIndexOutOfBoundsException("Previous step is not exists");
        }
        if (!getCurrentStepFragment().isValidateBeforePrev() || validateCurrentFragment()) {
            hideCurrentFrame();
            currentFrameIdx--;
            showCurrentFragment();
            return true;
        } else {
            return false;
        }
    }

    public boolean nextFragment() {
        if (currentFrameIdx > stepFragments.size()) {
            throw new ArrayIndexOutOfBoundsException("Next step is not exists");
        }
        if (!getCurrentStepFragment().isValidateBeforeNext() || validateCurrentFragment()) {
            hideCurrentFrame();
            currentFrameIdx++;
            showCurrentFragment();
            return true;
        } else {
            return false;
        }
    }

    public boolean validateCurrentFragment() {
        List<String> validationErrors = getCurrentStepFragment().validateFragment();
        if (!validationErrors.isEmpty()) {
            notifications.create(Notifications.NotificationType.TRAY)
                    .withHtmlSanitizer(true)
                    .withCaption(StringUtils.arrayToDelimitedString(validationErrors.toArray(), "<br/>"))
                    .show();
            return false;
        }
        return true;
    }

    protected void hideCurrentFrame() {
        getCurrentStepFragment().beforeHide();
        getCurrentStepFragment().getFragment().setVisible(false);
    }
}