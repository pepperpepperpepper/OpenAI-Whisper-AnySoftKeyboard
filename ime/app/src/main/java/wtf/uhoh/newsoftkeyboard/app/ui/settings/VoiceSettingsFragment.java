package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import net.evendanan.pixel.UiUtils;
import wtf.uhoh.newsoftkeyboard.R;

public class VoiceSettingsFragment extends SpeechToTextSettingsFragment {

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    UiUtils.setActivityTitle(this, R.string.settings_category_voice);
  }
}
