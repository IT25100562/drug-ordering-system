package com.medisys.servlet.order;

import com.medisys.dao.OrderDAO;
import com.medisys.model.OrderStatus;
import com.medisys.service.OrderService;
import com.medisys.util.TextUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Admin list of all orders with status tabs and search.
 *
 *   GET /admin/orders?status=OPEN&q=nimal
 *   (status: OPEN (default), PAID, PROCESSING, SHIPPED, DELIVERED, CANCELLED, ALL)
 *
 * Module : 02 - Order Placement and Checkout
 * Owner  : Hewage B. H. A. S.
 */
@WebServlet("/admin/orders")
public class ManageOrdersServlet extends HttpServlet {

    private final OrderService orderService = new OrderService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String filter = TextUtil.clean(request.getParameter("status")).toUpperCase();
        if (OrderStatus.fromText(filter) == null && !OrderDAO.FILTER_ALL.equals(filter)) {
            filter = OrderDAO.FILTER_OPEN;
        }
        String keyword = TextUtil.clean(request.getParameter("q"));
        if (keyword.length() > 100) {
            keyword = keyword.substring(0, 100);
        }

        try {
            request.setAttribute("orders", orderService.getOrders(filter, keyword));
            request.setAttribute("counts", orderService.getCounts());
        } catch (SQLException e) {
            throw new ServletException("Could not load the orders", e);
        }
        request.setAttribute("filter", filter);
        request.setAttribute("keyword", keyword);
        request.getRequestDispatcher("/WEB-INF/views/order/manage-orders.jsp").forward(request, response);
    }
}
