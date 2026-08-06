/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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

package com.percussion.delivery.metadata.rdfa;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

/**
 * Document source interface for RDF extraction.
 * Defines methods for opening input streams and retrieving document metadata.
 * 
 * @author miltonpividori
 *
 */
public interface IPSDocumentSource extends Closeable
{
    /**
     * Opens an InputStream to read the document content.
     * @return InputStream for reading document
     * @throws IOException if an I/O error occurs
     */
    InputStream openInputStream() throws IOException;
    
    /**
     * Gets the URI/identifier of this document source.
     * @return String URI/identifier
     */
    String getDocumentIRI();
    
    /**
     * Gets the MIME type of the document.
     * @return String MIME type
     */
    String getContentType();
    
    /**
     * Closes all open input streams.
     */
    void close();
}
