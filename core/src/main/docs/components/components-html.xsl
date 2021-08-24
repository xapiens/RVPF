<?xml version="1.0"?>

<!--
 Related Values Processing Framework.

 $Id: components-html.xsl 3097 2016-07-13 20:12:59Z SFB $
 -->

<xsl:stylesheet
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        version='1.0'>

    <xsl:import href="http://docbook.sourceforge.net/release/xsl/current/html/docbook.xsl"/>

    <xsl:param name="html.stylesheet">../docbook.css</xsl:param>

    <xsl:param name="toc.section.depth">3</xsl:param>
    <xsl:param name="make.year.ranges">1</xsl:param>
    <xsl:param name="make.single.year.ranges">1</xsl:param>

    <xsl:param name="img.src.path">figures/jpg/</xsl:param>
    <xsl:param name="graphic.default.extension">jpg</xsl:param>

    <xsl:output method="html" encoding="ISO-8859-1" indent="yes"/>

</xsl:stylesheet>

<!-- End. -->
