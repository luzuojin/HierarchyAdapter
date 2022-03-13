package dev.xframe.hierarchy;

import com.intellij.ide.hierarchy.CallHierarchyBrowserBase;
import com.intellij.ide.hierarchy.HierarchyBrowser;
import com.intellij.ide.hierarchy.HierarchyProvider;
import com.intellij.ide.hierarchy.HierarchyTreeStructure;
import com.intellij.ide.hierarchy.call.CallHierarchyBrowser;
import com.intellij.ide.hierarchy.call.CalleeMethodsTreeStructure;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

//copy from com.intellij.ide.hierarchy.call.JavaCallHierarchyProvider
public class CallHierarchyProvider implements HierarchyProvider {

    @Override
    public PsiElement getTarget(@NotNull DataContext dataContext) {
        Project project = CommonDataKeys.PROJECT.getData(dataContext);
        if (project == null)
            return null;
        PsiElement element = CommonDataKeys.PSI_ELEMENT.getData(dataContext);
        if (element == null)
            return null;
        if (element instanceof PsiField)
            return element;
        PsiMethod method = PsiTreeUtil.getParentOfType(element, PsiMethod.class, false);
        if (method != null)
            return method;
        return PsiTreeUtil.getParentOfType(element, PsiClass.class, false);
    }

    @Override
    public HierarchyBrowser createHierarchyBrowser(@NotNull PsiElement target) {
        return new CallHierarchyBrowser(target.getProject(), (PsiMember)target){
            @Override
            protected boolean isApplicableElement(@NotNull PsiElement e) {
                return super.isApplicableElement(e) || e instanceof PsiClass;
            }
            @Override
            protected HierarchyTreeStructure createHierarchyTreeStructure(@NotNull String typeName, @NotNull PsiElement psiElement) {
                if (getCallerType().equals(typeName)) {
                    return new CallerMethodsTreeStructure(myProject, (PsiMember)psiElement, getCurrentScopeType());
                }
                if (getCalleeType().equals(typeName)) {
                    return new CalleeMethodsTreeStructure(myProject, (PsiMember)psiElement, getCurrentScopeType());
                }
                return null;
            }
        };
    }

    @Override
    public void browserActivated(@NotNull HierarchyBrowser hierarchyBrowser) {
        ((CallHierarchyBrowser)hierarchyBrowser).changeView(CallHierarchyBrowserBase.getCallerType());
    }
}
