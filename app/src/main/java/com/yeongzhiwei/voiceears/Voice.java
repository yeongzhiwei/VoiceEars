package com.yeongzhiwei.voiceears;

class Voice {
    enum Gender {
        Male, Female
    }

    final String lang;
    final String voiceName;
    Gender gender;

    Gender getGender() {
        return this.gender;
    }

    Voice() {
        this("en-US", "Microsoft Server Speech Text to Speech Voice (en-US, GuyNeural)", Gender.Male);
    }

    Voice(String lang, String voiceName, Gender gender) {
        this.lang = lang;
        this.voiceName = voiceName;
        this.gender = gender;
    }

    Gender toggleVoice() {
        if (this.gender == Gender.Male) {
            this.gender = Gender.Female;
        } else {
            this.gender = Gender.Male;
        }
        return this.gender;
    }
}
