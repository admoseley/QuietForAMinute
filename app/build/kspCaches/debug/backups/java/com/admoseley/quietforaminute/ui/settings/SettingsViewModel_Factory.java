package com.admoseley.quietforaminute.ui.settings;

import com.admoseley.quietforaminute.data.datastore.PreferencesRepository;
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
public final class SettingsViewModel_Factory implements Factory<SettingsViewModel> {
  private final Provider<PreferencesRepository> prefsRepositoryProvider;

  private SettingsViewModel_Factory(Provider<PreferencesRepository> prefsRepositoryProvider) {
    this.prefsRepositoryProvider = prefsRepositoryProvider;
  }

  @Override
  public SettingsViewModel get() {
    return newInstance(prefsRepositoryProvider.get());
  }

  public static SettingsViewModel_Factory create(
      Provider<PreferencesRepository> prefsRepositoryProvider) {
    return new SettingsViewModel_Factory(prefsRepositoryProvider);
  }

  public static SettingsViewModel newInstance(PreferencesRepository prefsRepository) {
    return new SettingsViewModel(prefsRepository);
  }
}
