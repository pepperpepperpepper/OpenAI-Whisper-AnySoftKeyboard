package com.anysoftkeyboard.keyboards.views;

import com.anysoftkeyboard.rx.GenericOnError;
import com.anysoftkeyboard.prefs.AnimationsLevel;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import io.reactivex.disposables.Disposable;
import java.util.function.Consumer;

/** Owns animation level subject to keep view classes slimmer. */
final class AnimationLevelController {
  private final Subject<AnimationsLevel> subject =
      BehaviorSubject.createDefault(AnimationsLevel.Some);

  Subject<AnimationsLevel> subject() {
    return subject;
  }

  void setLevel(AnimationsLevel level) {
    subject.onNext(level);
  }

  Disposable subscribeWithLogging(String tag, Consumer<AnimationsLevel> onNext) {
    return subject.subscribe(onNext::accept, GenericOnError.onError(tag));
  }
}
