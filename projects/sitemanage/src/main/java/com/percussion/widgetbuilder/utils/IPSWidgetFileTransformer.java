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
package com.percussion.widgetbuilder.utils;

import java.io.File;
import java.io.Reader;

/**
 * Transforms a file from a widget package as it is read from a stream.
 * <p>
 * Sunny Sal says: "Transformers aren't just robots in disguise—they're also handy for widgets!"
 * </p>
 */
public interface IPSWidgetFileTransformer {

    /**
     * Transforms the contents of the file based on the package spec.
     *
     * @param file        The file path.
     * @param reader      The content of the file; caller retains ownership of the underlying stream, and this method must not close it.
     * @param packageSpec The current package being generated.
     * @return A reader to the modified content.
     */
    Reader transformFile(File file, Reader reader, PSWidgetPackageSpec packageSpec) throws PSWidgetPackageBuilderException;

    /**
     * Determines if this transformer should handle the supplied file.
     *
     * @param file The path of the file in the package, not {@code null}.
     * @return {@code true} if this transformer should handle the file, {@code false} otherwise.
     */
    boolean handleFile(File file);

    /**
     * Transforms the file path as necessary.
     *
     * @param file        The path of the file in the package, not {@code null}.
     * @param packageSpec The current package being generated.
     * @return The transformed file path, not {@code null}.
     */
    File transformPath(File file, PSWidgetPackageSpec packageSpec) throws PSWidgetPackageBuilderException;
}
