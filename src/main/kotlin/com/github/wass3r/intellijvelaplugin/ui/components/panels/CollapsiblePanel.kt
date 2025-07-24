package com.github.wass3r.intellijvelaplugin.ui.components.panels

import com.intellij.icons.AllIcons
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import java.awt.*
import java.awt.event.*
import javax.swing.*

/**
 * A collapsible panel component that can show/hide its content with a clickable header.
 */
class CollapsiblePanel(
    title: String,
    private val content: JComponent,
    isInitiallyExpanded: Boolean = true
) : JPanel() {

    private var isExpanded = isInitiallyExpanded
    private val headerPanel = JPanel(BorderLayout())
    private val expandIcon = JBLabel()
    private val titleLabel = JBLabel(title)
    private val contentPane = JPanel()
    
    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        alignmentX = Component.LEFT_ALIGNMENT
        
        setupHeader()
        setupContent()
        updateExpandedState()
    }

    private fun setupHeader() {
        headerPanel.apply {
            background = JBUI.CurrentTheme.DefaultTabs.background()
            border = JBUI.Borders.customLine(JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground(), 0, 0, 1, 0)
            minimumSize = Dimension(0, 28)
            preferredSize = Dimension(Int.MAX_VALUE, 28)
            maximumSize = Dimension(Int.MAX_VALUE, 28)
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            alignmentX = Component.LEFT_ALIGNMENT
        }

        expandIcon.apply {
            icon = if (isExpanded) AllIcons.General.ArrowDown else AllIcons.General.ArrowRight
            border = JBUI.Borders.empty(4, 8, 4, 4)
        }

        titleLabel.apply {
            font = font.deriveFont(Font.BOLD)
            border = JBUI.Borders.empty(4, 0, 4, 8)
        }

        headerPanel.add(expandIcon, BorderLayout.WEST)
        headerPanel.add(titleLabel, BorderLayout.CENTER)

        val clickListener = object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                toggle()
            }
        }
        headerPanel.addMouseListener(clickListener)
        expandIcon.addMouseListener(clickListener)
        titleLabel.addMouseListener(clickListener)

        add(headerPanel)
    }

    private fun setupContent() {
        content.border = JBUI.Borders.empty(8)
        
        contentPane.layout = BorderLayout()
        contentPane.add(content, BorderLayout.CENTER)
        contentPane.alignmentX = Component.LEFT_ALIGNMENT
        
        // Create a panel with custom sizing that respects the expanded state
        val sizedPanel = object : JPanel(BorderLayout()) {
            override fun getMaximumSize(): Dimension {
                return if (isExpanded) {
                    val prefSize = preferredSize
                    Dimension(Int.MAX_VALUE, prefSize.height)
                } else {
                    Dimension(Int.MAX_VALUE, 0)
                }
            }
            
            override fun getPreferredSize(): Dimension {
                return if (isExpanded) {
                    val contentSize = content.preferredSize
                    Dimension(contentSize.width, contentSize.height + 16)
                } else {
                    Dimension(super.getPreferredSize().width, 0)
                }
            }
        }
        sizedPanel.add(contentPane, BorderLayout.CENTER)
        sizedPanel.alignmentX = Component.LEFT_ALIGNMENT
        
        add(sizedPanel)
    }

    private fun updateExpandedState() {
        expandIcon.icon = if (isExpanded) AllIcons.General.ArrowDown else AllIcons.General.ArrowRight
        contentPane.isVisible = isExpanded
        
        invalidate()
        
        var topParent = parent
        while (topParent?.parent != null) {
            topParent = topParent.parent
        }
        
        if (topParent != null) {
            topParent.revalidate()
            topParent.repaint()
        } else if (parent != null) {
            parent.revalidate() 
            parent.repaint()
        } else {
            revalidate()
            repaint()
        }
    }

    fun toggle() {
        isExpanded = !isExpanded
        updateExpandedState()
    }

    fun updateTitle(newTitle: String) {
        titleLabel.text = newTitle
        titleLabel.revalidate()
        titleLabel.repaint()
    }
}
