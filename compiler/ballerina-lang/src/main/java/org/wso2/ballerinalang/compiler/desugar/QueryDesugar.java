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
import org.ballerinalang.model.elements.Flag;
import org.ballerinalang.model.types.NilType;
import org.ballerinalang.model.types.TypeKind;
import org.wso2.ballerinalang.compiler.parser.BLangAnonymousModelHelper;
import org.wso2.ballerinalang.compiler.semantics.analyzer.SymbolEnter;
import org.wso2.ballerinalang.compiler.semantics.analyzer.SymbolResolver;
import org.wso2.ballerinalang.compiler.semantics.analyzer.Types;
import org.wso2.ballerinalang.compiler.semantics.model.SymbolEnv;
import org.wso2.ballerinalang.compiler.semantics.model.SymbolTable;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BInvokableSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BVarSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.SymTag;
import org.wso2.ballerinalang.compiler.semantics.model.types.BArrayType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BNilType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BStreamType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BType;
import org.wso2.ballerinalang.compiler.tree.BLangIdentifier;
import org.wso2.ballerinalang.compiler.tree.BLangInvokableNode;
import org.wso2.ballerinalang.compiler.tree.BLangNodeVisitor;
import org.wso2.ballerinalang.compiler.tree.BLangSimpleVariable;
import org.wso2.ballerinalang.compiler.tree.clauses.BLangDoClause;
import org.wso2.ballerinalang.compiler.tree.clauses.BLangFromClause;
import org.wso2.ballerinalang.compiler.tree.clauses.BLangLetClause;
import org.wso2.ballerinalang.compiler.tree.clauses.BLangSelectClause;
import org.wso2.ballerinalang.compiler.tree.clauses.BLangWhereClause;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangExpression;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangIndexBasedAccess;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangInvocation;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangLambdaFunction;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangListConstructorExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangQueryExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangSimpleVarRef;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangStatementExpression;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangXMLTextLiteral;
import org.wso2.ballerinalang.compiler.tree.statements.BLangAssignment;
import org.wso2.ballerinalang.compiler.tree.statements.BLangBlockStmt;
import org.wso2.ballerinalang.compiler.tree.statements.BLangForeach;
import org.wso2.ballerinalang.compiler.tree.statements.BLangIf;
import org.wso2.ballerinalang.compiler.tree.statements.BLangQueryAction;
import org.wso2.ballerinalang.compiler.tree.statements.BLangSimpleVariableDef;
import org.wso2.ballerinalang.compiler.tree.types.BLangLetVariable;
import org.wso2.ballerinalang.compiler.tree.types.BLangType;
import org.wso2.ballerinalang.compiler.tree.types.BLangUnionTypeNode;
import org.wso2.ballerinalang.compiler.tree.types.BLangValueType;
import org.wso2.ballerinalang.compiler.util.ClosureVarSymbol;
import org.wso2.ballerinalang.compiler.util.CompilerContext;
import org.wso2.ballerinalang.compiler.util.Name;
import org.wso2.ballerinalang.compiler.util.Names;
import org.wso2.ballerinalang.compiler.util.TypeTags;
import org.wso2.ballerinalang.compiler.util.diagnotic.BLangDiagnosticLogHelper;
import org.wso2.ballerinalang.compiler.util.diagnotic.DiagnosticPos;
import org.wso2.ballerinalang.util.Lists;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

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
    private BLangDiagnosticLogHelper dlog;
    private final SymbolResolver symResolver;
    private final Names names;
    private final Types types;
    private BLangForeach parentForeach = null;
    private  BLangSelectClause selectClause = null;

    private QueryDesugar(CompilerContext context) {
        context.put(QUERY_DESUGAR_KEY, this);
        this.symTable = SymbolTable.getInstance(context);
        this.symResolver = SymbolResolver.getInstance(context);
        this.symbolEnter = SymbolEnter.getInstance(context);
        this.names = Names.getInstance(context);
        this.types = Types.getInstance(context);
        this.dlog = BLangDiagnosticLogHelper.getInstance(context);
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
        selectClause = queryExpr.selectClause;
        List<BLangWhereClause> whereClauseList = queryExpr.whereClauseList;
        List<BLangLetClause> letClauseList = queryExpr.letClausesList;
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
        BLangForeach leafForeach = buildFromClauseBlock(fromClauseList);
        BLangBlockStmt foreachBody = ASTBuilderUtil.createBlockStmt(pos);

        if (selectClause.expression.type.tag == TypeTags.XML) {
            return buildExpressionStatementForXML(selectClause, pos, env, whereClauseList, letClauseList,
                    foreachBody, leafForeach);
        } else {
//            return buildExpressionStatementForList(selectClause, pos, env, whereClauseList, letClauseList,
//                    foreachBody, leafForeach);
            return buildExpressionStatementForList(selectClause, pos, env, whereClauseList, letClauseList,
                    fromClauseList);
        }
    }

    private BLangStatementExpression buildExpressionStatementForXML(BLangSelectClause selectClause,
                                                                    DiagnosticPos pos, SymbolEnv env,
                                                                    List<BLangWhereClause> whereClauseList,
                                                                    List<BLangLetClause> letClauseList,
                                                                    BLangBlockStmt foreachBody,
                                                                    BLangForeach leafForeach) {
        BLangXMLTextLiteral xmlTextLiteral = ASTBuilderUtil.createXMLTextLiteralNode(null, null, pos,
                selectClause.expression.type);
        BLangLiteral emptyLiteral = ASTBuilderUtil.createLiteral(pos, symTable.stringType, " ");
        xmlTextLiteral.addTextFragment(emptyLiteral);
        BVarSymbol xmlVarSymbol = new BVarSymbol(0, new Name("$outputDataXML$"),
                env.scope.owner.pkgID, selectClause.expression.type, env.scope.owner);
        BLangSimpleVariable outputArrayVariable = ASTBuilderUtil.createVariable(pos, "$outputDataXML$",
                selectClause.expression.type, xmlTextLiteral, xmlVarSymbol);

        BLangSimpleVariableDef outputVariableDef =
                ASTBuilderUtil.createVariableDef(pos, outputArrayVariable);
        BLangSimpleVarRef outputVarRef = ASTBuilderUtil.createVariableRef(pos, outputArrayVariable.symbol);

        List<BLangExpression> restArgs = new ArrayList<>();
        restArgs.add(selectClause.expression);
        BLangInvocation concatInvocation = createLangLibInvocation("concat", outputVarRef,
                restArgs, selectClause.expression.type, pos);

        buildWhereClauseBlock(whereClauseList, letClauseList, leafForeach, foreachBody, selectClause.pos);

        BLangAssignment outputVarAssignment = ASTBuilderUtil.createAssignmentStmt(pos, outputVarRef,
                concatInvocation);
        foreachBody.addStatement(outputVarAssignment);

        // Create block statement with temp variable definition statement & foreach statement
        BLangBlockStmt blockStmt = ASTBuilderUtil.createBlockStmt(pos);
        blockStmt.addStatement(outputVariableDef);
        blockStmt.addStatement(parentForeach);
        BLangStatementExpression stmtExpr = ASTBuilderUtil.createStatementExpression(blockStmt, outputVarRef);
        stmtExpr.type = selectClause.expression.type;
        return stmtExpr;
    }

   /* private BLangStatementExpression buildExpressionStatementForList(BLangSelectClause selectClause,
                                                                     DiagnosticPos pos, SymbolEnv env,
                                                                     List<BLangWhereClause> whereClauseList,
                                                                     List<BLangLetClause> letClauseList,
                                                                     BLangBlockStmt foreachBody,
                                                                     BLangForeach leafForeach) {
        BArrayType outputArrayType = new BArrayType(selectClause.expression.type);
        BLangListConstructorExpr emptyArrayExpr = ASTBuilderUtil.createEmptyArrayLiteral(pos,
                outputArrayType);
        BVarSymbol emptyArrayVarSymbol = new BVarSymbol(0, new Name("$outputDataArray$"),
                env.scope.owner.pkgID, outputArrayType, env.scope.owner);
        BLangSimpleVariable outputArrayVariable =
                ASTBuilderUtil.createVariable(pos, "$outputDataArray$", outputArrayType,
                        emptyArrayExpr, emptyArrayVarSymbol);

        // Create temp array variable
        //      Person[] x = [];

        BLangSimpleVariableDef outputVariableDef =
                ASTBuilderUtil.createVariableDef(pos, outputArrayVariable);
        BLangSimpleVarRef outputVarRef = ASTBuilderUtil.createVariableRef(pos, outputArrayVariable.symbol);

        // Create indexed based access expression statement
        //      x[x.length()] = {
        //         firstName: person.firstName,
        //         lastName: person.lastName
        //      };

        BLangInvocation lengthInvocation = createLengthInvocation(selectClause.pos, outputArrayVariable.symbol);
        lengthInvocation.expr = outputVarRef;
        BLangIndexBasedAccess indexAccessExpr = ASTBuilderUtil.createIndexAccessExpr(outputVarRef, lengthInvocation);
        indexAccessExpr.type = selectClause.expression.type;

        buildWhereClauseBlock(whereClauseList, letClauseList, leafForeach, foreachBody, selectClause.pos);

        // Set the indexed based access expression statement as foreach body
        BLangAssignment outputVarAssignment = ASTBuilderUtil.createAssignmentStmt(pos, indexAccessExpr,
                selectClause.expression);
        foreachBody.addStatement(outputVarAssignment);

        // Create block statement with temp variable definition statement & foreach statement
        BLangBlockStmt blockStmt = ASTBuilderUtil.createBlockStmt(pos);
        blockStmt.addStatement(outputVariableDef);
        blockStmt.addStatement(parentForeach);
        BLangStatementExpression stmtExpr = ASTBuilderUtil.createStatementExpression(blockStmt, outputVarRef);

        stmtExpr.type = outputArrayType;
        return stmtExpr;
    }*/

    private BLangStatementExpression buildExpressionStatementForList(BLangSelectClause selectClause,
                                                                     DiagnosticPos pos, SymbolEnv env,
                                                                     List<BLangWhereClause> whereClauseList,
                                                                     List<BLangLetClause> letClauseList,
                                                                     List<BLangFromClause> fromClauseList) {
        BArrayType outputArrayType = new BArrayType(selectClause.expression.type);
        BLangListConstructorExpr emptyArrayExpr = ASTBuilderUtil.createEmptyArrayLiteral(pos,
                outputArrayType);
        BVarSymbol emptyArrayVarSymbol = new BVarSymbol(0, new Name("$outputDataArray$"),
                env.scope.owner.pkgID, outputArrayType, env.scope.owner);
//        emptyArrayVarSymbol.closure = true;
        BLangSimpleVariable outputArrayVariable =
                ASTBuilderUtil.createVariable(pos, "$outputDataArray$", outputArrayType,
                        emptyArrayExpr, emptyArrayVarSymbol);

        // Create temp array variable
        //      Person[] x = [];

        BLangSimpleVariableDef outputVariableDef =
                ASTBuilderUtil.createVariableDef(pos, outputArrayVariable);
        BLangSimpleVarRef outputVarRef = ASTBuilderUtil.createVariableRef(pos, outputArrayVariable.symbol);

        // Create indexed based access expression statement
        //      x[x.length()] = {
        //         firstName: person.firstName,
        //         lastName: person.lastName
        //      };

        BLangInvocation lengthInvocation = createLengthInvocation(selectClause.pos, outputArrayVariable.symbol);
        lengthInvocation.expr = outputVarRef;
        BLangIndexBasedAccess indexAccessExpr = ASTBuilderUtil.createIndexAccessExpr(outputVarRef, lengthInvocation);
        indexAccessExpr.type = selectClause.expression.type;

//        buildWhereClauseBlock(whereClauseList, letClauseList, leafForeach, foreachBody, selectClause.pos);

        BLangBlockFunctionBody foreachBody = ASTBuilderUtil.createBlockFunctionBody(pos);

        // Set the indexed based access expression statement as foreach body
        BLangAssignment outputVarAssignment = ASTBuilderUtil.createAssignmentStmt(pos, indexAccessExpr,
                selectClause.expression);
        foreachBody.addStatement(outputVarAssignment);
        BLangLambdaFunction fromLambda = null;
        for (BLangFromClause fromClause : fromClauseList) {
            fromLambda = desugar.createLambdaFunction(fromClause.pos, "$fromCluase$",
                    Lists.of(((BLangSimpleVariableDef)fromClause.variableDefinitionNode).var),
                    ASTBuilderUtil.createTypeNode(symTable.nilType),
                    foreachBody);


//            for (BLangSimpleVariable var : funcNode.requiredParams) {
//                fromLambda.function.closureVarSymbols.add(new ClosureVarSymbol(var.symbol, var.pos));
//            }
        }
        fromLambda.capturedClosureEnv = env;

       /* for (BLangSimpleVariable var : fromLambda.function.requiredParams) {
            fromLambda.function.closureVarSymbols.add(new ClosureVarSymbol(var.symbol, var.pos));
        }*/
        fromLambda.function.closureVarSymbols.add(new ClosureVarSymbol(outputArrayVariable.symbol, outputArrayVariable.pos));
        fromLambda.function.closureVarSymbols.add(new ClosureVarSymbol(outputArrayVariable.symbol, outputArrayVariable.pos));

        BLangUnionTypeNode unionTypeNode = (BLangUnionTypeNode) TreeBuilder.createUnionTypeNode();
        unionTypeNode.type = symTable.errorOrNilType;
        unionTypeNode.memberTypeNodes.add(desugar.getErrorTypeNode());
        BLangValueType nullType = new BLangValueType();
        nullType.typeKind = TypeKind.NIL;
        nullType.type = new BNilType();
        unionTypeNode.memberTypeNodes.add(nullType);

        BVarSymbol forEachReturnSym = new BVarSymbol(0, new Name("err"),
                env.scope.owner.pkgID, unionTypeNode.type, env.scope.owner);

        BInvokableSymbol forEachInvokableSymbol =
                (BInvokableSymbol) symResolver.lookupLangLibMethod(new BStreamType(TypeTags.STREAM, null, null, null),
                        names.fromString("forEach"));
        BLangInvocation forEachInvocation = ASTBuilderUtil.createInvocationExprForMethod(pos, forEachInvokableSymbol,
                Lists.of(fromClauseList.get(0).collection, fromLambda), symResolver);
        forEachInvocation.expr = fromClauseList.get(0).collection;
        forEachInvocation.type = unionTypeNode.type;
        forEachInvocation.argExprs = Lists.of(fromClauseList.get(0).collection, fromLambda);


        BLangSimpleVariable foreachVar =
                ASTBuilderUtil.createVariable(pos, "$err$", unionTypeNode.type,
                        forEachInvocation, forEachReturnSym);
        BLangSimpleVariableDef foreachDef = ASTBuilderUtil.createVariableDef(pos);
        foreachDef.var = foreachVar;
        foreachDef.type = foreachVar.type;

        // Create block statement with temp variable definition statement & foreach statement
        BLangBlockStmt blockStmt = ASTBuilderUtil.createBlockStmt(pos);
        blockStmt.addStatement(outputVariableDef);
        blockStmt.addStatement(foreachDef);
        BLangStatementExpression stmtExpr = ASTBuilderUtil.createStatementExpression(blockStmt, outputVarRef);

        stmtExpr.type = outputArrayType;
        return stmtExpr;
    }

    BLangBlockStmt desugarQueryAction(BLangQueryAction queryAction, SymbolEnv env) {
        BLangBlockStmt blockNode = ASTBuilderUtil.createBlockStmt(queryAction.pos);
        List<BLangFromClause> fromClauseList = queryAction.fromClauseList;
        List<BLangLetClause> letClauseList = queryAction.letClauseList;
        BLangFromClause fromClause = fromClauseList.get(0);
        BLangDoClause doClause = queryAction.doClause;
        List<BLangWhereClause> whereClauseList = queryAction.whereClauseList;
        DiagnosticPos pos = fromClause.pos;

        BLangForeach leafForeach = buildFromClauseBlock(fromClauseList);
        BLangBlockStmt foreachBody = ASTBuilderUtil.createBlockStmt(pos);
        buildWhereClauseBlock(whereClauseList, letClauseList, leafForeach, foreachBody, doClause.pos);
        foreachBody.addStatement(doClause.body);
        blockNode.stmts.add(parentForeach);
        return blockNode;
    }

    private void buildLetClauseBlock(List<BLangLetClause> letClauseList, BLangBlockStmt bLangBlockStmt) {
        // Create variable definitions for the let variable declarations
        if (letClauseList != null) {
            for (BLangLetClause letClause : letClauseList) {
                for (BLangLetVariable letVariable : letClause.letVarDeclarations) {
                    bLangBlockStmt.addStatement(letVariable.definitionNode);
                }
            }
        }
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

    private void buildWhereClauseBlock(List<BLangWhereClause> whereClauseList, List<BLangLetClause> letClauseList,
                                       BLangForeach leafForEach, BLangBlockStmt foreachBody, DiagnosticPos pos) {
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
            buildLetClauseBlock(letClauseList, bLangBlockStmt);
            bLangBlockStmt.addStatement(outerIf);
            leafForEach.setBody(bLangBlockStmt);
        } else {
            buildLetClauseBlock(letClauseList, foreachBody);
            leafForEach.setBody(foreachBody);
        }
    }

    private BLangInvocation createLengthInvocation(DiagnosticPos pos, BVarSymbol collectionSymbol) {
        BInvokableSymbol lengthInvokableSymbol =
                (BInvokableSymbol) symResolver.lookupLangLibMethod(collectionSymbol.type,
                        names.fromString("length"));
        BLangSimpleVarRef collection = ASTBuilderUtil.createVariableRef(pos, collectionSymbol);
        BLangInvocation lengthInvocation = ASTBuilderUtil.createInvocationExprForMethod(pos, lengthInvokableSymbol,
                Lists.of(collection), symResolver);
        lengthInvocation.type = lengthInvokableSymbol.type.getReturnType();
        // Note: No need to set lengthInvocation.expr for langLib functions as they are in requiredArgs
        return lengthInvocation;
    }

    private BLangInvocation createLangLibInvocation(String functionName,
                                                    BLangExpression onExpr,
                                                    List<BLangExpression> args,
                                                    BType retType,
                                                    DiagnosticPos pos) {
        BLangInvocation invocationNode = (BLangInvocation) TreeBuilder.createInvocationNode();
        invocationNode.pos = pos;
        BLangIdentifier name = (BLangIdentifier) TreeBuilder.createIdentifierNode();
        name.setLiteral(false);
        name.setValue(functionName);
        name.pos = pos;
        invocationNode.name = name;
        invocationNode.pkgAlias = (BLangIdentifier) TreeBuilder.createIdentifierNode();

        invocationNode.expr = onExpr;
        invocationNode.symbol = symResolver.lookupLangLibMethod(onExpr.type, names.fromString(functionName));
        ArrayList<BLangExpression> requiredArgs = new ArrayList<>();
        requiredArgs.add(onExpr);
        requiredArgs.addAll(args);
        invocationNode.argExprs = requiredArgs;
        invocationNode.restArgs = requiredArgs;
        invocationNode.type = retType != null ? retType : ((BInvokableSymbol) invocationNode.symbol).retType;
        invocationNode.langLibInvocation = true;
        return invocationNode;
    }
}
