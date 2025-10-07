# Plan: Add-on UI Card Publisher Class

## Overview
This document outlines a plan to create a class that enables add-ons to publish their own UI cards to AnySoftKeyboard's main settings screen, using the exact same UI card system as the built-in cards.

## Current System Analysis

### Existing UI Card Infrastructure
1. **UI Card Display**: Cards are shown in `MainFragment.java` within `main_fragment.xml` as `CardView` widgets
2. **Card Data Model**: `AddOnUICard.java` - stores title, message, package name, and target fragment
3. **Card Management**: `AddOnUICardManager.java` - handles registration, storage, and retrieval of cards
4. **Broadcast System**: `AddOnUICardReceiver.java` - receives registration/unregistration intents
5. **Public API**: `AddOnUICardAPI.java` - defines intent actions and extras for add-on communication

### Current Card Types
- Setup wizard card (keyboard not configured)
- Notification permission card
- Testing build card
- Beta sign-up card
- Change log card

### Add-on System Architecture
- **Base Interface**: `AddOn.java` - defines core add-on contract
- **Implementation**: `AddOnImpl.java` - base implementation with package context handling
- **Factory Pattern**: `AddOnsFactory.java` - discovers and manages add-on lifecycle
- **Discovery**: Add-ons register via broadcast receivers with specific intent filters and meta-data

## Proposed Solution: AddOnUICardPublisher Class

### Class Design

#### 1. Core Publisher Class
```java
public class AddOnUICardPublisher {
    private final Context mContext;
    private final String mPackageName;
    private final AddOnUICardManager mCardManager;
    
    public AddOnUICardPublisher(Context context, AddOn addOn);
    
    public void publishUICard(String title, String message, String targetFragment);
    public void publishUICard(String title, String message);
    public void unpublishUICard();
    public boolean isCardPublished();
    public AddOnUICard getPublishedCard();
}
```

#### 2. Add-on Integration
```java
public abstract class AddOnWithUICard extends AddOnImpl {
    private AddOnUICardPublisher mUICardPublisher;
    
    protected AddOnWithUICard(/* existing parameters */);
    
    protected final void publishUICard(String title, String message, String targetFragment);
    protected final void unpublishUICard();
    protected abstract boolean shouldShowUICard();
    protected abstract AddOnUICard createUICard();
}
```

### Implementation Details

#### Phase 1: Core Publisher Implementation
1. **Create AddOnUICardPublisher class**
   - Constructor takes Context and AddOn instance
   - Store package name from add-on
   - Initialize AddOnUICardManager

2. **Card Publishing Methods**
   - `publishUICard()` - create and register card via broadcast intent
   - `unpublishUICard()` - send unregister broadcast intent
   - `isCardPublished()` - check AddOnUICardManager for active cards
   - Validation to ensure only add-on's own cards are managed

3. **Lifecycle Management**
   - Auto-unpublish on add-on disable/remove
   - Handle package context changes
   - Error handling for broadcast failures

#### Phase 2: Add-on Base Class Integration
1. **Create AddOnWithUICard abstract class**
   - Extends AddOnImpl
   - Contains AddOnUICardPublisher instance
   - Provides protected methods for subclasses

2. **Abstract Methods for Customization**
   - `shouldShowUICard()` - determine if card should be shown
   - `createUICard()` - create card with add-on specific content

3. **Automatic Lifecycle Integration**
   - Publish card when add-on is enabled
   - Unpublish when add-on is disabled
   - Handle configuration changes

#### Phase 3: Factory Integration
1. **Extend AddOnsFactory**
   - Detect add-ons with UI card capability
   - Initialize UI card publisher for supported add-ons
   - Handle card lifecycle with add-on lifecycle

2. **Meta-data Support**
   - Add XML attribute for UI card support declaration
   - Parse card-specific meta-data from add-on manifests
   - Backward compatibility with existing add-ons

### Usage Examples

#### Simple Add-on Implementation
```java
public class MyThemeAddOn extends AddOnWithUICard {
    public MyThemeAddOn(Context askContext, Context packageContext, /*...*/) {
        super(askContext, packageContext, /*...*/);
    }
    
    @Override
    protected boolean shouldShowUICard() {
        return true; // Always show card for this theme
    }
    
    @Override
    protected AddOnUICard createUICard() {
        return new AddOnUICard(
            getPackageName(),
            "New Theme Available",
            "Check out our awesome new theme with custom colors!",
            "keyboardThemeSelectorFragment"
        );
    }
}
```

#### Advanced Add-on with Dynamic Cards
```java
public class SmartDictionaryAddOn extends AddOnWithUICard {
    @Override
    protected boolean shouldShowUICard() {
        return getDictionarySize() > 1000; // Only show if dictionary is substantial
    }
    
    @Override
    protected AddOnUICard createUICard() {
        return new AddOnUICard(
            getPackageName(),
            "Enhanced Dictionary Ready",
            String.format("%d words available for improved predictions!", getDictionarySize()),
            "additionalLanguageSettingsFragment"
        );
    }
    
    private int getDictionarySize() {
        // Implementation specific logic
    }
}
```

### Integration Points

#### 1. MainFragment Updates
- Add method to display add-on UI cards alongside existing cards
- Handle card click navigation to target fragments
- Ensure proper card ordering and layout

#### 2. AddOnUICardManager Enhancements
- Add filtering for add-on cards vs system cards
- Support for card priority/ordering
- Better error handling for invalid cards

#### 3. Broadcast Receiver Updates
- Enhanced validation in AddOnUICardReceiver
- Support for card update operations
- Better logging and debugging

### Technical Considerations

#### Security & Validation
1. **Package Verification**: Ensure only add-on's own package can register cards for that package
2. **Content Validation**: Validate card content length and format
3. **Target Fragment Validation**: Ensure target fragments exist and are accessible

#### Performance
1. **Lazy Loading**: Only create card publishers when needed
2. **Efficient Storage**: Optimize SharedPreferences usage for card storage
3. **Memory Management**: Proper cleanup of card resources

#### Backward Compatibility
1. **API Versioning**: Support different API levels for UI card features
2. **Graceful Degradation**: Handle add-ons that don't support UI cards
3. **Feature Detection**: Allow add-ons to detect UI card support

### Testing Strategy

#### Unit Tests
1. **AddOnUICardPublisher Tests**
   - Card publish/unpublish functionality
   - Broadcast intent validation
   - Error handling scenarios

2. **AddOnWithUICard Tests**
   - Lifecycle management
   - Abstract method implementations
   - Integration with AddOnImpl

#### Integration Tests
1. **End-to-End Card Flow**
   - Add-on installation → card registration → display → interaction
   - Add-on removal → card cleanup
   - Configuration changes → card updates

2. **MainFragment Integration**
   - Card display alongside existing cards
   - Navigation to target fragments
   - Card dismissal and persistence

### Migration Path

#### Phase 1: Foundation (Week 1-2)
- Implement AddOnUICardPublisher class
- Create AddOnUICardAPI enhancements
- Add basic unit tests

#### Phase 2: Integration (Week 3-4)
- Create AddOnWithUICard base class
- Update AddOnsFactory for UI card support
- Implement MainFragment integration
- Add integration tests

#### Phase 3: Polish & Documentation (Week 5-6)
- Add advanced features (card priority, updates)
- Complete test coverage
- Write add-on developer documentation
- Create example add-on implementations

### Success Metrics
1. **Functional**: Add-ons can successfully publish and manage UI cards
2. **Performance**: No significant impact on app startup or memory usage
3. **Compatibility**: Works with existing add-ons without breaking changes
4. **Developer Experience**: Simple API that add-on developers can easily implement
5. **User Experience**: Seamless integration with existing UI card system

## Click Action Implementation Research

### Current Click Handling System Analysis
Based on thorough research of the existing codebase, the current UI card system already has a working click implementation:

1. **Existing Click Pattern**: The current `AddOnUICard` class supports a `targetFragment` field that enables navigation to predefined fragments
2. **Working Implementation**: In `MainFragment.java` (lines 194-208), add-on UI cards already handle clicks using:
   ```java
   if (card.getTargetFragment() != null) {
       cardView.setOnClickListener(v -> {
           try {
               Navigation.findNavController(requireView())
                   .navigate(card.getTargetFragment());
           } catch (Exception e) {
               Logger.w(TAG, "Failed to navigate to target fragment: " + card.getTargetFragment(), e);
           }
       });
   }
   ```

3. **Existing "Click Here" Patterns**: The app already has multiple working "click here" implementations:
   - **Setup Wizard Card**: Uses `ClickableSpan` with `SpannableStringBuilder` for inline clickable text (lines 266-286)
   - **Notification Permission Card**: Uses simple `setOnClickListener` on the entire card view (lines 124-125)
   - **Social Links**: Uses `ClickableSpan` for web links (lines 288-310)

### Current System Capabilities
The existing system already supports:
- ✅ Fragment navigation to any predefined fragment in the navigation graph
- ✅ Click handling for entire card views
- ✅ Inline clickable text within card content
- ✅ Error handling for navigation failures
- ✅ Activity launching (via Intent in existing click handlers)
- ✅ Web URL opening (via Intent.ACTION_VIEW)

### Demo Add-on Issue Analysis
The demo add-on (`DemoUIAddOn.java`) is trying to use non-existent APIs:
```java
UICardData card = new UICardData.Builder()
    .setId("demo_card_1")
    .setTitle("Demo Add-on Active")
    .setSubtitle("This card demonstrates UI card functionality")
    .setIcon(android.R.drawable.ic_dialog_info)
    .setClickAction(new Intent(getContext(), DemoUIAddOn.class)) // ❌ Does not exist
    .setPriority(1)
    .build();
```

**CRITICAL FINDING**: The demo add-on is referencing completely non-existent classes (`UICardData`, `Builder`) that were never implemented. The actual working system uses `AddOnUICard` with a simple constructor and `targetFragment` for navigation.

### Investigation: Where Did `UICardData.Builder()` Come From?

#### **Complete Analysis Results**

**The Short Answer:** The `UICardData.Builder()` pattern in the demo add-on **does not exist anywhere** in the codebase. It appears to be **fictional code that was written but never implemented** - essentially a placeholder or aspirational API that was never actually created.

#### **Detailed Investigation Findings**

1. **Complete Absence from Codebase**
   - **No `UICardData` class exists** anywhere in the project
   - **No `Builder` pattern for UI cards** exists anywhere in the project  
   - **Only references** are in the demo add-on and this plan document
   - **Git history shows no traces** of these classes ever existing

2. **Pattern Inspiration Analysis**
   The `UICardData.Builder()` pattern appears to be inspired by common Android Builder patterns that exist in the codebase:
   
   **Similar existing patterns:**
   ```java
   // NotificationChannelCompat.Builder (NotificationDriverImpl.java:37-42)
   new NotificationChannelCompat.Builder(channel.mChannelId, channel.mImportance)
       .setName(channel.mChannelId)
       .setDescription(channel.mDescription)
       .setLightsEnabled(true)
       .setVibrationEnabled(true)
       .build();

   // AlertDialog.Builder pattern (used throughout the codebase)
   new AlertDialog.Builder(requireContext())
       .setTitle("Title")
       .setMessage("Message")
       .setPositiveButton("OK", null)
       .show();
   ```

3. **What Actually Exists vs. What Was Written**

   **✅ What Actually Exists:**
   ```java
   // Real working API
   AddOnUICard card = new AddOnUICard(addonPackage, title, message, targetFragment);
   ```

   **❌ What Was Written in Demo (Non-existent):**
   ```java
   // Fictional API
   UICardData card = new UICardData.Builder()
       .setId("demo_card_1")
       .setTitle("Demo Add-on Active")
       .setSubtitle("This card demonstrates UI card functionality")
       .setIcon(android.R.drawable.ic_dialog_info)
       .setClickAction(new Intent(getContext(), DemoUIAddOn.class))
       .setPriority(1)
       .build();
   ```

4. **Timeline Analysis**
   - **Demo add-on created**: October 6, 2025 at 23:34 (yesterday)
   - **Files are untracked**: Not committed to git yet
   - **No git history**: No commits showing the evolution of this code
   - **Pattern appears fully-formed**: The Builder pattern was written as if it already existed

5. **Most Likely Explanation**
   The demo add-on was written with **aspirational/planned API design** rather than the actual working API. The developer likely:
   - **Assumed a Builder pattern would be implemented** (following Android conventions)
   - **Designed an ideal API** without checking what actually existed
   - **Wrote the demo code** based on this assumed future API
   - **Never implemented the underlying classes** to support this pattern

6. **Why This Pattern Was Chosen**
   The Builder pattern in the demo follows these Android design principles:
   - **Fluent interface**: `.setX().setY().setZ().build()`
   - **Immutable objects**: Built once, then used
   - **Optional parameters**: Some fields can be omitted
   - **Type safety**: Compile-time checking of parameters

   This is a **common and well-regarded pattern** in Android development, which explains why someone would naturally reach for it.

7. **The Working Alternative**
   The actual working system is much simpler:
   ```java
   // Real API in AddOnUICardReceiver.java (line 47)
   AddOnUICard card = new AddOnUICard(addonPackage, title, message, null);
   ```

#### **Conclusion**

The `UICardData.Builder()` is **fictional code** that represents a **planned but never implemented enhancement** to the UI card system. It was written as if the enhancement already existed, but the underlying classes were never created. This explains why:

- The demo add-on doesn't compile
- No references exist anywhere else in the codebase
- The pattern follows Android conventions but isn't implemented
- The working system uses a much simpler constructor-based approach

**The fix is simple**: Replace the fictional `UICardData.Builder()` code with the actual working `AddOnUICard` constructor that already exists in the codebase.

### Assessment: ClickAction Class Necessity

**FINDING**: The proposed `ClickAction` class is **NOT NECESSARY** for the following reasons:

1. **Existing Functionality Sufficient**: The current `targetFragment` approach already handles the primary use case (navigation within settings)
2. **Flexibility Already Present**: Click handlers can already launch activities, open URLs, or perform custom actions by extending the click listener logic
3. **Working Implementation**: The system already has working click handling that doesn't need replacement
4. **Complexity vs Benefit**: Adding a complex ClickAction system would overcomplicate a simple use case

**RECOMMENDATION**: Instead of implementing the ClickAction class, we should:
1. Fix the demo add-on to use the existing `AddOnUICard` API correctly
2. Extend the existing click handler to support additional common patterns if needed
3. Keep the simple, working `targetFragment` approach for basic navigation

## Revised Click Action Approach

### Finding: ClickAction Class Not Needed

After thorough investigation of the existing codebase, the proposed `ClickAction` class is **unnecessary**. The current system already provides sufficient click functionality through the existing `targetFragment` approach and can be easily extended for additional use cases.

### Recommended Approach: Extend Existing System

Instead of implementing a complex ClickAction class, we should:

#### 1. Fix the Demo Add-on (Immediate Priority)
The demo add-on is using non-existent APIs. It should be fixed to use the working `AddOnUICard` class:

```java
// CURRENT (BROKEN) - DemoUIAddOn.java
UICardData card = new UICardData.Builder()
    .setId("demo_card_1")
    .setTitle("Demo Add-on Active")
    .setClickAction(new Intent(getContext(), DemoUIAddOn.class)) // ❌ Does not exist
    .build();

// CORRECTED - Should use existing AddOnUICard
@Override
protected AddOnUICard createUICard() {
    return new AddOnUICard(
        getPackageName(),
        "Demo Add-on Active", 
        "This card demonstrates UI card functionality",
        "openAIVoiceControlSettingsFragment" // Navigate to OpenAI settings
    );
}
```

#### 2. Extend Current Click Handling (If Needed)
The existing click handler in `MainFragment` can be enhanced to support additional patterns without changing the data model:

```java
// Enhanced click handler in MainFragment.java
private void setupCardClickHandler(CardView cardView, AddOnUICard card) {
    cardView.setOnClickListener(v -> {
        if (card.getTargetFragment() != null) {
            // Existing fragment navigation
            try {
                Navigation.findNavController(requireView())
                    .navigate(card.getTargetFragment());
            } catch (Exception e) {
                Logger.w(TAG, "Failed to navigate to target fragment: " + card.getTargetFragment(), e);
            }
        } else {
            // NEW: Handle cards without targetFragment
            handleCardWithoutTargetFragment(card);
        }
    });
}

private void handleCardWithoutTargetFragment(AddOnUICard card) {
    // Could implement custom logic based on card content or package name
    // For example: launch specific activities based on add-on type
    if (card.getPackageName().contains("demo")) {
        // Handle demo add-on specific actions
        showDemoDialog(card);
    }
}
```

#### 3. Optional: Add Simple Extensions to AddOnUICard
If additional functionality is needed, we can add simple fields to the existing `AddOnUICard` class:

```java
public class AddOnUICard implements Parcelable {
    private final String packageName;
    private final String title;
    private final String message;
    private final String targetFragment; // Existing
    
    // OPTIONAL: Simple extensions (not complex ClickAction)
    private final String customActionData; // NEW: simple string data for custom handling
    private final int priority; // NEW: card ordering
    private final String iconResourceName; // NEW: card icons
    
    // Simple constructor for basic use case (unchanged)
    public AddOnUICard(String packageName, String title, String message, String targetFragment) {
        // existing implementation
    }
    
    // Enhanced constructor for advanced use cases
    public AddOnUICard(String packageName, String title, String message, String targetFragment, 
                      String customActionData, int priority, String iconResourceName) {
        // enhanced implementation
    }
}
```

### Benefits of This Approach

1. **Simplicity**: Maintains the existing, working system without unnecessary complexity
2. **Backward Compatibility**: No breaking changes to existing APIs
3. **Immediate Fix**: Demo add-on can be fixed quickly using existing patterns
4. **Flexibility**: Click handlers can be extended as needed without data model changes
5. **Proven Pattern**: Uses the same approach as existing working cards (setup wizard, notification permission)

### Implementation Priority

#### HIGH PRIORITY: Fix Demo Add-on
- Update `DemoUIAddOn.java` to use existing `AddOnUICard` API
- Remove references to non-existent `UICardData` and `Builder` classes
- Test with existing `targetFragment` navigation

#### MEDIUM PRIORITY: Enhanced Click Handling (If Needed)
- Extend `MainFragment` click handler for custom actions
- Add simple extensions to `AddOnUICard` if required
- Maintain backward compatibility

#### LOW PRIORITY: Advanced Features (Future)
- Add card priority and icon support
- Implement custom action handling
- Add analytics and tracking

### Conclusion

The research shows that the existing UI card system already has robust click handling capabilities. The proposed `ClickAction` class would add unnecessary complexity to a system that already works well. The immediate priority should be fixing the demo add-on to use the existing, proven APIs rather than implementing new, unnecessary infrastructure.

## Summary of Key Findings

### What Works Already
- ✅ **Fragment Navigation**: `targetFragment` field enables navigation to any predefined fragment
- ✅ **Click Handling**: Robust click listener implementation in `MainFragment`
- ✅ **Error Handling**: Proper exception handling for navigation failures
- ✅ **Multiple Click Patterns**: Support for both card-level and inline text clicks
- ✅ **Activity Launching**: Existing patterns show how to launch activities from clicks
- ✅ **Web Links**: Working implementation for opening URLs via Intent.ACTION_VIEW

### What Doesn't Work
- ❌ **Demo Add-on**: Uses non-existent `UICardData` and `Builder` classes
- ❌ **Documentation**: Demo add-on provides incorrect implementation example
- ❌ **API Confusion**: Demo suggests complex APIs that don't exist

### Recommended Actions
1. **Fix Demo Add-on**: Update to use existing `AddOnUICard` API (HIGH PRIORITY)
2. **Document Proper Usage**: Create clear examples using working APIs (MEDIUM PRIORITY)
3. **Enhance If Needed**: Extend existing system only if actual use cases emerge (LOW PRIORITY)

### Final Recommendation
**Do not implement the ClickAction class.** Instead, focus on fixing the demo add-on and documenting the existing, working system. The current implementation already provides the necessary functionality for UI card click handling.