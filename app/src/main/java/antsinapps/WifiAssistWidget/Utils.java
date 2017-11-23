package antsinapps.WifiAssistWidget;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Write SharedPref data
 * Created by Brad on 11/17/2017.
 */

class Utils {
    static void writeSessionData(Context c, String key, String value){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(key, value);
        editor.apply();
    }

    static String readSessionData(Context c, String key){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        return sp.getString(key, "DNE");
    }
}
