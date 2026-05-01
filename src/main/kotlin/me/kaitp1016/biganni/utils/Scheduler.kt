package me.kaitp1016.biganni.utils

import com.destroystokyo.paper.event.server.ServerTickStartEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

object Scheduler: Listener {
    private class Task (var ticksLeft: Int,val task: () -> Unit)

    private var scheduledTasks = ArrayList<Task>()

    fun scheduleTask(tick: Int, task:() -> Unit) {
        scheduledTasks.add(Task(tick, task))
    }

    @EventHandler
    fun onTick(event: ServerTickStartEvent) {
        val currentTasks = scheduledTasks
        scheduledTasks = ArrayList(currentTasks.size)

        currentTasks.forEach {
            it.ticksLeft--

            if (it.ticksLeft < 1) {
                it.task()
            }
            else {
                scheduledTasks.add(it)
            }
        }
    }
}