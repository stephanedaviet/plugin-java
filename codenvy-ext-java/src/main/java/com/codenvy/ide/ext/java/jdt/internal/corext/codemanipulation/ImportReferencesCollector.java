/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.codenvy.ide.ext.java.jdt.internal.corext.codemanipulation;

import com.codenvy.ide.ext.java.jdt.core.dom.AST;
import com.codenvy.ide.ext.java.jdt.core.dom.ASTNode;
import com.codenvy.ide.ext.java.jdt.core.dom.ArrayType;
import com.codenvy.ide.ext.java.jdt.core.dom.ClassInstanceCreation;
import com.codenvy.ide.ext.java.jdt.core.dom.CompilationUnit;
import com.codenvy.ide.ext.java.jdt.core.dom.Expression;
import com.codenvy.ide.ext.java.jdt.core.dom.FieldAccess;
import com.codenvy.ide.ext.java.jdt.core.dom.IBinding;
import com.codenvy.ide.ext.java.jdt.core.dom.IMethodBinding;
import com.codenvy.ide.ext.java.jdt.core.dom.ITypeBinding;
import com.codenvy.ide.ext.java.jdt.core.dom.IVariableBinding;
import com.codenvy.ide.ext.java.jdt.core.dom.ImportDeclaration;
import com.codenvy.ide.ext.java.jdt.core.dom.MarkerAnnotation;
import com.codenvy.ide.ext.java.jdt.core.dom.MemberRef;
import com.codenvy.ide.ext.java.jdt.core.dom.MethodDeclaration;
import com.codenvy.ide.ext.java.jdt.core.dom.MethodInvocation;
import com.codenvy.ide.ext.java.jdt.core.dom.MethodRef;
import com.codenvy.ide.ext.java.jdt.core.dom.MethodRefParameter;
import com.codenvy.ide.ext.java.jdt.core.dom.Modifier;
import com.codenvy.ide.ext.java.jdt.core.dom.Name;
import com.codenvy.ide.ext.java.jdt.core.dom.NormalAnnotation;
import com.codenvy.ide.ext.java.jdt.core.dom.PackageDeclaration;
import com.codenvy.ide.ext.java.jdt.core.dom.QualifiedName;
import com.codenvy.ide.ext.java.jdt.core.dom.QualifiedType;
import com.codenvy.ide.ext.java.jdt.core.dom.SimpleName;
import com.codenvy.ide.ext.java.jdt.core.dom.SimpleType;
import com.codenvy.ide.ext.java.jdt.core.dom.SingleMemberAnnotation;
import com.codenvy.ide.ext.java.jdt.core.dom.SuperConstructorInvocation;
import com.codenvy.ide.ext.java.jdt.core.dom.TagElement;
import com.codenvy.ide.ext.java.jdt.core.dom.ThisExpression;
import com.codenvy.ide.ext.java.jdt.core.dom.TypeDeclaration;
import com.codenvy.ide.ext.java.jdt.internal.corext.dom.GenericVisitor;
import com.codenvy.ide.ext.java.jdt.internal.corext.dom.ScopeAnalyzer;
import com.codenvy.ide.api.text.Region;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class ImportReferencesCollector extends GenericVisitor {

    public static void collect(ASTNode node, Region rangeLimit, Collection<SimpleName> resultingTypeImports,
                               Collection<SimpleName> resultingStaticImports) {
        collect(node, rangeLimit, false, resultingTypeImports, resultingStaticImports);
    }

    public static void collect(ASTNode node, Region rangeLimit, boolean skipMethodBodies,
                               Collection<SimpleName> resultingTypeImports, Collection<SimpleName> resultingStaticImports) {
        ASTNode root = node.getRoot();
        CompilationUnit astRoot = root instanceof CompilationUnit ? (CompilationUnit)root : null;
        node.accept(new ImportReferencesCollector(astRoot, rangeLimit, skipMethodBodies, resultingTypeImports,
                                                  resultingStaticImports));
    }

    private CompilationUnit fASTRoot;

    private Region fSubRange;

    private Collection<SimpleName> fTypeImports;

    private Collection<SimpleName> fStaticImports;

    private boolean fSkipMethodBodies;

    private ImportReferencesCollector(CompilationUnit astRoot, Region rangeLimit, boolean skipMethodBodies,
                                      Collection<SimpleName> resultingTypeImports, Collection<SimpleName> resultingStaticImports) {
        super(processJavadocComments(astRoot));
        fTypeImports = resultingTypeImports;
        fStaticImports = resultingStaticImports;
        fSubRange = rangeLimit;
        //      if (project == null || !JavaModelUtil.is50OrHigher(project)) {
        //         fStaticImports= null; // do not collect
        //      }
        fASTRoot = astRoot; // can be null
        fSkipMethodBodies = skipMethodBodies;
    }

    private static boolean processJavadocComments(CompilationUnit astRoot) {
        // don't visit Javadoc for 'package-info' (bug 216432)
        //      if (astRoot != null && astRoot.getTypeRoot() != null)
        //      {
        //         return !"package-info.java".equals(astRoot.getTypeRoot().getElementName()); //$NON-NLS-1$
        //      }
        return true;
    }

    private boolean isAffected(ASTNode node) {
        if (fSubRange == null) {
            return true;
        }
        int nodeStart = node.getStartPosition();
        int offset = fSubRange.getOffset();
        return nodeStart + node.getLength() > offset && offset + fSubRange.getLength() > nodeStart;
    }

    private void addReference(SimpleName name) {
        if (isAffected(name)) {
            fTypeImports.add(name);
        }
    }

    private void typeRefFound(Name node) {
        if (node != null) {
            while (node.isQualifiedName()) {
                node = ((QualifiedName)node).getQualifier();
            }
            addReference((SimpleName)node);
        }
    }

    private void possibleTypeRefFound(Name node) {
        while (node.isQualifiedName()) {
            node = ((QualifiedName)node).getQualifier();
        }
        IBinding binding = node.resolveBinding();
        if (binding == null || binding.getKind() == IBinding.TYPE) {
            // if the binding is null, we cannot determine if
            // we have a type binding or not, so we will assume
            // we do.
            addReference((SimpleName)node);
        }
    }

    private void possibleStaticImportFound(Name name) {
        if (fStaticImports == null || fASTRoot == null) {
            return;
        }

        while (name.isQualifiedName()) {
            name = ((QualifiedName)name).getQualifier();
        }
        if (!isAffected(name)) {
            return;
        }

        IBinding binding = name.resolveBinding();
        SimpleName simpleName = (SimpleName)name;
        if (binding == null || binding instanceof ITypeBinding || !Modifier.isStatic(binding.getModifiers())
            || simpleName.isDeclaration()) {
            return;
        }

        if (binding instanceof IVariableBinding) {
            IVariableBinding varBinding = (IVariableBinding)binding;
            if (varBinding.isField()) {
                varBinding = varBinding.getVariableDeclaration();
                ITypeBinding declaringClass = varBinding.getDeclaringClass();
                if (declaringClass != null && !declaringClass.isLocal()) {
                    if (new ScopeAnalyzer(fASTRoot).isDeclaredInScope(varBinding, simpleName, ScopeAnalyzer.VARIABLES
                                                                                              | ScopeAnalyzer.CHECK_VISIBILITY))
                        return;
                    fStaticImports.add(simpleName);
                }
            }
        } else if (binding instanceof IMethodBinding) {
            IMethodBinding methodBinding = ((IMethodBinding)binding).getMethodDeclaration();
            ITypeBinding declaringClass = methodBinding.getDeclaringClass();
            if (declaringClass != null && !declaringClass.isLocal()) {
                if (new ScopeAnalyzer(fASTRoot).isDeclaredInScope(methodBinding, simpleName, ScopeAnalyzer.METHODS
                                                                                             | ScopeAnalyzer.CHECK_VISIBILITY))
                    return;
                fStaticImports.add(simpleName);
            }
        }

    }

    private void doVisitChildren(List elements) {
        int nElements = elements.size();
        for (int i = 0; i < nElements; i++) {
            ((ASTNode)elements.get(i)).accept(this);
        }
    }

    private void doVisitNode(ASTNode node) {
        if (node != null) {
            node.accept(this);
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.jdt.internal.corext.dom.GenericVisitor#visitNode(org.eclipse.jdt.core.dom.ASTNode)
     */
    @Override
    protected boolean visitNode(ASTNode node) {
        return isAffected(node);
    }

    /*
     * @see ASTVisitor#visit(ArrayType)
     */
    @Override
    public boolean visit(ArrayType node) {
        doVisitNode(node.getElementType());
        return false;
    }

    /*
     * @see ASTVisitor#visit(SimpleType)
     */
    @Override
    public boolean visit(SimpleType node) {
        typeRefFound(node.getName());
        return false;
    }

    /*
     * @see ASTVisitor#visit(QualifiedType)
     */
    @Override
    public boolean visit(QualifiedType node) {
        // nothing to do here, let the qualifier be visited
        return true;
    }

    /*
     * @see ASTVisitor#visit(QualifiedName)
     */
    @Override
    public boolean visit(QualifiedName node) {
        possibleTypeRefFound(node); // possible ref
        possibleStaticImportFound(node);
        return false;
    }

    /*
     * @see ASTVisitor#visit(ImportDeclaration)
     */
    @Override
    public boolean visit(ImportDeclaration node) {
        return false;
    }

    /*
     * @see ASTVisitor#visit(PackageDeclaration)
     */
    @Override
    public boolean visit(PackageDeclaration node) {
        if (node.getAST().apiLevel() >= AST.JLS3) {
            doVisitNode(node.getJavadoc());
            doVisitChildren(node.annotations());
        }
        return false;
    }

    /*
     * @see ASTVisitor#visit(ThisExpression)
     */
    @Override
    public boolean visit(ThisExpression node) {
        typeRefFound(node.getQualifier());
        return false;
    }

    private void evalQualifyingExpression(Expression expr, Name selector) {
        if (expr != null) {
            if (expr instanceof Name) {
                Name name = (Name)expr;
                possibleTypeRefFound(name);
                possibleStaticImportFound(name);
            } else {
                expr.accept(this);
            }
        } else if (selector != null) {
            possibleStaticImportFound(selector);
        }
    }

    /*
     * @see ASTVisitor#visit(ClassInstanceCreation)
     */
    @Override
    public boolean visit(ClassInstanceCreation node) {
        doVisitChildren(node.typeArguments());
        doVisitNode(node.getType());
        evalQualifyingExpression(node.getExpression(), null);
        if (node.getAnonymousClassDeclaration() != null) {
            node.getAnonymousClassDeclaration().accept(this);
        }
        doVisitChildren(node.arguments());
        return false;
    }

    /*
     * @see ASTVisitor#endVisit(MethodInvocation)
     */
    @Override
    public boolean visit(MethodInvocation node) {
        evalQualifyingExpression(node.getExpression(), node.getName());
        doVisitChildren(node.typeArguments());
        doVisitChildren(node.arguments());
        return false;
    }

    /*
     * @see ASTVisitor#visit(SuperConstructorInvocation)
     */
    @Override
    public boolean visit(SuperConstructorInvocation node) {
        if (!isAffected(node)) {
            return false;
        }

        evalQualifyingExpression(node.getExpression(), null);
        doVisitChildren(node.typeArguments());
        doVisitChildren(node.arguments());
        return false;
    }

    /*
     * @see ASTVisitor#visit(FieldAccess)
     */
    @Override
    public boolean visit(FieldAccess node) {
        evalQualifyingExpression(node.getExpression(), node.getName());
        return false;
    }

    /*
     * @see ASTVisitor#visit(SimpleName)
     */
    @Override
    public boolean visit(SimpleName node) {
        // if the call gets here, it can only be a variable reference
        possibleStaticImportFound(node);
        return false;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jdt.internal.corext.dom.GenericVisitor#visit(org.eclipse.jdt.core.dom.MarkerAnnotation)
     */
    @Override
    public boolean visit(MarkerAnnotation node) {
        typeRefFound(node.getTypeName());
        return false;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jdt.internal.corext.dom.GenericVisitor#visit(org.eclipse.jdt.core.dom.MarkerAnnotation)
     */
    @Override
    public boolean visit(NormalAnnotation node) {
        typeRefFound(node.getTypeName());
        doVisitChildren(node.values());
        return false;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jdt.internal.corext.dom.GenericVisitor#visit(org.eclipse.jdt.core.dom.MarkerAnnotation)
     */
    @Override
    public boolean visit(SingleMemberAnnotation node) {
        typeRefFound(node.getTypeName());
        doVisitNode(node.getValue());
        return false;
    }

    /*
     * @see ASTVisitor#visit(TypeDeclaration)
     */
    @Override
    public boolean visit(TypeDeclaration node) {
        if (!isAffected(node)) {
            return false;
        }
        return true;
    }

    /*
     * @see ASTVisitor#visit(MethodDeclaration)
     */
    @Override
    public boolean visit(MethodDeclaration node) {
        if (!isAffected(node)) {
            return false;
        }
        doVisitNode(node.getJavadoc());

        if (node.getAST().apiLevel() >= AST.JLS3) {
            doVisitChildren(node.modifiers());
            doVisitChildren(node.typeParameters());
        }

        if (!node.isConstructor()) {
            doVisitNode(node.getReturnType2());
        }
        doVisitChildren(node.parameters());
        Iterator<Name> iter = node.thrownExceptions().iterator();
        while (iter.hasNext()) {
            typeRefFound(iter.next());
        }
        if (!fSkipMethodBodies) {
            doVisitNode(node.getBody());
        }
        return false;
    }

    @Override
    public boolean visit(TagElement node) {
        String tagName = node.getTagName();
        List<? extends ASTNode> list = node.fragments();
        int idx = 0;
        if (tagName != null && !list.isEmpty()) {
            Object first = list.get(0);
            if (first instanceof Name) {
                if ("@throws".equals(tagName) || "@exception".equals(tagName)) { //$NON-NLS-1$//$NON-NLS-2$
                    typeRefFound((Name)first);
                } else if ("@see".equals(tagName) || "@link".equals(tagName) ||
                           "@linkplain".equals(tagName)) { //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
                    Name name = (Name)first;
                    possibleTypeRefFound(name);
                }
                idx++;
            }
        }
        for (int i = idx; i < list.size(); i++) {
            doVisitNode(list.get(i));
        }
        return false;
    }

    @Override
    public boolean visit(MemberRef node) {
        Name qualifier = node.getQualifier();
        if (qualifier != null) {
            typeRefFound(qualifier);
        }
        return false;
    }

    @Override
    public boolean visit(MethodRef node) {
        Name qualifier = node.getQualifier();
        if (qualifier != null) {
            typeRefFound(qualifier);
        }
        List<MethodRefParameter> list = node.parameters();
        if (list != null) {
            doVisitChildren(list); // visit MethodRefParameter with Type
        }
        return false;
    }
}
