package com.viis.rozkamai.di

import com.viis.rozkamai.domain.parser.SmsParser
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object ParserModule {
    /**
     * Provides an empty parser set as the base binding.
     * Individual parsers (GPay, PhonePe, etc.) will be added via @IntoSet in P1-005+.
     * Hilt multibindings will merge them automatically.
     */
    @Provides
    fun provideParserSet(): Set<@JvmSuppressWildcards SmsParser> = emptySet()
}
