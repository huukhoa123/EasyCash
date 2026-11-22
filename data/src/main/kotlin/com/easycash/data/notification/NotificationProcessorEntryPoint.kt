package com.easycash.data.notification

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface NotificationProcessorEntryPoint {
    fun notificationProcessor(): NotificationProcessor
    fun database(): com.easycash.data.local.EasyCashDatabase
}
