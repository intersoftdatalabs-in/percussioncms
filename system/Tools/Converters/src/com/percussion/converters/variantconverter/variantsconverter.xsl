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
<xsl:stylesheet version="1.1" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                exclude-result-prefixes="urlencoder">
	<!-- Converter tool fills the variant map varible with the values from properties file.-->
   <xsl:variable name="variantMap"/>
	<xsl:output method="xml"/>
	<!-- main template -->
	<xsl:template match="/">
		<xsl:apply-templates select="." mode="rxbodyfield"/>
	</xsl:template>
	<!--Template to check for the sys_dependentvariantid and convert-->
	<xsl:template match="@sys_dependentvariantid" mode="rxbodyfield" priority="100">
		<xsl:variable name="oldVar" select="."/>
		<xsl:choose>
			<xsl:when test="$oldVar=$variantMap//variant/@old">
				<xsl:attribute name="sys_dependentvariantid"><xsl:value-of select="$variantMap//variant[@old = $oldVar]/@new"/></xsl:attribute>
			</xsl:when>
			<xsl:otherwise>
				<xsl:copy>
					<xsl:copy-of select="@*"/>
				</xsl:copy>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<!--Template to check for the sys_variantid and convert-->
	<xsl:template match="@sys_variantid" mode="rxbodyfield" priority="100">
		<xsl:variable name="oldVar" select="."/>
		<xsl:choose>
			<xsl:when test="$oldVar=$variantMap//variant/@old">
				<xsl:attribute name="sys_variantid"><xsl:value-of select="$variantMap//variant[@old = $oldVar]/@new"/></xsl:attribute>
			</xsl:when>
			<xsl:otherwise>
				<xsl:copy>
					<xsl:copy-of select="@*"/>
				</xsl:copy>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<!-- Template to match on other attributes to just copy them-->
	<xsl:template match="@*" mode="rxbodyfield">
		<xsl:copy>
			<xsl:copy-of select="@*"/>
		</xsl:copy>
	</xsl:template>
	<xsl:template match="node()" mode="rxbodyfield">
		<xsl:copy>
			<xsl:apply-templates select="@*" mode="rxbodyfield"/>
			<xsl:apply-templates mode="rxbodyfield"/>
		</xsl:copy>
	</xsl:template>
</xsl:stylesheet>
