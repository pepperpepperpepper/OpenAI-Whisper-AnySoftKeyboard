package com.anysoftkeyboard.demo;

import android.content.Context;
import android.content.Intent;

import com.anysoftkeyboard.addons.AddOnImpl;
import com.anysoftkeyboard.addons.AddOnUICardPublisher;
import com.anysoftkeyboard.dictionaries.Dictionary;
import com.anysoftkeyboard.dictionaries.DictionaryBase;


/**
 * Demo add-on that demonstrates UI card functionality.
 * This is a simple dictionary add-on that also publishes UI cards.
 */
public class DemoUIAddOn extends AddOnImpl {
    
    private AddOnUICardPublisher mUICardPublisher;

    public DemoUIAddOn(Context context, String id, int nameResId, int descriptionResId, int iconResId) {
        super(context, context, 1, id, 
              context.getResources().getString(nameResId),
              context.getResources().getString(descriptionResId),
              false, 0);
    }

    /**
     * Initializes the UI card publisher and publishes the demo card.
     * This should be called when the add-on is loaded.
     */
    public void initializeAndPublishCard() {
        try {
            mUICardPublisher = new AddOnUICardPublisher(getContext(), this);
            publishDemoCard();
        } catch (Exception e) {
            // Log error but don't fail the add-on
            android.util.Log.e("DemoUIAddOn", "Failed to initialize UI card publisher", e);
        }
    }

    /**
     * Cleans up the UI card publisher.
     * This should be called when the add-on is unloaded.
     */
    public void cleanup() {
        if (mUICardPublisher != null) {
            mUICardPublisher.unpublishUICard();
            mUICardPublisher.cleanup();
            mUICardPublisher = null;
        }
    }

    /**
     * Publishes a demo UI card to demonstrate the functionality
     */
    private void publishDemoCard() {
        if (mUICardPublisher == null) return;
        
        // Create a message with a clickable link to app settings
        String message = "This card demonstrates UI card functionality. " +
                        "Click <a href=\"settings://app\">here</a> to open " +
                        "AnySoftKeyboard's app settings and manage permissions.";
        
        mUICardPublisher.publishUICard(
            "Demo Add-on Active",
            message,
            null // Using inline link instead of fragment navigation
        );
    }

    @Override
    public Dictionary createDictionary() {
        // Create a simple demo dictionary
        return new DemoDictionary(getContext());
    }

    /**
     * Simple demo dictionary implementation
     */
    private static class DemoDictionary extends DictionaryBase {
        
        public DemoDictionary(Context context) {
            super(context);
        }

        @Override
        public void loadDictionary() {
            // Load some demo words
            String[] demoWords = {"demo", "test", "addon", "card", "ui", "functionality"};
            for (String word : demoWords) {
                addWord(word, 100);
            }
        }

        @Override
        protected void closeAllResources() {
            // No resources to close
        }
    }
}