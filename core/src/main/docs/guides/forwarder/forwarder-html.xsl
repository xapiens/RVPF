<?xml version="1.0"?>

<!--
 Related Values Processing Framework.

 $Id: forwarder-html.xsl 904 2008-08-17 21:20:55Z SFB $
 -->

<xsl:stylesheet
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        version='1.0'>

    <xsl:import href="http://docbook.sourceforge.net/release/xsl/current/html/docbook.xsl"/>

    <xsl:param name="html.stylesheet">../docbook.css</xsl:param>

    <xsl:param name="toc.section.depth">3</xsl:param>
    <xsl:param name="make.year.ranges">1</xsl:param>
    <xsl:param name="make.single.year.ranges">1</xsl:param>

    <xsl:output method="html" encoding="ISO-8859-1" indent="yes"/>

</xsl:stylesheet>

<!-- End. -->
