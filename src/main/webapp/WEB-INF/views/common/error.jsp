<%--
    Shown for 404 and 500 errors (see web.xml).
    Shared file - agree with the team before changing it.
--%>
<%@ page contentType="text/html;charset=UTF-8" isErrorPage="true" %>
<% String pageTitle = "Something went wrong"; %>
<%@ include file="header.jspf" %>

<h1>Something went wrong</h1>
<p>The page you asked for could not be shown.
   <a href="<%= ctx %>/">Go back to the home page</a>.</p>

<%@ include file="footer.jspf" %>
