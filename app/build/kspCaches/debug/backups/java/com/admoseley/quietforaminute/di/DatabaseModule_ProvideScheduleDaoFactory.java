package com.admoseley.quietforaminute.di;

import com.admoseley.quietforaminute.data.db.AppDatabase;
import com.admoseley.quietforaminute.data.db.ScheduleDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class DatabaseModule_ProvideScheduleDaoFactory implements Factory<ScheduleDao> {
  private final Provider<AppDatabase> dbProvider;

  private DatabaseModule_ProvideScheduleDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public ScheduleDao get() {
    return provideScheduleDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideScheduleDaoFactory create(Provider<AppDatabase> dbProvider) {
    return new DatabaseModule_ProvideScheduleDaoFactory(dbProvider);
  }

  public static ScheduleDao provideScheduleDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideScheduleDao(db));
  }
}
