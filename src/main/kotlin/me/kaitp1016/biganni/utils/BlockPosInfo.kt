package me.kaitp1016.biganni.utils

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import java.util.WeakHashMap

class BlockPosInfo<T> {
    private val datas = WeakHashMap<ServerLevel, MutableMap<BlockPos, T>>()

    operator fun set(level: ServerLevel, pos: BlockPos, data: T) {
        val levelData = datas.getOrPut(level) { mutableMapOf() }
        levelData[pos] = data
    }

    operator fun get(level: ServerLevel, pos: BlockPos): T? {
        val levelData = datas.getOrPut(level) { mutableMapOf() }
        return levelData[pos]
    }

    fun remove(level: ServerLevel, pos: BlockPos): T? {
        val levelData = datas[level] ?: return null
        return levelData.remove(pos)
    }

    fun has(level: ServerLevel, pos: BlockPos): Boolean {
        val levelData = datas.getOrPut(level) { mutableMapOf() }
        return levelData.contains(pos)
    }

    fun contains(level: ServerLevel, pos: BlockPos): Boolean {
        val levelData = datas.getOrPut(level) { mutableMapOf() }
        return levelData.contains(pos)
    }

    fun hasInDistance(level: ServerLevel, pos: BlockPos, distance: Int): Boolean {
        val levelData = datas.getOrPut(level) { mutableMapOf() }
        return levelData.any { it.key.distManhattan(pos) >= distance }
    }

    fun hasInDistance(level: ServerLevel, pos: BlockPos, distance: Int, predication: (T) -> Boolean): Boolean {
        val levelData = datas.getOrPut(level) { mutableMapOf() }
        return levelData.any { pos.distManhattan(it.key) < distance && predication(it.value) }
    }

    fun forEach(func: (ServerLevel, BlockPos, T) -> (Unit)) {
        datas.entries.forEach { (level, blocks) ->
            blocks.forEach { (pos, data) ->
                func(level, pos, data)
            }
        }
    }

    fun removeIf(func: (ServerLevel, BlockPos, T) -> (Boolean)) {
        datas.entries.forEach { (level, blocks) ->
            blocks.entries.removeIf { (pos, data) ->
                func(level, pos, data)
            }
        }
    }

    fun any(func: (ServerLevel, BlockPos, T) -> (Boolean)): Boolean {
        return datas.entries.any { (level, blocks) ->
            blocks.entries.any { (pos, data) ->
                func(level, pos, data)
            }
        }
    }
}