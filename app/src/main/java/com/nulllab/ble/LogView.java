package com.nulllab.ble;

import android.content.Context;
import android.text.method.ScrollingMovementMethod;
import android.util.AttributeSet;
import android.util.Log;

import androidx.annotation.Nullable;

import com.nulllab.util.MainThreadUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LogView extends androidx.appcompat.widget.AppCompatTextView {
    private static final String TAG = "LogView";

    private final SimpleDateFormat sSimpleDateFormat = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault());
    private final StringBuffer mStringBuffer = new StringBuffer();

    public LogView(Context context) {
        super(context);
        setMovementMethod(ScrollingMovementMethod.getInstance());
    }

    public LogView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setMovementMethod(ScrollingMovementMethod.getInstance());
    }

    public LogView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setMovementMethod(ScrollingMovementMethod.getInstance());
    }

    public void print(String text) {
        mStringBuffer.append(sSimpleDateFormat.format(new Date()));
        mStringBuffer.append(' ');
        mStringBuffer.append(text);
        mStringBuffer.append('\n');
        MainThreadUtils.run(() -> {
            setText(mStringBuffer);
            int offset = getLineCount() * getLineHeight();
            if (offset > getHeight()) {
                scrollTo(0, offset - getHeight());
            }
//            scrollTo(0, );
            Log.d(TAG, "print: height:" + getHeight() + ", x:" + getX() + ", y: " + getY() + ", offset: " + offset);
        });
    }
}
