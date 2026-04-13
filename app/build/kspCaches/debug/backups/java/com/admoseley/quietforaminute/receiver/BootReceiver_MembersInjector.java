package com.admoseley.quietforaminute.receiver;

import com.admoseley.quietforaminute.data.repository.ScheduleRepository;
import com.admoseley.quietforaminute.scheduler.AlarmScheduler;
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
public final class BootReceiver_MembersInjector implements MembersInjector<BootReceiver> {
  private final Provider<ScheduleRepository> scheduleRepositoryProvider;

  private final Provider<AlarmScheduler> alarmSchedulerProvider;

  private BootReceiver_MembersInjector(Provider<ScheduleRepository> scheduleRepositoryProvider,
      Provider<AlarmScheduler> alarmSchedulerProvider) {
    this.scheduleRepositoryProvider = scheduleRepositoryProvider;
    this.alarmSchedulerProvider = alarmSchedulerProvider;
  }

  @Override
  public void injectMembers(BootReceiver instance) {
    injectScheduleRepository(instance, scheduleRepositoryProvider.get());
    injectAlarmScheduler(instance, alarmSchedulerProvider.get());
  }

  public static MembersInjector<BootReceiver> create(
      Provider<ScheduleRepository> scheduleRepositoryProvider,
      Provider<AlarmScheduler> alarmSchedulerProvider) {
    return new BootReceiver_MembersInjector(scheduleRepositoryProvider, alarmSchedulerProvider);
  }

  @InjectedFieldSignature("com.admoseley.quietforaminute.receiver.BootReceiver.scheduleRepository")
  public static void injectScheduleRepository(BootReceiver instance,
      ScheduleRepository scheduleRepository) {
    instance.scheduleRepository = scheduleRepository;
  }

  @InjectedFieldSignature("com.admoseley.quietforaminute.receiver.BootReceiver.alarmScheduler")
  public static void injectAlarmScheduler(BootReceiver instance, AlarmScheduler alarmScheduler) {
    instance.alarmScheduler = alarmScheduler;
  }
}
