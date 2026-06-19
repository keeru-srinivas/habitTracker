package com.example.cozytrack.core.di

import com.example.cozytrack.data.repository.AuthRepositoryImpl
import com.example.cozytrack.data.repository.HabitRepositoryImpl
import com.example.cozytrack.data.repository.ThoughtRepositoryImpl
import com.example.cozytrack.domain.repository.AuthRepository
import com.example.cozytrack.domain.repository.HabitRepository
import com.example.cozytrack.domain.repository.ThoughtRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindHabitRepository(impl: HabitRepositoryImpl): HabitRepository

    @Binds
    @Singleton
    abstract fun bindThoughtRepository(impl: ThoughtRepositoryImpl): ThoughtRepository
}
