package com.yeongzhiwei.voiceears;

class Voice {
    enum Gender {
        Male, Female
    }

    final String lang;
    String voiceName;
    Gender gender;

    Gender getGender() {
        return this.gender;
    }

    Voice() {
        this("en-US", "en-US-GuyNeural", Gender.Male);
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
}
