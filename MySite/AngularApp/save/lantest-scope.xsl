<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
   xmlns:dri="http://lantest">
<xsl:output method="html"/>

<xsl:template match="/">
   <xsl:apply-templates select="*[not(name()='label' or name()='head')]" />
</xsl:template>


<xsl:template match="dri:list[@id='aspect.discovery.SimpleSearch.list.primary-search']//dri:item[dri:field[@id='aspect.discovery.SimpleSearch.field.scope']]" priority="3">
   <script type="text/javascript">
          <xsl:text>
              if (!window.DSpace.i18n) {
                  window.DSpace.i18n = {};
              } 
              if (!window.DSpace.i18n.discovery) {
                  window.DSpace.i18n.discovery = {};
              }
              if (!window.DSpace.i18n.discovery.scope) {
                  window.DSpace.i18n.discovery.scope = [];
              }</xsl:text>
              <xsl:for-each select="dri:field/dri:option"><xsl:text>
              window.DSpace.i18n.discovery.scope.push({
                  id:'</xsl:text><xsl:value-of select="@returnValue"/><xsl:text>',
                  label:'</xsl:text>
                     <xsl:choose>
                         <xsl:when test='./*'>
                            <xsl:copy-of select="./*"/>
                         </xsl:when>
                         <xsl:otherwise>
                            <xsl:value-of select="."/>
                         </xsl:otherwise>
                     </xsl:choose>
                     <xsl:text>'
              });</xsl:text>
              </xsl:for-each>

   </script>
</xsl:template>
</xsl:stylesheet>
