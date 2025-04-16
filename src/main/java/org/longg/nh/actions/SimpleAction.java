package org.longg.nh.actions;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

public class SimpleAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        Messages.showInfoMessage(project, "Plugin biên dịch thành công!", "Entity Generator Plugin");
    }
} 