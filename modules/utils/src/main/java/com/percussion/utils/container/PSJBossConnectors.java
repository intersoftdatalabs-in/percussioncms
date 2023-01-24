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

package com.percussion.utils.container;

import com.percussion.utils.xml.PSInvalidXmlException;
import com.percussion.utils.xml.PSXmlUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PSJBossConnectors extends PSAbstractXmlConnectors {


    private static final Path JBOSS_APPSERVER_PATH = Paths.get("AppServer");
    private static final Path JBOSS_SERVER_XML_PATH = JBOSS_APPSERVER_PATH.resolve(Paths.get("server","rx","deploy","jboss-web.deployer","server.xml"));
    private static final Path CM1_LEGACY_LAX_FILE = Paths.get("PercussionServer.lax");


    private final Path rxRootDir;



    private final Path serverFile;
    private final Path laxFile;


    public PSJBossConnectors(File rxDir)
    {
        super(JBOSS_APPSERVER_PATH.resolve(Paths.get("server","rx")));
        rxRootDir = rxDir.toPath();
        this.serverFile = rxRootDir.resolve(JBOSS_SERVER_XML_PATH);
        this.laxFile = rxRootDir.resolve(CM1_LEGACY_LAX_FILE);
    }

    public PSJBossConnectors(File rxDir,PSAbstractConnectors connectorInfo) {
        this(rxDir);
        setConnectors(connectorInfo.getConnectors());
    }



    public void load() {
        Map<String,String> laxProps = loadLaxProperties(rxRootDir);
        try {
            // load the doc
            Document doc = PSXmlUtils.getDocFromFile(getServerFile().toFile());
            fromXml(doc.getDocumentElement(),laxProps);
        } catch (IOException | SAXException | PSInvalidXmlException e) {
            throw new RuntimeException(e);
        }

    }


    public void save() {
        try {
            Map<String,String> laxProps = loadLaxProperties(rxRootDir);
            List<IPSConnector> toSaveConnectors = new ArrayList<>(this.getConnectors());

            this.getConnectors().clear();
            load();
            mergeConnectors(toSaveConnectors);
            Document doc = toXml();
            PSXmlUtils.saveDocToFile(serverFile.toFile(), doc);

        } catch (Exception e)
        {
            throw new RuntimeException(e);
        }

    }


    /**
     * Copy all attributes from the source to the target
     *
     * @param source The source element, assumed not <code>null</code>.
     * @param target The target element, assumed not <code>null</code>.
     */
    private static void copyAttributes(Element source, Element target)
    {
        NamedNodeMap attrs = source.getAttributes();
        int len = attrs.getLength();
        for (int i = 0; i < len; i++)
        {
            Attr attr = (Attr) attrs.item(i);
            target.setAttribute(attr.getName(), attr.getValue());
        }
    }

    @Override
    public Path getServerFile() {
        return serverFile;
    }

    @Override
    public String toString() {
        return "PSJBossConnectors{" +
                "rxRootDir=" + rxRootDir +
                ", serverFile=" + serverFile +
                ", laxFile=" + laxFile + "," +super.toString() +
                '}';
    }


}
