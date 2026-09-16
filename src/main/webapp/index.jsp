<%--
    Home page.
    Shared file - agree with the team before changing it.

    TODO: once login exists, send each role to its own start page
          (customer -> /medicines, pharmacist -> /pharmacist/dashboard, ...).
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<% String pageTitle = "Home"; %>
<%@ include file="WEB-INF/views/common/header.jspf" %>

<h1>Welcome to MediSys</h1>
<p>Order your medicines online. Prescription-only medicines are checked by a
   senior pharmacist before the order is released.</p>

<%@ include file="WEB-INF/views/common/footer.jspf" %>
