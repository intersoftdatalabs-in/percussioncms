/*
 *     Percussion CMS
 *     Copyright (C) 1999-2020 Percussion Software, Inc.
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
 *      https://www.percusssion.com
 *
 *     You should have received a copy of the GNU Affero General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>
 */

package com.percussion.HTTPClient;

import java.io.IOException;
import java.util.Vector;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;


/**
 * This module handles the TransferEncoding response header. It currently
 * handles the "gzip", "deflate", "compress", "chunked" and "identity"
 * tokens.
 *
 * @version	0.3-3  06/05/2001
 * @author	Ronald Tschalär
 */
@Deprecated
class TransferEncodingModule implements HTTPClientModule
{
    // Methods

    /**
     * Invoked by the HTTPClient.
     */
    public int requestHandler(Request req, Response[] resp)
	    throws ModuleException
    {
	// Parse TE header

	int idx;
	NVPair[] hdrs = req.getHeaders();
	for (idx=0; idx<hdrs.length; idx++)
	    if (hdrs[idx].getName().equalsIgnoreCase("TE"))
		break;

	Vector pte;
	if (idx == hdrs.length)
	{
	    hdrs = Util.resizeArray(hdrs, idx+1);
	    req.setHeaders(hdrs);
	    pte = new Vector();
	}
	else
	{
	    try
		{ pte = Util.parseHeader(hdrs[idx].getValue()); }
	    catch (ParseException pe)
		{ throw new ModuleException(pe.toString()); }
	}


        // done if "*;q=1.0" present

        HttpHeaderElement all = Util.getElement(pte, "*");
        if (all != null)
	{
	    NVPair[] params = all.getParams();
	    for (idx=0; idx<params.length; idx++)
		if (params[idx].getName().equalsIgnoreCase("q"))  break;

	    if (idx == params.length)   // no qvalue, i.e. q=1.0
		return REQ_CONTINUE;

	    if (params[idx].getValue() == null  ||
		params[idx].getValue().length() == 0)
		throw new ModuleException("Invalid q value for \"*\" in TE " +
					  "header: ");

	    try
	    {
		if (Float.valueOf(params[idx].getValue()).floatValue() > 0.)
		    return REQ_CONTINUE;
	    }
	    catch (NumberFormatException nfe)
	    {
		throw new ModuleException("Invalid q value for \"*\" in TE " +
					  "header: " + nfe.getMessage());
	    }
	}


	// Add gzip, deflate, and compress tokens to the TE header

	if (!pte.contains(new HttpHeaderElement("deflate")))
	    pte.addElement(new HttpHeaderElement("deflate"));
	if (!pte.contains(new HttpHeaderElement("gzip")))
	    pte.addElement(new HttpHeaderElement("gzip"));
	if (!pte.contains(new HttpHeaderElement("compress")))
	    pte.addElement(new HttpHeaderElement("compress"));

	hdrs[idx] = new NVPair("TE", Util.assembleHeader(pte));

	return REQ_CONTINUE;
    }


    /**
     * Invoked by the HTTPClient.
     */
    public void responsePhase1Handler(Response resp, RoRequest req)
    {
    }


    /**
     * Invoked by the HTTPClient.
     */
    public int responsePhase2Handler(Response resp, Request req)
    {
	return RSP_CONTINUE;
    }


    /**
     * Invoked by the HTTPClient.
     */
    public void responsePhase3Handler(Response resp, RoRequest req)
		throws IOException, ModuleException
    {
	String te = resp.getHeader("Transfer-Encoding");
	if (te == null  ||  req.getMethod().equals("HEAD"))
	    return;

	Vector pte;
	try
	    { pte = Util.parseHeader(te); }
	catch (ParseException pe)
	    { throw new ModuleException(pe.toString()); }

	while (pte.size() > 0)
	{
	    String encoding = ((HttpHeaderElement) pte.lastElement()).getName();
	    if (encoding.equalsIgnoreCase("gzip"))
	    {
		Log.write(Log.MODS, "TEM:   pushing gzip-input-stream");

		resp.inp_stream = new GZIPInputStream(resp.inp_stream);
	    }
	    else if (encoding.equalsIgnoreCase("deflate"))
	    {
		Log.write(Log.MODS, "TEM:   pushing inflater-input-stream");

		resp.inp_stream = new InflaterInputStream(resp.inp_stream);
	    }
	    else if (encoding.equalsIgnoreCase("compress"))
	    {
		Log.write(Log.MODS, "TEM:   pushing uncompress-input-stream");

		resp.inp_stream = new UncompressInputStream(resp.inp_stream);
	    }
	    else if (encoding.equalsIgnoreCase("chunked"))
	    {
		Log.write(Log.MODS, "TEM:   pushing chunked-input-stream");

		resp.inp_stream = new ChunkedInputStream(resp.inp_stream);
	    }
	    else if (encoding.equalsIgnoreCase("identity"))
	    {
		Log.write(Log.MODS, "TEM:   ignoring 'identity' token");
	    }
	    else
	    {
		Log.write(Log.MODS, "TEM:   Unknown transfer encoding '" +
				    encoding + "'");

		break;
	    }

	    pte.removeElementAt(pte.size()-1);
	}

	if (pte.size() > 0)
	    resp.setHeader("Transfer-Encoding", Util.assembleHeader(pte));
	else
	    resp.deleteHeader("Transfer-Encoding");
    }


    /**
     * Invoked by the HTTPClient.
     */
    public void trailerHandler(Response resp, RoRequest req)
    {
    }
}
