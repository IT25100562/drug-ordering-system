<%--
    Admin category list with add and delete.
    Filled by CategoryServlet (/admin/categories).

    Module : 03 - Medicine Catalog and Inventory
    Owner  : Divisekara A. W. D. M. D. M. B.
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.medisys.model.Category" %>
<%@ page import="java.util.List" %>
<% String pageTitle = "Categories"; %>
<%@ include file="../common/header.jspf" %>
<%
    @SuppressWarnings("unchecked")
    List<Category> categories = (List<Category>) request.getAttribute("categories");
%>

<p><a href="<%= ctx %>/admin/medicines">&larr; Back to inventory</a></p>

<div class="card">
    <h1>Categories</h1>
    <p class="subtitle">A category can only be deleted when no medicine uses it.</p>

    <div class="table-wrap">
    <table>
        <tr>
            <th>Name</th>
            <th>Description</th>
            <th>Medicines</th>
            <th></th>
        </tr>
        <% for (Category c : categories) { %>
        <tr>
            <td><strong><%= TextUtil.html(c.getName()) %></strong></td>
            <td><%= c.getDescription() == null ? "-" : TextUtil.html(c.getDescription()) %></td>
            <td>
                <a href="<%= ctx %>/admin/medicines?category=<%= c.getId() %>"><%= c.getMedicineCount() %></a>
            </td>
            <td>
                <% if (c.getMedicineCount() == 0) { %>
                    <form method="post" action="<%= ctx %>/admin/categories"
                          data-confirm="Delete the category <%= TextUtil.html(c.getName()) %>?">
                        <input type="hidden" name="action" value="delete">
                        <input type="hidden" name="id" value="<%= c.getId() %>">
                        <button class="btn small reject" type="submit">Delete</button>
                    </form>
                <% } else { %>
                    <span class="meta">In use</span>
                <% } %>
            </td>
        </tr>
        <% } %>
    </table>
    </div>
</div>

<div class="card">
    <h2>Add a category</h2>
    <form method="post" action="<%= ctx %>/admin/categories">
        <input type="hidden" name="action" value="add">
        <div class="row">
            <div class="field">
                <label for="name">Name *</label>
                <input type="text" id="name" name="name" minlength="2" maxlength="100" required>
            </div>
            <div class="field">
                <label for="description">Description</label>
                <input type="text" id="description" name="description" maxlength="255">
            </div>
        </div>
        <button class="btn" type="submit">Add category</button>
    </form>
</div>

<%@ include file="../common/footer.jspf" %>
