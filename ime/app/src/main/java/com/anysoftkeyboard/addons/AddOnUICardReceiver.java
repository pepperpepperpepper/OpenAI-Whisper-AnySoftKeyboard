package com.anysoftkeyboard.addons;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.anysoftkeyboard.ui.settings.AddOnUICard;
import com.anysoftkeyboard.ui.settings.AddOnUICardManager;

/**
 * Broadcast receiver that listens for UI card updates from add-ons
 */
public class AddOnUICardReceiver extends BroadcastReceiver {
    
    private static final String TAG = "AddOnUICardReceiver";
    private static final String ACTION_UI_CARD_UPDATE = "com.anysoftkeyboard.UI_CARD_UPDATE";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive() called with action: " + intent.getAction());
        if (ACTION_UI_CARD_UPDATE.equals(intent.getAction())) {
            handleUICardUpdate(context, intent);
        } else {
            Log.d(TAG, "Ignoring unknown action: " + intent.getAction());
        }
    }
    
    private void handleUICardUpdate(Context context, Intent intent) {
        String addonPackage = intent.getStringExtra("addon_package");
        String action = intent.getStringExtra("action");
        String title = intent.getStringExtra("title");
        String message = intent.getStringExtra("message");
        String priority = intent.getStringExtra("priority");
        
        Log.d(TAG, "Received UI card update from " + addonPackage + 
                   " - Action: " + action + ", Title: " + title + ", Message: " + message);
        
        if (addonPackage == null || action == null) {
            Log.w(TAG, "Invalid UI card update: missing required fields");
            return;
        }
        
        AddOnUICardManager manager = new AddOnUICardManager(context);
        
        if ("show_card".equals(action)) {
            if (title != null && message != null) {
                AddOnUICard card = new AddOnUICard(addonPackage, title, message, null);
                manager.registerUICard(card);
                Log.d(TAG, "UI card registered for " + addonPackage);
            }
        } else if ("hide_card".equals(action)) {
            manager.unregisterUICard(addonPackage);
            Log.d(TAG, "UI card unregistered for " + addonPackage);
        }
    }
}