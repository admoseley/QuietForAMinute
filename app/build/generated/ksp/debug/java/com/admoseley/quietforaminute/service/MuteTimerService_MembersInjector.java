package com.admoseley.quietforaminute.service;

import com.admoseley.quietforaminute.audio.ChimePlayer;
import com.admoseley.quietforaminute.data.datastore.PreferencesRepository;
import com.admoseley.quietforaminute.data.repository.ScheduleRepository;
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
public final class MuteTimerService_MembersInjector implements MembersInjector<MuteTimerService> {
  private final Provider<PreferencesRepository> prefsRepositoryProvider;

  private final Provider<ScheduleRepository> scheduleRepositoryProvider;

  private final Provider<ChimePlayer> chimePlayerProvider;

  private MuteTimerService_MembersInjector(Provider<PreferencesRepository> prefsRepositoryProvider,
      Provider<ScheduleRepository> scheduleRepositoryProvider,
      Provider<ChimePlayer> chimePlayerProvider) {
    this.prefsRepositoryProvider = prefsRepositoryProvider;
    this.scheduleRepositoryProvider = scheduleRepositoryProvider;
    this.chimePlayerProvider = chimePlayerProvider;
  }

  @Override
  public void injectMembers(MuteTimerService instance) {
    injectPrefsRepository(instance, prefsRepositoryProvider.get());
    injectScheduleRepository(instance, scheduleRepositoryProvider.get());
    injectChimePlayer(instance, chimePlayerProvider.get());
  }

  public static MembersInjector<MuteTimerService> create(
      Provider<PreferencesRepository> prefsRepositoryProvider,
      Provider<ScheduleRepository> scheduleRepositoryProvider,
      Provider<ChimePlayer> chimePlayerProvider) {
    return new MuteTimerService_MembersInjector(prefsRepositoryProvider, scheduleRepositoryProvider, chimePlayerProvider);
  }

  @InjectedFieldSignature("com.admoseley.quietforaminute.service.MuteTimerService.prefsRepository")
  public static void injectPrefsRepository(MuteTimerService instance,
      PreferencesRepository prefsRepository) {
    instance.prefsRepository = prefsRepository;
  }

  @InjectedFieldSignature("com.admoseley.quietforaminute.service.MuteTimerService.scheduleRepository")
  public static void injectScheduleRepository(MuteTimerService instance,
      ScheduleRepository scheduleRepository) {
    instance.scheduleRepository = scheduleRepository;
  }

  @InjectedFieldSignature("com.admoseley.quietforaminute.service.MuteTimerService.chimePlayer")
  public static void injectChimePlayer(MuteTimerService instance, ChimePlayer chimePlayer) {
    instance.chimePlayer = chimePlayer;
  }
}
