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
package com.percussion.sitemanage.task.impl;

import com.percussion.cms.IPSConstants;
import com.percussion.error.PSExceptionUtils;
import com.percussion.extension.IPSExtensionDef;
import com.percussion.extension.PSExtensionException;
import com.percussion.rx.delivery.impl.PSDatabaseDeliveryHandler;
import com.percussion.rx.delivery.impl.PSDatabaseDeliveryHandler.DbmsConnection;
import com.percussion.rx.delivery.impl.PSDatabaseDeliveryHandler.DbmsInfo;
import com.percussion.rx.publisher.IPSEditionTask;
import com.percussion.rx.publisher.IPSEditionTaskStatusCallback;
import com.percussion.services.assembly.IPSAssemblyTemplate;
import com.percussion.services.assembly.PSAssemblyException;
import com.percussion.services.assembly.PSAssemblyServiceLocator;
import com.percussion.services.publisher.IPSEdition;
import com.percussion.services.pubserver.IPSPubServer;
import com.percussion.services.pubserver.IPSPubServerDao;
import com.percussion.services.pubserver.PSPubServerDaoLocator;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.share.spring.PSSpringWebApplicationContextUtils;
import com.percussion.tablefactory.PSJdbcTableFactory;
import com.percussion.xml.PSXmlDocumentBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

import static org.apache.commons.lang.StringUtils.isBlank;

/**
 * This edition task is used to create or update the tables that is defined by perc.pageDatabase template.
 * 
 * @author YuBingChen
 */
public class PSUpdateTablesEditionTask implements IPSEditionTask
{

    @Override
    public TaskType getType()
    {
        return TaskType.PREEDITION;
    }

    @Override
    public void perform(IPSEdition edition, IPSSite site, Date startTime, Date endTime, long jobId, long duration,
            boolean success, Map<String, String> params, IPSEditionTaskStatusCallback status) throws Exception
    {
        IPSAssemblyTemplate template = getTemplate(params);
        if (template == null)
            return;

        Document doc = getTableDefs(edition, template);
        updateTables(doc, edition);
    }

    /**
     * Gets the template defined by the given parameters. The template name is default to "perc.pageDatabase"
     * if the template name is not defined in the parameters.
     * 
     * @param params the parameters of the edition task, assumed not <code>null</code>.
     * 
     * @return the template with the specified name. It may be <code>null</code> if cannot find the template.
     */
    private IPSAssemblyTemplate getTemplate(Map<String, String> params)
    {
        IPSAssemblyTemplate template = null;
        String templateName = isBlank(params.get("template")) ? "perc.pageDatabase" : params.get("template");
        
        try
        {
            template = PSAssemblyServiceLocator.getAssemblyService().findTemplateByName(templateName);
        }
        catch (PSAssemblyException e)
        {
            log.error("Skip update tables because cannot find template: {} Error: {}",
                    templateName,
                    PSExceptionUtils.getMessageForLog(e));
        }
        return template;
    }
    
    /**
     * Create or update the tables according to the supplied definition.
     * @param doc the table definition, never <code>null</code>.
     * @throws Exception if error occurs.
     */
    private void updateTables(Document doc, IPSEdition edition) throws Exception
    {
        DbmsConnection dbmsConn = null;
        
        try
        {
            DbmsInfo dbmsInfo = getConnectionInfo(edition);
            if (dbmsInfo == null)
                dbmsInfo = new DbmsInfo(doc);
            dbmsConn = new DbmsConnection(dbmsInfo);
            dbmsConn.setConnection();
            
            boolean transactionSupport = true;
            final StringBuilder error_builder = new StringBuilder();
            final StringBuilder message_builder = new StringBuilder();
    
            PSJdbcTableFactory.processTables(dbmsConn.getConnection(), dbmsConn.getDbmsDef(),
                  dbmsConn.getTableMetaMap(), null, doc,
                  new PrintStream(System.out)
                  {
                     @Override
                     public void println(String msg)
                     {
                        if (StringUtils.isBlank(msg))
                           return;
    
                        if (msg.indexOf("Error") >= 0)
                           error_builder.append(msg);
                        else
                           message_builder.append(msg);
                        char c = msg.charAt(msg.length()-1);
                        if (c != '\r' && c != '\n')
                           message_builder.append('\n');
                     }
                  }, log.isDebugEnabled(), transactionSupport);
        }
        catch (Exception e)
        {
            log.error("Failed to create or update tables", e);
            throw e;
        }
        finally
        {
            if (dbmsConn != null)
                dbmsConn.close();
        }
    }
    
    /**
     * Gets the table definition from the perc.pageDatabase template.
     * @return the table definition, it may be <code>null</code> if failed to get one.
     * @throws PSAssemblyException if failed to get the template.
     */
    private Document getTableDefs(IPSEdition edition, IPSAssemblyTemplate template) throws PSAssemblyException
    {
        String xml = template.getTemplate();
        xml = xml.replaceAll("allowSchemaChanges=\"n\" alter=\"n\"", "allowSchemaChanges=\"y\" alter=\"y\"");
        xml = xml.replace("<tabledefset>", "\n<datapublisher dbname=\"cmlite_db\" drivertype=\"jtds:sqlserver\" origin=\"dbo\" resourceName=\"jdbc/cmlite_db\" >\n<tabledefset>");
        
        xml = xml.replace("</tabledefset>", "</tabledefset>\n" +
        "<tabledataset>\n" +
          "<table name=\"PERC_EXPORT_PAGE\">\n" +
              "<childtable name=\"PERC_EXPORT_PAGE_TAG\" />\n" +
              "<childtable name=\"PERC_EXPORT_PAGE_WIDGET\"/>\n" +
              "<childtable name=\"PERC_EXPORT_PAGE_CATEGORY\" />\n" +
              "<childtable name=\"PERC_EXPORT_PAGE_CALENDAR\" />\n" +
          "</table>\n" +
        "</tabledataset>\n" +
      "</datapublisher>");
      		
        //System.out.println("template content: " + xml);
        
        InputStream in;
        try
        {
            in = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
            return PSXmlDocumentBuilder.createXmlDocument(new InputSource(in), false);
        }
        catch (Exception e)
        {
            log.error("Failed to create XML for String: {} Error: {}" ,
                    xml,
                    PSExceptionUtils.getMessageForLog(e));
            return null;
        }
    }

    private DbmsInfo getConnectionInfo(IPSEdition edition)
    {
        if (edition.getPubServerId() == null)
            return null;
            
        IPSPubServer pubServer = getPubServerDao().findPubServer(edition.getPubServerId());
        if (pubServer != null)
            return PSDatabaseDeliveryHandler.getDbmsInfoFromPubServer(pubServer);
        
        return null;
    }
    
    @Override
    public void init(IPSExtensionDef arg0, File arg1) throws PSExtensionException
    {
        PSSpringWebApplicationContextUtils.injectDependencies(this);
    }

    private IPSPubServerDao getPubServerDao()
    {
        if (pubServerDao == null)
            pubServerDao = PSPubServerDaoLocator.getPubServerManager();
        return pubServerDao;
    }
    
    private IPSPubServerDao pubServerDao = null;
    
    private static final Logger log = LogManager.getLogger(IPSConstants.PUBLISHING_LOG);
}
