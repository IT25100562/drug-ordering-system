package com.medisys.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

/**
 * A person who can log in: customer, pharmacist, admin or delivery staff.
 * One row of the users table.
 *
 * It is kept in the HTTP session after login, so it is Serializable and does
 * NOT hold the password hash.
 *
 * Module : 04 - User and Role Management
 * Owner  : Kaweesha P. M. G. S.
 */
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private String fullName;
    private String email;
    private String phone;
    private String whatsapp;
    private String nic;
    private LocalDate dateOfBirth;
    private String address;
    private String photoKey;          // null = no profile photo
    private Role role;
    private boolean active;
    private LocalDateTime createdAt;

    // Red flag set by a pharmacist (never shown to the customer).
    private boolean flagged;
    private String flagReason;
    private String flaggedByName;
    private LocalDateTime flaggedAt;

    public boolean hasPhoto() {
        return photoKey != null;
    }

    /** "NP" for Nimal Perera - shown when there is no photo. */
    public String getInitials() {
        if (fullName == null || fullName.isBlank()) {
            return "?";
        }
        String[] words = fullName.trim().replaceAll("(?i)^(dr|mr|mrs|ms|miss|prof|rev)\\.?\\s+", "").split("\\s+");
        String initials = words[0].substring(0, 1);
        if (words.length > 1) {
            initials += words[words.length - 1].substring(0, 1);
        }
        return initials.toUpperCase();
    }

    /** Age in full years, or null when the date of birth is not known. */
    public Integer getAge() {
        return dateOfBirth == null ? null : Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    /** The WhatsApp number as wa.me needs it: 0771234567 -> 94771234567. */
    public String getWhatsappLinkNumber() {
        if (whatsapp == null) {
            return null;
        }
        String digits = whatsapp.replaceAll("\\D", "");
        return digits.startsWith("0") ? "94" + digits.substring(1) : digits;
    }

    public boolean isCustomer() {
        return role == Role.CUSTOMER;
    }

    public boolean isPharmacist() {
        return role == Role.PHARMACIST;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public boolean isDeliveryStaff() {
        return role == Role.DELIVERY_STAFF;
    }

    /**
     * First word of the name, for "Hi, Nimal". A title is kept together with
     * the next word, so "Dr. Sunil Fernando" gives "Dr. Sunil".
     */
    public String getFirstName() {
        if (fullName == null || fullName.isBlank()) {
            return "";
        }
        String[] words = fullName.trim().split("\\s+");
        boolean isTitle = words[0].matches("(?i)(dr|mr|mrs|ms|miss|prof|rev)\\.?");
        return isTitle && words.length > 1 ? words[0] + " " + words[1] : words[0];
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getWhatsapp() {
        return whatsapp;
    }

    public void setWhatsapp(String whatsapp) {
        this.whatsapp = whatsapp;
    }

    public String getNic() {
        return nic;
    }

    public void setNic(String nic) {
        this.nic = nic;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getPhotoKey() {
        return photoKey;
    }

    public void setPhotoKey(String photoKey) {
        this.photoKey = photoKey;
    }

    public boolean isFlagged() {
        return flagged;
    }

    public void setFlagged(boolean flagged) {
        this.flagged = flagged;
    }

    public String getFlagReason() {
        return flagReason;
    }

    public void setFlagReason(String flagReason) {
        this.flagReason = flagReason;
    }

    public String getFlaggedByName() {
        return flaggedByName;
    }

    public void setFlaggedByName(String flaggedByName) {
        this.flaggedByName = flaggedByName;
    }

    public LocalDateTime getFlaggedAt() {
        return flaggedAt;
    }

    public void setFlaggedAt(LocalDateTime flaggedAt) {
        this.flaggedAt = flaggedAt;
    }
}
