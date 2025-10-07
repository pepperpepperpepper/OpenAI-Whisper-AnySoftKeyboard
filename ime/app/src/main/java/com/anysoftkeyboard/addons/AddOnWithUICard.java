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
import android.content.res.Configuration;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.ui.settings.AddOnUICard;
import com.anysoftkeyboard.ui.settings.AddOnUICardManager;
import java.util.List;

/**
 * Abstract base class for add-ons that want to publish UI cards to AnySoftKeyboard's
 * main settings screen. This class extends AddOnImpl and provides built-in support
 * for managing UI cards throughout the add-on's lifecycle.
 * 
 * <p>Add-ons extending this class will automatically:
 * <ul>
 *   <li>Create an AddOnUICardPublisher instance for card management</li>
 *   <li>Publish a UI card when the add-on is enabled (if shouldShowUICard() returns true)</li>
 *   <li>Unpublish the UI card when the add-on is disabled or cleaned up</li>
 *   <li>Handle configuration changes that might affect card content</li>
 * </ul>
 * 
 * <p>Subclasses must implement:
 * <ul>
 *   <li>{@link #shouldShowUICard()} - determine if a card should be shown</li>
 *   <li>{@link #createUICard()} - create the card with add-on specific content</li>
 * </ul>
 * 
 * <p>Usage example:
 * <pre>{@code
 * public class MyThemeAddOn extends AddOnWithUICard {
 *     public MyThemeAddOn(Context askContext, Context packageContext, /*...* /) {
 *         super(askContext, packageContext, /*...* /);
 *     }
 *     
 *     @Override
 *     protected boolean shouldShowUICard() {
 *         return true; // Always show card for this theme
 *     }
 *     
 *     @Override
 *     protected AddOnUICard createUICard() {
 *         return new AddOnUICard(
 *             getPackageName(),
 *             "New Theme Available",
 *             "Check out our awesome new theme with custom colors!",
 *             "keyboardThemeSelectorFragment"
 *         );
 *     }
 * }
 * }</pre>
 */
public abstract class AddOnWithUICard extends AddOnImpl {

    private AddOnUICardPublisher mUICardPublisher;
    private boolean mIsCardPublished = false;

    /**
     * Creates a new AddOnWithUICard instance.
     * 
     * @param askContext The AnySoftKeyboard application context
     * @param packageContext The add-on's package context
     * @param apiVersion The API version of the add-on
     * @param id The unique identifier for this add-on
     * @param name The display name of the add-on
     * @param description A description of the add-on
     * @param hidden Whether this add-on should be hidden from UI
     * @param sortIndex The sort index for ordering add-ons
     */
    protected AddOnWithUICard(
            @NonNull Context askContext,
            @NonNull Context packageContext,
            int apiVersion,
            @NonNull CharSequence id,
            @NonNull CharSequence name,
            @NonNull CharSequence description,
            boolean hidden,
            int sortIndex) {
        super(askContext, packageContext, apiVersion, id, name, description, hidden, sortIndex);
        
        // Initialize the UI card publisher
        mUICardPublisher = new AddOnUICardPublisher(askContext, this);
        
        // Auto-publish card if it should be shown
        if (shouldShowUICard()) {
            publishUICard();
        }
    }

    /**
     * Determines whether this add-on should show a UI card.
     * This method is called during construction and when the add-on's state changes.
     * 
     * @return true if a UI card should be shown, false otherwise
     */
    protected abstract boolean shouldShowUICard();

    /**
     * Creates the UI card for this add-on. This method is called when the card
     * needs to be published or updated.
     * 
     * @return The UI card to display, or null if no card should be shown
     */
    @Nullable
    protected abstract AddOnUICard createUICard();

    /**
     * Publishes the UI card for this add-on. If a card is already published,
     * it will be updated with the new content from createUICard().
     * 
     * <p>This method is protected so subclasses can call it to manually
     * trigger card publishing when their state changes.
     */
    protected final void publishUICard() {
        if (!shouldShowUICard()) {
            // Should not show card, unpublish if it's currently published
            if (mIsCardPublished) {
                unpublishUICard();
            }
            return;
        }

        AddOnUICard card = createUICard();
        if (card == null) {
            // No card to publish, unpublish if it's currently published
            if (mIsCardPublished) {
                unpublishUICard();
            }
            return;
        }

        if (mIsCardPublished) {
            // Update existing card
            mUICardPublisher.updateUICard(card.getTitle(), card.getMessage(), card.getTargetFragment());
        } else {
            // Publish new card
            mUICardPublisher.publishUICard(card.getTitle(), card.getMessage(), card.getTargetFragment());
            mIsCardPublished = true;
        }
    }

    /**
     * Unpublishes the UI card for this add-on. This method is called automatically
     * when the add-on is disabled or cleaned up, but can also be called manually
     * by subclasses if needed.
     */
    protected final void unpublishUICard() {
        if (mIsCardPublished) {
            mUICardPublisher.unpublishUICard();
            mIsCardPublished = false;
        }
    }

    /**
     * Checks if this add-on currently has a published UI card.
     * 
     * @return true if a card is currently published, false otherwise
     */
    protected final boolean isCardPublished() {
        return mIsCardPublished && mUICardPublisher.isCardPublished();
    }

    /**
     * Gets the currently published UI card for this add-on.
     * 
     * @return The published card, or null if no card is currently published
     */
    @Nullable
    protected final AddOnUICard getPublishedCard() {
        return mUICardPublisher.getPublishedCard();
    }

    /**
     * Gets the AddOnUICardPublisher instance for advanced card management.
     * This allows subclasses to use the full API of the publisher if needed.
     * 
     * @return The UI card publisher instance
     */
    @NonNull
    protected final AddOnUICardPublisher getUICardPublisher() {
        return mUICardPublisher;
    }

    /**
     * Called when the add-on's configuration changes. This method will automatically
     * update the UI card if it's currently published and shouldShowUICard() returns true.
     * 
     * @param newConfiguration The new configuration
     */
    @Override
    public void setNewConfiguration(@NonNull Configuration newConfiguration) {
        super.setNewConfiguration(newConfiguration);
        
        // Update card if it's published and should still be shown
        if (mIsCardPublished && shouldShowUICard()) {
            AddOnUICard card = createUICard();
            if (card != null) {
                mUICardPublisher.updateUICard(card.getTitle(), card.getMessage(), card.getTargetFragment());
            } else {
                unpublishUICard();
            }
        } else if (!shouldShowUICard() && mIsCardPublished) {
            // Should not show card anymore, unpublish it
            unpublishUICard();
        } else if (shouldShowUICard() && !mIsCardPublished) {
            // Should show card now, publish it
            publishUICard();
        }
    }

    /**
     * Performs cleanup when the add-on is being disabled or removed.
     * This method automatically unpublishes any active UI cards.
     */
    public void cleanup() {
        unpublishUICard();
        if (mUICardPublisher != null) {
            mUICardPublisher.cleanup();
        }
    }

    /**
     * Refreshes the UI card based on current state. This method can be called
     * by subclasses when the add-on's internal state changes in a way that
     * might affect the card content or visibility.
     * 
     * <p>For example, if an add-on has a feature that can be enabled/disabled,
     * calling this method will update or remove the card accordingly.
     */
    protected final void refreshUICard() {
        if (shouldShowUICard()) {
            publishUICard();
        } else {
            unpublishUICard();
        }
    }

    /**
     * Gets a list of all active UI cards in the system, including cards from
     * other add-ons. This can be useful for add-ons that want to avoid showing
     * duplicate or conflicting information.
     * 
     * @return A list of all active UI cards
     */
    @NonNull
    protected final List<AddOnUICard> getAllActiveUICards() {
        AddOnUICardManager manager = new AddOnUICardPublisher(getPackageContext(), this).getPublishedCard() != null 
            ? new AddOnUICardManager(getPackageContext()) 
            : new AddOnUICardManager(getPackageContext());
        return manager.getActiveUICards();
    }

    @Override
    public String toString() {
        return String.format(
                "%s '%s' from %s (id %s), API-%d, UI-Card: %s",
                getClass().getName(),
                getName(),
                getPackageName(),
                getId(),
                getApiVersion(),
                mIsCardPublished ? "published" : "not published");
    }
}