package com.anysoftkeyboard.dictionaries.presage;

import androidx.annotation.NonNull;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/** Provides network streams for Presage model catalog and bundle downloads. */
public interface RemoteModelStreamProvider {

  @NonNull
  InputStream open(@NonNull String url) throws IOException;

  @NonNull
  static RemoteModelStreamProvider http() {
    return new HttpRemoteModelStreamProvider();
  }

  final class HttpRemoteModelStreamProvider implements RemoteModelStreamProvider {

    private static final int CONNECT_TIMEOUT_MS = 15_000;
    private static final int READ_TIMEOUT_MS = 60_000;

    @Override
    @NonNull
    public InputStream open(@NonNull String urlString) throws IOException {
      final URL url = new URL(urlString);
      final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
      connection.setReadTimeout(READ_TIMEOUT_MS);
      connection.setInstanceFollowRedirects(true);
      connection.setRequestProperty(
          "User-Agent", "NewSoftKeyboard/engine-presage");
      connection.setRequestProperty("Accept", "application/json, application/zip, */*");
      connection.setRequestProperty("Accept-Encoding", "identity");
      connection.connect();
      final int responseCode = connection.getResponseCode();
      if (responseCode >= HttpURLConnection.HTTP_BAD_REQUEST) {
        connection.disconnect();
        throw new IOException(
            "HTTP " + responseCode + " when fetching " + urlString + ".");
      }
      final InputStream inputStream = connection.getInputStream();
      return new ConnectionInputStream(connection, inputStream);
    }

    private static final class ConnectionInputStream extends FilterInputStream {

      private final HttpURLConnection mConnection;

      ConnectionInputStream(HttpURLConnection connection, InputStream delegate) {
        super(delegate);
        mConnection = connection;
      }

      @Override
      public void close() throws IOException {
        try {
          super.close();
        } finally {
          mConnection.disconnect();
        }
      }
    }
  }
}
