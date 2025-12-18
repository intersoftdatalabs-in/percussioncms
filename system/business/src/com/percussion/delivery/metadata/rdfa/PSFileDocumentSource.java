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

package com.percussion.delivery.metadata.rdfa;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * This is a DocumentSource implementation to use files as a source for RDF extraction.
 * <p>
 * This class manages InputStream lifecycle and closes every InputStream created when the close method is invoked.
 * 
 * @author miltonpividori
 * 
 */
class PSFileDocumentSource implements IPSDocumentSource
{
    private final File file;
    private List<InputStream> openInputStreams;

    public PSFileDocumentSource(File file)
    {
        this.file = file;
        this.openInputStreams = new ArrayList<>();
    }

    @Override
    public InputStream openInputStream() throws IOException
    {
        InputStream inputStream = new FileInputStream(file);
        openInputStreams.add(inputStream);
        return inputStream;
    }

    @Override
    public String getDocumentIRI()
    {
        return file.toURI().toString();
    }

    @Override
    public String getContentType()
    {
        // Return a default content type for HTML
        return "text/html";
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
