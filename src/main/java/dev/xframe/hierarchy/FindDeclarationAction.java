package dev.xframe.hierarchy;

import com.intellij.ide.hierarchy.HierarchyBrowser;
import com.intellij.ide.hierarchy.HierarchyTreeStructure;
import com.intellij.ide.hierarchy.actions.BrowseHierarchyActionBase;
import com.intellij.ide.hierarchy.call.CallHierarchyBrowser;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiTypeElement;
import com.intellij.ui.PopupHandler;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Map;

public class FindDeclarationAction extends AnAction {

    static String viewType() {
        return "Declaration of {0}";
    }

    final CallHierarchyProvider provider = new CallHierarchyProvider() {
        @Override
        public HierarchyBrowser createHierarchyBrowser(@NotNull PsiElement target) {
            return new CallHierarchyBrowser(target.getProject(), (PsiMember)target){
                @Override
                protected boolean isApplicableElement(@NotNull PsiElement e) {
                    return e instanceof PsiClass;
                }
                @Override
                protected void createTrees(@NotNull Map<? super @Nls String, ? super JTree> type2TreeMap) {
                    JTree tree1 = createTree(false);
                    PopupHandler.installPopupMenu(tree1, IdeActions.GROUP_CALL_HIERARCHY_POPUP, ActionPlaces.CALL_HIERARCHY_VIEW_POPUP);
                    BaseOnThisMethodAction baseOnThisMethodAction = new BaseOnThisMethodAction();
                    baseOnThisMethodAction.registerCustomShortcutSet(ActionManager.getInstance().getAction(IdeActions.ACTION_CALL_HIERARCHY).getShortcutSet(), tree1);
                    type2TreeMap.put(viewType(), tree1);
                }
                @Override
                protected HierarchyTreeStructure createHierarchyTreeStructure(@NotNull String typeName, @NotNull PsiElement psiElement) {
                    if (viewType().equals(typeName)) {
                        return new CallerMethodsTreeStructure(myProject, (PsiMember)psiElement, getCurrentScopeType()) {
                            @Override
                            protected boolean isClassReferenceMatched(PsiElement e) {
                                return e instanceof PsiTypeElement || e.getParent() instanceof PsiTypeElement;
                            }
                        };
                    }
                    return null;
                }
            };
        }
        @Override
        public void browserActivated(@NotNull HierarchyBrowser hierarchyBrowser) {
            ((CallHierarchyBrowser)hierarchyBrowser).changeView(viewType());
        }
    };

    @Override
    public void update(@NotNull AnActionEvent e) {
        PsiElement target = provider.getTarget(e.getDataContext());
        e.getPresentation().setEnabledAndVisible(target instanceof PsiClass);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        DataContext dataContext = e.getDataContext();
        Project project = e.getProject();
        if (project == null) return;

        PsiDocumentManager.getInstance(project).commitAllDocuments(); // prevents problems with smart pointers creation

        PsiElement target = provider.getTarget(dataContext);
        if (target == null) return;
        BrowseHierarchyActionBase.createAndAddToPanel(project, provider, target);
    }
}
