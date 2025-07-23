package com.github.wass3r.intellijvelaplugin.utils

object VelaEvent {
    private const val PUSH = "push"
    const val PULL_REQUEST = "pull_request"
    const val DEPLOYMENT = "deployment"
    const val COMMENT = "comment"
    private const val SCHEDULE = "schedule"
    private const val TAG = "tag"
    const val DELETE = "delete"

    val ALL = listOf(PUSH, PULL_REQUEST, DEPLOYMENT, COMMENT, SCHEDULE, TAG, DELETE)
}

object VelaAction {
    // Pull request actions
    private const val OPENED = "opened"
    private const val REOPENED = "reopened"
    private const val SYNCHRONIZE = "synchronize"
    private const val EDITED = "edited"
    private const val CLOSED = "closed"
    private const val LABELED = "labeled"
    private const val UNLABELED = "unlabeled"
    private const val REVIEW_REQUESTED = "review_requested"
    private const val REVIEW_REQUEST_REMOVED = "review_request_removed"

    // Comment actions
    private const val CREATED = "created"
    private const val UPDATED = "updated"

    // Deployment actions
    private const val DEPLOYMENT_CREATED = "created"

    // Delete actions
    private const val BRANCH = "branch"
    private const val TAG_ACTION = "tag"

    // Map of events to their valid actions based on Vela server constants
    val EVENT_ACTIONS = mapOf(
        VelaEvent.PULL_REQUEST to listOf(
            OPENED, REOPENED, SYNCHRONIZE, EDITED, CLOSED,
            LABELED, UNLABELED,
            REVIEW_REQUESTED, REVIEW_REQUEST_REMOVED
        ),
        VelaEvent.COMMENT to listOf(CREATED, UPDATED),
        VelaEvent.DEPLOYMENT to listOf(DEPLOYMENT_CREATED),
        VelaEvent.DELETE to listOf(BRANCH, TAG_ACTION)
    )
}

object VelaUI {
    // Common spacing values
    const val SMALL_SPACING = 4
    const val MEDIUM_SPACING = 8
    const val LARGE_SPACING = 16
    
    // Common padding values
    const val PANEL_PADDING = 8
    const val CONTENT_PADDING = 12
    
    // Common dimensions
    const val SIDEBAR_WIDTH = 350
    const val SIDEBAR_MIN_WIDTH = 300
    const val PANEL_MAX_WIDTH = 320
    const val COMPONENT_WIDTH = 280
}