package com.github.wass3r.intellijvelaplugin.settings

import com.intellij.openapi.components.*
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "com.github.wass3r.intellijvelaplugin.settings.VelaSettings",
    storages = [Storage("VelaSettings.xml")]
)
class VelaSettings : PersistentStateComponent<VelaSettings> {
    var velaCliPath: String = "vela" // Default to global vela command

    override fun getState(): VelaSettings = this

    override fun loadState(state: VelaSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        fun getInstance(): VelaSettings = service()
    }
}