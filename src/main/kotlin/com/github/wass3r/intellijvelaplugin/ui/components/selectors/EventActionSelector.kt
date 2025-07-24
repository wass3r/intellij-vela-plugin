package com.github.wass3r.intellijvelaplugin.ui.components.selectors

import com.github.wass3r.intellijvelaplugin.utils.VelaAction
import com.github.wass3r.intellijvelaplugin.utils.VelaEvent
import com.intellij.openapi.ui.ComboBox
import java.awt.event.ItemEvent
import java.awt.event.ItemListener
import javax.swing.DefaultComboBoxModel
import javax.swing.SwingUtilities

/**
 * Component for selecting Vela event types and their associated actions.
 * Provides two linked combo boxes where selecting an event type filters the available actions.
 */
class EventActionSelector {
    private val events = listOf("") + VelaEvent.ALL  // Add empty option at start
    private val eventComboBox = ComboBox<String>()
    private val actionComboBox = ComboBox<String>()
    private var currentEvent: String = ""  // Default to empty
    private val externalListeners = mutableListOf<(String) -> Unit>()
    private var updating = false
    private var programmaticUpdate = false

    init {
        // Set up the event combo box with events
        val eventModel = DefaultComboBoxModel<String>()
        events.forEach { eventModel.addElement(it) }
        eventComboBox.model = eventModel

        // Ensure both combo boxes are always enabled and focusable
        eventComboBox.isEnabled = true
        eventComboBox.isFocusable = true
        actionComboBox.isEnabled = true
        actionComboBox.isFocusable = true

        // Using ItemListener instead of ActionListener to catch both programmatic and user changes
        eventComboBox.addItemListener(object : ItemListener {
            override fun itemStateChanged(e: ItemEvent) {
                if (e.stateChange != ItemEvent.SELECTED || programmaticUpdate) return

                // To avoid UI freezes, execute on EDT
                if (!SwingUtilities.isEventDispatchThread()) {
                    SwingUtilities.invokeLater { handleEventSelection() }
                } else {
                    handleEventSelection()
                }
            }
        })

        // Initialize with empty selection
        updateActionComboBox("")

        // Set reasonable sizes for the combo boxes
        eventComboBox.preferredSize = java.awt.Dimension(250, eventComboBox.preferredSize.height)
        eventComboBox.minimumSize = java.awt.Dimension(200, eventComboBox.preferredSize.height)

        actionComboBox.preferredSize = java.awt.Dimension(250, actionComboBox.preferredSize.height)
        actionComboBox.minimumSize = java.awt.Dimension(200, actionComboBox.preferredSize.height)
    }

    /**
     * Safely handles event selection changes by scheduling them on EDT
     */
    private fun handleEventSelection() {
        if (updating) return

        try {
            updating = true
            val selectedEvent = eventComboBox.selectedItem as? String ?: ""
            if (selectedEvent != currentEvent) {
                updateActionComboBox(selectedEvent)
                notifyExternalListeners(selectedEvent)
            }
        } finally {
            updating = false
        }
    }

    /**
     * Notifies external event listeners about event changes
     */
    private fun notifyExternalListeners(selectedEvent: String) {
        for (listener in externalListeners) {
            try {
                listener(selectedEvent)
            } catch (e: Exception) {
                // Prevent listener exceptions from breaking UI
                e.printStackTrace()
            }
        }
    }

    /**
     * Updates the action combo box based on the selected event.
     * When an event is selected, only shows actions that are valid for that event.
     */
    private fun updateActionComboBox(selectedEvent: String) {
        currentEvent = selectedEvent
        val actions = when {
            selectedEvent.isEmpty() -> listOf("") // Always include empty option
            else -> listOf("") + (VelaAction.EVENT_ACTIONS[selectedEvent] ?: emptyList())
        }

        try {
            // Set flag to prevent recursive item events during model update
            programmaticUpdate = true
            actionComboBox.model = DefaultComboBoxModel(actions.toTypedArray())
            // Always keep the action combo box enabled so users can interact with it
            actionComboBox.isEnabled = true
            actionComboBox.isFocusable = true
            // Select first item by default
            if (actions.isNotEmpty()) {
                actionComboBox.selectedIndex = 0
            }
        } finally {
            programmaticUpdate = false
        }
    }

    /**
     * Returns the current event and action selection formatted for the Vela CLI.
     * Format: "event:action" if both are selected, or just "event" if only event is selected.
     *
     * @return The formatted event/action string or null if nothing is selected
     */
    fun getCurrentEventAction(): String? {
        if (currentEvent.isEmpty()) return null

        val action = actionComboBox.selectedItem as? String
        return when {
            action.isNullOrEmpty() -> currentEvent
            else -> "$currentEvent:$action"
        }
    }

    /** Gets the event combo box component */
    fun getEventComboBox() = eventComboBox

    /** Gets the action combo box component */
    fun getActionComboBox() = actionComboBox

    /**
     * Registers an external listener for event selection changes.
     * This is safer than directly adding action listeners to the combo box.
     */
    fun addEventSelectionListener(listener: (String) -> Unit) {
        externalListeners.add(listener)

        // Immediately notify the listener of the current state
        if (currentEvent.isNotEmpty()) {
            listener(currentEvent)
        }
    }
}