/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.wso2.ballerinalang.compiler.desugar;

import org.ballerinalang.model.TreeBuilder;
import org.ballerinalang.model.tree.types.TypeNode;
import org.ballerinalang.model.types.TypeKind;
import org.wso2.ballerinalang.compiler.parser.BLangAnonymousModelHelper;
import org.wso2.ballerinalang.compiler.semantics.analyzer.SymbolEnter;
import org.wso2.ballerinalang.compiler.semantics.analyzer.SymbolResolver;
import org.wso2.ballerinalang.compiler.semantics.analyzer.Types;
import org.wso2.ballerinalang.compiler.semantics.model.SymbolEnv;
import org.wso2.ballerinalang.compiler.semantics.model.SymbolTable;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BInvokableSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BPackageSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BVarSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.SymTag;
import org.wso2.ballerinalang.compiler.semantics.model.types.BArrayType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BInvokableType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BStreamType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BType;
import org.wso2.ballerinalang.compiler.tree.BLangFunction;
import org.wso2.ballerinalang.compiler.tree.BLangIdentifier;
import org.wso2.ballerinalang.compiler.tree.BLangInvokableNode;
import org.wso2.ballerinalang.compiler.tree.BLangNodeVisitor;
import org.wso2.ballerinalang.compiler.tree.BLangPackage;
import org.wso2.ballerinalang.compiler.tree.BLangSimpleVariable;
import org.wso2.ballerinalang.compiler.tree.clauses.BLangDoClause;
import org.wso2.ballerinalang.compiler.tree.clauses.BLangFromClause;
import org.wso2.ballerinalang.compiler.tree.clauses.BLangSelectClause;
import org.wso2.ballerinalang.compiler.tree.clauses.BLangWhereClause;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangExpression;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangIndexBasedAccess;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangInvocation;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangLambdaFunction;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangListConstructorExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangQueryExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangSimpleVarRef;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangStatementExpression;
import org.wso2.ballerinalang.compiler.tree.statements.BLangAssignment;
import org.wso2.ballerinalang.compiler.tree.statements.BLangBlockStmt;
import org.wso2.ballerinalang.compiler.tree.statements.BLangExpressionStmt;
import org.wso2.ballerinalang.compiler.tree.statements.BLangForeach;
import org.wso2.ballerinalang.compiler.tree.statements.BLangIf;
import org.wso2.ballerinalang.compiler.tree.statements.BLangQueryAction;
import org.wso2.ballerinalang.compiler.tree.statements.BLangSimpleVariableDef;
import org.wso2.ballerinalang.compiler.tree.types.BLangValueType;
import org.wso2.ballerinalang.compiler.util.ClosureVarSymbol;
import org.wso2.ballerinalang.compiler.util.CompilerContext;
import org.wso2.ballerinalang.compiler.util.Name;
import org.wso2.ballerinalang.compiler.util.Names;
import org.wso2.ballerinalang.compiler.util.TypeTags;
import org.wso2.ballerinalang.compiler.util.diagnotic.BLangDiagnosticLog;
import org.wso2.ballerinalang.compiler.util.diagnotic.DiagnosticPos;
import org.wso2.ballerinalang.util.Flags;
import org.wso2.ballerinalang.util.Lists;

import java.util.ArrayList;
import java.util.List;

/**
 * Class responsible for desugar query pipeline into actual Ballerina code.
 *
 * @since 1.2.0
 */
public class QueryDesugar extends BLangNodeVisitor {

    private static final CompilerContext.Key<QueryDesugar> QUERY_DESUGAR_KEY =
            new CompilerContext.Key<>();
    private final SymbolEnter symbolEnter;
    private final Desugar desugar;
    private final SymbolTable symTable;
    private final BLangAnonymousModelHelper anonymousModelHelper;
    private BLangDiagnosticLog dlog;
    private final SymbolResolver symResolver;
    private final Names names;
    private final Types types;
    private BLangForeach parentForeach = null;

    private QueryDesugar(CompilerContext context) {
        context.put(QUERY_DESUGAR_KEY, this);
        this.symTable = SymbolTable.getInstance(context);
        this.symResolver = SymbolResolver.getInstance(context);
        this.symbolEnter = SymbolEnter.getInstance(context);
        this.names = Names.getInstance(context);
        this.types = Types.getInstance(context);
        this.dlog = BLangDiagnosticLog.getInstance(context);
        this.desugar = Desugar.getInstance(context);
        this.anonymousModelHelper = BLangAnonymousModelHelper.getInstance(context);
    }

    public static QueryDesugar getInstance(CompilerContext context) {
        QueryDesugar desugar = context.get(QUERY_DESUGAR_KEY);
        if (desugar == null) {
            desugar = new QueryDesugar(context);
        }

        return desugar;
    }

    BLangStatementExpression desugarQueryExpr(BLangQueryExpr queryExpr, SymbolEnv env) {
        List<BLangFromClause> fromClauseList = queryExpr.fromClauseList;
        BLangFromClause fromClause = fromClauseList.get(0);
        BLangSelectClause selectClause = queryExpr.selectClause;
        List<BLangWhereClause> whereClauseList = queryExpr.whereClauseList;
        DiagnosticPos pos = fromClause.pos;

        // Create Foreach statement
        //
        // Below query expression :
        //      from var person in personList
        //
        // changes as,
        //      foreach var person in personList {
        //          ....
        //      }

        BType outputArrayType;
        if (selectClause.expression != null && selectClause.expression.type != null) {
            outputArrayType = new BArrayType(selectClause.expression.type);
        } else {
            outputArrayType = fromClause.varType;
        }

        // Create temp array variable
        //      Person[] x = [];

        BLangListConstructorExpr emptyArrayExpr = ASTBuilderUtil.createEmptyArrayLiteral(pos,
                (BArrayType) outputArrayType);

        BVarSymbol emptyArrayVarSymbol = new BVarSymbol(Flags.PUBLIC, new Name("$outputDataArray$"),
                env.scope.owner.pkgID, outputArrayType, env.scope.owner);
        BLangSimpleVariable outputArrayVariable =
                ASTBuilderUtil.createVariable(pos, "$outputDataArray$", outputArrayType,
                        emptyArrayExpr, emptyArrayVarSymbol);
        outputArrayVariable.symbol.closure = true;
//        symbolEnter.defineSymbol(pos, outputArrayVariable.symbol, env);


//        BLangInvokableNode encInvokable = env.enclInvokable;
//        ((BLangFunction) encInvokable).closureVarSymbols.add(new ClosureVarSymbol(outputArrayVariable.symbol, pos));
//        ((BLangFunction) encInvokable).closureVarSymbols.add(new ClosureVarSymbol(outputArrayVariable.symbol, pos));

        BLangSimpleVariableDef outputVariableDef =
                ASTBuilderUtil.createVariableDef(pos, outputArrayVariable);
        BLangSimpleVarRef outputVarRef = ASTBuilderUtil.createVariableRef(pos, outputArrayVariable.symbol);

        // Create indexed based access expression statement
        //      x[x.length()] = {
        //         firstName: person.firstName,
        //         lastName: person.lastName
        //      };
        if (selectClause.expression.type == null) {
            selectClause.expression.type = fromClause.varType;
        }

        BLangInvocation lengthInvocation = desugar.createLengthInvocation(selectClause.pos, outputVarRef);
        lengthInvocation.expr = outputVarRef;
        BLangIndexBasedAccess indexAccessExpr = ASTBuilderUtil.createIndexAccessExpr(outputVarRef, lengthInvocation);
        indexAccessExpr.type = selectClause.expression.type;

        BLangAssignment outputVarAssignment = ASTBuilderUtil.createAssignmentStmt(pos, indexAccessExpr,
                selectClause.expression);
        // nill return type for the annonymous fucntion
        BLangValueType nullType = new BLangValueType();
        nullType.typeKind = TypeKind.NIL;
        nullType.type = symTable.nilType;

        // Set the indexed based access expression statement as foreach body
        BLangSimpleVariable input = ((BLangSimpleVariableDef) fromClause.getVariableDefinitionNode()).var;
        BLangLambdaFunction labmdafunc = createLambdaFunction(pos, Lists.of(input), nullType);
        BLangBlockStmt lambdaBody = labmdafunc.function.body;
        lambdaBody.stmts.add(outputVarAssignment);
//        labmdafunc.function.closureVarSymbols.add(new ClosureVarSymbol(outputArrayVariable.symbol, pos));
//        outputArrayVariable.symbol.closure = true;


        labmdafunc.function.body = buildWhereBlock(whereClauseList, lambdaBody, pos);;
        labmdafunc.cachedEnv = desugar.env.createClone();

        BLangExpressionStmt stmt = ASTBuilderUtil.createExpressionStmt(pos, ASTBuilderUtil.createBlockStmt(pos));


        //Crete toStream invocation
        BType streamType = new BStreamType(TypeTags.STREAM,  selectClause.expression.type, symTable.streamType.tsymbol);
        BLangInvocation toStreamInvocation = createToStreamInvocation(pos,(BLangSimpleVarRef)fromClause.collection, streamType);

        BVarSymbol streamVarSymbol = new BVarSymbol(Flags.PUBLIC, new Name("$streamedList$"),
                env.scope.owner.pkgID, outputArrayType, env.scope.owner);
        BLangSimpleVariable streamVariable =
                ASTBuilderUtil.createVariable(pos, "$streamedList$", streamType,
                        toStreamInvocation, streamVarSymbol);
        BLangSimpleVariableDef streamVariableDef =
                ASTBuilderUtil.createVariableDef(pos, streamVariable);
        BLangSimpleVarRef streamVarRef = ASTBuilderUtil.createVariableRef(pos, streamVariable.symbol);

        //Create forEach invocation
        BInvokableSymbol foreachInvokableSymbol =
                (BInvokableSymbol) symResolver.lookupLangLibMethod(streamType,
                        names.fromString("forEach"));

        BLangInvocation foreachInvocation = ASTBuilderUtil.createInvocationExprForMethod(pos, foreachInvokableSymbol,
                Lists.of(streamVarRef, labmdafunc), symResolver);
        foreachInvocation.type = foreachInvokableSymbol.type.getReturnType();
        foreachInvocation.symbol = foreachInvokableSymbol;
        foreachInvocation.argExprs = Lists.of(streamVarRef, labmdafunc);
        foreachInvocation.expr = streamVarRef;

        stmt.expr = foreachInvocation;

        // Create block statement with temp variable definition statement & foreach statement
        BLangBlockStmt blockStmt = ASTBuilderUtil.createBlockStmt(pos);
        blockStmt.addStatement(streamVariableDef);
        blockStmt.addStatement(outputVariableDef);
        blockStmt.addStatement(stmt);
        BLangStatementExpression stmtExpr = ASTBuilderUtil.createStatementExpression(blockStmt, outputVarRef);

        stmtExpr.type = outputArrayType;
        return stmtExpr;
    }

    BLangBlockStmt desugarQueryAction(BLangQueryAction queryAction, SymbolEnv env) {
        BLangBlockStmt blockNode = ASTBuilderUtil.createBlockStmt(queryAction.pos);
        List<BLangFromClause> fromClauseList = queryAction.fromClauseList;
        BLangFromClause fromClause = fromClauseList.get(0);
        BLangDoClause doClause = queryAction.doClause;
        List<BLangWhereClause> whereClauseList = queryAction.whereClauseList;
        DiagnosticPos pos = fromClause.pos;

        BLangForeach leafForeach = buildFromClauseBlock(fromClauseList);
        BLangBlockStmt foreachBody = ASTBuilderUtil.createBlockStmt(pos);
        buildWhereClauseBlock(whereClauseList, leafForeach, foreachBody, doClause.pos);

        leafForeach.setBody(doClause.body);
        blockNode.stmts.add(parentForeach);
        return blockNode;
    }

    private BLangLambdaFunction createLambdaFunction(DiagnosticPos pos,
                                                     List<BLangSimpleVariable> lambdaFunctionVariable,
                                                     TypeNode returnType) {

        BLangLambdaFunction lambdaFunction = (BLangLambdaFunction) TreeBuilder.createLambdaFunctionNode();
        BLangFunction func = ASTBuilderUtil.createFunction(pos,
                anonymousModelHelper.getNextAnonymousFunctionKey(desugar.env.enclPkg.packageID));
        lambdaFunction.function = func;
        BLangBlockStmt lambdaBody = ASTBuilderUtil.createBlockStmt(pos);
        func.requiredParams.addAll(lambdaFunctionVariable);
        func.setReturnTypeNode(returnType);
        func.desugaredReturnType = true;
        defineFunction(func, desugar.env.enclPkg);
        lambdaFunctionVariable = func.requiredParams;

        func.body = lambdaBody;
        func.desugared = false;
        lambdaFunction.pos = pos;
        List<BType> paramTypes = new ArrayList<>();
        lambdaFunctionVariable.forEach(variable -> paramTypes.add(variable.symbol.type));
        lambdaFunction.type = new BInvokableType(paramTypes, func.symbol.type.getReturnType(),
                null);
        return lambdaFunction;
    }


    private void defineFunction(BLangFunction funcNode, BLangPackage targetPkg) {
        final BPackageSymbol packageSymbol = targetPkg.symbol;
        final SymbolEnv packageEnv = this.symTable.pkgEnvMap.get(packageSymbol);
        symbolEnter.defineNode(funcNode, packageEnv);
        packageEnv.enclPkg.functions.add(funcNode);
        packageEnv.enclPkg.topLevelNodes.add(funcNode);
    }

    private BLangForeach buildFromClauseBlock(List<BLangFromClause> fromClauseList) {
        BLangForeach leafForeach = null;
        for (BLangFromClause fromClause : fromClauseList) {
            BLangForeach foreach = (BLangForeach) TreeBuilder.createForeachNode();
            foreach.pos = fromClause.pos;
            foreach.collection = fromClause.collection;
            types.setForeachTypedBindingPatternType(foreach);

            foreach.variableDefinitionNode = fromClause.variableDefinitionNode;
            foreach.isDeclaredWithVar = fromClause.isDeclaredWithVar;

            if (leafForeach != null) {
                BLangBlockStmt foreachBody = ASTBuilderUtil.createBlockStmt(fromClause.pos);
                foreachBody.addStatement(foreach);
                leafForeach.setBody(foreachBody);
            } else {
                parentForeach = foreach;
            }

            leafForeach = foreach;
        }

        return leafForeach;
    }

    private void buildWhereClauseBlock(List<BLangWhereClause> whereClauseList, BLangForeach leafForEach,
                                       BLangBlockStmt foreachBody, DiagnosticPos pos) {
        if (whereClauseList.size() > 0) {
            // Create If Statement with Where expression and foreach body
            BLangIf outerIf = null;
            BLangIf innerIf = null;
            for (BLangWhereClause whereClause : whereClauseList) {
                BLangIf bLangIf = (BLangIf) TreeBuilder.createIfElseStatementNode();
                bLangIf.pos = whereClause.pos;
                bLangIf.expr = whereClause.expression;
                if (innerIf != null) {
                    BLangBlockStmt bLangBlockStmt = ASTBuilderUtil.createBlockStmt(whereClause.pos);
                    bLangBlockStmt.addStatement(bLangIf);
                    innerIf.setBody(bLangBlockStmt);
                } else {
                    outerIf = bLangIf;
                }
                innerIf = bLangIf;
            }
            innerIf.setBody(foreachBody);
            BLangBlockStmt bLangBlockStmt = ASTBuilderUtil.createBlockStmt(pos);
            bLangBlockStmt.addStatement(outerIf);
            leafForEach.setBody(bLangBlockStmt);
        } else {
            leafForEach.setBody(foreachBody);
        }
    }

    private BLangBlockStmt buildWhereBlock(List<BLangWhereClause> whereClauseList,
                                       BLangBlockStmt foreachBody, DiagnosticPos pos) {
        if (whereClauseList.size() > 0) {
            BLangBlockStmt bLangBlockStmt = ASTBuilderUtil.createBlockStmt(pos);
            // Create If Statement with Where expression and foreach body
            BLangIf outerIf = null;
            BLangIf innerIf = null;
            for (BLangWhereClause whereClause : whereClauseList) {
                BLangIf bLangIf = (BLangIf) TreeBuilder.createIfElseStatementNode();
                bLangIf.pos = whereClause.pos;
                bLangIf.expr = whereClause.expression;
                if (innerIf != null) {
                    BLangBlockStmt bLangBlockInnerStmt = ASTBuilderUtil.createBlockStmt(whereClause.pos);
                    bLangBlockInnerStmt.addStatement(bLangIf);
                    innerIf.setBody(bLangBlockInnerStmt);
                } else {
                    outerIf = bLangIf;
                }
                innerIf = bLangIf;
            }
            innerIf.setBody(foreachBody);
            bLangBlockStmt.addStatement(outerIf);
            return  bLangBlockStmt;
        } else {
            return foreachBody;
        }
    }

//    private BLangInvocation createLengthInvocation(DiagnosticPos pos, BLangSimpleVarRef varRef) {
//        BInvokableSymbol lengthInvokableSymbol =
//                (BInvokableSymbol) symResolver.lookupLangLibMethod(varRef.symbol,
//                        names.fromString("length"));
//        BLangSimpleVarRef collection = ASTBuilderUtil.createVariableRef(pos, collectionSymbol);
//        BLangInvocation lengthInvocation = ASTBuilderUtil.createInvocationExprForMethod(pos, lengthInvokableSymbol,
//                Lists.of(collection), symResolver);
//        lengthInvocation.type = lengthInvokableSymbol.type.getReturnType();
//        // Note: No need to set lengthInvocation.expr for langLib functions as they are in requiredArgs
//        return lengthInvocation;
//    }

    private BLangInvocation createToStreamInvocation(DiagnosticPos pos, BLangSimpleVarRef variableReference,
                                                     BType  bStreamType) {
        BInvokableSymbol toStreamInvokableSymbol =
                (BInvokableSymbol) symResolver.lookupLangLibMethod(variableReference.type,
                        names.fromString("toStream"));
        BLangIdentifier toStreamIdentifier = ASTBuilderUtil.createIdentifier(pos, "toStream");
        BLangInvocation toStreamInvocation = ASTBuilderUtil.createInvocationExprForMethod(pos, toStreamInvokableSymbol,
                Lists.of(variableReference), symResolver);
        toStreamInvocation.pos = pos;
        toStreamInvocation.name = toStreamIdentifier;
        toStreamInvocation.expr = variableReference;
        toStreamInvocation.requiredArgs = Lists.of(variableReference);
        toStreamInvocation.argExprs = toStreamInvocation.requiredArgs;
        toStreamInvocation.symbol = toStreamInvokableSymbol;
        toStreamInvocation.type = bStreamType;

        return toStreamInvocation;
    }
}
