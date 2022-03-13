package dev.xframe.hierarchy;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiNewExpression;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceList;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeElement;
import org.jetbrains.annotations.NotNull;

public class OpenHierarchyAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Editor editor = e.getData(PlatformDataKeys.EDITOR);
        PsiFile psFile = e.getData(PlatformDataKeys.PSI_FILE);
        if(editor != null && psFile != null) {
            PsiElement element = psFile.findElementAt(editor.getSelectionModel().getSelectionStart());
            if(element != null) {
                doTrigger(e, element.getParent());
            }
        }
    }
    private void doTrigger(@NotNull AnActionEvent e, PsiElement element) {
        if(element != null) {
            if(element instanceof PsiClass || element instanceof PsiType || element instanceof PsiNewExpression ||
                    element instanceof PsiTypeElement || element instanceof PsiReferenceList) {
                ActionManager.getInstance().getAction(IdeActions.ACTION_TYPE_HIERARCHY).actionPerformed(e);
            } else if(element instanceof PsiMethod || element instanceof PsiMethodCallExpression) {
                ActionManager.getInstance().getAction(IdeActions.ACTION_METHOD_HIERARCHY).actionPerformed(e);
            } else if(element instanceof PsiReference || element instanceof PsiJavaCodeReferenceElement) {
                doTrigger(e, element.getParent());
            }
        }
    }
}
