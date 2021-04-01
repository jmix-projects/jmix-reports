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
import io.jmix.ui.component.Window;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component("report_StepFrameManager")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class StepFrameManager {

    @Autowired
    protected Messages messages;

    @Autowired
    protected Notifications notifications;

    protected List<StepFragment> stepFragments;
    protected MainWizardScreen mainWizardScreen;
    protected int currentFrameIdx = 0;

    public void setStepFragments(List<StepFragment> stepFragments) {
        this.stepFragments = stepFragments;
    }

    public void setMainWizardFrame(MainWizardScreen mainWizardScreen) {
        this.mainWizardScreen = mainWizardScreen;
    }

    public void showCurrentFrame() {
        setWizardCaption();

        setNavigationButtonProps();
        getCurrentStepFrame().initFrame();
        getCurrentStepFrame().beforeShow();
        getCurrentStepFrame().getFragment().setVisible(true);
    }

    protected StepFragment getCurrentStepFrame() {
        return stepFragments.get(currentFrameIdx);
    }

    public void setWizardCaption() {
        String newWindowCaption = getCurrentStepFrame().getCaption() + " " +
                messages.formatMessage(getClass(), "stepNo", currentFrameIdx + 1, stepFragments.size());

        Window window = mainWizardScreen.getMainWizardFrame().getWindow();
        window.setCaption(newWindowCaption);
    }

    protected void setNavigationButtonProps() {
        if (getCurrentStepFrame().isLast()) {
            mainWizardScreen.getForwardBtn().setVisible(false);
        } else if (currentFrameIdx + 1 >= stepFragments.size()) {
            mainWizardScreen.getForwardBtn().setEnabled(false);
        } else {
            mainWizardScreen.getForwardBtn().setVisible(true);
            mainWizardScreen.getForwardBtn().setEnabled(true);
        }

        if (getCurrentStepFrame().isFirst()) {
            mainWizardScreen.getBackwardBtn().setVisible(false);
        } else if (currentFrameIdx - 1 < 0) {
            mainWizardScreen.getBackwardBtn().setEnabled(false);
        } else {
            mainWizardScreen.getBackwardBtn().setVisible(true);
            mainWizardScreen.getBackwardBtn().setEnabled(true);
        }
//        mainWizardScreen.removeBtns();
//        if (mainWizardScreen.getBackwardBtn().isVisible())
//            mainWizardScreen.addBackwardBtn();
//        if (mainWizardScreen.getForwardBtn().isVisible())
//            mainWizardScreen.addForwardBtn();
//        mainWizardScreen.addSaveBtn();
    }

    public boolean prevFragment() {
        if (currentFrameIdx == 0) {
            throw new ArrayIndexOutOfBoundsException("Previous frame is not exists");
        }
        if (!getCurrentStepFrame().isValidateBeforePrev() || validateCurrentFrame()) {
            hideCurrentFrame();
            currentFrameIdx--;
            showCurrentFrame();
            return true;
        } else {
            return false;
        }
    }

    public boolean nextFragment() {
        if (currentFrameIdx > stepFragments.size()) {
            throw new ArrayIndexOutOfBoundsException("Next frame is not exists");
        }
        if (!getCurrentStepFrame().isValidateBeforeNext() || validateCurrentFrame()) {
            hideCurrentFrame();
            currentFrameIdx++;
            showCurrentFrame();
            return true;
        } else {
            return false;
        }
    }

    protected boolean validateCurrentFrame() {
        List<String> validationErrors = getCurrentStepFrame().validateFrame();
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
        getCurrentStepFrame().beforeHide();
        getCurrentStepFrame().getFragment().setVisible(false);
    }
}