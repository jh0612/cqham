package com.reiwaxr.cq.cqham.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MorseItem {
    private String charKey;
    private String morseCode;

    public MorseItem(String charKey, String morseCode) {
        this.charKey = charKey;
        this.morseCode = morseCode;
    }

    public String getCharKey() { return charKey; }
    public void setCharKey(String charKey) { this.charKey = charKey; }
    public String getMorseCode() { return morseCode; }
    public void setMorseCode(String morseCode) { this.morseCode = morseCode; }
}