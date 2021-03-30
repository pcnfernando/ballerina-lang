/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package io.ballerina.quoter.formatter;

import io.ballerina.quoter.config.QuoterConfig;
import io.ballerina.quoter.segment.Segment;

import java.io.IOException;

/**
 * Base formatter.
 *
 * @since 2.0.0
 */
public abstract class SegmentFormatter {
    /**
     * Creates a formatter based on the configuration option.
     * Creates a template formatter with the internal formatter if that option is set.
     *
     * @param config Configuration object.
     * @return Created formatter.
     */
    public static SegmentFormatter getFormatter(QuoterConfig config) throws IOException {
        if (config.useTemplate()) {
            return TemplateFormatter.fromConfig(config);
        } else {
            return getInternalFormatter(config);
        }
    }

    /**
     * Creates a internal formatter. (A formatter without templates)
     * Throws an error if the formatter is unknown.
     *
     * @param config Configuration object.
     * @return Created formatter.
     */
    protected static SegmentFormatter getInternalFormatter(QuoterConfig config) {
        switch (config.formatter()) {
            case NONE:
                return new NoFormatter();
            case VARIABLE:
                return new VariableFormatter();
            default:
                return new DefaultFormatter();
        }
    }

    /**
     * Formatting method.
     * Formats a segment into string representation of the required format.
     *
     * @param segment Segment root node.
     * @return String representation.
     */
    public abstract String format(Segment segment);
}
