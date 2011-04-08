<%--
File:       $Id$
Revision:   $Revision$
Author:     $Author$
Date:       $Date$

The Netarchive Suite - Software to harvest and preserve websites
Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark

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

--%><%-- This page handles searching for domains.  With no parameters, it
gives an input box for searching that feeds back into itself.
With parameter name, it performs a search.  Name can be a glob pattern
(using ? and * only) or a single domain.  If domains are found, they are
displayed, if no domains are found for a non-glob search, the user is
asked if they should be created.
--%><%@ page import="javax.servlet.RequestDispatcher,
                 java.util.List,
                 dk.netarkivet.common.CommonSettings,
                 dk.netarkivet.common.utils.Settings,
                 dk.netarkivet.common.utils.DomainUtils,
                 dk.netarkivet.common.utils.I18n,
                 dk.netarkivet.common.webinterface.HTMLUtils,
                 dk.netarkivet.harvester.datamodel.DomainDAO,
                 dk.netarkivet.harvester.webinterface.Constants"
         pageEncoding="UTF-8"
%><%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"
%><fmt:setLocale value="<%=HTMLUtils.getLocale(request)%>" scope="page"
/><fmt:setBundle scope="page" basename="<%=dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE%>"/>

<%! private static final I18n I18N
            = new I18n(dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);
%><%
    HTMLUtils.setUTF8(request);
    String domainName = request.getParameter(Constants.DOMAIN_PARAM);
    if (domainName != null && domainName.length() > 0) {
        domainName = domainName.trim();
        if (domainName.contains("?") || domainName.contains("*")) {
            List<String> matchingDomains = DomainDAO.getInstance().getDomains(
                    domainName);
            if (matchingDomains.isEmpty()) {//No matching domains
                HTMLUtils.forwardWithErrorMessage(pageContext, I18N,
                        "errormsg;no.matching.domains.for.0", domainName);
                return;
            } else {
                // Include navigate.js
                HTMLUtils.generateHeader(pageContext, "navigate.js");
                %><h3 class="page_heading">
                <fmt:message key="searching.for.0.gave.1.hits">
                    <fmt:param value="<%=domainName%>"/>
                    <fmt:param value="<%=matchingDomains.size()%>"/>
                </fmt:message>
                </h3>

                <%
                 String startPage=request.getParameter("START_PAGE_INDEX");

                 if(startPage == null){
                     startPage="1";
                 }

                 long totalResultsCount = matchingDomains.size();
                 long pageSize = Long.parseLong(Settings.get(
                         CommonSettings.HARVEST_STATUS_DFT_PAGE_SIZE));
                 long actualPageSize = (pageSize == 0 ?
                         totalResultsCount : pageSize);

                 long startPageIndex = Long.parseLong(startPage);
                 long startIndex = 0;
                 long endIndex = 0;

                 if (totalResultsCount > 0) {
                     startIndex = ((startPageIndex - 1) * actualPageSize);
                     endIndex = Math.min(startIndex + actualPageSize ,
                             totalResultsCount);
                 }
                 boolean prevLinkActive = false;
                 if (pageSize != 0 && totalResultsCount > 0 && startIndex > 1) {
                     prevLinkActive = true;
                 }

                 boolean nextLinkActive = false;
                 if (pageSize != 0 && totalResultsCount > 0
                         && endIndex < totalResultsCount) {
                     nextLinkActive = true;
                 }
                 %>

                <fmt:message key="status.results.displayed">
                <fmt:param><%=totalResultsCount%></fmt:param>
                <fmt:param><%=startIndex+1%></fmt:param>
                <fmt:param><%=endIndex%></fmt:param>
                </fmt:message>
                <%
                String startPagePost=request.getParameter("START_PAGE_INDEX");

                if(startPagePost == null){
                    startPagePost="1";
                }

                String searchParam=request.getParameter(Constants.DOMAIN_PARAM);
                searchParam = HTMLUtils.encode(searchParam);
                %>

                <p style="text-align: right">
                    <fmt:message key="status.results.displayed.pagination">
                        <fmt:param>
                        <%
                        if (prevLinkActive) {
                        %>
                            <a href="javascript:previousPage('<%=Constants.DOMAIN_PARAM%>','<%=searchParam%>');">
                                <fmt:message
                                key="status.results.displayed.prevPage"/>
                            </a>
                        <%
                        } else {
                        %>
                            <fmt:message
                            key="status.results.displayed.prevPage"/>
                        <%
                        }
                        %>
                        </fmt:param>
                        <fmt:param>
                        <%
                        if (nextLinkActive) {
                        %>
                            <a href="javascript:nextPage('<%=Constants.DOMAIN_PARAM%>','<%=searchParam%>');">
                                <fmt:message
                                key="status.results.displayed.nextPage"/>
                            </a>
                        <%
                        } else {
                        %>
                            <fmt:message key="status.results.displayed.nextPage"/>
                        <%
                        }
                        %>
                        </fmt:param>

                    </fmt:message>
                </p>

                <form method="post" name="filtersForm"
                action="Definitions-find-domains.jsp">
                    <input type="hidden" name="START_PAGE_INDEX"
                    value="<%=startPagePost%>"/>
                 </form>

<%
                List<String> matchingDomainsSubList=matchingDomains.
                                      subList((int)startIndex,(int)endIndex);
                for (String domainS : matchingDomainsSubList) {
                    String encodedDomain = HTMLUtils.encode(domainS);
                    %>
                   <a href="Definitions-edit-domain.jsp?<%=
                      Constants.DOMAIN_PARAM%>=<%=
                      HTMLUtils.escapeHtmlValues(encodedDomain)%>"><%=
                      HTMLUtils.escapeHtmlValues(domainS)%>
                    </a><br/>
<%
                }
                HTMLUtils.generateFooter(out);
                return;
            }
        } else if (DomainDAO.getInstance().exists(domainName)) {
            RequestDispatcher rd
                    = pageContext.getServletContext().getRequestDispatcher(
                    "/Definitions-edit-domain.jsp");
            rd.forward(request, response);
        } else {
            //Is it a legal domain name
            boolean isLegal = DomainUtils.isValidDomainName(domainName);
            String message;
            if (isLegal) {
                String createUrl = "Definitions-create-domain.jsp?"
                        + Constants.DOMAINLIST_PARAM + "="
                        + HTMLUtils.encode(domainName);
                message = I18N.getString(response.getLocale(),
                        "domain.0.not.found.create", domainName);
                message += " <a href=\"" + createUrl + "\">";
                message += I18N.getString(response.getLocale(), "yes");
                message += "</a>";
            } else {
                message = I18N.getString(response.getLocale(),
                        "0.is.illegal.domain.name", domainName);
            }
            request.setAttribute("message", message);
            RequestDispatcher rd = pageContext.getServletContext()
                    .getRequestDispatcher("/message.jsp");
            rd.forward(request, response);
            return;
        }
    }

    //Note: This point is only reached if no name was sent to the JSP-page
    HTMLUtils.generateHeader(
            pageContext);
%>
<h3 class="page_heading"><fmt:message key="pagetitle;find.domains"/></h3>


<form method="post"   onclick="resetPagination();"
                                      action="Definitions-find-domains.jsp">
    <table>
        <tr>
            <td><fmt:message key="prompt;enter.name.of.domain.to.find"/></td>
            <td><span id="focusElement">
                <input name="<%=Constants.DOMAIN_PARAM%>"
                	size="<%=Constants.DOMAIN_NAME_FIELD_SIZE %>" value=""/>
                </span>
            </td>
            <td><input type="submit" value="<fmt:message key="search"/>"/></td>
        </tr>
        <tr>
            <td colspan="2"><fmt:message key="may.use.wildcards"/></td>
        </tr>
    </table>
</form>
<%
    HTMLUtils.generateFooter(out);
%>