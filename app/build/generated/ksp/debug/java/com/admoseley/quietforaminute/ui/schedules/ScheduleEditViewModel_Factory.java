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
public final class ScheduleEditViewModel_Factory implements Factory<ScheduleEditViewModel> {
  private final Provider<ScheduleRepository> repositoryProvider;

  private ScheduleEditViewModel_Factory(Provider<ScheduleRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public ScheduleEditViewModel get() {
    return newInstance(repositoryProvider.get());
  }

  public static ScheduleEditViewModel_Factory create(
      Provider<ScheduleRepository> repositoryProvider) {
    return new ScheduleEditViewModel_Factory(repositoryProvider);
  }

  public static ScheduleEditViewModel newInstance(ScheduleRepository repository) {
    return new ScheduleEditViewModel(repository);
  }
}
