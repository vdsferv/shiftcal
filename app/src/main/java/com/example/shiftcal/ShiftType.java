package com.example.shiftcal;

import com.google.gson.annotations.SerializedName;

public class ShiftType {
    @SerializedName("name")
    private String name;

    @SerializedName("color")
    private int color;

    public ShiftType(String name, int color) {
        this.name = name;
        this.color = color;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }
}