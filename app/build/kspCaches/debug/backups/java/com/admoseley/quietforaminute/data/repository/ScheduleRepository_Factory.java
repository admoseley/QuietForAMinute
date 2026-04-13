package com.admoseley.quietforaminute.data.repository;

import com.admoseley.quietforaminute.data.db.ScheduleDao;
import com.admoseley.quietforaminute.scheduler.AlarmScheduler;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
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
public final class ScheduleRepository_Factory implements Factory<ScheduleRepository> {
  private final Provider<ScheduleDao> daoProvider;

  private final Provider<AlarmScheduler> alarmSchedulerProvider;

  private ScheduleRepository_Factory(Provider<ScheduleDao> daoProvider,
      Provider<AlarmScheduler> alarmSchedulerProvider) {
    this.daoProvider = daoProvider;
    this.alarmSchedulerProvider = alarmSchedulerProvider;
  }

  @Override
  public ScheduleRepository get() {
    return newInstance(daoProvider.get(), alarmSchedulerProvider.get());
  }

  public static ScheduleRepository_Factory create(Provider<ScheduleDao> daoProvider,
      Provider<AlarmScheduler> alarmSchedulerProvider) {
    return new ScheduleRepository_Factory(daoProvider, alarmSchedulerProvider);
  }

  public static ScheduleRepository newInstance(ScheduleDao dao, AlarmScheduler alarmScheduler) {
    return new ScheduleRepository(dao, alarmScheduler);
  }
}
