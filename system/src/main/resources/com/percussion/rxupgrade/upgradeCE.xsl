<?xml version="1.0" encoding="UTF-8"?>


<!-- This stylesheet is used to upgrade 4.0 and 4.5 content editor applications to 5.0. It adds the new attributes with default values supported by 5.0-->
<xsl:stylesheet version="1.1" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
   <xsl:output method="xml" indent="no" omit-xml-declaration="no" encoding="UTF-8"/>
   <xsl:variable name="rcenabled" select="//SharedFieldGroupName='relatedcontent'"/>
   <xsl:template match="/">
      <xsl:apply-templates mode="copy"/>
   </xsl:template>
   <xsl:template match="*" mode="copy">
      <xsl:copy>
         <xsl:copy-of select="@*"/>
         <xsl:apply-templates mode="copy"/>
      </xsl:copy>
   </xsl:template>
   <!-- template for adding new attribute objectType to content editor applications -->
   <xsl:template match="PSXContentEditor" mode="copy" priority="10">
      <xsl:copy>
         <xsl:copy-of select="@*"/>
         <xsl:if test="not(@objectType) or @objectType=''">
            <xsl:attribute name="objectType">1</xsl:attribute>
         </xsl:if>
         <xsl:if test="not(@enableRelatedContent) or @enableRelatedContent=''">
            <xsl:choose>
               <xsl:when test="$rcenabled">
                  <xsl:attribute name="enableRelatedContent">yes</xsl:attribute>
               </xsl:when>
               <xsl:otherwise>
                  <xsl:attribute name="enableRelatedContent">no</xsl:attribute>
               </xsl:otherwise>
            </xsl:choose>
         </xsl:if>
         <xsl:apply-templates mode="copy"/>
      </xsl:copy>
   </xsl:template>
   <!-- remove shared field include for related content  -->
   <xsl:template match="SharedFieldGroupName[.='relatedcontent']" mode="copy" priority="10"/>
   <!-- remove shared field excludes referring only to fields in the relatedcontent shared group, only if the relatedcontent shared group was included as a SharedFieldIncludes/SharedFieldGroupName. -->
   <xsl:template match="SharedFieldExcludes" mode="copy" priority="10">
      <xsl:copy>
         <xsl:copy-of select="@*"/>
         <xsl:apply-templates select="FieldRef" mode="copyfieldrefs"/>
      </xsl:copy>
   </xsl:template>
   <!-- remove FieldRef for sysid, slotid, itemcontentid, and variantid if RC is enabled-->
   <xsl:template match="FieldRef[.='sysid' or .='slotid' or .='itemcontentid' or .='variantid']" mode="copyfieldrefs" priority="10">
      <xsl:if test="not($rcenabled)">
         <xsl:copy-of select="."/>
      </xsl:if>
   </xsl:template>
   <!-- keep other FieldRefs unchanged -->
   <xsl:template match="FieldRef" mode="copyfieldrefs" priority="1">
      <xsl:copy-of select="."/>
   </xsl:template>
</xsl:stylesheet>
