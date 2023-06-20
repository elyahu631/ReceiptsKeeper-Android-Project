package com.example.receiptskeeper.classes;


public class Customer {
    String name;
    String hour;
    String imageLink ;

    int day;
    int month ;
    int year;
    Boolean havingReceipt;

    public Customer() {}

    public void setName(String name) {
        this.name = name;
    }

    public void setHour(String hour) {
        this.hour = hour;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public void setImageLink(String imageLink) {
        this.imageLink = imageLink;
    }


    public String getName() {
        return name;
    }

    public String getHour() {
        return hour;
    }

    public String getQid() {
        return   hour.replace(":","") + name;
    }

    public int getDay() {
        return day;
    }
    public int getMonth() {
        return month;
    }
    public int getYear() {
        return year;
    }

    public Customer(String name, String hour, int day, int month, int year) {
        this.name = name;
        this.hour = hour;
        this.day = day;
        this.month = month;
        this.year = year;
    }

    public Customer(String name, String hour, int day, int month, int year,Boolean havingReceipt) {
        this.name = name;
        this.hour = hour;
        this.day = day;
        this.month = month;
        this.year = year;
        this.havingReceipt=havingReceipt;
    }

}
