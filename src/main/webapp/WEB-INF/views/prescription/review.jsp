<%--
    Pharmacist reviews one prescription: reads the document, lists the
    medicines with quantity and how to use them, then Approve / Reject /
    Request correction.
    Filled by ReviewPrescriptionServlet (/pharmacist/review?id=).

    Module : 05 - Prescription Upload and Verification
    Owner  : Perera D. A. A. N. S.
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.medisys.model.Medicine" %>
<%@ page import="com.medisys.model.Prescription" %>
<%@ page import="com.medisys.service.PrescriptionService" %>
<%@ page import="java.util.List" %>
<% String pageTitle = "Review Prescription"; %>
<%@ include file="../common/header.jspf" %>
<%
    Prescription p = (Prescription) request.getAttribute("prescription");
    User customer = (User) request.getAttribute("customer");
    @SuppressWarnings("unchecked")
    java.util.Map<String, Integer> customerStats = (java.util.Map<String, Integer>) request.getAttribute("customerStats");
    String note = (String) request.getAttribute("note");
    @SuppressWarnings("unchecked")
    List<String> errors = (List<String>) request.getAttribute("errors");
    @SuppressWarnings("unchecked")
    List<Medicine> medicines = (List<Medicine>) request.getAttribute("medicines");
    String[] rowMedicineIds = (String[]) request.getAttribute("rowMedicineIds");
    String[] rowQuantities = (String[]) request.getAttribute("rowQuantities");
    String[] rowDosages = (String[]) request.getAttribute("rowDosages");
    String fileUrl = ctx + "/prescriptions/file?id=" + p.getId();
    boolean canDecide = p.isAwaitingReview();
    // Show at least 3 medicine lines, or as many as were posted.
    int rowCount = Math.max(3, rowMedicineIds.length);
%>
<%!
    private String at(String[] values, int i) {
        return values != null && i < values.length && values[i] != null ? values[i] : "";
    }
%>

<nav class="breadcrumb" aria-label="Breadcrumb">
    <a href="<%= ctx %>/pharmacist/dashboard">Verification Dashboard</a> <span aria-hidden="true">/</span>
    <span aria-current="page"><%= p.getReference() %></span>
</nav>

<div class="title-row">
    <div>
        <h1>Prescription <%= p.getReference() %></h1>
        <p class="subtitle">
            <% if (p.isPaid()) { %>
                <span class="badge badge-paid">Paid</span>
            <% } else { %>
                <span class="badge <%= p.getStatus().getCssClass() %>"><%= p.getStatus().getLabel() %></span>
            <% } %>
            <% if (p.isExpired()) { %><span class="badge badge-expired">Expired</span><% } %>
            &nbsp;Uploaded <%= TextUtil.timeAgo(p.getUploadedAt()) %> (<%= TextUtil.dateTime(p.getUploadedAt()) %>)
            by <%= TextUtil.html(p.getCustomerName()) %>
            <% if (customer.isFlagged()) { %><span class="badge flag-badge">&#9873; Flagged</span><% } %>
        </p>
    </div>
</div>

<% if (errors != null && !errors.isEmpty()) { %>
    <div class="message error" role="alert">
        <ul><% for (String e : errors) { %><li><%= TextUtil.html(e) %></li><% } %></ul>
    </div>
<% } %>

<div class="review-layout">
    <%-- --------------------------------------------------- the document --%>
    <section class="card document-card">
        <div class="document-bar">
            <span><strong><%= TextUtil.html(p.getOriginalFileName()) %></strong>
                <span class="meta">&middot; <%= p.getFileSizeLabel() %></span></span>
            <span>
                <a href="<%= fileUrl %>" target="_blank" rel="noopener">Open full size</a> &middot;
                <a href="<%= fileUrl %>&amp;download=1">Download</a>
            </span>
        </div>
        <div class="document-view">
            <% if (p.isPdf()) { %>
                <iframe src="<%= fileUrl %>" title="Prescription document <%= p.getReference() %>"></iframe>
            <% } else { %>
                <a href="<%= fileUrl %>" target="_blank" rel="noopener">
                    <img src="<%= fileUrl %>" alt="Prescription document <%= p.getReference() %>">
                </a>
            <% } %>
        </div>
    </section>

    <%-- ------------------------------------------------ the details --%>
    <aside class="review-side">
        <%-- Who uploaded it (module 04): photo, identity, contact, history and the red flag. --%>
        <section class="card customer-card <%= customer.isFlagged() ? "flagged" : "" %>">
            <div class="customer-head">
                <%= avatar(ctx, customer, "avatar large") %>
                <div>
                    <h2><%= TextUtil.html(customer.getFullName()) %></h2>
                    <span class="meta">Customer since <%= TextUtil.date(customer.getCreatedAt().toLocalDate()) %></span>
                </div>
            </div>

            <% if (customer.isFlagged()) { %>
                <div class="flag-banner" role="note">
                    <strong>&#9873; Flagged customer</strong>
                    <p><%= TextUtil.html(customer.getFlagReason()) %></p>
                    <span class="meta">by <%= TextUtil.html(customer.getFlaggedByName()) %>,
                        <%= TextUtil.dateTime(customer.getFlaggedAt()) %></span>
                </div>
            <% } %>

            <dl class="info compact">
                <dt>NIC</dt><dd><%= TextUtil.html(customer.getNic()) %></dd>
                <dt>Age</dt><dd><%= customer.getAge() == null ? "-" : customer.getAge() + " years" %>
                    <span class="meta">(born <%= TextUtil.date(customer.getDateOfBirth()) %>)</span></dd>
                <dt>Phone</dt><dd><% if (customer.getPhone() != null) { %>
                    <a href="tel:<%= TextUtil.html(customer.getPhone()) %>"><%= TextUtil.html(customer.getPhone()) %></a><% } else { %>-<% } %></dd>
                <dt>WhatsApp</dt><dd><% if (customer.getWhatsapp() != null) { %>
                    <a href="https://wa.me/<%= customer.getWhatsappLinkNumber() %>" target="_blank" rel="noopener"><%= TextUtil.html(customer.getWhatsapp()) %></a><% } else { %>-<% } %></dd>
                <dt>Email</dt><dd><a href="mailto:<%= TextUtil.html(customer.getEmail()) %>"><%= TextUtil.html(customer.getEmail()) %></a></dd>
                <dt>Address</dt><dd><%= customer.getAddress() == null ? "-" : TextUtil.html(customer.getAddress()) %></dd>
                <dt>Note</dt><dd><%= p.getCustomerNote() == null ? "-" : TextUtil.html(p.getCustomerNote()) %></dd>
                <% if (p.getCorrectionCount() > 0) { %>
                    <dt>Copy</dt><dd>Corrected copy #<%= p.getCorrectionCount() %></dd>
                <% } %>
            </dl>

            <p class="customer-stats">
                <span><strong><%= customerStats.get("prescriptions") %></strong> prescriptions</span>
                <span class="<%= customerStats.get("rejected") > 0 ? "bad" : "" %>"><strong><%= customerStats.get("rejected") %></strong> rejected</span>
                <span><strong><%= customerStats.get("orders") %></strong> orders</span>
            </p>

            <% if (customer.isFlagged()) { %>
                <form method="post" action="<%= ctx %>/users/flag"
                      data-confirm="Remove the red flag from <%= TextUtil.html(customer.getFullName()) %>?">
                    <input type="hidden" name="id" value="<%= customer.getId() %>">
                    <input type="hidden" name="action" value="unflag">
                    <input type="hidden" name="returnTo" value="<%= TextUtil.html(currentUrl) %>">
                    <button class="btn plain block" type="submit">Remove the flag</button>
                </form>
            <% } else { %>
                <details class="flag-box">
                    <summary>&#9873; Flag this customer</summary>
                    <p class="meta">For customers who misuse MediSys, e.g. sending unrelated photos.
                        They are <strong>not</strong> blocked - the flag only warns the staff.</p>
                    <form method="post" action="<%= ctx %>/users/flag">
                        <input type="hidden" name="id" value="<%= customer.getId() %>">
                        <input type="hidden" name="action" value="flag">
                        <input type="hidden" name="returnTo" value="<%= TextUtil.html(currentUrl) %>">
                        <div class="field">
                            <label for="flagReason">Reason (other staff will see it) *</label>
                            <input type="text" id="flagReason" name="reason" minlength="5" maxlength="300" required
                                   placeholder="e.g. Uploaded holiday photos instead of prescriptions">
                        </div>
                        <button class="btn reject block" type="submit">Flag in red</button>
                    </form>
                </details>
            <% } %>
        </section>

        <% if (!canDecide) { %>
        <section class="card">
            <h2>Decision</h2>
            <p>
                <span class="badge <%= p.getStatus().getCssClass() %>"><%= p.getStatus().getLabel() %></span>
                <span class="meta">by <%= TextUtil.html(p.getReviewedByName()) %>, <%= TextUtil.dateTime(p.getReviewedAt()) %></span>
            </p>
            <% if (p.getPharmacistNote() != null) { %>
                <div class="note-box"><span class="note-title">Note to the customer</span>
                    <p><%= TextUtil.html(p.getPharmacistNote()) %></p></div>
            <% } %>
            <% if (p.needsCorrection()) { %>
                <p class="meta">Waiting for the customer to send a new copy.</p>
            <% } %>
            <% if (p.isPaid()) { %>
                <p class="paid-note">&#10003; Paid <%= TextUtil.money(p.getAmountPaid()) %> on
                    <%= TextUtil.dateTime(p.getPaidAt()) %> (<%= TextUtil.html(p.getPaymentReference()) %>)</p>
                <p class="meta">Deliver to <%= TextUtil.html(p.getDeliveryName()) %>,
                    <%= TextUtil.html(p.getDeliveryAddress()) %>, <%= TextUtil.html(p.getDeliveryPhone()) %></p>
            <% } else if (p.getStatus() == com.medisys.model.PrescriptionStatus.APPROVED) { %>
                <p class="meta">Waiting for the customer to pay.</p>
            <% } %>
        </section>
        <% } %>

        <% if (p.isDeletable()) { %>
            <form class="delete-rx" method="post" action="<%= ctx %>/pharmacist/delete"
                  data-confirm="Delete <%= p.getReference() %> and its file? The customer will be notified.">
                <input type="hidden" name="id" value="<%= p.getId() %>">
                <button class="link-button danger" type="submit">Delete this prescription (invalid or expired)</button>
            </form>
        <% } %>
    </aside>
</div>

<% if (!p.getItems().isEmpty()) { %>
    <section class="card">
        <h2>Medicines on this prescription</h2>
        <%@ include file="items-table.jspf" %>
    </section>
<% } %>

<% if (canDecide) { %>
<section class="card decision-card">
    <h2>Decision</h2>
    <% if (p.getCorrectionCount() > 0 && p.getPharmacistNote() != null) { %>
        <div class="note-box">
            <span class="note-title">Your earlier request</span>
            <p><%= TextUtil.html(p.getPharmacistNote()) %></p>
        </div>
    <% } %>
    <% if (p.isExpired()) { %>
        <div class="message info">This prescription is older than <%= Prescription.EXPIRY_DAYS %>
            days, so it cannot be approved. Reject it or delete it.</div>
    <% } %>

    <form method="post" action="<%= ctx %>/pharmacist/review" data-decision-form novalidate>
        <input type="hidden" name="id" value="<%= p.getId() %>">

        <% if (!p.isExpired()) { %>
        <fieldset class="medicine-lines">
            <legend>Medicines to approve</legend>
            <p class="meta">Write each medicine on the prescription, how many packs, and how the customer
                should use it. Prices are taken from the catalog.</p>

            <div class="item-rows" data-item-rows>
            <% for (int i = 0; i < rowCount; i++) {
                   String chosen = at(rowMedicineIds, i);
            %>
                <div class="item-row" data-item-row>
                    <div class="field medicine-col">
                        <label>Medicine</label>
                        <select name="medicineId" data-item-medicine>
                            <option value="">-- choose --</option>
                            <% String lastCategory = null;
                               for (Medicine m : medicines) {
                                   if (!m.getCategoryName().equals(lastCategory)) {
                                       if (lastCategory != null) { %></optgroup><% }
                                       lastCategory = m.getCategoryName(); %>
                                       <optgroup label="<%= TextUtil.html(lastCategory) %>">
                            <%     } %>
                                <option value="<%= m.getId() %>" data-price="<%= m.getPrice() %>"
                                        data-stock="<%= m.getStockQuantity() %>"
                                        <%= m.isOutOfStock() ? "disabled" : "" %>
                                        <%= String.valueOf(m.getId()).equals(chosen) ? "selected" : "" %>>
                                    <%= TextUtil.html(m.getDisplayName()) %><%= m.isRequiresPrescription() ? " (Rx)" : "" %>
                                    - <%= TextUtil.money(m.getPrice()) %><%= m.isOutOfStock() ? " - out of stock" : "" %>
                                </option>
                            <% }
                               if (lastCategory != null) { %></optgroup><% } %>
                        </select>
                    </div>
                    <div class="field qty-col">
                        <label>Qty</label>
                        <input type="number" name="quantity" min="1" max="<%= PrescriptionService.ITEM_QUANTITY_MAX %>"
                               value="<%= TextUtil.html(at(rowQuantities, i).isEmpty() ? "1" : at(rowQuantities, i)) %>"
                               data-item-qty>
                    </div>
                    <div class="field dosage-col">
                        <label>How to use</label>
                        <input type="text" name="dosage" maxlength="<%= PrescriptionService.DOSAGE_MAX %>"
                               list="dosage-suggestions" value="<%= TextUtil.html(at(rowDosages, i)) %>"
                               placeholder="e.g. 1 tablet twice daily after meals" data-item-dosage>
                    </div>
                    <div class="line-col">
                        <span class="line-price" data-item-total>-</span>
                        <button type="button" class="remove-line" data-item-remove aria-label="Remove this line">&times;</button>
                    </div>
                    <p class="field-error hidden" data-item-error></p>
                </div>
            <% } %>
            </div>

            <datalist id="dosage-suggestions">
                <option value="1 tablet once daily">
                <option value="1 tablet twice daily after meals">
                <option value="1 tablet three times daily after meals">
                <option value="1 capsule three times daily for 7 days">
                <option value="1 capsule twice daily for 5 days">
                <option value="10 ml three times daily">
                <option value="1-2 tablets when needed, at most 8 in 24 hours">
                <option value="Apply a thin layer twice daily">
            </datalist>

            <div class="lines-footer">
                <button type="button" class="btn plain small" data-item-add>+ Add another medicine</button>
                <span class="lines-total">Total: <strong data-items-total>Rs. 0.00</strong></span>
            </div>
        </fieldset>

        <fieldset class="checklist">
            <legend>Before approving, confirm:</legend>
            <label><input type="checkbox" data-check> Doctor's name, registration and signature are present</label>
            <label><input type="checkbox" data-check> Patient name matches <%= TextUtil.html(p.getCustomerName()) %></label>
            <label><input type="checkbox" data-check> Every medicine above is on the prescription</label>
            <label><input type="checkbox" data-check> Quantities and "how to use" match the doctor's dosage</label>
            <label><input type="checkbox" data-check> Issued within the last <%= Prescription.EXPIRY_DAYS %> days</label>
        </fieldset>
        <% } %>

        <div class="field">
            <label for="note">Note for the customer</label>
            <textarea id="note" name="note" rows="3" maxlength="500" data-counter="note-count"
                      placeholder="Required when rejecting or asking for a correction. Optional when approving."><%= TextUtil.html(note) %></textarea>
            <div class="hint" id="note-count"></div>
            <p class="field-error hidden" data-note-error role="alert"></p>
        </div>

        <div class="decision-buttons">
            <button class="btn approve" type="submit" name="decision" value="APPROVE"
                    data-approve <%= p.isExpired() ? "disabled" : "" %>>Approve medicines</button>
            <button class="btn correction" type="submit" name="decision" value="CORRECTION">Request correction</button>
            <button class="btn reject" type="submit" name="decision" value="REJECT">Reject</button>
        </div>
        <p class="meta" data-approve-hint <%= p.isExpired() ? "hidden" : "" %>>To approve: add at least one
            medicine and tick all five checks.</p>
    </form>
</section>
<% } %>

<script src="<%= ctx %>/js/prescription.js" defer></script>
<%@ include file="../common/footer.jspf" %>
