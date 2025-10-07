package com.anysoftkeyboard.demo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Simple demo pack broadcast receiver for testing
 */
public class DemoPackBroadcastReceiver extends BroadcastReceiver {
    
    private static final String TAG = "DemoPackBroadcastReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "Received broadcast: " + action);
        
        if ("com.anysoftkeyboard.languagepack.demo_ui_card_addon.DEMO_UI_CARD_UPDATE".equals(action)) {
            handleUICardUpdate(context, intent);
        } else if ("com.menny.android.anysoftkeyboard.DICTIONARY".equals(action)) {
            Log.d(TAG, "Dictionary add-on initialized");
        }
    }
    
    private void handleUICardUpdate(Context context, Intent intent) {
        String cardAction = intent.getStringExtra("action");
        String title = intent.getStringExtra("title");
        String message = intent.getStringExtra("message");
        String priority = intent.getStringExtra("priority");
        
        // For testing, if message is null, use a default
        if (message == null) {
            message = "This is a demo UI card from the demo add-on!";
        }
        if (priority == null) {
            priority = "high";
        }
        
        Log.d(TAG, "UI Card update - Action: " + cardAction + ", Title: " + title + ", Message: " + message);
        
        // Create UI card update intent for AnySoftKeyboard
        Intent uiCardIntent = new Intent("com.anysoftkeyboard.UI_CARD_UPDATE");
        uiCardIntent.putExtra("addon_package", context.getPackageName());
        uiCardIntent.putExtra("action", cardAction);
        uiCardIntent.putExtra("title", title);
        uiCardIntent.putExtra("message", message);
        uiCardIntent.putExtra("priority", priority);
        
        // Set explicit component to ensure delivery
        uiCardIntent.setClassName("com.menny.android.anysoftkeyboard", 
                                 "com.anysoftkeyboard.addons.AddOnUICardReceiver");
        
        // Send to AnySoftKeyboard
        context.sendBroadcast(uiCardIntent);
        Log.d(TAG, "UI Card broadcast sent to AnySoftKeyboard");
    }
}