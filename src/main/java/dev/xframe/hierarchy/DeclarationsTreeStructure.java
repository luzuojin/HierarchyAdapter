package dev.xframe.hierarchy;

import com.intellij.ide.hierarchy.HierarchyNodeDescriptor;
import com.intellij.ide.hierarchy.HierarchyTreeStructure;
import com.intellij.ide.hierarchy.call.CallHierarchyNodeDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiTypeElement;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ArrayUtilRt;
import org.jetbrains.annotations.NotNull;

//copy from com.intellij.ide.hierarchy.call.CallerMethodsTreeStructure
public class DeclarationsTreeStructure extends HierarchyTreeStructure {

    private final String myScopeType;
    /**
     * Should be called in read action
     */
    public DeclarationsTreeStructure(@NotNull Project project, @NotNull PsiMember member, String scopeType) {
        super(project, new CallHierarchyNodeDescriptor(project, null, member, true, false));
        myScopeType = scopeType;
    }

    protected boolean isClassReferenceMatched(PsiElement e) {
        return e instanceof PsiTypeElement || e.getParent() instanceof PsiTypeElement;
    }

    @Override
    protected Object @NotNull [] buildChildren(@NotNull HierarchyNodeDescriptor descriptor) {
        PsiMember enclosingElement = ((CallHierarchyNodeDescriptor) descriptor).getEnclosingElement();
        if (enclosingElement == null) return ArrayUtilRt.EMPTY_OBJECT_ARRAY;

        HierarchyNodeDescriptor nodeDescriptor = getBaseDescriptor();
        if (nodeDescriptor == null) return ArrayUtilRt.EMPTY_OBJECT_ARRAY;

        if(!(enclosingElement instanceof PsiClass)) {
            enclosingElement =  PsiTreeUtil.getParentOfType(enclosingElement, PsiClass.class, false);
        }
        if(enclosingElement == null) return ArrayUtilRt.EMPTY_OBJECT_ARRAY;
        PsiClass psiClass = (PsiClass) enclosingElement;
        //search by reference
        return ReferencesSearch.search(psiClass, psiClass.getUseScope()).findAll().stream().map(PsiReference::getElement).filter(this::isClassReferenceMatched).distinct().map(e -> new CallHierarchyNodeDescriptor(myProject, nodeDescriptor, e, false, false)).toArray();
    }

    @Override
    public String toString() {
        return "Declarations for " + formatBaseElementText();
    }
}

