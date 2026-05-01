package me.kaitp1016.biganni.modifiers

import com.destroystokyo.paper.event.server.ServerTickStartEvent
import me.kaitp1016.biganni.mc
import me.kaitp1016.biganni.utils.MCUtils.toMC
import net.minecraft.core.BlockPos
import net.minecraft.core.NonNullList
import net.minecraft.network.chat.Component
import net.minecraft.util.CommonColors
import net.minecraft.util.Mth
import net.minecraft.world.MenuProvider
import net.minecraft.world.SimpleMenuProvider
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.BlastFurnaceMenu
import net.minecraft.world.inventory.ContainerData
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.AbstractCookingRecipe
import net.minecraft.world.item.crafting.RecipeHolder
import net.minecraft.world.item.crafting.RecipeManager
import net.minecraft.world.item.crafting.SingleRecipeInput
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlastFurnaceBlockEntity
import net.minecraft.world.level.block.entity.FuelValues
import org.bukkit.Material
import org.bukkit.craftbukkit.block.CraftBlock
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.inventory.FurnaceSmeltEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.CookingRecipe
import java.util.UUID
import kotlin.math.min

object EnderFurnace: Listener {
    val furnaces = mutableMapOf<UUID, EnderBlastFurnaceBlockEntity>()
    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return

        val block = event.clickedBlock ?: return
        if (block.type != Material.BLAST_FURNACE) return

        val player = event.player.toMC()
        val level = player.level()
        if ((level.getBlockEntity(BlockPos(block.x,block.y,block.z)) as? BlastFurnaceBlockEntity)?.name?.string?.contains("Ender Blast Furnace") != true) return

        val furnace = furnaces.getOrPut(player.uuid) { EnderBlastFurnaceBlockEntity() }
        player.openMenu(furnace)

        event.isCancelled = true
    }

    @EventHandler
    fun onTick(event: ServerTickStartEvent) {
        if (furnaces.isEmpty()) return

        furnaces.forEach {
            it.value.onTick()
        }
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        furnaces.remove(event.player.uniqueId)
    }

    class EnderBlastFurnaceBlockEntity: BlastFurnaceBlockEntity, MenuProvider {
        constructor():super(BlockPos.ZERO, Blocks.BLAST_FURNACE.defaultBlockState()) {
        }

        val quickCheckEnder = RecipeManager.createCheck(recipeType)

        fun onTick() {
            var isLit: Boolean
            if (litTimeRemaining > 0) {
                --litTimeRemaining
                isLit = litTimeRemaining > 0
            } else {
                isLit = false
            }

            val fuel = items[1]
            val ingredient = items[0]
            val hasIngredient = !ingredient.isEmpty
            val hasFuel = !fuel.isEmpty
            if (hasFuel && hasIngredient) {
                if (hasIngredient) {
                    val input = SingleRecipeInput(ingredient)
                    val recipe = quickCheckEnder.getRecipeFor(input, mc.overworld()).orElse(null)
                    if (recipe != null) {
                        val maxStackSize = getMaxStackSize()
                        val burnResult = recipe.value().assemble(input)
                        if (!burnResult.isEmpty && canBurn(items, maxStackSize, burnResult)) {
                            if (!isLit) {
                                val newLitTime = getBurnDuration(mc.fuelValues(), fuel)

                                litTimeRemaining = newLitTime

                                if (newLitTime > 0) {
                                    consumeFuel(items, fuel)

                                    isLit = true
                                }
                            }

                            if (isLit) {
                                if (cookingTimer == 0) {
                                    cookingTotalTime = getTotalCookTime(mc.overworld(), this, recipeType, cookSpeedMultiplier)
                                }

                                ++cookingTimer
                                if (cookingTimer >= cookingTotalTime) {
                                    cookingTimer = 0
                                    cookingTotalTime = getTotalCookTime(mc.overworld(), this, recipeType, cookSpeedMultiplier)
                                    if (burn(items, ingredient, burnResult, recipe, level, worldPosition)) {
                                        recipeUsed = recipe
                                    }
                                }
                            } else {
                                cookingTimer = 0
                            }
                        } else {
                            cookingTimer = 0
                        }
                    }
                } else {
                    cookingTimer = 0
                }
            } else if (cookingTimer > 0) {
                cookingTimer = Mth.clamp(cookingTimer - 2, 0, cookingTotalTime)
            }
        }

        override fun createMenu(containerId: Int, inventory: Inventory, player: Player): AbstractContainerMenu? {
            return BlastFurnaceMenu(containerId, inventory, this, this.dataAccess)
        }

        override fun getDisplayName(): Component {
            return Component.literal("Ender Blast Furnace").withColor(CommonColors.DARK_PURPLE)
        }

        override fun getBurnDuration(fuelValues: FuelValues, itemStack: ItemStack): Int {
            return super.getBurnDuration(fuelValues, itemStack) / 2
        }

        override fun stillValid(player: Player): Boolean {
            return true
        }


        companion object {
            fun canBurn(items:List<ItemStack>,maxStackSize:Int, burnResult:ItemStack): Boolean {
                val resultItemStack = items[2]
                if (resultItemStack.isEmpty()) {
                    return true
                } else if (!ItemStack.isSameItemSameComponents(resultItemStack, burnResult)) {
                    return false
                } else {
                    val resultCount: Int = resultItemStack.getCount() + burnResult.count()
                    val maxResultCount: Int = min(maxStackSize, burnResult.getMaxStackSize())
                    return resultCount <= maxResultCount
                }
            }

            fun consumeFuel(items: NonNullList<ItemStack>, fuel: ItemStack) {
                val fuelItem = fuel.getItem()
                fuel.shrink(1)
                if (fuel.isEmpty()) {
                    val remainder = fuelItem.getCraftingRemainder()
                    items.set(1, if (remainder != null) remainder.create() else ItemStack.EMPTY)
                }
            }

            private fun burn(items: NonNullList<ItemStack>, inputItemStack: ItemStack, result: ItemStack, recipe: RecipeHolder<out AbstractCookingRecipe>, level: Level?, blockPos: BlockPos): Boolean {
                var result = result
                val resultItemStack = items.get(2)
                val apiIngredient = CraftItemStack.asCraftMirror(inputItemStack)
                var apiResult = CraftItemStack.asBukkitCopy(result)
                val furnaceSmeltEvent = FurnaceSmeltEvent(CraftBlock.at(level, blockPos), apiIngredient, apiResult, recipe.toBukkitRecipe() as CookingRecipe<*>)
                if (!furnaceSmeltEvent.callEvent()) {
                    return false
                } else {
                    apiResult = furnaceSmeltEvent.getResult()
                    result = CraftItemStack.asNMSCopy(apiResult)
                    if (!result.isEmpty()) {
                        if (resultItemStack.isEmpty()) {
                            items.set(2, result.copy())
                        } else {
                            if (!CraftItemStack.asCraftMirror(resultItemStack).isSimilar(apiResult)) {
                                return false
                            }

                            resultItemStack.grow(result.getCount())
                        }
                    }

                    if (inputItemStack.`is`(Items.WET_SPONGE) && !items.get(1).isEmpty() && items.get(1).`is`(Items.BUCKET)) {
                        items.set(1, ItemStack(Items.WATER_BUCKET))
                    }

                    inputItemStack.shrink(1)
                    return true
                }
            }

        }
    }
}