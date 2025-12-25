package com.anysoftkeyboard.ime;

import android.content.ClipDescription;
import android.content.Intent;
import android.os.Build;
import android.view.inputmethod.EditorInfo;
import androidx.annotation.VisibleForTesting;
import androidx.core.view.inputmethod.EditorInfoCompat;
import androidx.core.view.inputmethod.InputConnectionCompat;
import androidx.core.view.inputmethod.InputContentInfoCompat;
import com.anysoftkeyboard.base.utils.Logger;
import com.anysoftkeyboard.remote.InsertionRequestCallback;
import com.anysoftkeyboard.remote.MediaType;
import com.anysoftkeyboard.remote.RemoteInsertion;
import com.anysoftkeyboard.remote.RemoteInsertionImpl;
import com.menny.android.anysoftkeyboard.R;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public abstract class ImeMediaInsertion extends ImeHardware {

  private final Set<MediaType> mSupportedMediaTypes = new HashSet<>();
  private final Set<MediaType> mSupportedMediaTypesUnmodifiable =
      Collections.unmodifiableSet(mSupportedMediaTypes);

  private InsertionRequestCallback mInsertionRequestCallback;
  private RemoteInsertion mKeyboardRemoteInsertion;

  private int mPendingRequestId;
  private InputContentInfoCompat mPendingCommit;

  @Override
  public void onCreate() {
    super.onCreate();
    mKeyboardRemoteInsertion = createRemoteInsertion();
    mInsertionRequestCallback = new MediaInsertionRequestCallback();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    mKeyboardRemoteInsertion.destroy();
  }

  protected RemoteInsertion createRemoteInsertion() {
    return new RemoteInsertionImpl(this);
  }

  @Override
  public void onStartInputView(EditorInfo info, boolean restarting) {
    super.onStartInputView(info, restarting);

    mSupportedMediaTypes.clear();
    final String[] mimeTypes = EditorInfoCompat.getContentMimeTypes(info);

    for (String mimeType : mimeTypes) {
      if (ClipDescription.compareMimeTypes(mimeType, "image/*")) {
        mSupportedMediaTypes.add(MediaType.Image);
      }
      if (ClipDescription.compareMimeTypes(mimeType, "image/gif")) {
        mSupportedMediaTypes.add(MediaType.Gif);
      }
    }
    if (mPendingCommit != null && mPendingRequestId == getIdForInsertionRequest(info)) {
      mInsertionRequestCallback.onMediaRequestDone(mPendingRequestId, mPendingCommit);
    }
  }

  @Override
  public void onFinishInputView(boolean finishingInput) {
    super.onFinishInputView(finishingInput);
    mSupportedMediaTypes.clear();
  }

  protected void handleMediaInsertionKey() {
    if (getInputConnectionRouter().hasConnection()) {
      final EditorInfo editorInfo = currentInputEditorInfo();
      mPendingRequestId = 0;
      mPendingCommit = null;
      mKeyboardRemoteInsertion.startMediaRequest(
          EditorInfoCompat.getContentMimeTypes(editorInfo),
          getIdForInsertionRequest(editorInfo),
          mInsertionRequestCallback);
    }
  }

  @VisibleForTesting
  static int getIdForInsertionRequest(EditorInfo info) {
    return info == null
        ? 0
        : Arrays.hashCode(new int[] {info.fieldId, info.packageName.hashCode()});
  }

  protected Set<MediaType> getSupportedMediaTypesForInput() {
    return mSupportedMediaTypesUnmodifiable;
  }

  private void onMediaInsertionReply(int requestId, InputContentInfoCompat inputContentInfo) {
    final InputConnectionRouter inputConnectionRouter = getInputConnectionRouter();
    final EditorInfo editorInfo = currentInputEditorInfo();
    if (inputContentInfo != null) {
      Logger.i(
          TAG,
          "Received media insertion for ID %d with URI %s",
          requestId,
          inputContentInfo.getContentUri());
      if (requestId != getIdForInsertionRequest(editorInfo)
          || !inputConnectionRouter.hasConnection()) {
        if (mPendingCommit == null) {
          Logger.d(TAG, "Input connection is not available or request ID is wrong. Waiting.");
          mPendingRequestId = requestId;
          mPendingCommit = inputContentInfo;
          showToastMessage(R.string.media_insertion_pending_message, false);
          return;
        }
      } else {
        int flags = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
          flags |= InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION;
        }

        grantUriPermission(
            editorInfo.packageName,
            inputContentInfo.getContentUri(),
            Intent.FLAG_GRANT_READ_URI_PERMISSION);
        final boolean commitContent =
            commitMediaToInputConnection(
                inputContentInfo, inputConnectionRouter, editorInfo, flags);
        Logger.i(TAG, "Committed content to input-connection. Result: %s", commitContent);
      }
    }

    mPendingRequestId = 0;
    mPendingCommit = null;
  }

  @VisibleForTesting
  protected boolean commitMediaToInputConnection(
      InputContentInfoCompat inputContentInfo,
      InputConnectionRouter inputConnectionRouter,
      EditorInfo editorInfo,
      int flags) {
    return inputConnectionRouter.commitContent(editorInfo, inputContentInfo, flags);
  }

  private class MediaInsertionRequestCallback implements InsertionRequestCallback {
    @Override
    public void onMediaRequestDone(int requestId, InputContentInfoCompat contentInputInfo) {
      onMediaInsertionReply(requestId, contentInputInfo);
    }

    @Override
    public void onMediaRequestCancelled(int requestId) {
      onMediaInsertionReply(0, null);
    }
  }
}
