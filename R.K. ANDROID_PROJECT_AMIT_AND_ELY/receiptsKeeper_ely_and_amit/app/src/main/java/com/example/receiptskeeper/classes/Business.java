package com.example.receiptskeeper.classes;

import java.io.Serializable;

public class Business implements Serializable {
    String ownerName;
    String businessName;
    String imageUrl;

    public Business(){};

    public Business(String ownerName, String businessName, String imageUrl) {
        this.ownerName = ownerName;
        this.businessName = businessName;
        this.imageUrl = imageUrl;
    }

    public Business(String ownerName, String businessName) {
        this.ownerName = ownerName;
        this.businessName = businessName;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
