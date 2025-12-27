package wtf.uhoh.newsoftkeyboard.app.keyboards.views;

import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.function.Function;
import wtf.uhoh.newsoftkeyboard.app.keyboards.Keyboard;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardDefinition;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardKey;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.preview.KeyPreviewsController;

/** Handles preview popup show/hide logic so {@link KeyboardViewBase} stays slimmer. */
final class PreviewPopupPresenter {

  private final KeyIconResolver keyIconResolver;
  private final KeyPreviewManagerFacade keyPreviewManager;
  private final PreviewThemeConfigurator previewThemeConfigurator;
  private final KeyboardViewBase hostView;

  PreviewPopupPresenter(
      @NonNull KeyboardViewBase hostView,
      @NonNull KeyIconResolver keyIconResolver,
      @NonNull KeyPreviewManagerFacade keyPreviewManager,
      @NonNull PreviewThemeConfigurator previewThemeConfigurator) {
    this.hostView = hostView;
    this.keyIconResolver = keyIconResolver;
    this.keyPreviewManager = keyPreviewManager;
    this.previewThemeConfigurator = previewThemeConfigurator;
  }

  void dismissAll() {
    keyPreviewManager.dismissAll();
  }

  void hidePreview(int keyIndex, @Nullable PointerTracker tracker) {
    final Keyboard.Key key = tracker == null ? null : tracker.getKey(keyIndex);
    if (keyIndex != KeyboardViewBase.NOT_A_KEY && key != null) {
      keyPreviewManager.getController().hidePreviewForKey(key);
    }
  }

  void showPreview(
      int keyIndex,
      @Nullable PointerTracker tracker,
      @Nullable KeyboardDefinition keyboard,
      @NonNull Function<Integer, CharSequence> labelGuesser) {
    final boolean hidePreviewOrShowSpaceKeyPreview = (tracker == null);
    final Keyboard.Key key = hidePreviewOrShowSpaceKeyPreview ? null : tracker.getKey(keyIndex);
    if (keyIndex != KeyboardViewBase.NOT_A_KEY && key != null) {
      Drawable iconToDraw = null;
      if (key instanceof KeyboardKey anyKey) {
        iconToDraw = anyKey.iconPreview != null ? anyKey.iconPreview : anyKey.icon;
      }
      if (iconToDraw == null) {
        iconToDraw = keyIconResolver.getIconToDrawForKey(key, true);
      }

      CharSequence label = tracker.getPreviewText(key);
      if (keyboard != null && key instanceof KeyboardKey anyKey) {
        label = KeyLabelAdjuster.adjustLabelForFunctionState(keyboard, anyKey, label);
      }
      if (TextUtils.isEmpty(label)) {
        label = labelGuesser.apply(key.getPrimaryCode());
        if (keyboard != null && key instanceof KeyboardKey anyKey) {
          label = KeyLabelAdjuster.adjustLabelForFunctionState(keyboard, anyKey, label);
        }
      }

      keyPreviewManager.showPreviewForKey(
          key, iconToDraw, hostView, previewThemeConfigurator.theme(), label);
    }
  }

  void setKeyPreviewController(@NonNull KeyPreviewsController controller) {
    keyPreviewManager.setController(controller);
  }
}
