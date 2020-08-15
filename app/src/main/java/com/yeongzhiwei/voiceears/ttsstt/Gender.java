package com.yeongzhiwei.voiceears.ttsstt;

public enum Gender {
    Male, Female;

    public Gender toggle() {
        return (this.equals(Male)) ? Female : Male;
    }
}
