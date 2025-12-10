package dev.xframe.hierarchy;

import com.intellij.ide.hierarchy.HierarchyNodeDescriptor;
import com.intellij.ide.hierarchy.HierarchyTreeStructure;
import com.intellij.ide.hierarchy.call.CallHierarchyNodeDescriptor;
import com.intellij.ide.hierarchy.call.CallReferenceProcessor;
import com.intellij.ide.hierarchy.call.JavaCallHierarchyData;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnonymousClass;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiNewExpression;
import com.intellij.psi.PsiQualifiedReference;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.psi.search.searches.MethodReferencesSearch;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

//copy from com.intellij.ide.hierarchy.call.CallerMethodsTreeStructure
public class CallerMethodsTreeStructure extends HierarchyTreeStructure {

    private final String myScopeType;
    /**
     * Should be called in read action
     */
    public CallerMethodsTreeStructure(@NotNull Project project, @NotNull PsiMember member, String scopeType) {
        super(project, new CallHierarchyNodeDescriptor(project, null, member, true, false));
        myScopeType = scopeType;
    }

    protected boolean isClassReferenceMatched(PsiElement e) {
        return e instanceof PsiNewExpression || e.getParent() instanceof PsiNewExpression;
    }

    protected boolean searchClassConstructor() {
        return true;
    }

    protected PsiElement getBaseMemberContainingClass(PsiMember e) {
        return  e instanceof PsiClass ? e : e.getContainingClass();
    }

    @Override
    protected Object @NotNull [] buildChildren(@NotNull HierarchyNodeDescriptor descriptor) {
        PsiMember enclosingElement = ((CallHierarchyNodeDescriptor) descriptor).getEnclosingElement();
        if (enclosingElement == null) return ArrayUtilRt.EMPTY_OBJECT_ARRAY;

        HierarchyNodeDescriptor nodeDescriptor = getBaseDescriptor();
        if (nodeDescriptor == null) return ArrayUtilRt.EMPTY_OBJECT_ARRAY;

        if(enclosingElement instanceof PsiClass) {
            PsiClass psiClass = (PsiClass) enclosingElement;
            if(searchClassConstructor()) {
                //search by constructors
                Object[] children = Arrays.stream(psiClass.getConstructors()).map(e -> new CallHierarchyNodeDescriptor(myProject, nodeDescriptor, e, false, false)).toArray();
                if(children.length > 0) return children;
                //search by sub classes;
                children = ClassInheritorsSearch.search(psiClass).findAll().stream().map(e -> new CallHierarchyNodeDescriptor(myProject, nodeDescriptor, e, false, false)).toArray();
                if(children.length > 0) return children;
            }
            //search by reference
            return ReferencesSearch.search(psiClass, psiClass.getUseScope()).findAll().stream().map(PsiReference::getElement).filter(this::isClassReferenceMatched).distinct().map(e -> new CallHierarchyNodeDescriptor(myProject, nodeDescriptor, e, false, false)).toArray();
        }

        PsiClass enclosingClass = enclosingElement.getContainingClass();
        PsiClass expectedQualifierClass; // we'll compare reference qualifier class against this to filter out irrelevant usages
        if (enclosingElement instanceof PsiMethod && isLocalOrAnonymousClass(enclosingClass)) {
            PsiElement parent = enclosingClass.getParent();
            PsiElement grandParent = parent instanceof PsiNewExpression ? parent.getParent() : null;
            if (grandParent instanceof PsiExpressionList) {
                // for created anonymous class that immediately passed as argument use instantiation point as next call point (IDEA-73312)
                enclosingElement = CallHierarchyNodeDescriptor.getEnclosingElement(grandParent);
                enclosingClass = enclosingElement == null ? null : enclosingElement.getContainingClass();
            }
            if (enclosingClass instanceof PsiAnonymousClass) {
                expectedQualifierClass = enclosingClass.getSuperClass();
            } else {
                expectedQualifierClass = enclosingClass;
            }
        } else {
            expectedQualifierClass = enclosingClass;
        }

        PsiMember baseMember = (PsiMember) ((CallHierarchyNodeDescriptor) nodeDescriptor).getTargetElement();
        if (baseMember == null) return ArrayUtilRt.EMPTY_OBJECT_ARRAY;

        SearchScope searchScope = getSearchScope(myScopeType, getBaseMemberContainingClass(baseMember));

        PsiMember member = enclosingElement;
        PsiClass originalClass = member.getContainingClass();

        if (originalClass == null) return ArrayUtilRt.EMPTY_OBJECT_ARRAY;

        PsiClassType originalType = JavaPsiFacade.getElementFactory(myProject).createType(originalClass);
        Set<PsiMethod> methodsToFind = new HashSet<>();

        if (enclosingElement instanceof PsiMethod) {
            PsiMethod method = (PsiMethod) enclosingElement;
            methodsToFind.add(method);
            ContainerUtil.addAll(methodsToFind, method.findDeepestSuperMethods());

            Map<PsiMember, NodeDescriptor<?>> methodToDescriptorMap = new HashMap<>();
            for (PsiMethod methodToFind : methodsToFind) {
                JavaCallHierarchyData data = new JavaCallHierarchyData(originalClass, methodToFind, originalType, method, methodsToFind, descriptor, methodToDescriptorMap, myProject);

                MethodReferencesSearch.search(methodToFind, searchScope, true).forEach(reference -> {
                    // references in javadoc really couldn't "call" anything
                    if (PsiUtil.isInsideJavadocComment(reference.getElement())) {
                        return true;
                    }
                    PsiClass receiverClass = null;
                    if (reference instanceof PsiQualifiedReference) {
                        PsiElement qualifier = ((PsiQualifiedReference) reference).getQualifier();
                        if (qualifier instanceof PsiExpression) {
                            PsiType type = ((PsiExpression) qualifier).getType();
                            receiverClass = PsiUtil.resolveClassInClassTypeOnly(type);
                        }
                    }
                    if (receiverClass == null) {
                        PsiElement resolved = reference.resolve();
                        if (resolved instanceof PsiMethod) {
                            receiverClass = ((PsiMethod) resolved).getContainingClass();
                        }
                    }

                    if (receiverClass != null
                            && expectedQualifierClass != null
                            && !InheritanceUtil.isInheritorOrSelf(expectedQualifierClass, receiverClass, true)
                            && !InheritanceUtil.isInheritorOrSelf(receiverClass, expectedQualifierClass, true)
                    ) {
                        // ignore impossible candidates. E.g. when A < B,A < C and we invoked call hierarchy for method in C we should filter out methods in B because B and C are assignment-incompatible
                        return true;
                    }
                    for (CallReferenceProcessor processor : CallReferenceProcessor.EP_NAME.getExtensions()) {
                        if (!processor.process(reference, data)) break;
                    }
                    return true;
                });
            }

            return ArrayUtil.toObjectArray(methodToDescriptorMap.values());
        }

        assert enclosingElement instanceof PsiField : "Enclosing element should be a field, but was " + enclosingElement.getClass() + ", text: " + enclosingElement.getText();

        return ReferencesSearch
                .search(enclosingElement, enclosingElement.getUseScope()).findAll().stream()
                .map(PsiReference::getElement)
                .distinct()
                .map(e -> new CallHierarchyNodeDescriptor(myProject, nodeDescriptor, e, false, false)).toArray();
    }

    private static boolean isLocalOrAnonymousClass(PsiMember enclosingElement) {
        return enclosingElement instanceof PsiClass && ((PsiClass) enclosingElement).getQualifiedName() == null;
    }

    @Override
    public String toString() {
        return "Caller Hierarchy for " + formatBaseElementText();
    }
}

