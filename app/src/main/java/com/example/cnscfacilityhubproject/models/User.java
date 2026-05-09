package com.example.cnscfacilityhubproject.models;

public class User {
    private String userID;
    private String fullName;
    private String number;
    private String dept;
    private String course;
    private String email;
    private String type;


    public User(String id, String fName, String cNum, String dept, String course, String email, String userType){
        this.userID = id;
        this.fullName = fName;
        this.number = cNum;
        this.dept = dept;
        this.course = course;
        this.email = email;
        this.type = userType;
    }


    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getDept() {
        return dept;
    }

    public void setDept(String dept) {
        this.dept = dept;
    }

    public String getCourse() {
        return course;
    }

    public void setCourse(String course) {
        this.course = course;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
