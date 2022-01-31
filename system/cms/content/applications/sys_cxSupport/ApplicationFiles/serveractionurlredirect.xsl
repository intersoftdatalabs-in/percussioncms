<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE xsl:stylesheet [
		<!ENTITY % HTMLlat1 PUBLIC "-//W3C//ENTITIES_Latin_1_for_XHTML//EN" "https://www.percussion.com/DTD/HTMLlat1x.ent">
		%HTMLlat1;
		<!ENTITY % HTMLsymbol PUBLIC "-//W3C//ENTITIES_Symbols_for_XHTML//EN" "https://www.percussion.com/DTD/HTMLsymbolx.ent">
		%HTMLsymbol;
		<!ENTITY % HTMLspecial PUBLIC "-//W3C//ENTITIES_Special_for_XHTML//EN" "https://www.percussion.com/DTD/HTMLspecialx.ent">
		%HTMLspecial;
		<!ENTITY % w3centities-f PUBLIC
				"-//W3C//ENTITIES Combined Set//EN//XML"
				"http://www.w3.org/2003/entities/2007/w3centities-f.ent"
				>
		%w3centities-f;
		]>
<xsl:stylesheet version="1.1" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns="http://www.w3.org/1999/xhtml"
                xmlns:psxi18n="com.percussion.i18n" extension-element-prefixes="psxi18n"
                exclude-result-prefixes="psxi18n">
   <xsl:import href="file:sys_resources/stylesheets/sys_I18nUtils.xsl"/>
   <xsl:variable name="lang" select="//@lang"/>
	<xsl:template match="/">
		<html>
			<head>
				<title>Redirect Page</title>
				<script>
					function redirectServerActionUrl()
					{
                 			window.location.href = "<xsl:value-of select="//@sys_serveractionurl"/>";
					}
				</script>
			</head>
			<body>
            <xsl:if test="not(//@sys_serveractionurl = '')">
               <xsl:attribute name="onload">javascript:redirectServerActionUrl();</xsl:attribute>
            </xsl:if>
				<table width="100%" height="100%" cellpadding="0" cellspacing="0" border="0" class="headercell">
					<tr class="outerboxcell">
						<td align="center" valign="middle" class="outerboxcellfont">
                     <xsl:choose>
                        <xsl:when test="not(//@sys_serveractionurl = '')">
                           <xsl:call-template name="getLocaleString">
                              <xsl:with-param name="key" select="'psx.generic@Your request is being processed'"/>
                              <xsl:with-param name="lang" select="$lang"/>
                           </xsl:call-template>. 
                           <xsl:call-template name="getLocaleString">
                              <xsl:with-param name="key" select="'psx.generic@Please wait a moment'"/>
                              <xsl:with-param name="lang" select="$lang"/>
                           </xsl:call-template>...
                        </xsl:when>
                        <xsl:otherwise>
                           <xsl:call-template name="getLocaleString">
                              <xsl:with-param name="key" select="'psx.sys_cxSupport.serveractionurlredirect@The url for the server action is empty. Action execution aborted.'"/>
                              <xsl:with-param name="lang" select="$lang"/>
                           </xsl:call-template>. 
                        </xsl:otherwise>
                     </xsl:choose>
                  </td>
					</tr>
				</table>
			</body>
		</html>
	</xsl:template>
   <psxi18n:lookupkeys>
      <key name="psx.sys_cxSupport.serveractionurlredirect@The url for the server action is empty. Action execution aborted.">The message displayed when the server action url is empty.</key>
   </psxi18n:lookupkeys>
</xsl:stylesheet>
