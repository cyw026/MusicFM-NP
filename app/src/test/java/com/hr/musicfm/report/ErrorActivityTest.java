package com.hr.musicfm.report;

import android.app.Activity;

import com.hr.musicfm.MainActivity;
import com.hr.musicfm.RouterActivity;
import com.hr.musicfm.fragments.detail.VideoDetailFragment;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Unit tests for {@link ErrorActivity}
 */
public class ErrorActivityTest {
    @Test
    public void getReturnActivity() throws Exception {
        Class<? extends Activity> returnActivity;
        returnActivity = ErrorActivity.getReturnActivity(MainActivity.class);
        assertEquals(MainActivity.class, returnActivity);

        returnActivity = ErrorActivity.getReturnActivity(RouterActivity.class);
        assertEquals(RouterActivity.class, returnActivity);

        returnActivity = ErrorActivity.getReturnActivity(null);
        assertNull(returnActivity);

        returnActivity = ErrorActivity.getReturnActivity(Integer.class);
        assertEquals(MainActivity.class, returnActivity);

        returnActivity = ErrorActivity.getReturnActivity(VideoDetailFragment.class);
        assertEquals(MainActivity.class, returnActivity);
    }



}