package com.android.car.media.testmediaapp;

import static junit.framework.Assert.assertEquals;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class TmaLibraryTests {

    @Test
    public void testGetParentPath() {
        String [] inputs = {"", "r", "r#", "r#n", "r#n#", "_ROOT_#advanced#single style node",
                "_ROOT_#advanced#single style node#"};
        String [] expected = {"", "", "", "r#", "r#", "_ROOT_#advanced#", "_ROOT_#advanced#"};
        for (int i = 0; i < inputs.length; i++) {
            assertEquals(expected[i], TmaLibrary.staticGetParentPath(inputs[i]));
        }
    }
}