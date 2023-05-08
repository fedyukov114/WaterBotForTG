package me.tgbot;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;

public class User {
    private int sumOfMg;
    private Long chatId;
    private ArrayList<Integer> mgOfWaterInDay;
    private ArrayList<String> completedDays;

    @JsonCreator
    public User(@JsonProperty("chatId") Long chatId) {
        this.chatId = chatId;
    }

    public Long getChatId() {
        return chatId;
    }

    public ArrayList<Integer> getMgOfWaterInDay() {
        return mgOfWaterInDay;
    }

    public void setMgOfWaterInDay(ArrayList<Integer> mgOfWaterInDay) {
        this.mgOfWaterInDay = mgOfWaterInDay;
    }

    public ArrayList<String> getCompletedDays() {
        return completedDays;
    }

    public void setCompletedDays(ArrayList<String> completedDays) {
        this.completedDays = completedDays;
    }

    public int getSumOfMg() {
        return sumOfMg;
    }

    public void setSumOfMg(int sumOfMg) {
        this.sumOfMg = sumOfMg;
    }
}
