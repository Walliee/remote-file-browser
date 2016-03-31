<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  version="1.0">

  <xsl:output method="html" encoding="iso-8859-1" indent="no"/>

  <xsl:template match="listing">
   <html>
    <head>
      <title><xsl:value-of select="@directory"/>
      </title>
      <style>
        h1{color : white;background-color : #444;}
        h3{color : white;background-color : #D00;}
        body{font-family : sans-serif,Arial,Tahoma;
             color : black;background-color : white;}
        b{color : white;background-color : #0086b2;}
        a{color : black;} HR{color : #0086b2;}
      </style>
    </head>
    <body>
      <h1><xsl:value-of select="@directory"/>
      </h1>
      <hr size="1" />
      <table cellspacing="0"
                  width="100%"
            cellpadding="5"
                  align="center">
        <tr>
          <th align="left">Filename</th>
          <th align="center">Size</th>
          <th align="right">Last Modified</th>
        </tr>
        <xsl:apply-templates select="entries"/>
        </table>
      <xsl:apply-templates select="readme"/>
      <hr size="1" />
      <h3>Tesla custom directory listing server</h3>
    </body>
   </html>
  </xsl:template>


  <xsl:template match="entries">
    <xsl:apply-templates select="entry"/>
  </xsl:template>

  <xsl:template match="readme">
    <hr size="1" />
    <pre><xsl:apply-templates/></pre>
  </xsl:template>

  <xsl:template match="entry">
    <tr>
      <td align="left">
      
        <xsl:variable name="icon" select="@icon"/>
        <img src="{$icon}"/>
      
        <xsl:variable name="urlPath" select="@urlPath"/>
        <a href="{$urlPath}">
          <tt><xsl:apply-templates/></tt>
        </a>
		<xsl:if test="@downloadLink !=''">
			<xsl:variable name="dlicon" select="@dlicon"/>
			<a href="{$urlPath}?download" style="margin-left: 5px"><img src="{$dlicon}"/></a>
		</xsl:if>
      </td>
      <td align="right">
        <tt><xsl:value-of select="@size"/></tt>
      </td>
      <td align="right">
        <tt><xsl:value-of select="@date"/></tt>
      </td>
    </tr>
  </xsl:template>

</xsl:stylesheet>