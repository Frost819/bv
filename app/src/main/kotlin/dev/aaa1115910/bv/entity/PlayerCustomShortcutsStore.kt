package dev.aaa1115910.bv.entity

import dev.aaa1115910.bv.util.Prefs

object PlayerCustomShortcutsStore {
    fun get(): List<PlayerCustomShortcut> {
        return PlayerCustomShortcutsCodec.parse(Prefs.playerCustomShortcuts)
    }

    fun getByKey(): Map<Int, PlayerCustomShortcut> {
        return get().associateBy { it.keyCode }
    }

    fun save(shortcuts: List<PlayerCustomShortcut>): List<PlayerCustomShortcut> {
        val normalized = PlayerCustomShortcutsCodec.normalize(shortcuts)
        Prefs.playerCustomShortcuts = PlayerCustomShortcutsCodec.serialize(normalized)
        return normalized
    }

    fun upsert(
        keyCode: Int,
        action: PlayerCustomShortcutAction
    ): List<PlayerCustomShortcut> {
        val next = get()
            .filterNot { it.keyCode == keyCode }
            .plus(PlayerCustomShortcut(keyCode, action))
        return save(next)
    }

    fun remove(keyCode: Int): List<PlayerCustomShortcut> {
        return save(get().filterNot { it.keyCode == keyCode })
    }

    fun clear(): List<PlayerCustomShortcut> {
        return save(emptyList())
    }
}
