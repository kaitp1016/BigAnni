package me.kaitp1016.biganni.game

import me.kaitp1016.biganni.plugin
import org.bukkit.Bukkit
import org.bukkit.GameRule
import org.bukkit.World
import org.bukkit.WorldCreator
import org.bukkit.entity.Player
import java.io.File
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.BasicFileAttributes

/**
 * 試合中に破壊/設置されたブロックを、サーバーを再起動せずに「試合開始前のきれいな状態」へ
 * 戻すためのマネージャー。
 *
 * 使い方:
 *   1. 荒らされていないきれいな状態のマップで `/anni savemapbackup` を一度実行し、
 *      そのワールドの region / entities / poi データを plugins/BigAnni/map_backups/<ワールド名>/ に保存する。
 *   2. 試合が終わったら `/anni resetmap` を実行する。
 *      -> ワールド内のプレイヤーを待機用ワールド(自動生成される void ワールド)へ退避
 *      -> 対象ワールドをアンロード
 *      -> 保存しておいたバックアップのファイルで region/entities/poi を上書き
 *      -> ワールドを再ロードし、退避していたプレイヤーを新しいスポーン地点へ戻す
 *   3. 完了後、Game.map を Config から再読み込みする(ワールドの再ロードで World/Location
 *      オブジェクトが作り直されるため)。これは AnniCommand 側で行っている。
 *
 * 注意:
 *   - この処理はワールドの un/reload を伴うため、対象ワールド内にいるプレイヤーは一時的に
 *     テレポートされます(専用のロビーワールドが無い運用とのことだったため、このクラスが
 *     void の待機ワールドを自動生成します)。
 *   - 「sys:coastal」のようにワールドが何らかの独自のマルチワールド管理の仕組みで
 *     ロードされている場合、WorldCreator での再生成が完全に元の設定(generator 等)を
 *     再現できない可能性があります。本番運用に組み込む前に、必ずテスト環境で
 *     `/anni savemapbackup` → 試合 → `/anni resetmap` の一連の流れを確認してください。
 *   - region フォルダのコピーはディスク I/O のためワールドサイズによっては数百ms〜数秒
 *     メインスレッドをブロックします(アンロード中なので安全ですが、体感のラグにはなります)。
 */
object MapResetManager {
    private const val HOLDING_WORLD_NAME = "biganni_holding"
    private val BACKUPS_DIRECTORY = File(plugin.dataFolder, "map_backups")
    private val RESETTABLE_SUBDIRECTORIES = listOf("region", "entities", "poi")

    var isResetting = false
        private set

    /**
     * 現在ロードされている [world] の状態を「きれいな状態」としてバックアップに保存する。
     * 試合開始前、ブロックが一切荒らされていないタイミングで実行すること。
     */
    fun saveBackup(world: World): Boolean {
        world.save()

        val worldFolder = world.worldFolder
        val backupFolder = File(BACKUPS_DIRECTORY, world.name)

        RESETTABLE_SUBDIRECTORIES.forEach { sub ->
            val src = File(worldFolder, sub)
            val dst = File(backupFolder, sub)

            deleteRecursively(dst)

            if (src.exists()) {
                copyRecursively(src.toPath(), dst.toPath())
            }
        }

        return true
    }

    /**
     * バックアップがすでに保存されているかどうか。
     */
    fun hasBackup(worldName: String): Boolean {
        return File(BACKUPS_DIRECTORY, worldName).exists()
    }

    /**
     * [worldName] のワールドを、保存済みのバックアップの状態までリセットする。
     * 完了したら再ロード後の [World] を引数に [onComplete] を呼び出す。
     *
     * サーバーの再起動は不要だが、対象ワールド内にいたプレイヤーは処理の間
     * 待機用ワールドへ一時的にテレポートされる。
     */
    fun resetWorld(worldName: String, onComplete: (World) -> Unit) {
        if (isResetting) {
            Bukkit.getLogger().warning("[BigAnni] すでにマップのリセット処理中です。")
            return
        }

        val backupFolder = File(BACKUPS_DIRECTORY, worldName)
        if (!backupFolder.exists()) {
            Bukkit.getLogger().warning("[BigAnni] $worldName のバックアップが見つかりません。先に /anni savemapbackup を実行してください。")
            return
        }

        isResetting = true

        val existingWorld = Bukkit.getWorld(worldName)
        val holdingLocation = getHoldingWorld().spawnLocation

        val environment = existingWorld?.environment
        val seed = existingWorld?.seed
        val generator = existingWorld?.generator

        val displacedPlayers: List<Player> = existingWorld?.players?.toList() ?: emptyList()
        displacedPlayers.forEach { it.teleport(holdingLocation) }

        if (existingWorld != null) {
            // プレイヤー以外(モブ・ドロップアイテム・矢など)は再ロード後に残っていると
            // 位置がズレるため、アンロード前に片付ける。
            existingWorld.entities.forEach { if (it !is Player) it.remove() }

            val unloaded = Bukkit.unloadWorld(existingWorld, false)
            if (!unloaded) {
                Bukkit.getLogger().warning("[BigAnni] $worldName のアンロードに失敗しました。マップのリセットを中断します。")
                isResetting = false
                return
            }
        }

        val worldFolder = File(Bukkit.getWorldContainer(), worldName)

        RESETTABLE_SUBDIRECTORIES.forEach { sub ->
            val dst = File(worldFolder, sub)
            deleteRecursively(dst)

            val src = File(backupFolder, sub)
            if (src.exists()) {
                copyRecursively(src.toPath(), dst.toPath())
            }
        }

        val creator = WorldCreator(worldName)
        environment?.let { creator.environment(it) }
        seed?.let { creator.seed(it) }
        generator?.let { creator.generator(it) }

        val newWorld = creator.createWorld()
        if (newWorld == null) {
            Bukkit.getLogger().warning("[BigAnni] $worldName の再ロードに失敗しました。")
            isResetting = false
            return
        }

        displacedPlayers.forEach { it.teleport(newWorld.spawnLocation) }

        isResetting = false
        onComplete(newWorld)
    }

    private fun getHoldingWorld(): World {
        Bukkit.getWorld(HOLDING_WORLD_NAME)?.let { return it }

        val world = WorldCreator(HOLDING_WORLD_NAME).apply {
            type(org.bukkit.WorldType.FLAT)
            generateStructures(false)
            generatorSettings("""{"layers":[{"block":"minecraft:air","height":1}],"biome":"minecraft:the_void"}""")
        }.createWorld()!!

        world.setGameRule(GameRule.DO_MOB_SPAWNING, false)
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false)
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false)
        world.setSpawnLocation(0, 65, 0)

        return world
    }

    private fun deleteRecursively(file: File) {
        if (!file.exists()) return
        file.walkBottomUp().forEach { it.delete() }
    }

    private fun copyRecursively(src: Path, dst: Path) {
        Files.walkFileTree(src, object : SimpleFileVisitor<Path>() {
            override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                Files.createDirectories(dst.resolve(src.relativize(dir)))
                return FileVisitResult.CONTINUE
            }

            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                Files.copy(file, dst.resolve(src.relativize(file)), StandardCopyOption.REPLACE_EXISTING)
                return FileVisitResult.CONTINUE
            }
        })
    }
}
