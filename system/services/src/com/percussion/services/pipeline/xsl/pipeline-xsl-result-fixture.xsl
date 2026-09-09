<?xml version="1.0" encoding="UTF-8"?>
<!--
  Copyright (c) 2026 Intersoft Data Labs, Inc.

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

  See the License for the specific language governing permissions and
  limitations under the License.
-->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:output method="html" encoding="UTF-8" indent="yes"/>
  <xsl:template match="/">
    <html>
      <body>
        <h1 id="pipe-xsl-html">PIPE-XSL-HTML</h1>
        <ul>
          <xsl:for-each select="//row">
            <li>
              <xsl:value-of select="sku"/>
              <xsl:text> </xsl:text>
              <xsl:value-of select="name"/>
            </li>
          </xsl:for-each>
        </ul>
      </body>
    </html>
  </xsl:template>
</xsl:stylesheet>
