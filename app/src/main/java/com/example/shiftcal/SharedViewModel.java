package com.example.shiftcal;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SharedViewModel extends ViewModel {
    private final MutableLiveData<Boolean> patternUpdated = new MutableLiveData<>();
    private final MutableLiveData<Boolean> alarmUpdated = new MutableLiveData<>();

    public void notifyPatternUpdated() {
        patternUpdated.setValue(true);
    }

    public LiveData<Boolean> getPatternUpdated() {
        return patternUpdated;
    }

    public void notifyAlarmUpdated() {
        alarmUpdated.setValue(true);
    }

    public LiveData<Boolean> getAlarmUpdated() {
        return alarmUpdated;
    }
}