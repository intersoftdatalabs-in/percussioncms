/*
 * Copyright 1999-2023 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.percussion.services.datasource;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

import java.util.Objects;
import java.util.stream.IntStream;

/**
 * A variation on the improved naming strategy that converts names to uppercase
 * with underscores to avoid problems with case-sensitive databases.
 *
 * <p>This strategy uses modern Java 11 features for efficient string processing
 * and provides consistent database naming across different platforms.</p>
 *
 * @author Percussion Software
 * @since 6.0
 */
public class UpperCaseNamingStrategy extends PhysicalNamingStrategyStandardImpl {

    private static final long serialVersionUID = 1L;

    @Override
    public Identifier toPhysicalTableName(Identifier name, JdbcEnvironment context) {
        Objects.requireNonNull(name, "name may not be null");
        return new Identifier(addUnderscores(name.getText()), name.isQuoted());
    }

    @Override
    public Identifier toPhysicalColumnName(Identifier name, JdbcEnvironment context) {
        Objects.requireNonNull(name, "name may not be null");
        return new Identifier(addUnderscores(name.getText()), name.isQuoted());
    }

    /**
     * Convert camelCase names to UPPER_CASE_WITH_UNDERSCORES using modern Java 11 patterns.
     *
     * <p>This method efficiently processes the input string to insert underscores
     * before uppercase letters that are surrounded by lowercase letters, then
     * converts the entire result to uppercase.</p>
     *
     * @param name The name to convert, may not be null
     * @return The converted name in UPPER_CASE_WITH_UNDERSCORES format
     * @throws IllegalArgumentException if name is null
     */
    protected static String addUnderscores(String name) {
        Objects.requireNonNull(name, "name may not be null");

        if (name.isEmpty()) {
            return name;
        }

        var buf = new StringBuilder(name.replace('.', '_'));

        // Use IntStream for efficient index processing
        var indices = IntStream.range(1, buf.length() - 1)
                .filter(i -> shouldInsertUnderscore(buf, i))
                .boxed()
                .sorted((a, b) -> Integer.compare(b, a)) // Process in reverse order
                .toArray(Integer[]::new);

        // Insert underscores in reverse order to maintain correct indices
        for (var index : indices) {
            buf.insert(index, '_');
        }

        return buf.toString().toUpperCase();
    }

    /**
     * Determine if an underscore should be inserted at the given position.
     *
     * @param buf The string buffer being processed
     * @param i   The index to check
     * @return true if underscore should be inserted, false otherwise
     */
    private static boolean shouldInsertUnderscore(StringBuilder buf, int i) {
        return Character.isLowerCase(buf.charAt(i - 1)) &&
                Character.isUpperCase(buf.charAt(i)) &&
                Character.isLowerCase(buf.charAt(i + 1));
    }
}
