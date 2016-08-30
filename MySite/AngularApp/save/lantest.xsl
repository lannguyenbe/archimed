<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
   xmlns:dri="http://lantest">
<xsl:output method="html"/>
<!--xsl:template match="/"-->
<!--xsl:template match="/">
   <xsl:apply-templates />
</xsl:template-->   
<xsl:template match="dri:list[@id='aspect.discovery.SimpleSearch.list.primary-search']//dri:item">
<!--xsl:template match="dri:list[@id='aspect.discovery.SimpleSearch.list.primary-search']//dri:item[dri:field[@id='aspect.discovery.SimpleSearch.field.query']]" priority="3"-->
<!--xsl:template match="dri:list[@id='aspect.discovery.SimpleSearch.list.primary-search']"-->
   <xsl:text>Hello2</xsl:text>
</xsl:template>
</xsl:stylesheet>

