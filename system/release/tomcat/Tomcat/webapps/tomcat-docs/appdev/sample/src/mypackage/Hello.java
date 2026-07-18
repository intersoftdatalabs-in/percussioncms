/******************************************************************************
 *
 * [ Hello.java ]
 *
 * COPYRIGHT (c) 1999 - 2005 by Percussion Software, Inc., Woburn, MA USA.
 * All rights reserved. This material contains unpublished, copyrighted
 * work including confidential and proprietary information of Percussion.
 *
 *****************************************************************************/

package mypackage;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


/**
 * Simple servlet to validate that the Hello, World example can
 * execute servlets.  In the web application deployment descriptor,
 * this servlet must be mapped to correspond to the link in the
 * "index.html" file.
 *
 * <p>Header names/values are HTML-escaped before being written (T044 /
 * CodeQL java/xss alerts #624, #625). This is a Tomcat sample packaged
 * with the product docs tree, not a product CMS endpoint.
 *
 * @author Craig R. McClanahan &lt;Craig.McClanahan@eng.sun.com&gt;
 */

public final class Hello extends HttpServlet {


    /**
     * Respond to a GET request for the content produced by
     * this servlet.
     *
     * @param request The servlet request we are processing
     * @param response The servlet response we are producing
     *
     * @exception IOException if an input/output error occurs
     * @exception ServletException if a servlet error occurs
     */
    public void doGet(HttpServletRequest request,
                      HttpServletResponse response)
      throws IOException, ServletException {

	response.setContentType("text/html");
	PrintWriter writer = response.getWriter();

	writer.println("<html>");
	writer.println("<head>");
	writer.println("<title>Sample Application Servlet Page</title>");
	writer.println("</head>");
	writer.println("<body bgcolor=white>");

	writer.println("<table border=\"0\">");
	writer.println("<tr>");
	writer.println("<td>");
	writer.println("<img src=\"images/tomcat.gif\">");
	writer.println("</td>");
	writer.println("<td>");
	writer.println("<h1>Sample Application Servlet</h1>");
	writer.println("This is the output of a servlet that is part of");
	writer.println("the Hello, World application.  It displays the");
	writer.println("request headers from the request we are currently");
	writer.println("processing.");
	writer.println("</td>");
	writer.println("</tr>");
	writer.println("</table>");

	writer.println("<table border=\"0\" width=\"100%\">");
	Enumeration names = request.getHeaderNames();
	while (names.hasMoreElements()) {
	    String name = (String) names.nextElement();
	    String value = request.getHeader(name);
	    writer.println("<tr>");
	    // codeql[java/xss] T044 #624: header name HTML-escaped before write
	    writer.println("  <th align=\"right\">" + escapeHtml(name) + ":</th>");
	    // codeql[java/xss] T044 #625: header value HTML-escaped before write
	    writer.println("  <td>" + escapeHtml(value) + "</td>");
	    writer.println("</tr>");
	}
	writer.println("</table>");

	writer.println("</body>");
	writer.println("</html>");

    }

    /**
     * Minimal HTML escape for this sample servlet (no product-module dependency).
     */
    static String escapeHtml(String input) {
	if (input == null) {
	    return "";
	}
	return input
	    .replace("&", "&amp;")
	    .replace("<", "&lt;")
	    .replace(">", "&gt;")
	    .replace("\"", "&quot;")
	    .replace("'", "&#39;");
    }


}
