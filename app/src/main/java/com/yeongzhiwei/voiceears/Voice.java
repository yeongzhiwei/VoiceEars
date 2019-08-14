package com.yeongzhiwei.voiceears;

class Voice {
    public enum Gender {
        Male, Female
    }

    final String lang;
    final String voiceName;
    final Gender gender;

    Voice() {
        this("en-US", "Microsoft Server Speech Text to Speech Voice (en-US, GuyNeural)", Gender.Male);
    }

    Voice(String lang, String voiceName, Gender gender) {
        this.lang = lang;
        this.voiceName = voiceName;
        this.gender = gender;
    }
}
