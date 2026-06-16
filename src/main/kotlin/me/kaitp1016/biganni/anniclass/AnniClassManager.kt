package me.kaitp1016.biganni.anniclass

import org.bukkit.entity.Player
import java.util.UUID

object AnniClassManager {
    val classes = mutableMapOf<UUID, AnniClass>()

    fun Player.getAnniClass(): AnniClass? {
        return classes[this.uniqueId]
    }

    fun Player.isClassSelected(): Boolean {
        return classes[this.uniqueId] != null
    }

    fun Player.isAnniClass(anniClass: AnniClass): Boolean {
        return classes[uniqueId] === anniClass
    }

    fun Player.selectAnniClass(anniClass: AnniClass) {
        val originalClass = classes[uniqueId]
        originalClass?.onUnselect(this)

        classes[uniqueId] = anniClass
        anniClass.onSelect(this)
    }
}