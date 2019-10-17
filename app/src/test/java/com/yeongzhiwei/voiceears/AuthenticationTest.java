package com.yeongzhiwei.voiceears;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(JUnit4.class)
public class AuthenticationTest {
    @Test
    public void authenticate1() {
        Boolean result = Authentication.authenticate("", "");
        Boolean expectedResult = false;
        assertThat(result, is(equalTo(expectedResult)));
    }

    @Test
    public void authenticate2() {
        Boolean result = Authentication.authenticate("", "someregion");
        Boolean expectedResult = false;
        assertThat(result, is(equalTo(expectedResult)));
    }

    @Test
    public void authenticate3() {
        Boolean result = Authentication.authenticate("somekey", "");
        Boolean expectedResult = false;
        assertThat(result, is(equalTo(expectedResult)));
    }

    @Test
    public void authenticate4() {
        Boolean result = Authentication.authenticate("somekey", "someregion");
        Boolean expectedResult = false;
        assertThat(result, is(equalTo(expectedResult)));
    }

    @Test
    public void authenticate5() {
        Boolean result = Authentication.authenticate("91d9894e45f84892bd351cfe30ebf610", "southeastasia");
        Boolean expectedResult = true;
        assertThat(result, is(equalTo(expectedResult)));
    }
}
