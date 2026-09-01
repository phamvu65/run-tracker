package com.example.runtracker.di

import javax.inject.Qualifier

/** Đánh dấu CoroutineDispatcher dùng cho I/O (Room, file, network). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher
