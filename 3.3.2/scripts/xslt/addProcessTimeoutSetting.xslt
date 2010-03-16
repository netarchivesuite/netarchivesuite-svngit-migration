<?xml version="1.0" encoding="UTF-8"?>
<!--
 File:        $Id: splitDatabaseClasses.xslt,v $
 Revision:    $Version: 1.2 $
 Author:      $Author: lc $
 Date:        $Date: 2007/03/09 15:38:38 $

 The Netarchive Suite - Software to harvest and preserve websites
 Copyright 2004-2007 Det Kongelige Bibliotek and Statsbiblioteket, Denmark

 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.

 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the Free Software
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 -->
<xsl:stylesheet version="1.0"
xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
xmlns:dk="http://www.netarkivet.dk/schemas/settings"
xmlns="http://www.netarkivet.dk/schemas/settings"
exclude-result-prefixes="dk">

    <!-- This script updates settings.xml files from versions before  3.3.3,
      adding a common processTimeout setting.
    -->

<xsl:output method="xml" encoding="UTF-8" />

    <xsl:template xml:space="preserve" match="dk:common/dk:cacheDir"><!--
    --><xsl:copy><xsl:copy-of select="@*"/><xsl:apply-templates/></xsl:copy>
        <xsl:comment>The number of milliseconds we wait for processes to react
            to shutdown requests.</xsl:comment>
        <processTimeout>5000</processTimeout><!--
 --></xsl:template>

<!-- Any other node gets copied unchanged. Don't change this. -->
<xsl:template match="*">
	<xsl:copy>
		<xsl:copy-of select="@*"/>
		<xsl:apply-templates/>
	</xsl:copy>
</xsl:template>

<!-- Please keep the comments around -->
    <xsl:template match="comment()">
        <xsl:copy/>
    </xsl:template>

</xsl:stylesheet>