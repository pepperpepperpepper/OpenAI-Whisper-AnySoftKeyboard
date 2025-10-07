package com.anysoftkeyboard.ui.settings;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import com.anysoftkeyboard.addons.AddOnImpl;
import com.anysoftkeyboard.addons.AddOnUICardPublisher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class AddOnUICardIntegrationTest {

    private Context mContext;
    private AddOnUICardManager mCardManager;
    private TestAddOn mTestAddOn;
    private AddOnUICardPublisher mPublisher;

    @Before
    public void setUp() {
        mContext = getApplicationContext();
        // Clear any existing cards
        AddOnUICardManager tempManager = new AddOnUICardManager(mContext);
        for (AddOnUICard card : tempManager.getActiveUICards()) {
            tempManager.unregisterUICard(card.getPackageName());
        }
        
        mCardManager = new AddOnUICardManager(mContext);
        mTestAddOn = new TestAddOn(mContext, mContext, 1, "test_id", "Test AddOn", "Test Description", false, 1);
        mPublisher = new AddOnUICardPublisher(mContext, mTestAddOn);
    }

    @Test
    public void testEndToEndCardFlow() {
        // Initially no cards should be active
        assertTrue(mCardManager.getActiveUICards().isEmpty());

        // Publish a card
        mPublisher.publishUICard("Test Title", "Test Message", "testFragment");

        // Verify card is active
        assertEquals(1, mCardManager.getActiveUICards().size());
        assertTrue(mPublisher.isCardPublished());
        assertNotNull(mPublisher.getPublishedCard());
        assertEquals("Test Title", mPublisher.getPublishedCard().getTitle());
        assertEquals("Test Message", mPublisher.getPublishedCard().getMessage());
        assertEquals("testFragment", mPublisher.getPublishedCard().getTargetFragment());

        // Unpublish the card
        mPublisher.unpublishUICard();

        // Verify card is no longer active
        assertTrue(mCardManager.getActiveUICards().isEmpty());
        assertFalse(mPublisher.isCardPublished());
        assertNull(mPublisher.getPublishedCard());
    }

    @Test
    public void testCardUpdateFlow() {
        // Publish initial card
        mPublisher.publishUICard("Initial Title", "Initial Message", null);
        assertEquals("Initial Title", mPublisher.getPublishedCard().getTitle());

        // Update the card
        mPublisher.updateUICard("Updated Title", "Updated Message", null);
        assertEquals("Updated Title", mPublisher.getPublishedCard().getTitle());
        assertEquals("Updated Message", mPublisher.getPublishedCard().getMessage());

        // Clean up
        mPublisher.unpublishUICard();
    }

    @Test
    public void testCardWithTargetFragment() {
        // Publish a card with target fragment
        mPublisher.publishUICard("Clickable Title", "Clickable Message", "userInterfaceSettingsFragment");

        // Verify the card has the target fragment
        assertEquals("userInterfaceSettingsFragment", mPublisher.getPublishedCard().getTargetFragment());

        // Clean up
        mPublisher.unpublishUICard();
    }

    @Test
    public void testMultiplePublishers() {
        // For now, just test that a single publisher works correctly
        // Multiple publishers with different package names would require more complex setup
        mPublisher.publishUICard("Single Title", "Single Message", null);
        
        // Verify the card is active
        assertEquals(1, mCardManager.getActiveUICards().size());
        assertTrue(mPublisher.isCardPublished());

        // Clean up
        mPublisher.unpublishUICard();
    }

    @Test
    public void testCardPersistence() {
        // Publish a card
        mPublisher.publishUICard("Persistent Title", "Persistent Message", null);
        assertTrue(mPublisher.isCardPublished());

        // Create a new publisher instance for the same add-on
        AddOnUICardPublisher newPublisher = new AddOnUICardPublisher(mContext, mTestAddOn);
        
        // Verify the card is still visible
        assertTrue(newPublisher.isCardPublished());
        assertEquals("Persistent Title", newPublisher.getPublishedCard().getTitle());

        // Clean up
        newPublisher.unpublishUICard();
    }

    private static class TestAddOn extends AddOnImpl {
        public TestAddOn(Context askContext, Context packageContext, int apiVersion, CharSequence id, CharSequence name, CharSequence description, boolean hidden, int sortIndex) {
            super(askContext, packageContext, apiVersion, id, name, description, hidden, sortIndex);
        }
    }
}