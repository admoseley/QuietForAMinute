package com.admoseley.quietforaminute.ui.schedules;

import com.admoseley.quietforaminute.data.repository.ScheduleRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata
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
public final class ScheduleListViewModel_Factory implements Factory<ScheduleListViewModel> {
  private final Provider<ScheduleRepository> repositoryProvider;

  private ScheduleListViewModel_Factory(Provider<ScheduleRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public ScheduleListViewModel get() {
    return newInstance(repositoryProvider.get());
  }

  public static ScheduleListViewModel_Factory create(
      Provider<ScheduleRepository> repositoryProvider) {
    return new ScheduleListViewModel_Factory(repositoryProvider);
  }

  public static ScheduleListViewModel newInstance(ScheduleRepository repository) {
    return new ScheduleListViewModel(repository);
  }
}
