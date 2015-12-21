package it.jaschke.alexandria;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

public class Utility
{
    /**
     * Check if the network is available
     * @return boolean
     */
    public static boolean isNetworkAvailable(Context context) {

        // get the connectivity manager service
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        // get info about the network
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();

        // return true if we have networkinfo and are connected
        if (activeNetworkInfo != null && activeNetworkInfo.isConnected()) {
            return true;
        }  else {
            return false;
        }
    }

    /**
     * Hide the softkeyboard from an activity
     * @param activity Activity
     */
    public static void hideKeyboardFromActivity(Activity activity) {

        // get the inputmanager from the activity
        InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);

        // find the currently focused view, so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();

        // if no view currently has focus, create a new one, just so we can grab a window token from it
        if(view == null) {
            view = new View(activity);
        }

        // and hide the keyboard
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

}