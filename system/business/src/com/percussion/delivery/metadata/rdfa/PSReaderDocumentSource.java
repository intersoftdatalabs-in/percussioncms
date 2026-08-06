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


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;

/**
 * This is a DocumentSource implementation to use Reader object as the
 * source for RDF extraction.
 * 
 * @author miltonpividori
 *
 */
public class PSReaderDocumentSource implements IPSDocumentSource
{
    private final byte[] content;
    private final String mimeType;
    private List<InputStream> openInputStreams = new ArrayList<>();

    public PSReaderDocumentSource(Reader reader, String mimeType) throws IOException
    {
        this.mimeType = mimeType + "; charset=utf-8";
        
        // Read the reader content into a byte array
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        char[] buffer = new char[8192];
        int read;
        while ((read = reader.read(buffer)) != -1)
        {
            baos.write(new String(buffer, 0, read).getBytes(StandardCharsets.UTF_8));
        }
        this.content = baos.toByteArray();
    }

    @Override
    public InputStream openInputStream() throws IOException
    {
        InputStream inputStream = new ByteArrayInputStream(content);
        openInputStreams.add(inputStream);
        return inputStream;
    }

    @Override
    public String getDocumentIRI()
    {
        return "file:///";
    }

    @Override
    public String getContentType()
    {
        return mimeType;
    }

    @Override
    public void close()
    {
        for (InputStream in : openInputStreams)
        {
            try
            {
                in.close();
            }
            catch (IOException e)
            {
                // ignore close failures
            }
        }
        openInputStreams.clear();
    }

}
