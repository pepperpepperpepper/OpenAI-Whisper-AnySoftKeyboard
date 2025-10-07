package com.anysoftkeyboard.addons;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.res.Configuration;
import com.anysoftkeyboard.AnySoftKeyboardRobolectricTestRunner;
import com.anysoftkeyboard.ui.settings.AddOnUICard;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AnySoftKeyboardRobolectricTestRunner.class)
public class AddOnWithUICardTest {

    private Context mContext;
    private TestAddOnWithUICard mTestAddOn;

    @Before
    public void setUp() {
        mContext = getApplicationContext();
        mTestAddOn = new TestAddOnWithUICard(mContext, mContext, 1, "test_id", "Test AddOn", "Test Description", false, 1);
    }

    @Test
    public void testConstructorAutoPublishesCardWhenShouldShowUICardReturnsTrue() {
        TestAddOnWithUICard addOn = new TestAddOnWithUICard(mContext, mContext, 1, "test_id", "Test AddOn", "Test Description", false, 1) {
            @Override
            protected boolean shouldShowUICard() {
                return true;
            }

            @Override
            protected AddOnUICard createUICard() {
                return new AddOnUICard(getPackageName(), "Test Title", "Test Message", "testFragment");
            }
        };

        assertTrue(addOn.isCardPublished());
        assertNotNull(addOn.getPublishedCard());
        assertEquals("Test Title", addOn.getPublishedCard().getTitle());
    }

    @Test
    public void testConstructorDoesNotPublishCardWhenShouldShowUICardReturnsFalse() {
        TestAddOnWithUICard addOn = new TestAddOnWithUICard(mContext, mContext, 1, "test_id", "Test AddOn", "Test Description", false, 1) {
            @Override
            protected boolean shouldShowUICard() {
                return false;
            }

            @Override
            protected AddOnUICard createUICard() {
                return new AddOnUICard(getPackageName(), "Test Title", "Test Message", "testFragment");
            }
        };

        assertFalse(addOn.isCardPublished());
        assertNull(addOn.getPublishedCard());
    }

    @Test
    public void testConstructorDoesNotPublishCardWhenCreateUICardReturnsNull() {
        TestAddOnWithUICard addOn = new TestAddOnWithUICard(mContext, mContext, 1, "test_id", "Test AddOn", "Test Description", false, 1) {
            @Override
            protected boolean shouldShowUICard() {
                return true;
            }

            @Override
            protected AddOnUICard createUICard() {
                return null;
            }
        };

        assertFalse(addOn.isCardPublished());
        assertNull(addOn.getPublishedCard());
    }

    @Test
    public void testPublishUICardWhenShouldShowUICardReturnsTrue() {
        mTestAddOn.setShouldShowUICard(true);
        mTestAddOn.setCreateUICardResult(new AddOnUICard("com.test.addon", "Test Title", "Test Message", "testFragment"));

        mTestAddOn.publishUICard();

        assertTrue(mTestAddOn.isCardPublished());
        assertNotNull(mTestAddOn.getPublishedCard());
        assertEquals("Test Title", mTestAddOn.getPublishedCard().getTitle());
    }

    @Test
    public void testPublishUICardWhenShouldShowUICardReturnsFalse() {
        mTestAddOn.setShouldShowUICard(false);
        mTestAddOn.setCreateUICardResult(new AddOnUICard("com.test.addon", "Test Title", "Test Message", "testFragment"));

        // First publish a card
        mTestAddOn.setShouldShowUICard(true);
        mTestAddOn.publishUICard();
        assertTrue(mTestAddOn.isCardPublished());

        // Now set shouldShowUICard to false and publish again
        mTestAddOn.setShouldShowUICard(false);
        mTestAddOn.publishUICard();

        assertFalse(mTestAddOn.isCardPublished());
        assertNull(mTestAddOn.getPublishedCard());
    }

    @Test
    public void testPublishUICardWhenCreateUICardReturnsNull() {
        mTestAddOn.setShouldShowUICard(true);
        mTestAddOn.setCreateUICardResult(null);

        mTestAddOn.publishUICard();

        assertFalse(mTestAddOn.isCardPublished());
        assertNull(mTestAddOn.getPublishedCard());
    }

    @Test
    public void testUnpublishUICard() {
        // First publish a card
        mTestAddOn.setShouldShowUICard(true);
        mTestAddOn.setCreateUICardResult(new AddOnUICard("com.test.addon", "Test Title", "Test Message", "testFragment"));
        mTestAddOn.publishUICard();
        assertTrue(mTestAddOn.isCardPublished());

        // Now unpublish
        mTestAddOn.unpublishUICard();

        assertFalse(mTestAddOn.isCardPublished());
        assertNull(mTestAddOn.getPublishedCard());
    }

    @Test
    public void testUnpublishUICardWhenNoCardPublished() {
        mTestAddOn.unpublishUICard();

        assertFalse(mTestAddOn.isCardPublished());
        assertNull(mTestAddOn.getPublishedCard());
    }

    @Test
    public void testRefreshUICardPublishesWhenShouldShowUICardReturnsTrue() {
        mTestAddOn.setShouldShowUICard(true);
        mTestAddOn.setCreateUICardResult(new AddOnUICard("com.test.addon", "Test Title", "Test Message", "testFragment"));

        mTestAddOn.refreshUICard();

        assertTrue(mTestAddOn.isCardPublished());
        assertNotNull(mTestAddOn.getPublishedCard());
    }

    @Test
    public void testRefreshUICardUnpublishesWhenShouldShowUICardReturnsFalse() {
        // First publish a card
        mTestAddOn.setShouldShowUICard(true);
        mTestAddOn.setCreateUICardResult(new AddOnUICard("com.test.addon", "Test Title", "Test Message", "testFragment"));
        mTestAddOn.publishUICard();
        assertTrue(mTestAddOn.isCardPublished());

        // Now set shouldShowUICard to false and refresh
        mTestAddOn.setShouldShowUICard(false);
        mTestAddOn.refreshUICard();

        assertFalse(mTestAddOn.isCardPublished());
        assertNull(mTestAddOn.getPublishedCard());
    }

    @Test
    public void testSetNewConfigurationUpdatesCardWhenCardIsPublishedAndShouldShowUICardReturnsTrue() {
        mTestAddOn.setShouldShowUICard(true);
        mTestAddOn.setCreateUICardResult(new AddOnUICard("com.test.addon", "Original Title", "Original Message", "testFragment"));
        mTestAddOn.publishUICard();
        assertTrue(mTestAddOn.isCardPublished());

        // Change the card that will be created
        mTestAddOn.setCreateUICardResult(new AddOnUICard("com.test.addon", "Updated Title", "Updated Message", "testFragment"));

        Configuration newConfig = new Configuration();
        mTestAddOn.setNewConfiguration(newConfig);

        assertTrue(mTestAddOn.isCardPublished());
        assertNotNull(mTestAddOn.getPublishedCard());
        assertEquals("Updated Title", mTestAddOn.getPublishedCard().getTitle());
        assertEquals("Updated Message", mTestAddOn.getPublishedCard().getMessage());
    }

    @Test
    public void testSetNewConfigurationUnpublishesCardWhenShouldShowUICardReturnsFalse() {
        mTestAddOn.setShouldShowUICard(true);
        mTestAddOn.setCreateUICardResult(new AddOnUICard("com.test.addon", "Original Title", "Original Message", "testFragment"));
        mTestAddOn.publishUICard();
        assertTrue(mTestAddOn.isCardPublished());

        // Set shouldShowUICard to false
        mTestAddOn.setShouldShowUICard(false);

        Configuration newConfig = new Configuration();
        mTestAddOn.setNewConfiguration(newConfig);

        assertFalse(mTestAddOn.isCardPublished());
        assertNull(mTestAddOn.getPublishedCard());
    }

    @Test
    public void testSetNewConfigurationPublishesCardWhenShouldShowUICardReturnsTrueAndCardNotPublished() {
        mTestAddOn.setShouldShowUICard(false);
        mTestAddOn.refreshUICard(); // Ensure no card is published
        assertFalse(mTestAddOn.isCardPublished());

        // Set shouldShowUICard to true
        mTestAddOn.setShouldShowUICard(true);
        mTestAddOn.setCreateUICardResult(new AddOnUICard("com.test.addon", "New Title", "New Message", "testFragment"));

        Configuration newConfig = new Configuration();
        mTestAddOn.setNewConfiguration(newConfig);

        assertTrue(mTestAddOn.isCardPublished());
        assertNotNull(mTestAddOn.getPublishedCard());
        assertEquals("New Title", mTestAddOn.getPublishedCard().getTitle());
    }

    @Test
    public void testCleanup() {
        // First publish a card
        mTestAddOn.setShouldShowUICard(true);
        mTestAddOn.setCreateUICardResult(new AddOnUICard("com.test.addon", "Test Title", "Test Message", "testFragment"));
        mTestAddOn.publishUICard();
        assertTrue(mTestAddOn.isCardPublished());

        mTestAddOn.cleanup();

        assertFalse(mTestAddOn.isCardPublished());
        assertNull(mTestAddOn.getPublishedCard());
    }

    @Test
    public void testCleanupWhenNoCardPublished() {
        mTestAddOn.cleanup();

        assertFalse(mTestAddOn.isCardPublished());
        assertNull(mTestAddOn.getPublishedCard());
    }

    @Test
    public void testGetUICardPublisher() {
        assertNotNull(mTestAddOn.getUICardPublisher());
        assertEquals(mTestAddOn.getPackageName(), mTestAddOn.getUICardPublisher().getPackageName());
    }

    @Test
    public void testToStringIncludesUICardInfo() {
        mTestAddOn.setShouldShowUICard(true);
        mTestAddOn.setCreateUICardResult(new AddOnUICard("com.test.addon", "Test Title", "Test Message", "testFragment"));
        mTestAddOn.publishUICard();

        String toString = mTestAddOn.toString();
        assertTrue(toString.contains("UI-Card: published"));
    }

    @Test
    public void testToStringIncludesUICardInfoWhenNotPublished() {
        String toString = mTestAddOn.toString();
        assertTrue(toString.contains("UI-Card: not published"));
    }

    private static class TestAddOnWithUICard extends AddOnWithUICard {
        private boolean mShouldShowUICard = false;
        private AddOnUICard mCreateUICardResult = null;

        public TestAddOnWithUICard(Context askContext, Context packageContext, int apiVersion, CharSequence id, CharSequence name, CharSequence description, boolean hidden, int sortIndex) {
            super(askContext, packageContext, apiVersion, id, name, description, hidden, sortIndex);
        }

        public void setShouldShowUICard(boolean shouldShowUICard) {
            mShouldShowUICard = shouldShowUICard;
        }

        public void setCreateUICardResult(AddOnUICard createUICardResult) {
            mCreateUICardResult = createUICardResult;
        }

        @Override
        protected boolean shouldShowUICard() {
            return mShouldShowUICard;
        }

        @Override
        protected AddOnUICard createUICard() {
            return mCreateUICardResult;
        }
    }
}