package com.yeongzhiwei.voiceears;

class Voice {
    enum Gender {
        Male, Female
    }

    private final String lang;
    private String voiceName;
    private Gender gender;

    String getLang() {
        return this.lang;
    }

    String getVoiceName() {
        return this.voiceName;
    }

    Gender getGender() {
        return this.gender;
    }

    Voice(String lang, String voiceName, Gender gender) {
        this.lang = lang;
        this.voiceName = voiceName;
        this.gender = gender;
    }

    Gender toggleVoice() {
        if (this.gender == Gender.Male) {
            this.voiceName = "en-US-JessaNeural";
            this.gender = Gender.Female;
        } else {
            this.voiceName = "en-US-GuyNeural";
            this.gender = Gender.Male;
        }
        return this.gender;
    }

    static Voice getDefaultVoice(Gender gender) {
        if (gender == Gender.Male) {
            return new Voice("en-US", "en-US-GuyNeural", Gender.Male);
        } else {
            return new Voice("en-US", "en-US-JessaNeural", Gender.Female);
        }
    }
}
