package com.example.receiptskeeper.classes;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SharedViewModel extends ViewModel {

    private final MutableLiveData<Business> business = new MutableLiveData<>();

    public void setBusiness(Business value) {
        business.setValue(value);
    }

    public MutableLiveData<Business> getBusiness() {
        return business;
    }
}

