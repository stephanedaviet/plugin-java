/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.codenvy.ide.ext.java.jdt.internal.compiler.ast;

import com.codenvy.ide.ext.java.jdt.internal.compiler.ASTVisitor;
import com.codenvy.ide.ext.java.jdt.internal.compiler.impl.CharConstant;
import com.codenvy.ide.ext.java.jdt.internal.compiler.lookup.BlockScope;
import com.codenvy.ide.ext.java.jdt.internal.compiler.lookup.TypeBinding;
import com.codenvy.ide.ext.java.jdt.internal.compiler.parser.ScannerHelper;

public class CharLiteral extends NumberLiteral {

    char value;

    public CharLiteral(char[] token, int s, int e) {
        super(token, s, e);
        computeValue();
    }

    @Override
    public void computeConstant() {
        //The source is a  char[3] first and last char are '
        //This is true for both regular char AND unicode char
        //BUT not for escape char like '\b' which are char[4]....
        this.constant = CharConstant.fromValue(this.value);
    }

    private void computeValue() {
        //The source is a  char[3] first and last char are '
        //This is true for both regular char AND unicode char
        //BUT not for escape char like '\b' which are char[4]....
        if ((this.value = this.source[1]) != '\\') {
            return;
        }
        char digit;
        switch (digit = this.source[2]) {
            case 'b':
                this.value = '\b';
                break;
            case 't':
                this.value = '\t';
                break;
            case 'n':
                this.value = '\n';
                break;
            case 'f':
                this.value = '\f';
                break;
            case 'r':
                this.value = '\r';
                break;
            case '\"':
                this.value = '\"';
                break;
            case '\'':
                this.value = '\'';
                break;
            case '\\':
                this.value = '\\';
                break;
            default: //octal (well-formed: ended by a ' )
                int number = ScannerHelper.getNumericValue(digit);
                if ((digit = this.source[3]) != '\'') {
                    number = (number * 8) + ScannerHelper.getNumericValue(digit);
                } else {
                    this.constant = CharConstant.fromValue(this.value = (char)number);
                    break;
                }
                if ((digit = this.source[4]) != '\'') {
                    number = (number * 8) + ScannerHelper.getNumericValue(digit);
                }
                this.value = (char)number;
                break;
        }
    }

    /**
     * CharLiteral code generation
     *
     * @param currentScope
     *         com.codenvy.ide.java.client.internal.compiler.lookup.BlockScope
     * @param codeStream
     *         com.codenvy.ide.java.client.internal.compiler.codegen.CodeStream
     * @param valueRequired
     *         boolean
     */
    @Override
    public void generateCode(BlockScope currentScope, boolean valueRequired) {

    }

    @Override
    public TypeBinding literalType(BlockScope scope) {
        return TypeBinding.CHAR;
    }

    @Override
    public void traverse(ASTVisitor visitor, BlockScope blockScope) {
        visitor.visit(this, blockScope);
        visitor.endVisit(this, blockScope);
    }
}
