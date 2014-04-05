package cz.avast.personalbudget;

import android.app.Application;

/**
 * Application class runs before any activity
 */
public class BudgetApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Data model. It needs to be ready.
        DataModel.init(getApplicationContext());
    }
}