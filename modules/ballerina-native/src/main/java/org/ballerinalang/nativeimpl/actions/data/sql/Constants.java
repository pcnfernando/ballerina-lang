/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
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
package org.ballerinalang.nativeimpl.actions.data.sql;

/**
 * Constants for SQL Data Connectors.
 *
 * @since 0.8.0
 */
public final class Constants {

    /**
     * Constants for SQL DataTypes.
     */
    public static final class SQLDataTypes {
        public static final String VARCHAR = "VARCHAR";
        public static final String CHAR = "CHAR";
        public static final String LONGVARCHAR = "LONGVARCHAR";
        public static final String NCHAR = "NCHAR";
        public static final String LONGNVARCHAR = "LONGNVARCHAR";
        public static final String NVARCHAR = "NVARCHAR";
        public static final String NUMERIC = "NUMERIC";
        public static final String DECIMAL = "DECIMAL";
        public static final String BIT = "BIT";
        public static final String BOOLEAN = "BOOLEAN";
        public static final String TINYINT = "TINYINT";
        public static final String SMALLINT = "SMALLINT";
        public static final String INTEGER = "INTEGER";
        public static final String BIGINT = "BIGINT";
        public static final String REAL = "REAL";
        public static final String FLOAT = "FLOAT";
        public static final String DOUBLE = "DOUBLE";
        public static final String BINARY = "BINARY";
        public static final String BLOB = "BLOB";
        public static final String LONGVARBINARY = "LONGVARBINARY";
        public static final String VARBINARY = "VARBINARY";
        public static final String CLOB = "CLOB";
        public static final String NCLOB = "NCLOB";
        public static final String DATE = "DATE";
        public static final String TIME = "TIME";
        public static final String DATETIME = "DATETIME";
        public static final String TIMESTAMP = "TIMESTAMP";
        public static final String ARRAY = "ARRAY";
        public static final String STRUCT = "STRUCT";
    }

    /**
     * Constants for SQL Query Parameter direction.
     */
    public static final class QueryParamDirection {
        public static final int IN = 0;
        public static final int OUT = 1;
        public static final int INOUT = 2;

    }

    public static final String CONNECTOR_NAME = "ClientConnector";
    public static final String DATASOURCE_KEY = "datasource_key";
    public static final String TIMEZONE_UTC = "UTC";
}
