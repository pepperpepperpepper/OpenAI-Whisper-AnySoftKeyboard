package com.anysoftkeyboard.ime;

import com.anysoftkeyboard.dictionaries.Suggest;
import com.anysoftkeyboard.keyboards.views.CandidateView;
import java.util.function.Supplier;

/** Lightweight host for {@link UserDictionaryWorker}. */
final class UserDictionaryHost implements UserDictionaryWorker.Host {

  private final Supplier<Suggest> suggestSupplier;
  private final Supplier<CandidateView> candidateViewSupplier;

  UserDictionaryHost(
      Supplier<Suggest> suggestSupplier, Supplier<CandidateView> candidateViewSupplier) {
    this.suggestSupplier = suggestSupplier;
    this.candidateViewSupplier = candidateViewSupplier;
  }

  @Override
  public Suggest suggest() {
    return suggestSupplier.get();
  }

  @Override
  public CandidateView candidateView() {
    return candidateViewSupplier.get();
  }
}
