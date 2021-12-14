/*
 *     Percussion CMS
 *     Copyright (C) 1999-2021 Percussion Software, Inc.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     Mailing Address:
 *
 *      Percussion Software, Inc.
 *      PO Box 767
 *      Burlington, MA 01803, USA
 *      +01-781-438-9900
 *      support@percussion.com
 *      https://www.percussion.com
 *
 *     You should have received a copy of the GNU Affero General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>
 */

package com.percussion.taxonomy.web;

import com.percussion.taxonomy.TaxonomySecurityHelper;
import com.percussion.taxonomy.service.Node_editorService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Controller
public class Node_editorController {

    protected final Logger logger = LogManager.getLogger(getClass());
    private Node_editorService node_editorService;

    public Node_editorController() {
        //TODO: Fix me
       /* setCommandClass(Node_editor.class);
        setCommandName("node_editor");
        */
    }

    protected ModelAndView handle(HttpServletRequest request, HttpServletResponse response,
            Object command, BindException errors) throws Exception {
        //--------------------------- Templated - Modify or replace -----------------------------
    	TaxonomySecurityHelper.raise_error_if_cannot_admin();
    	Collection all = node_editorService.getAllNode_editors();
        Map<String, Object> myModel = new HashMap<String, Object>();
        myModel.put("all", all);
        return new ModelAndView("node_editor", "model", myModel);
        //------------------------------------- End Template -----------------------------------------
    }

    public void setNode_editorService(Node_editorService node_editorService) {
        this.node_editorService = node_editorService;
    }
}
