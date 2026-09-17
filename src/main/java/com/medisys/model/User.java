package com.medisys.model;

import java.io.Serializable;
import java.time.LocalDateTime;

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
    private String address;
    private Role role;
    private boolean active;
    private LocalDateTime createdAt;

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
}
