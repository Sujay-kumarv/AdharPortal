package com.adhar.newapp.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "users")
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Personal Details
    private String fullName;
    private String dob;
    private String gender;
    private String mobile;
    private String email;

    // Address Details
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String pincode;

    // Documents (Paths)
    private String photoPath;
    private String idProofPath;
    private String signaturePath;

    // Status
    private String status; // PENDING, APPROVED, REJECTED

    // Authentication
    private String password;
    private String roles; // Comma separated: ROLE_USER,ROLE_ADMIN
    private boolean mfaEnabled;
    private String mfaSecret;

    // New Aadhaar Details
    private String aadhaarNumber;
    private String aadhaarPdfPath;

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = "PENDING";
        }
        if (roles == null) {
            roles = "ROLE_USER";
        }
    }
}
