package com.easycash.data.notification

import com.easycash.data.local.NotificationSourceEntity

internal object SupportedNotificationSources {
    fun supports(packageName: String) = packageName == BidvNotificationParser.PACKAGE_NAME || packageName == TimoNotificationParser.PACKAGE_NAME

    fun defaults(now: Long) = listOf(
        NotificationSourceEntity(packageName = BidvNotificationParser.PACKAGE_NAME, displayName = "BIDV SmartBanking", parserId = "bidv-v1", createdAt = now, updatedAt = now),
        NotificationSourceEntity(packageName = TimoNotificationParser.PACKAGE_NAME, displayName = "Timo", parserId = "timo-v1", createdAt = now, updatedAt = now),
    )
}
