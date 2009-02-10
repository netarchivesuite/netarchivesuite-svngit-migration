<%--
File:       $Id$
Revision:   $Revision$
Author:     $Author$
Date:       $Date$

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

--%><%--
This page shows the status of all applications that were known to exist
when the GUI-application was started. That information is currently taken from
the 'deploy' element in the setting.xml assigned to the
dk.netarkivet.common.webinterface.GUIApplication.
But the actual reading is done in auxiliary class 
dk.netarkivet.monitor.jmx.HostForwarding

If the application is down, this can be seen on this page. Furthermore,
the last 100 significant (log-level INFO and above) logmessages
for each application can be browsed here.

Warning: Any applications added to the system after starting the GUI-application
will not appear here.

--%><%@ page import="java.util.List,
java.util.Locale,
 dk.netarkivet.common.exceptions.ForwardedToErrorPage,
 dk.netarkivet.common.utils.I18n,
 dk.netarkivet.common.webinterface.HTMLUtils,
 dk.netarkivet.monitor.Constants,
 dk.netarkivet.monitor.webinterface.JMXSummaryUtils,
 dk.netarkivet.monitor.webinterface.StatusEntry"
             pageEncoding="UTF-8"
%><%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"
%><fmt:setLocale value="<%=HTMLUtils.getLocale(request)%>" scope="page"
/><fmt:setBundle scope="page" basename="<%=Constants.TRANSLATIONS_BUNDLE%>"/><%!
    private static final I18n I18N = new I18n(Constants.TRANSLATIONS_BUNDLE);
    private static final String RELOAD_PARAM = "reload";
%><%
    HTMLUtils.setUTF8(request);
    // Reload settings if changed
    HTMLUtils.generateHeader(pageContext);
%>
<h3 class="page_heading">
<fmt:message key="pagetitle;monitor.summary"/>
</h3>
<%
    JMXSummaryUtils.StarredRequest starredRequest =
            new JMXSummaryUtils.StarredRequest(request);
    List<StatusEntry> result = null;
	try {
    	result = JMXSummaryUtils.queryJMXFromRequest(
    		JMXSummaryUtils.STARRABLE_PARAMETERS, starredRequest, 
    		pageContext);
    } catch (ForwardedToErrorPage e) {
    	return;
    }
    Locale currentLocale = response.getLocale();
%>

<table>
    <tr><%
         if(JMXSummaryUtils.showColumn(starredRequest, 
             JMXSummaryUtils.JMXLocationProperty) { 
         %>
            <th><fmt:message key="tablefield;location"/> <%=
            JMXSummaryUtils.generateShowLink(starredRequest,
                    JMXSummaryUtils.JMXLocationProperty, currentLocale)%></th>
        <% 
         } 
    	 %>
        <th><fmt:message key="tablefield;machine"/> <%=
        JMXSummaryUtils.generateShowLink(starredRequest,
        	JMXSummaryUtils.JMXHostnameProperty, currentLocale)%></th>
        <th><fmt:message key="tablefield;application"/> <%=
        JMXSummaryUtils.generateShowLink(starredRequest,
        	JMXSummaryUtils.JMXApplicationnameProperty, currentLocale)%>
        </th>
        <th><fmt:message key="tablefield;applicationid"/> <%=
        JMXSummaryUtils.generateShowLink(starredRequest,
        	JMXSummaryUtils.JMXApplicationinstidProperty, currentLocale)%>
        </th>
        <th><fmt:message key="tablefield;port"/> <%=
        JMXSummaryUtils.generateShowLink(starredRequest,
                JMXSummaryUtils.JMXHttpportProperty, currentLocale)%></th>
        <th><fmt:message key="tablefield;priority"/> <%=
        JMXSummaryUtils.generateShowLink(starredRequest,
        	JMXSummaryUtils.JMXHarvestPriorityProperty, currentLocale)%>
        </th>
        <th><fmt:message key="tablefield;replica"/> <%=
        JMXSummaryUtils.generateShowLink(starredRequest,
        	JMXSummaryUtils.JMXArchiveReplicaIdProperty, currentLocale)%>
        </th>
        <th><fmt:message key="tablefield;index"/> <%=
        JMXSummaryUtils.generateShowLink(starredRequest,
        	JMXSummaryUtils.JMXIndexProperty, currentLocale)%></th>
        <th><fmt:message key="tablefield;logmessage"/></th>
    </tr>
    <%
    for (StatusEntry entry : result) {
        if (entry.getLogMessage(response.getLocale()).trim().length() > 0) {
    %>
    <tr>
        <td><%=JMXSummaryUtils.generateLink(starredRequest,
        		JMXSummaryUtils.JMXLocationProperty,
                entry.getLocation(),
                HTMLUtils.escapeHtmlValues(entry.getLocation()))%></td>
        <td><%=JMXSummaryUtils.generateLink(starredRequest,
                JMXSummaryUtils.JMXHostnameProperty,
                entry.getHostName(),
                HTMLUtils.escapeHtmlValues
                        (JMXSummaryUtils.reduceHostname(entry.getHostName())))%>
        </td>
        <td><%=JMXSummaryUtils.generateLink(starredRequest,
                JMXSummaryUtils.JMXApplicationnameProperty,
                entry.getApplicationName(),
                HTMLUtils.escapeHtmlValues
                        (JMXSummaryUtils.reduceApplicationName(
                                entry.getApplicationName())))%>
        </td>
        <td><%=JMXSummaryUtils.generateLink(starredRequest,
                JMXSummaryUtils.JMXApplicationinstidProperty,
                entry.getApplicationInstanceID(),
                HTMLUtils.escapeHtmlValues(entry.getApplicationInstanceID()))%>
        </td>
        <td><%=JMXSummaryUtils.generateLink(starredRequest,
                JMXSummaryUtils.JMXHttpportProperty,
                entry.getHTTPPort(),
                HTMLUtils.escapeHtmlValues(entry.getHTTPPort()))%></td>
        <td><%=JMXSummaryUtils.generateLink(starredRequest,
                JMXSummaryUtils.JMXHarvestPriorityProperty,
                entry.getHarvestPriority(),
                HTMLUtils.escapeHtmlValues(entry.getHarvestPriority()))%>
        </td>
        <td><%=JMXSummaryUtils.generateLink(starredRequest,
                JMXSummaryUtils.JMXArchiveReplicaIdProperty,
                entry.getArchiveReplicaId(),
                HTMLUtils.escapeHtmlValues(entry.getArchiveReplicaId()))%>
        </td>
        <td><%=JMXSummaryUtils.generateLink(starredRequest,
                JMXSummaryUtils.JMXIndexProperty,
                entry.getIndex(),
                HTMLUtils.escapeHtmlValues(entry.getIndex()))%></td>
        <td><%=JMXSummaryUtils.generateMessage(entry.getLogMessage(response
        .getLocale()), currentLocale)%></td>
    </tr>
    <%
        }
    }
    %>
</table>

<%
    HTMLUtils.generateFooter(out);
%>