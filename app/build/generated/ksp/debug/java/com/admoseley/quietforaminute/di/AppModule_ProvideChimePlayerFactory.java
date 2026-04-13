package com.admoseley.quietforaminute.di;

import android.content.Context;
import com.admoseley.quietforaminute.audio.ChimePlayer;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class AppModule_ProvideChimePlayerFactory implements Factory<ChimePlayer> {
  private final Provider<Context> contextProvider;

  private AppModule_ProvideChimePlayerFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public ChimePlayer get() {
    return provideChimePlayer(contextProvider.get());
  }

  public static AppModule_ProvideChimePlayerFactory create(Provider<Context> contextProvider) {
    return new AppModule_ProvideChimePlayerFactory(contextProvider);
  }

  public static ChimePlayer provideChimePlayer(Context context) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideChimePlayer(context));
  }
}
