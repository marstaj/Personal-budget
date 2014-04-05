package cz.avast.personalbudget;

import android.content.Context;
import android.preference.PreferenceManager;

/**
 * Helper class for manipulation data into adam
 */
public class PrefManager {

    /**
     * Constant for saving server timestamp
     */
    private static final String SERVER_TIMESTAMP = "SERVER_TIMESTAMP";

    /**
     * Loads server timestamp from shared preferences
     *
     * @param context Context
     * @return Server timestamp
     */
    public static long getServerTimestamp(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getLong(SERVER_TIMESTAMP, 0);
    }

    /**
     * Saves server timestamp into the shared preferences
     *
     * @param context   Context
     * @param timestamp Timestamp
     * @return Whether the save was successful
     */
    public static boolean setServerTimestamp(Context context, long timestamp) {
        return PreferenceManager.getDefaultSharedPreferences(context).edit().putLong(SERVER_TIMESTAMP, timestamp).commit();
    }

}