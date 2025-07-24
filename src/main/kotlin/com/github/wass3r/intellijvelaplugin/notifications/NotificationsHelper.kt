package com.github.wass3r.intellijvelaplugin.notifications

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

/**
 * Helper for displaying notifications in the IDE.
 */
object NotificationsHelper {
    private const val GROUP_ID = "Vela Notifications"

    /**
     * Shows an error notification.
     */
    fun notifyError(project: Project?, title: String, content: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup(GROUP_ID)
            .createNotification(title, content, NotificationType.ERROR)
            .notify(project)
    }

    /**
     * Shows a success notification.
     */
    fun notifySuccess(project: Project?, title: String, content: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup(GROUP_ID)
            .createNotification(title, content, NotificationType.INFORMATION)
            .setImportant(false)
            .notify(project)
    }
}