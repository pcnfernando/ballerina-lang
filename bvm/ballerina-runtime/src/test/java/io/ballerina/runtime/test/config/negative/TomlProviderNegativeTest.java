/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.runtime.test.config.negative;

import io.ballerina.runtime.api.Module;
import io.ballerina.runtime.api.PredefinedTypes;
import io.ballerina.runtime.api.creators.TypeCreator;
import io.ballerina.runtime.api.flags.SymbolFlags;
import io.ballerina.runtime.api.types.ArrayType;
import io.ballerina.runtime.api.types.Field;
import io.ballerina.runtime.api.types.IntersectionType;
import io.ballerina.runtime.api.types.MapType;
import io.ballerina.runtime.api.types.RecordType;
import io.ballerina.runtime.api.types.TableType;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.internal.configurable.ConfigResolver;
import io.ballerina.runtime.internal.configurable.VariableKey;
import io.ballerina.runtime.internal.configurable.providers.toml.TomlContentProvider;
import io.ballerina.runtime.internal.configurable.providers.toml.TomlFileProvider;
import io.ballerina.runtime.internal.diagnostics.RuntimeDiagnosticLog;
import io.ballerina.runtime.internal.types.BIntersectionType;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.ballerina.runtime.test.TestUtils.getConfigPath;
import static io.ballerina.runtime.test.TestUtils.getConfigPathForNegativeCases;

/**
 * Test cases specific for configuration provided via TOML files/content.
 */
public class TomlProviderNegativeTest {

    private static final Module ROOT_MODULE = new Module("rootOrg", "test_module", "1.0.0");
    private final Module subModule = new Module("rootOrg", "test_module.util.foo", "1.0.0");
    private final Module importedModule = new Module("myOrg", "mod", "1.0.0");

    @Test(dataProvider = "path-test-provider")
    public void testPathErrors(String tomlFileName, String errorMsg, int warningCount) {
        RuntimeDiagnosticLog diagnosticLog = new RuntimeDiagnosticLog();
        VariableKey intVar = new VariableKey(ROOT_MODULE, "byteVar", PredefinedTypes.TYPE_BYTE, true);
        ConfigResolver configResolver = new ConfigResolver(Map.ofEntries(Map.entry(ROOT_MODULE,
                new VariableKey[]{intVar})), diagnosticLog, List.of(new TomlFileProvider(ROOT_MODULE,
                getConfigPathForNegativeCases(tomlFileName), Set.of(ROOT_MODULE))));
        configResolver.resolveConfigs();
        Assert.assertEquals(diagnosticLog.getErrorCount(), 1);
        Assert.assertEquals(diagnosticLog.getWarningCount(), warningCount);
        Assert.assertEquals(diagnosticLog.getDiagnosticList().get(0).toString(), errorMsg);
    }

    @DataProvider(name = "path-test-provider")
    public Object[][] getPathErrorTests() {
        return new Object[][]{
                {"NoConfig.toml",
                        "warning: configuration file is not found in path '" +
                                getConfigPathForNegativeCases("NoConfig.toml") + "'", 1},
                {"Empty.toml", "error: value not provided for required configurable variable 'byteVar'", 0},
                {"InvalidConfig.toml", "warning: invalid toml file : \n" +
                        "[InvalidConfig.toml:(1:8,1:8)] missing identifier\n" +
                        "[InvalidConfig.toml:(1:26,1:26)] missing identifier\n" +
                        "[InvalidConfig.toml:(1:27,1:27)] missing identifier\n", 1},
                {"InvalidByteRange.toml", "error: [InvalidByteRange.toml:(2:11,2:14)] value provided for byte " +
                        "variable 'byteVar' is out of range. Expected range is (0-255), found '355'", 0}
        };
    }

    @Test(dataProvider = "simple-negative-tests")
    public void testSimpleNegativeConfig(String tomlFileName, String errorMsg, int errorCount, int warnCount) {
        VariableKey intVar = new VariableKey(importedModule, "intVar", PredefinedTypes.TYPE_INT, true);
        VariableKey stringVar = new VariableKey(importedModule, "stringVar", PredefinedTypes.TYPE_STRING, true);
        Map<Module, VariableKey[]> configVarMap =
                Map.ofEntries(Map.entry(importedModule, new VariableKey[]{intVar, stringVar}));
        validateTomlProviderErrors(tomlFileName, errorMsg, configVarMap, errorCount, warnCount);
    }

    @DataProvider(name = "simple-negative-tests")
    public Object[][] getSimpleNegativeTests() {
        return new Object[][]{
                {"InvalidModuleStructure1", "[InvalidModuleStructure1.toml:(1:13,1:22)] invalid toml structure " +
                        "found for module 'myOrg.mod'. Please provide the module name as '[myOrg.mod]'", 2, 1},
                {"InvalidModuleStructure2", "[InvalidModuleStructure2.toml:(1:1,2:7)] invalid toml structure found " +
                        "for module 'myOrg.mod'. Please provide the module name as '[myOrg.mod]'", 2, 1},
                {"RequiredNegative",
                        "value not provided for required configurable variable 'stringVar'", 1, 0},
                {"PrimitiveTypeError", "[PrimitiveTypeError.toml:(2:10,2:14)] configurable variable 'intVar' is " +
                        "expected to be of type 'int', but found 'float'", 2, 0},
                {"PrimitiveStructureError", "[PrimitiveStructureError.toml:(2:1,2:24)] configurable variable " +
                        "'intVar' is expected to be of type 'int', but found 'record'", 2, 0},
        };
    }

    @Test
    public void testNoModuleInformation() {
        VariableKey[] variableKeys = getSimpleVariableKeys(importedModule);
        Map<Module, VariableKey[]> configVarMap = Map.ofEntries(Map.entry(importedModule, variableKeys));
        RuntimeDiagnosticLog diagnosticLog = new RuntimeDiagnosticLog();
        ConfigResolver configResolver = new ConfigResolver(configVarMap, diagnosticLog,
                List.of(new TomlFileProvider(TomlProviderNegativeTest.ROOT_MODULE,
                        getConfigPathForNegativeCases("NoModuleConfig.toml"), configVarMap.keySet())));
        configResolver.resolveConfigs();
        Assert.assertEquals(diagnosticLog.getErrorCount(), 2);
        Assert.assertEquals(diagnosticLog.getWarningCount(), 2);
        Assert.assertEquals(diagnosticLog.getDiagnosticList().get(0).toString(),
                "error: value not provided for required configurable variable 'intVar'");
        Assert.assertEquals(diagnosticLog.getDiagnosticList().get(1).toString(),
                "error: value not provided for required configurable variable 'stringVar'");
        Assert.assertEquals(diagnosticLog.getDiagnosticList().get(2).toString(), "warning: [NoModuleConfig.toml:(1:1," +
                "1:15)] unused configuration value 'intInMain'");
        Assert.assertEquals(diagnosticLog.getDiagnosticList().get(3).toString(), "warning: [NoModuleConfig.toml:(2:1," +
                "2:12)] unused configuration value 'intVar'");
    }

    @Test()
    public void testInvalidSubModuleStructureInConfigFile() {
        VariableKey intVar = new VariableKey(subModule, "intVar", PredefinedTypes.TYPE_INT, true);
        VariableKey stringVar = new VariableKey(subModule, "stringVar", PredefinedTypes.TYPE_STRING, true);
        Map<Module, VariableKey[]> configVarMap =
                Map.ofEntries(Map.entry(subModule, new VariableKey[]{intVar, stringVar}));
        String errorMsg = "[InvalidSubModuleStructure1.toml:(2:12,2:14)] invalid toml structure found for module " +
                "'test_module.util.foo'. Please provide the module name as '[test_module.util.foo]'";
        validateTomlProviderErrors("InvalidSubModuleStructure1", errorMsg, configVarMap, 2, 1);

        errorMsg = "[InvalidSubModuleStructure2.toml:(1:1,2:23)] invalid toml structure found for module " +
                "'test_module.util.foo'. Please provide the module name as '[test_module.util.foo]'";
        validateTomlProviderErrors("InvalidSubModuleStructure2", errorMsg, configVarMap, 2, 1);
    }

    @Test(dataProvider = "array-negative-tests")
    public void testArrayNegativeConfig(String tomlFileName, String errorMsg, int warnCount) {
        ArrayType arrayType = TypeCreator.createArrayType(PredefinedTypes.TYPE_INT, true);
        VariableKey intArr = new VariableKey(ROOT_MODULE, "intArr",
                new BIntersectionType(ROOT_MODULE, new Type[]{arrayType, PredefinedTypes.TYPE_READONLY}, arrayType, 0,
                        true),
                true);
        Map<Module, VariableKey[]> configVarMap = Map.ofEntries(Map.entry(ROOT_MODULE, new VariableKey[]{intArr}));
        validateTomlProviderErrors(tomlFileName, errorMsg, configVarMap, 1, warnCount);
    }

    @DataProvider(name = "array-negative-tests")
    public Object[][] getArrayNegativeTests() {
        return new Object[][]{
                {"ArrayTypeError", "[ArrayTypeError.toml:(1:10,1:17)] configurable variable 'intArr'" +
                        " is expected to be of type 'int[] & readonly', but found 'string'", 0},
                {"ArrayStructureError", "[ArrayStructureError.toml:(1:1,1:32)] configurable variable" +
                        " 'intArr' is expected to be of type 'int[] & readonly', but found 'record'", 0},
                {"ArrayElementStructure", "[ArrayElementStructure.toml:(1:19,1:26)] configurable variable " +
                        "'intArr[2]' is expected to be of type 'int', but found 'array'", 0},
                {"ArrayMultiType", "[ArrayMultiType.toml:(1:15,1:21)] configurable variable " +
                        "'intArr[1]' is expected to be of type 'int', but found 'string'", 0},
        };
    }

    @Test()
    public void testInvalidTableField() {
        MapType mapType =  TypeCreator.createMapType(PredefinedTypes.TYPE_STRING, true);
        Field intArr = TypeCreator.createField(mapType, "invalidMap", SymbolFlags.REQUIRED);
        Field name = TypeCreator.createField(PredefinedTypes.TYPE_STRING, "name", SymbolFlags.REQUIRED);
        Map<String, Field> fields = Map.ofEntries(Map.entry("name", name), Map.entry("invalidField", intArr));
        RecordType type =
                TypeCreator.createRecordType("Person", ROOT_MODULE, SymbolFlags.READONLY, fields, null, true, 6);
        TableType tableType = TypeCreator.createTableType(type, new String[]{"name"}, true);
        IntersectionType intersectionType = new BIntersectionType(ROOT_MODULE, new Type[]{tableType,
                PredefinedTypes.TYPE_READONLY}, tableType, 1, true);
        VariableKey tableVar = new VariableKey(ROOT_MODULE, "tableVar", intersectionType, true);
        Map<Module, VariableKey[]> configVarMap = Map.ofEntries(Map.entry(ROOT_MODULE, new VariableKey[]{tableVar}));
        String errorMsg = "[InvalidTableField.toml:(3:1,3:27)] additional field 'invalidMap' provided for " +
                "configurable variable 'tableVar' of record 'test_module:Person' is not supported";
        validateTomlProviderErrors("InvalidTableField", errorMsg, configVarMap, 1, 0);
    }

    @Test(dataProvider = "record-negative-tests")
    public void testRecordNegativeConfig(String tomlFileName, String errorMsg, int warnCount) {
        Field name = TypeCreator.createField(PredefinedTypes.TYPE_STRING, "name", SymbolFlags.REQUIRED);
        Map<String, Field> fields = Map.ofEntries(Map.entry("name", name));
        RecordType type =
                TypeCreator.createRecordType("Person", ROOT_MODULE, SymbolFlags.READONLY, fields, null, true, 6);

        VariableKey recordVar = new VariableKey(ROOT_MODULE, "recordVar", type, true);

        Map<Module, VariableKey[]> configVarMap = Map.ofEntries(Map.entry(ROOT_MODULE, new VariableKey[]{recordVar}));

        validateTomlProviderErrors(tomlFileName, errorMsg, configVarMap, 1, warnCount);
    }

    @DataProvider(name = "record-negative-tests")
    public Object[][] getRecordNegativeTests() {
        return new Object[][]{
                {"RecordTypeError", "[RecordTypeError.toml:(2:1,2:40)] configurable variable 'recordVar' " +
                        "is expected to be of type 'test_module:Person', but found 'string'", 0},
                {"RecordFieldTypeError", "[RecordFieldTypeError.toml:(2:8,2:12)] field 'name' from configurable " +
                        "variable 'recordVar' is expected to be of type 'string', but found 'int'", 1},
                {"RecordFieldStructureError", "[RecordFieldStructureError.toml:(2:1,2:24)] field 'name' from " +
                        "configurable variable 'recordVar' is expected to be of type 'string', but found " +
                        "'record'", 0},
                {"AdditionalFieldRecord", "[AdditionalFieldRecord.toml:(3:1,3:9)] additional field 'age' provided for" +
                        " configurable variable 'recordVar' of record 'test_module:Person' is not " +
                        "supported", 0},
                {"MissingRecordField", "[MissingRecordField.toml:(1:1,1:24)] value not provided for non-defaultable " +
                        "required field 'name' of record 'test_module:Person' in configurable variable " +
                        "'recordVar'", 0},
        };
    }

    @Test()
    public void testInvalidMapType() {
        MapType mapType = TypeCreator.createMapType(PredefinedTypes.TYPE_INT, true);
        VariableKey mapInt = new VariableKey(ROOT_MODULE, "mapVar", mapType, true);
        Map<Module, VariableKey[]> configVarMap = Map.ofEntries(Map.entry(ROOT_MODULE, new VariableKey[]{mapInt}));
        String errorMsg = "configurable variable 'mapVar' with type 'map<int> & readonly' is not " +
                "supported";
        validateTomlProviderErrors("InvalidMapType", errorMsg, configVarMap, 1, 3);
    }

    @Test(dataProvider = "table-negative-tests")
    public void testTableNegativeConfig(String tomlFileName, String errorMsg, int warnCount) {
        Field name = TypeCreator.createField(PredefinedTypes.TYPE_STRING, "name", SymbolFlags.REQUIRED);
        Field id = TypeCreator.createField(PredefinedTypes.TYPE_INT, "id", SymbolFlags.REQUIRED);
        Field age = TypeCreator.createField(PredefinedTypes.TYPE_INT, "age", SymbolFlags.OPTIONAL);
        Map<String, Field> fields = Map.ofEntries(Map.entry("name", name), Map.entry("age", age), Map.entry("id", id));
        RecordType type =
                TypeCreator.createRecordType("Person", ROOT_MODULE, SymbolFlags.READONLY, fields, null, true, 6);
        TableType tableType = TypeCreator.createTableType(type, new String[]{"name"}, true);
        IntersectionType intersectionType = new BIntersectionType(ROOT_MODULE, new Type[]{tableType,
                PredefinedTypes.TYPE_READONLY}, tableType, 1, true);

        VariableKey tableVar = new VariableKey(ROOT_MODULE, "tableVar", intersectionType, true);
        Map<Module, VariableKey[]> configVarMap = Map.ofEntries(Map.entry(ROOT_MODULE, new VariableKey[]{tableVar}));

        validateTomlProviderErrors(tomlFileName, errorMsg, configVarMap, 1, warnCount);
    }

    @DataProvider(name = "table-negative-tests")
    public Object[][] getTableNegativeTests() {
        return new Object[][]{
                {"MissingTableKey", "[MissingTableKey.toml:(6:1,8:9)] value required for key 'name' of " +
                        "type 'table<test_module:Person> key(name) & readonly' in configurable variable" +
                        " 'tableVar'", 2},
                {"TableTypeError", "[TableTypeError.toml:(1:1,3:9)] configurable variable 'tableVar'" +
                        " is expected to be of type 'table<test_module:Person> key(name) & readonly'," +
                        " but found 'record'", 2},
                {"TableFieldTypeError", "[TableFieldTypeError.toml:(2:8,2:11)] field 'name' " +
                        "from configurable variable 'tableVar' is expected to be of type 'string'," +
                        " but found 'int'", 2},
                {"TableFieldStructureError", "[TableFieldStructureError.toml:(2:1,2:24)] field 'name' " +
                        "from configurable variable 'tableVar' is expected to be of type 'string'," +
                        " but found 'record'", 1},
                {"AdditionalField", "[AdditionalField.toml:(4:1,4:17)] additional field 'city' provided for" +
                        " configurable variable 'tableVar' of record 'test_module:Person'" +
                        " is not supported", 0},
                {"MissingTableField", "[MissingTableField.toml:(1:1,3:9)] value not provided for " +
                        "non-defaultable required field 'id' of record 'test_module:Person' in configurable" +
                        " variable 'tableVar'", 0},
        };
    }

    @Test()
    public void testInvalidTableConstraint() {
        MapType mapType = TypeCreator.createMapType(PredefinedTypes.TYPE_STRING, true);
        TableType tableType = TypeCreator.createTableType(mapType, true);
        IntersectionType intersectionType = new BIntersectionType(ROOT_MODULE, new Type[]{tableType,
                PredefinedTypes.TYPE_READONLY}, tableType, 1, true);

        VariableKey tableVar = new VariableKey(ROOT_MODULE, "tableVar", intersectionType, true);

        Map<Module, VariableKey[]> configVarMap = Map.ofEntries(Map.entry(ROOT_MODULE, new VariableKey[]{tableVar}));
        String errorMsg = "[InvalidTableConstraint.toml:(1:1,2:16)] table constraint type 'map<string> & readonly'" +
                " in configurable variable 'tableVar' is not supported";
        validateTomlProviderErrors("InvalidTableConstraint", errorMsg, configVarMap, 1, 1);
    }

    @Test(dataProvider = "multi-module-data-provider")
    public void testTomlProviderWithMultipleModules(Map<Module, VariableKey[]> variableMap, String errorMsg,
                                                    String tomlFileName) {
        validateTomlProviderErrors(tomlFileName, errorMsg, variableMap, 2, 2);
    }

    @DataProvider(name = "multi-module-data-provider")
    public Object[][] multiModuleDataProvider() {
        VariableKey[] variableKeys = getSimpleVariableKeys(ROOT_MODULE);
        VariableKey[] subVariableKeys = getSimpleVariableKeys(subModule);
        VariableKey[] importedVariableKeys = getSimpleVariableKeys(importedModule);

        Module clashingModule1 = new Module("myOrg", "test_module", "1.0.0");
        VariableKey[] clashingVariableKeys1 = getSimpleVariableKeys(clashingModule1);

        Module clashingModule2 = new Module("myOrg", "test_module.util.foo", "1.0.0");
        VariableKey[] clashingVariableKeys2 = getSimpleVariableKeys(clashingModule2);

        Module clashingModule3 = new Module("test_module", "util.foo", "1.0.0");
        VariableKey[] clashingVariableKeys3 = getSimpleVariableKeys(clashingModule3);

        Module clashingModule4 = new Module("test_module", "util", "1.0.0");
        VariableKey[] clashingVariableKeys4 = getSimpleVariableKeys(clashingModule4);

        return new Object[][]{
                {Map.ofEntries(Map.entry(subModule, subVariableKeys)), "[SubModuleError.toml:(1:1,3:21)] invalid " +
                        "toml structure found for module 'test_module.util.foo'. Please provide the module name as " +
                        "'[test_module.util.foo]'", "SubModuleError"},
                {Map.ofEntries(Map.entry(importedModule, importedVariableKeys)),
                        "[ImportedModuleError.toml:(1:1,3:21)] invalid toml structure found for module 'myOrg.mod'" +
                                ". Please provide the module name as '[myOrg.mod]'",
                        "ImportedModuleError"},
                {Map.ofEntries(Map.entry(ROOT_MODULE, variableKeys), Map.entry(clashingModule1, clashingVariableKeys1)),
                        "[ClashingModuleError1.toml:(5:1,7:21)] invalid toml structure found for module 'myOrg" +
                                ".test_module'. Please provide the module name as '[myOrg.test_module]'",
                        "ClashingModuleError1"},
                {Map.ofEntries(Map.entry(subModule, subVariableKeys), Map.entry(clashingModule2,
                        clashingVariableKeys2)),
                        "[ClashingModuleError2.toml:(5:1,7:21)] invalid toml structure found for module 'myOrg" +
                                ".test_module.util.foo'. Please provide the module name as '[myOrg.test_module.util" +
                                ".foo]'", "ClashingModuleError2"},
                //TODO: Should be removed after fixing #29989
                {Map.ofEntries(Map.entry(ROOT_MODULE, variableKeys), Map.entry(clashingModule3,
                        clashingVariableKeys3)),
                        "[ClashingOrgModuleError2.toml:(1:1,6:19)] the module name 'test_module' clashes with an " +
                                "imported organization name. Please provide the module name as '[rootOrg.test_module]'",
                        "ClashingOrgModuleError2"},
                {Map.ofEntries(Map.entry(ROOT_MODULE, variableKeys), Map.entry(clashingModule4, clashingVariableKeys4)),
                        "[ClashingOrgModuleError3.toml:(1:1,7:19)] the module name 'test_module' clashes with an " +
                                "imported organization name. Please provide the module name as '[rootOrg.test_module]'",
                        "ClashingOrgModuleError3"},
        };
    }

    @Test
    public void testSubModuleValueNotProvided() {
        Map<Module, VariableKey[]> variableMap =
                Map.ofEntries(Map.entry(ROOT_MODULE, getSimpleVariableKeys(ROOT_MODULE)),
                Map.entry(subModule, getSimpleVariableKeys(subModule)));
        RuntimeDiagnosticLog diagnosticLog = new RuntimeDiagnosticLog();
        ConfigResolver configResolver = new ConfigResolver(variableMap, diagnosticLog,
                List.of(new TomlFileProvider(TomlProviderNegativeTest.ROOT_MODULE,
                        getConfigPathForNegativeCases("ValueNotProvidedSubModule.toml"), variableMap.keySet())));
        configResolver.resolveConfigs();
        Assert.assertEquals(diagnosticLog.getErrorCount(), 2);
        Assert.assertEquals(diagnosticLog.getWarningCount(), 0);
        Assert.assertEquals(diagnosticLog.getDiagnosticList().get(0).toString(), "error: value not provided for " +
                "required configurable variable 'intVar'");
        Assert.assertEquals(diagnosticLog.getDiagnosticList().get(1).toString(), "error: value not provided for " +
                "required configurable variable 'stringVar'");
    }

    @Test
    public void testClashingOrgSubModules() {
        VariableKey[] subVariableKeys = getSimpleVariableKeys(subModule);
        Module clashingModule = new Module("test_module", "util.foo", "1.0.0");
        VariableKey[] clashingVariableKeys = getSimpleVariableKeys(clashingModule);
        Map<Module, VariableKey[]> variableMap =
                Map.ofEntries(Map.entry(subModule, subVariableKeys), Map.entry(clashingModule, clashingVariableKeys));
        String errorMsg = "warning: invalid toml file : \n" +
                    "[ClashingOrgModuleError1.toml:(5:1,7:19)] existing node 'foo'\n";
        RuntimeDiagnosticLog diagnosticLog = new RuntimeDiagnosticLog();
        ConfigResolver configResolver = new ConfigResolver(variableMap, diagnosticLog,
                List.of(new TomlFileProvider(TomlProviderNegativeTest.ROOT_MODULE,
                        getConfigPathForNegativeCases("ClashingOrgModuleError1.toml"), variableMap.keySet())));
        configResolver.resolveConfigs();
        Assert.assertEquals(diagnosticLog.getErrorCount(), 4);
        Assert.assertEquals(diagnosticLog.getWarningCount(), 1);
        Assert.assertEquals(diagnosticLog.getDiagnosticList().get(0).toString(), errorMsg);
    }

    private void validateTomlProviderErrors(String tomlFileName, String errorMsg,
                                            Map<Module, VariableKey[]> configVarMap, int errorCount, int warnCount) {
        RuntimeDiagnosticLog diagnosticLog = new RuntimeDiagnosticLog();
        ConfigResolver configResolver = new ConfigResolver(configVarMap, diagnosticLog,
                List.of(new TomlFileProvider(TomlProviderNegativeTest.ROOT_MODULE,
                        getConfigPathForNegativeCases(tomlFileName + ".toml"), configVarMap.keySet())));
        configResolver.resolveConfigs();
        Assert.assertEquals(diagnosticLog.getErrorCount(), errorCount);
        Assert.assertEquals(diagnosticLog.getWarningCount(), warnCount);
        Assert.assertEquals(diagnosticLog.getDiagnosticList().get(0).toString(), "error: " + errorMsg);
    }

    @Test
    public void testTomlProviderWithStringNegative() {
        Map<Module, VariableKey[]> configVarMap = new HashMap<>();
        VariableKey[] keys = getSimpleVariableKeys(ROOT_MODULE);
        String tomlContent = "[rootOrg.test_module] intVar = 42.22 floatVar = 3 stringVar = 11";
        configVarMap.put(ROOT_MODULE, keys);
        RuntimeDiagnosticLog diagnosticLog = new RuntimeDiagnosticLog();
        ConfigResolver configResolver = new ConfigResolver(configVarMap, diagnosticLog,
                List.of(new TomlContentProvider(ROOT_MODULE, tomlContent, configVarMap.keySet())));
        configResolver.resolveConfigs();
        Assert.assertEquals(diagnosticLog.getErrorCount(), 2);
        Assert.assertEquals(diagnosticLog.getDiagnosticList().get(0).toString(), "error: [BAL_CONFIG_DATA:(1:32,1:37)" +
                "] configurable variable 'intVar' is expected to be of type 'int', but found 'float'");
        Assert.assertEquals(diagnosticLog.getDiagnosticList().get(1).toString(), "error: [BAL_CONFIG_DATA:(1:63,1:65)" +
                "] configurable variable 'stringVar' is expected to be of type 'string', but found 'int'");
    }

    @Test
    public void testMultipleTomlProvidersNegative() {
        Map<Module, VariableKey[]> configVarMap = Map.ofEntries(Map.entry(
                ROOT_MODULE, getSimpleVariableKeys(ROOT_MODULE)));
        RuntimeDiagnosticLog diagnosticLog = new RuntimeDiagnosticLog();
        ConfigResolver configResolver = new ConfigResolver(configVarMap, diagnosticLog,
                List.of(new TomlFileProvider(ROOT_MODULE, getConfigPath("Config_First.toml"), configVarMap.keySet()),
                        new TomlFileProvider(
                                ROOT_MODULE, getConfigPath("Config_Second.toml"), configVarMap.keySet())));
        configResolver.resolveConfigs();
        Assert.assertEquals(diagnosticLog.getErrorCount(), 1);
        Assert.assertEquals(diagnosticLog.getDiagnosticList().get(0).toString(), "error: value not provided for " +
                "required configurable variable 'stringVar'");
    }

    @Test
    public void testUnusedTomlParts() {
        VariableKey intVar = new VariableKey(ROOT_MODULE, "intVar", PredefinedTypes.TYPE_INT, true);
        VariableKey stringVar = new VariableKey(ROOT_MODULE, "stringVar", PredefinedTypes.TYPE_STRING, true);

        Field name = TypeCreator.createField(PredefinedTypes.TYPE_STRING, "name", SymbolFlags.REQUIRED);
        RecordType type = TypeCreator.createRecordType("Person", ROOT_MODULE, SymbolFlags.READONLY,
                        Map.ofEntries(Map.entry("name", name)), null, true, 6);

        VariableKey recordVar = new VariableKey(ROOT_MODULE, "recordVar", type, true);

        TableType tableType = TypeCreator.createTableType(type, new String[]{"name"}, true);
        IntersectionType intersectionType = new BIntersectionType(ROOT_MODULE, new Type[]{tableType,
                PredefinedTypes.TYPE_READONLY}, tableType, 1, true);

        VariableKey tableVar = new VariableKey(ROOT_MODULE, "tableVar", intersectionType, true);
        VariableKey[] rootVariableKeys = new VariableKey[] {intVar, stringVar, recordVar, tableVar};

        Map<Module, VariableKey[]> configVarMap = Map.ofEntries(Map.entry(ROOT_MODULE, rootVariableKeys),
                Map.entry(subModule, getSimpleVariableKeys(subModule)),
                Map.entry(importedModule, getSimpleVariableKeys(importedModule)));

        RuntimeDiagnosticLog diagnosticLog = new RuntimeDiagnosticLog();
        ConfigResolver configResolver = new ConfigResolver(configVarMap, diagnosticLog,
                List.of(new TomlFileProvider(ROOT_MODULE, getConfigPathForNegativeCases("UnusedTomlParts.toml"),
                        configVarMap.keySet())));
        configResolver.resolveConfigs();
        Assert.assertEquals(diagnosticLog.getWarningCount(), 7);
        String[] warnings = new String[] {
                "warning: [UnusedTomlParts.toml:(3:1,3:20)] unused configuration value 'undefinedVar1'",
                "warning: [UnusedTomlParts.toml:(11:1,11:20)] unused configuration value 'test_module.util.foo" +
                        ".undefinedVar2'",
                "warning: [UnusedTomlParts.toml:(16:1,16:20)] unused configuration value 'myOrg.mod.undefinedVar3'",
                "warning: [UnusedTomlParts.toml:(18:1,19:20)] unused configuration value 'undefined_Module'",
                "warning: [UnusedTomlParts.toml:(19:1,19:20)] unused configuration value 'undefined_Module" +
                        ".undefinedVar4'",
                "warning: [UnusedTomlParts.toml:(21:1,22:20)] unused configuration value 'undefined_Table'",
                "warning: [UnusedTomlParts.toml:(22:1,22:20)] unused configuration value 'undefined_Table" +
                        ".undefinedVar5'"
        };
        for (int i = 0; i < warnings.length; i++) {
            Assert.assertEquals(diagnosticLog.getDiagnosticList().get(i).toString(), warnings[i]);
        }
    }

    @Test
    public void testOptionalImportedModuleWarning() {
        VariableKey intVar = new VariableKey(importedModule, "intVar", PredefinedTypes.TYPE_INT, false);
        VariableKey stringVar = new VariableKey(importedModule, "stringVar", PredefinedTypes.TYPE_STRING, false);
        VariableKey[] variableKeys = new VariableKey[] {intVar, stringVar};

        Map<Module, VariableKey[]> configVarMap = Map.ofEntries(Map.entry(importedModule, variableKeys));
        RuntimeDiagnosticLog diagnosticLog = new RuntimeDiagnosticLog();
        ConfigResolver configResolver = new ConfigResolver(configVarMap, diagnosticLog,
                List.of(new TomlFileProvider(ROOT_MODULE, getConfigPathForNegativeCases("OptionalImportedModule.toml"),
                        configVarMap.keySet())));
        configResolver.resolveConfigs();
        Assert.assertEquals(diagnosticLog.getWarningCount(), 3);
        String[] warnings = new String[] {
                "warning: [OptionalImportedModule.toml:(1:1,3:18)] invalid toml structure found for module 'mod'. " +
                        "Please provide the module name as '[myOrg.mod]'",
                "warning: [OptionalImportedModule.toml:(2:1,2:13)] unused configuration value 'mod.intVar'",
                "warning: [OptionalImportedModule.toml:(3:1,3:18)] unused configuration value 'mod.stringVar'"
        };
        for (int i = 0; i < warnings.length; i++) {
            Assert.assertEquals(diagnosticLog.getDiagnosticList().get(i).toString(), warnings[i]);
        }
    }

    private VariableKey[] getSimpleVariableKeys(Module module) {
        VariableKey intVar = new VariableKey(module, "intVar", PredefinedTypes.TYPE_INT, true);
        VariableKey stringVar = new VariableKey(module, "stringVar", PredefinedTypes.TYPE_STRING, true);
        return new VariableKey[]{intVar, stringVar};
    }
}
