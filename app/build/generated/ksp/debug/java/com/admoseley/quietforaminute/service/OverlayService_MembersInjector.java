package com.admoseley.quietforaminute.service;

import com.admoseley.quietforaminute.audio.ChimePlayer;
import com.admoseley.quietforaminute.data.datastore.PreferencesRepository;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;

@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class OverlayService_MembersInjector implements MembersInjector<OverlayService> {
  private final Provider<PreferencesRepository> prefsRepositoryProvider;

  private final Provider<ChimePlayer> chimePlayerProvider;

  private OverlayService_MembersInjector(Provider<PreferencesRepository> prefsRepositoryProvider,
      Provider<ChimePlayer> chimePlayerProvider) {
    this.prefsRepositoryProvider = prefsRepositoryProvider;
    this.chimePlayerProvider = chimePlayerProvider;
  }

  @Override
  public void injectMembers(OverlayService instance) {
    injectPrefsRepository(instance, prefsRepositoryProvider.get());
    injectChimePlayer(instance, chimePlayerProvider.get());
  }

  public static MembersInjector<OverlayService> create(
      Provider<PreferencesRepository> prefsRepositoryProvider,
      Provider<ChimePlayer> chimePlayerProvider) {
    return new OverlayService_MembersInjector(prefsRepositoryProvider, chimePlayerProvider);
  }

  @InjectedFieldSignature("com.admoseley.quietforaminute.service.OverlayService.prefsRepository")
  public static void injectPrefsRepository(OverlayService instance,
      PreferencesRepository prefsRepository) {
    instance.prefsRepository = prefsRepository;
  }

  @InjectedFieldSignature("com.admoseley.quietforaminute.service.OverlayService.chimePlayer")
  public static void injectChimePlayer(OverlayService instance, ChimePlayer chimePlayer) {
    instance.chimePlayer = chimePlayer;
  }
}
