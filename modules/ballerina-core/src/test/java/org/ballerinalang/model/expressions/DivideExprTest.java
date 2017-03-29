/*
 *  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.ballerinalang.model.expressions;

import org.ballerinalang.core.utils.BTestUtils;
import org.ballerinalang.model.BLangProgram;
import org.ballerinalang.model.values.BFloat;
import org.ballerinalang.model.values.BInteger;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.util.exceptions.BallerinaException;
import org.ballerinalang.util.exceptions.SemanticException;
import org.ballerinalang.util.program.BLangFunctions;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Primitive divide expression test.
 */
public class DivideExprTest {

    private BLangProgram bLangProgram;

    @BeforeClass
    public void setup() {
        bLangProgram = BTestUtils.parseBalFile("lang/expressions/divide-expr.bal");
    }

    @Test(description = "Test two int divide expression")
    public void testIntDivideExpr() {
        BValue[] args = { new BInteger(2000), new BInteger(50) };
        BValue[] returns = BLangFunctions.invoke(bLangProgram, "intDivide", args);

        Assert.assertEquals(returns.length, 1);
        Assert.assertSame(returns[0].getClass(), BInteger.class);

        int actual = ((BInteger) returns[0]).intValue();
        int expected = 40;
        Assert.assertEquals(actual, expected);
    }

    @Test(description = "Test two int divide expression", expectedExceptions = BallerinaException.class)
    public void testIntDivideByZeroExpr() {
        BValue[] args = { new BInteger(2000), new BInteger(0) };
        BLangFunctions.invoke(bLangProgram, "intDivide", args);
    }

    @Test(description = "Test two float divide expression")
    public void testFloatDivideExpr() {
        float a = 8.5f;
        float b = 4.1f;

        float expectedResult = a / b;

        BValue[] args = { new BFloat(a), new BFloat(b) };
        BValue[] returns = BLangFunctions.invoke(bLangProgram, "floatDivide", args);

        Assert.assertEquals(returns.length, 1);
        Assert.assertSame(returns[0].getClass(), BFloat.class);

        float actual = ((BFloat) returns[0]).floatValue();
        Assert.assertEquals(actual, expectedResult);
    }

    @Test(description = "Test two float divide expression", expectedExceptions = BallerinaException.class)
    public void testFloatDivideByZeroExpr() {
        BValue[] args = { new BFloat(300.0f), new BFloat(0) };
        BLangFunctions.invoke(bLangProgram, "floatDivide", args);
    }

    @Test(description = "Test integer division by float")
    public void testIntDivideByFloat() {
        int a = Integer.MAX_VALUE;
        float b = 1.23456789f;

        float expectedResult = a / b;

        BValue[] args = { new BInteger(a), new BFloat(b) };
        BValue[] returns = BLangFunctions.invoke(bLangProgram, "intDivideByFloat", args);

        Assert.assertEquals(returns.length, 1);
        Assert.assertSame(returns[0].getClass(), BFloat.class, "Return type of the division is invalid");

        float actualResult = ((BFloat) returns[0]).floatValue();
        Assert.assertEquals(actualResult, expectedResult, "Result of the division operation is incorrect");
    }

    @Test(description = "Test float number division by integer")
    public void testFloatDivideByInt() {
        float a = Float.MAX_VALUE;
        int b = 123456789;

        float expectedResult = a / b;

        BValue[] args = { new BFloat(a), new BInteger(b) };
        BValue[] returns = BLangFunctions.invoke(bLangProgram, "floatDivideByInt", args);

        Assert.assertEquals(returns.length, 1);
        Assert.assertSame(returns[0].getClass(), BFloat.class, "Return type of the division is invalid");

        float actualResult = ((BFloat) returns[0]).floatValue();
        Assert.assertEquals(actualResult, expectedResult, "Result of the division operation is incorrect");
    }

    /*
     * Negative tests
     */
    
    @Test(description = "Test dividing values of two types",
            expectedExceptions = {SemanticException.class },
            expectedExceptionsMessageRegExp = "divide-incompatible-types.bal:5: invalid operation: " +
                    "incompatible types 'string' and 'float'")
    public void testAddIncompatibleTypes() {
        BTestUtils.parseBalFile("lang/expressions/divide-incompatible-types.bal");
    }
    
    @Test(description = "Test dividing values of unsupported types (json)",
            expectedExceptions = {SemanticException.class },
            expectedExceptionsMessageRegExp = "divide-unsupported-types.bal:10: invalid operation: " +
                    "operator / not defined on 'json'")
    public void testAddUnsupportedTypes() {
        BTestUtils.parseBalFile("lang/expressions/divide-unsupported-types.bal");
    }
}
