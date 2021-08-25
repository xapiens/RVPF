<?xml  version='1.0' encoding='ISO-8859-1'?>

<!--
 Related Values Processing Framework.

 $Id: rvpf.xsl 1342 2010-08-26 13:14:50Z sfb $
 -->

<xsl:stylesheet version='1.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>

    <xsl:output
            method='xml' encoding='ISO-8859-1'
            standalone='yes' indent='yes'/>

    <xsl:template match='/'>
        <xsl:processing-instruction name='xml-stylesheet'>
            href='rvpf.css' type='text/css'
        </xsl:processing-instruction>
        <rvpf>
            <xsl:apply-templates select='./*/*'/>
        </rvpf>
    </xsl:template>

    <xsl:template match='*'>
        <xsl:copy>
            <xsl:for-each select='@*'>
                <xsl:element name='{name()}'>
                    <xsl:value-of select='.'/>
                </xsl:element>
            </xsl:for-each>
            <xsl:apply-templates/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>

<!-- End. -->
