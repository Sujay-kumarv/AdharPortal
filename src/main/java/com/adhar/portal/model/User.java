package com.adhar.portal.model;

public class User {
    private String username;
    private String password;
    private String fullName;
    private String aadharNumber;
    private String dob;
    private String address;

    public User(String username, String password, String fullName, String aadharNumber, String dob, String address) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.aadharNumber = aadharNumber;
        this.dob = dob;
        this.address = address;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getFullName() {
        return fullName;
    }

    public String getAadharNumber() {
        return aadharNumber;
    }

    public String getDob() {
        return dob;
    }

    public String getAddress() {
        return address;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
