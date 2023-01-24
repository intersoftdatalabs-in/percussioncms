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
<xsl:stylesheet exclude-result-prefixes="psxi18n" version="1.1" xmlns:psxi18n="com.percussion.i18n" extension-element-prefixes="psxi18n" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:import href="file:sys_resources/stylesheets/assemblers/sys_InlineLinks.xsl"/>
	<xsl:import href="file:rx_resources/stylesheets/assemblers/rx_InlineLinks.xsl"/>
	<xsl:import href="file:sys_resources/stylesheets/assemblers/sys_Slots.xsl"/>
	<xsl:import href="file:rx_resources/stylesheets/assemblers/rx_Slots.xsl"/>
	<xsl:import href="file:sys_resources/stylesheets/assemblers/sys_ContextTemplates.xsl"/>
	<xsl:import href="file:sys_resources/stylesheets/assemblers/sys_Globals.xsl"/>
	<xsl:import href="file:rx_resources/stylesheets/assemblers/rx_Globals.xsl"/>
	<xsl:output encoding="UTF-8"/>
	<xsl:variable name="related" select="/*/sys_AssemblerInfo/RelatedContent"/>
	<xsl:variable name="syscommand" select="//@sys_command"/>
	<xsl:variable name="this" select="/"/>
	<xsl:template match="/" name="xsplit_root">
		<html>
			<head>
				<meta content="Percussion Rhythmyx" name="generator"/>
				<title>
      Navigation Reset
    </title>
			</head>
			<body>
				<div align="center">
      Navigation Reset
    </div>
				<xsl:apply-templates select="*/gb"/>
				<br id="0"/>
				<div align="center">
					<input onclick="window.close()" title="Close Window" type="button" value="Close Window"/>
				</div>
			</body>
		</html>
	</xsl:template>
	<xsl:template match="*">
		<xsl:choose>
			<xsl:when test="text()">
				<xsl:choose>
					<xsl:when test="@no-escaping">
						<xsl:value-of disable-output-escaping="yes" select="."/>
					</xsl:when>
					<xsl:otherwise>
						<xsl:value-of select="."/>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:when>
			<xsl:otherwise>&nbsp;</xsl:otherwise>
		</xsl:choose>
		<xsl:if test="not(position()=last())">
			<br id="XSpLit"/>
		</xsl:if>
	</xsl:template>
	<xsl:template match="attribute::*">
		<xsl:value-of select="."/>
		<xsl:if test="not(position()=last())">
			<br id="XSpLit"/>
		</xsl:if>
	</xsl:template>
	<xsl:template match="*[div/@class=&apos;rxbodyfield&apos;]">
		<xsl:apply-templates mode="rxbodyfield" select="*"/>
	</xsl:template>
	<xsl:template match="sys_AssemblerInfo"/>
</xsl:stylesheet>
