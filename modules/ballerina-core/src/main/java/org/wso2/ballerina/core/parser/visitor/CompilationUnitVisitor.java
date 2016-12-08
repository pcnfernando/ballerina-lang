/*
 * Copyright (c) 2016, WSO2 Inc. (http://wso2.com) All Rights Reserved.
 * <p>
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.ballerina.core.parser.visitor;

import org.wso2.ballerina.core.model.Import;
import org.wso2.ballerina.core.model.Package;
import org.wso2.ballerina.core.parser.BallerinaBaseVisitor;
import org.wso2.ballerina.core.parser.BallerinaParser;

/**
 * Visitor for compilation unit
 */
public class CompilationUnitVisitor extends BallerinaBaseVisitor {

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation returns the result of calling
     * {@link #visitChildren} on {@code ctx}.</p>
     *
     * @param ctx
     */
    @Override
    public Object visitPackageDeclaration(BallerinaParser.PackageDeclarationContext ctx) {
        String packageName = ctx.packageName().getText();
        return new Package(packageName);
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation returns the result of calling
     * {@link #visitChildren} on {@code ctx}.</p>
     *
     * @param ctx
     */
    @Override
    public Object visitImportDeclaration(BallerinaParser.ImportDeclarationContext ctx) {
        String importPackageName = ctx.packageName().getText();
        if (ctx.Identifier() != null) {
            String importAsName = ctx.Identifier().getText();
            return new Import(importAsName, importPackageName);
        } else {
            return new Import(importPackageName);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation returns the result of calling
     * {@link #visitChildren} on {@code ctx}.</p>
     *
     * @param ctx
     */
    @Override
    public Object visitPackageName(BallerinaParser.PackageNameContext ctx) {
        String packageName = ctx.getText();
        return packageName;
    }
}
