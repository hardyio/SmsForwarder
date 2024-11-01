package com.idormy.sms.forwarder.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.idormy.sms.forwarder.App;

public class SPUtil {

    public static String PREFERENCE_NAME = "user_pref";

    private SPUtil() {
        throw new UnsupportedOperationException("cannot be instantiated");
    }

    public static void write(String key, Object value) {
        SharedPreferences sp = App.Companion.getContext().getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        if (value instanceof String) {
            editor.putString(key, (String) value);
        } else if (value instanceof Integer) {
            editor.putInt(key, (Integer) value);
        } else if (value instanceof Float) {
            editor.putFloat(key, (Float) value);
        } else if (value instanceof Long) {
            editor.putLong(key, (Long) value);
        } else if (value instanceof Boolean) {
            editor.putBoolean(key, (Boolean) value);
        }
        editor.apply();
    }

    public static String readString(String key) {
        return read(key, "");
    }

    public static String read(String key, String defValue) {
        SharedPreferences sp = App.Companion.getContext().getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        return sp.getString(key, defValue);
    }

    public static int readInt(String key) {
        return read(key, 0);
    }

    public static int read(String key, int defValue) {
        SharedPreferences sp = App.Companion.getContext().getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        return sp.getInt(key, defValue);
    }

    public static long readLong(String key) {
        return read(key, 0L);
    }

    public static long read(String key, long defValue) {
        SharedPreferences sp = App.Companion.getContext().getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        return sp.getLong(key, defValue);
    }

    public static boolean readBoolean(String key) {
        return read(key, false);
    }

    public static boolean read(String key, boolean defValue) {
        SharedPreferences sp = App.Companion.getContext().getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        return sp.getBoolean(key, defValue);
    }

}
