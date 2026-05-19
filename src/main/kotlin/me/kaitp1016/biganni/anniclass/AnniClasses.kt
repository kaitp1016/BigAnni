package me.kaitp1016.biganni.anniclass

import me.kaitp1016.biganni.anniclass.impl.AcrobatClass
import me.kaitp1016.biganni.anniclass.impl.AlchemistClass
import me.kaitp1016.biganni.anniclass.impl.ArcherClass
import me.kaitp1016.biganni.anniclass.impl.AssassinClass
import me.kaitp1016.biganni.anniclass.impl.BardClass
import me.kaitp1016.biganni.anniclass.impl.BerserkerClass
import me.kaitp1016.biganni.anniclass.impl.BloodmageClass
import me.kaitp1016.biganni.anniclass.impl.BuilderClass
import me.kaitp1016.biganni.anniclass.impl.ChargerClass
import me.kaitp1016.biganni.anniclass.impl.CivilianClass
import me.kaitp1016.biganni.anniclass.impl.DasherClass
import me.kaitp1016.biganni.anniclass.impl.DefenderClass
import me.kaitp1016.biganni.anniclass.impl.EnchanterClass
import me.kaitp1016.biganni.anniclass.impl.EngineerClass
import me.kaitp1016.biganni.anniclass.impl.FarmerClass
import me.kaitp1016.biganni.anniclass.impl.HandymanClass
import me.kaitp1016.biganni.anniclass.impl.HealerClass
import me.kaitp1016.biganni.anniclass.impl.IcemanClass
import me.kaitp1016.biganni.anniclass.impl.ImmobilizerClass
import me.kaitp1016.biganni.anniclass.impl.LumberjackClass
import me.kaitp1016.biganni.anniclass.impl.MercenaryClass
import me.kaitp1016.biganni.anniclass.impl.MinerClass
import me.kaitp1016.biganni.anniclass.impl.RiftWalkerClass
import me.kaitp1016.biganni.anniclass.impl.ScorpioClass
import me.kaitp1016.biganni.anniclass.impl.ScoutClass
import me.kaitp1016.biganni.anniclass.impl.SniperClass
import me.kaitp1016.biganni.anniclass.impl.SuccubusClass
import me.kaitp1016.biganni.anniclass.impl.SwapperClass
import me.kaitp1016.biganni.anniclass.impl.ThorClass
import me.kaitp1016.biganni.anniclass.impl.TinkererClass
import me.kaitp1016.biganni.anniclass.impl.TransporterClass
import me.kaitp1016.biganni.anniclass.impl.WarriorClass
import me.kaitp1016.biganni.anniclass.impl.WizardClass

object AnniClasses {
    val ALL_CLASSES = mutableListOf<AnniClass>()
    val ANNI_CLASS_MAP = mutableMapOf<String, AnniClass>()

    val CIVILIAN = register("civilian", CivilianClass)
    val MINER = register("miner", MinerClass)
    val SCOUT = register("scout",ScoutClass)
    val TRANSPORTER = register("transporter", TransporterClass)
    val RIFT_WALKER = register("rift_walker", RiftWalkerClass)
    val ENCHANTER = register("enchanter", EnchanterClass)
    val BUILDER = register("builder", BuilderClass)
    val DASHER = register("dasher", DasherClass)
    val ICEMAN = register("iceman", IcemanClass)
    val BERSERKER = register("berserker", BerserkerClass)
    val HEALER = register("healer", HealerClass)
    val TINKERER = register("tinkerer", TinkererClass)
    val IMMOBILIZER = register("immobilizer", ImmobilizerClass)
    val DEFENDER = register("defender", DefenderClass)
    val FARMER = register("farmer", FarmerClass)
    val WARRIOR = register("warrior", WarriorClass)
    val SUCCUBUS = register("succubus", SuccubusClass)
    val THOR = register("thor", ThorClass)
    val ASSASSIN = register("assassin", AssassinClass)
    val ALCHEMIST = register("alchemist", AlchemistClass)
    val SCORPIO = register("scorpio", ScorpioClass)
    val SWAPPER = register("swapper", SwapperClass)
    val ENGINEER = register("engineer", EngineerClass)
    val ACROBAT = register("acrobat", AcrobatClass)
    val BARD = register("bard", BardClass)
    val CHARGER = register("charger", ChargerClass)
    val LUMBERJACK = register("lumberjack", LumberjackClass)
    val ARCHER = register("archer", ArcherClass)
    val SNIPER = register("sniper", SniperClass)
    val WIZARD = register("wizard", WizardClass)
    val BLOODMAGE = register("bloodmage", BloodmageClass)
    val HANDYMAN = register("handyman", HandymanClass)
    val MERCENARY = register("mercenary", MercenaryClass)

    fun <T: AnniClass> register(id: String, anniClass: T): T {
        ALL_CLASSES.add(anniClass)
        ANNI_CLASS_MAP[id] = anniClass
        anniClass.register()

        return anniClass
    }
}