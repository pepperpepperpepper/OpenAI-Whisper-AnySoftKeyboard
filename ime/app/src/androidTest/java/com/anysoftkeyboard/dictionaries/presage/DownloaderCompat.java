package com.anysoftkeyboard.dictionaries.presage;

import androidx.annotation.NonNull;
import org.json.JSONException;
import java.io.IOException;

/** Test-only compatibility shim for calling the refactored downloader API. */
public final class DownloaderCompat {
  private DownloaderCompat() {}

  public static PresageModelDefinition run(
      @NonNull PresageModelDownloader downloader,
      @NonNull PresageModelCatalog.CatalogEntry entry) throws IOException, JSONException {
    return downloader.downloadAndInstall(
        entry.getDefinition(), entry.getBundleUrl(), entry.getBundleSha256());
  }
}
