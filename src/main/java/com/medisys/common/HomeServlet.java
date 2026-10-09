package com.medisys.common;

import com.medisys.medicine.CategoryDAO;
import com.medisys.medicine.Medicine;
import com.medisys.medicine.MedicineDAO;
import com.medisys.user.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * The home page (the shop front).
 *
 *   GET /      categories + popular medicines                              READ
 *
 * Staff have nothing to buy, so they are sent straight to their work page.
 * ("" is the Servlet way to map exactly the site's root address.)
 *
 * Module : Shared - agree with the team before changing it
 * Owner  : Whole team
 */
@WebServlet("")
public class HomeServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/common/home.jsp";
    private static final int FEATURED = 5;

    private final MedicineDAO medicineDAO = new MedicineDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = SessionUtil.currentUser(request);
        if (user != null && !user.isCustomer()) {
            String start;
            switch (user.getRole()) {
                case ADMIN:
                    start = "/admin/orders";
                    break;
                case PHARMACIST:
                    start = "/pharmacist/dashboard";
                    break;
                default:
                    start = "/staff/deliveries";
            }
            response.sendRedirect(request.getContextPath() + start);
            return;
        }

        try {
            request.setAttribute("categories", categoryDAO.getAllCategories());
            request.setAttribute("featured", featured());
        } catch (SQLException e) {
            throw new ServletException("Could not load the home page", e);
        }
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    /** Medicines on sale that can go straight into the cart, ones with a photo first. */
    private List<Medicine> featured() throws SQLException {
        List<Medicine> withPhoto = new ArrayList<>();
        List<Medicine> others = new ArrayList<>();
        for (Medicine m : medicineDAO.searchMedicines(null, null, MedicineDAO.FILTER_CATALOG)) {
            if (m.isAvailable() && !m.isRequiresPrescription()) {
                (m.hasImage() ? withPhoto : others).add(m);
            }
        }
        withPhoto.addAll(others);
        return withPhoto.subList(0, Math.min(FEATURED, withPhoto.size()));
    }
}
