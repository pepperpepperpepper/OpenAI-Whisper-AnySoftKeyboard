/*
 * Copyright (c) 2025 AnySoftKeyboard
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.anysoftkeyboard.addons;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.anysoftkeyboard.api.AddOnUICardAPI;
import com.anysoftkeyboard.ui.settings.AddOnUICard;
import com.anysoftkeyboard.ui.settings.AddOnUICardManager;

/**
 * Publisher class that enables add-ons to publish their own UI cards to AnySoftKeyboard's
 * main settings screen. This class provides a high-level API for add-ons to manage
 * their UI cards without dealing with broadcast intents directly.
 * 
 * <p>Add-ons can use this class to:
 * <ul>
 *   <li>Publish a UI card that appears in the main settings screen</li>
 *   <li>Update an existing card's content</li>
 *   <li>Unpublish a card when it's no longer needed</li>
 *   <li>Check if a card is currently published</li>
 * </ul>
 * 
 * <p>Usage example:
 * <pre>{@code
 * AddOnUICardPublisher publisher = new AddOnUICardPublisher(context, addOn);
 * publisher.publishUICard("New Theme Available", "Check out our awesome new theme!");
 * }</pre>
 */
public class AddOnUICardPublisher {
    private static final String TAG = "AddOnUICardPublisher";
    
    private final Context mContext;
    private final String mPackageName;
    private final AddOnUICardManager mCardManager;
    
    /**
     * Creates a new AddOnUICardPublisher for the specified add-on.
     * 
     * @param context The application context (usually AnySoftKeyboard's context)
     * @param addOn The add-on instance that wants to publish UI cards
     */
    public AddOnUICardPublisher(@NonNull Context context, @NonNull AddOn addOn) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        if (addOn == null) {
            throw new IllegalArgumentException("AddOn cannot be null");
        }
        
        mContext = context.getApplicationContext();
        mPackageName = addOn.getPackageName();
        mCardManager = new AddOnUICardManager(mContext);
        
        Log.d(TAG, "Created AddOnUICardPublisher for package: " + mPackageName);
    }
    
    /**
     * Publishes a UI card with the specified title, message, and target fragment.
     * The card will appear in AnySoftKeyboard's main settings screen.
     * 
     * @param title The title to display on the UI card
     * @param message The message to display on the UI card
     * @param targetFragment The target fragment to navigate to when the card is clicked
     * @throws IllegalArgumentException if title or message is null or empty
     */
    public void publishUICard(@NonNull String title, @NonNull String message, @Nullable String targetFragment) {
        validateCardContent(title, message);
        
        AddOnUICard card = new AddOnUICard(mPackageName, title, message, targetFragment);
        publishCardInternal(card);
    }
    
    /**
     * Publishes a UI card with the specified title and message. The card will not be
     * clickable since no target fragment is specified.
     * 
     * @param title The title to display on the UI card
     * @param message The message to display on the UI card
     * @throws IllegalArgumentException if title or message is null or empty
     */
    public void publishUICard(@NonNull String title, @NonNull String message) {
        publishUICard(title, message, null);
    }
    
    
    
    /**
     * Updates an existing published card with new content. If no card is currently
     * published for this add-on, this method will publish a new card.
     * 
     * @param title The new title for the card
     * @param message The new message for the card
     * @param targetFragment The new target fragment for the card (can be null)
     * @throws IllegalArgumentException if title or message is null or empty
     */
    public void updateUICard(@NonNull String title, @NonNull String message, @Nullable String targetFragment) {
        validateCardContent(title, message);
        
        // Unpublish existing card first, then publish the new one
        unpublishUICard();
        publishUICard(title, message, targetFragment);
    }
    
    /**
     * Updates an existing published card with new content. If no card is currently
     * published for this add-on, this method will publish a new card. If a card is 
     * already published, its target fragment will be preserved.
     * 
     * @param title The new title for the card
     * @param message The new message for the card
     * @throws IllegalArgumentException if title or message is null or empty
     */
    public void updateUICard(@NonNull String title, @NonNull String message) {
        // Check if there's an existing card to preserve its target fragment
        AddOnUICard existingCard = getPublishedCard();
        String targetFragment = (existingCard != null) ? existingCard.getTargetFragment() : null;
        updateUICard(title, message, targetFragment);
    }
    

    
    
    /**
     * Unpublishes the UI card for this add-on. The card will be removed from the
     * main settings screen. If no card is currently published, this method does nothing.
     */
    public void unpublishUICard() {
        try {
            Intent intent = new Intent(AddOnUICardAPI.ACTION_UNREGISTER_UI_CARD);
            intent.putExtra(AddOnUICardAPI.EXTRA_PACKAGE_NAME, mPackageName);
            mContext.sendBroadcast(intent);
            
            // Also remove from local manager for immediate effect
            mCardManager.unregisterUICard(mPackageName);
            
            Log.d(TAG, "Unpublished UI card for package: " + mPackageName);
        } catch (Exception e) {
            Log.e(TAG, "Failed to unpublish UI card for package: " + mPackageName, e);
        }
    }
    
    /**
     * Checks if this add-on currently has a published UI card.
     * 
     * @return true if a card is currently published, false otherwise
     */
    public boolean isCardPublished() {
        return getPublishedCard() != null;
    }
    
    /**
     * Gets the currently published UI card for this add-on.
     * 
     * @return The published card, or null if no card is currently published
     */
    @Nullable
    public AddOnUICard getPublishedCard() {
        for (AddOnUICard card : mCardManager.getActiveUICards()) {
            if (mPackageName.equals(card.getPackageName())) {
                return card;
            }
        }
        return null;
    }
    
    /**
     * Gets the package name of the add-on this publisher is associated with.
     * 
     * @return The package name
     */
    @NonNull
    public String getPackageName() {
        return mPackageName;
    }
    
    /**
     * Performs cleanup when the add-on is being disabled or removed.
     * This method automatically unpublishes any active UI cards.
     */
    public void cleanup() {
        if (isCardPublished()) {
            unpublishUICard();
        }
        Log.d(TAG, "Cleaned up AddOnUICardPublisher for package: " + mPackageName);
    }
    
    private void publishCardInternal(@NonNull AddOnUICard card) {
        try {
            // Register via broadcast intent
            Intent intent = new Intent(AddOnUICardAPI.ACTION_REGISTER_UI_CARD);
            intent.putExtra(AddOnUICardAPI.EXTRA_PACKAGE_NAME, card.getPackageName());
            intent.putExtra(AddOnUICardAPI.EXTRA_TITLE, card.getTitle());
            intent.putExtra(AddOnUICardAPI.EXTRA_MESSAGE, card.getMessage());
            if (card.getTargetFragment() != null) {
                intent.putExtra(AddOnUICardAPI.EXTRA_TARGET_FRAGMENT, card.getTargetFragment());
            }
            
            mContext.sendBroadcast(intent);
            
            // Also register directly with manager for immediate effect
            mCardManager.registerUICard(card);
            
            Log.d(TAG, "Published UI card for package: " + mPackageName + 
                      " with title: " + card.getTitle());
        } catch (Exception e) {
            Log.e(TAG, "Failed to publish UI card for package: " + mPackageName, e);
        }
    }
    
    protected void validateCardContent(@NonNull String title, @NonNull String message) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title cannot be null or empty");
        }
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Message cannot be null or empty");
        }
        if (title.length() > 100) {
            throw new IllegalArgumentException("Title cannot exceed 100 characters");
        }
        if (message.length() > 500) {
            throw new IllegalArgumentException("Message cannot exceed 500 characters");
        }
    }
    
    
    
    /**
     * Creates a new AddOnUICardPublisher instance for testing purposes.
     * This method allows injection of a mock AddOnUICardManager.
     * 
     * @param context The application context
     * @param addOn The add-on instance
     * @param cardManager The card manager to use (for testing)
     * @return A new AddOnUICardPublisher instance
     */
    @VisibleForTesting
    static AddOnUICardPublisher createForTesting(@NonNull Context context, 
                                                 @NonNull AddOn addOn,
                                                 @NonNull AddOnUICardManager cardManager) {
        return new AddOnUICardPublisher(context, addOn) {
            private final AddOnUICardManager mTestCardManager = cardManager;
            
            @Override
            public boolean isCardPublished() {
                for (AddOnUICard card : mTestCardManager.getActiveUICards()) {
                    if (addOn.getPackageName().equals(card.getPackageName())) {
                        return true;
                    }
                }
                return false;
            }
            
            @Override
            public AddOnUICard getPublishedCard() {
                for (AddOnUICard card : mTestCardManager.getActiveUICards()) {
                    if (addOn.getPackageName().equals(card.getPackageName())) {
                        return card;
                    }
                }
                return null;
            }
            
            @Override
            public void unpublishUICard() {
                try {
                    mTestCardManager.unregisterUICard(addOn.getPackageName());
                } catch (Exception e) {
                    // Log but don't re-throw in test version
                }
            }
            
            @Override
            public void publishUICard(String title, String message, String targetFragment) {
                validateCardContent(title, message);
                AddOnUICard card = new AddOnUICard(addOn.getPackageName(), title, message, targetFragment);
                try {
                    mTestCardManager.registerUICard(card);
                } catch (Exception e) {
                    // Log but don't re-throw in test version
                }
            }
            
            @Override
            public void updateUICard(String title, String message, String targetFragment) {
                validateCardContent(title, message);
                try {
                    mTestCardManager.unregisterUICard(addOn.getPackageName());
                    AddOnUICard card = new AddOnUICard(addOn.getPackageName(), title, message, targetFragment);
                    mTestCardManager.registerUICard(card);
                } catch (Exception e) {
                    // Log but don't re-throw in test version
                }
            }
            
            @Override
            public void updateUICard(String title, String message) {
                // Check if there's an existing card to preserve its target fragment
                AddOnUICard existingCard = getPublishedCard();
                String targetFragment = (existingCard != null) ? existingCard.getTargetFragment() : null;
                updateUICard(title, message, targetFragment);
            }
        };
    }
}