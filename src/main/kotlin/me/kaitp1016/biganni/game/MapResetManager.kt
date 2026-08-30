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
 *      -> 対象ワールドの読み込み済みチャンクを「保存せずに」メモリから解放
 *      -> 保存しておいたバックアップのファイルで region/entities/poi を上書き
 *      -> 退避していたプレイヤーを元のスポーン地点へ戻す(再アクセス時にディスクから読み直される)
 *
 * 注意:
 *   - 対象ワールドは「データパック等で追加されたカスタムディメンション」であっても動作するように、
 *     World オブジェクト自体はアンロード/再生成せず、チャンク単位でメモリを破棄する方式を採っています。
 *     (Bukkit の WorldCreator.createWorld() は Environment.CUSTOM のワールドの再生成を許可していない
 *     ため、当初の「ワールドごとアンロードして作り直す」方式はカスタムディメンションでは
 *     IllegalArgumentException: Illegal dimension (CUSTOM) で失敗します。)
 *   - 対象ワールド内にいるプレイヤーは一時的にテレポートされます(専用のロビーワールドが無い運用との
 *     ことだったため、このクラスが void の待機ワールドを自動生成します)。
 *   - region フォルダのコピーはディスク I/O のためワールドサイズによっては数百ms〜数秒
 *     メインスレッドをブロックします(処理中は誰もいない状態のはずなので実害は小さいですが、
 *     体感のラグにはなり得ます)。
 *   - チャンクの解放(save=false)は、解放した瞬間から再アクセスされるまでの間、そのチャンクの
 *     ブロック情報はディスク上のファイルが正になります。解放が完了してからファイルを差し替える
 *     必要があるため、この2つの処理の順序は変えないでください。
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
     * デバッグ用: 実際に読み書きしようとしているパスと、そこに何があるかを文字列のリストで返す。
     * パスの推測がずれていないか確認するために使う。
     */
    fun debugInfo(world: World): List<String> {
        val lines = mutableListOf<String>()

        lines.add("world.name = ${world.name}")
        lines.add("world.key = ${world.key}")
        lines.add("world.environment = ${world.environment}")
        lines.add("world.worldFolder(絶対パス) = ${world.worldFolder.absolutePath}")

        RESETTABLE_SUBDIRECTORIES.forEach { sub ->
            val dir = File(world.worldFolder, sub)
            lines.add(describeDirectory("  現在のワールド/$sub", dir))
        }

        val backupFolder = File(BACKUPS_DIRECTORY, world.name)
        lines.add("バックアップ先(絶対パス) = ${backupFolder.absolutePath}")

        RESETTABLE_SUBDIRECTORIES.forEach { sub ->
            val dir = File(backupFolder, sub)
            lines.add(describeDirectory("  バックアップ/$sub", dir))
        }

        return lines
    }

    private fun describeDirectory(label: String, dir: File): String {
        if (!dir.exists()) return "$label: 存在しません"

        val files = dir.listFiles() ?: return "$label: 一覧取得失敗"
        val newest = files.maxByOrNull { it.lastModified() }
        val newestText = if (newest != null) "${newest.name} (更新: ${java.time.Instant.ofEpochMilli(newest.lastModified())})" else "ファイルなし"

        return "$label: ファイル数=${files.size}, 最新ファイル=$newestText"
    }

    /**
     * [worldName] のワールドを、保存済みのバックアップの状態までリセットする。
     * 完了したらリセット後の [World] を引数に [onComplete] を呼び出す。
     *
     * サーバーの再起動もワールドの再ロードも不要。対象ワールド内にいたプレイヤーは
     * 処理の間だけ待機用ワールドへ一時的にテレポートされる。
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

        val world = Bukkit.getWorld(worldName)
        if (world == null) {
            Bukkit.getLogger().warning("[BigAnni] $worldName がロードされていません。マップのリセットを中断します。")
            return
        }

        isResetting = true

        val holdingLocation = getHoldingWorld().spawnLocation
        val displacedPlayers = world.players.toList()
        displacedPlayers.forEach { it.teleport(holdingLocation) }

        // プレイヤー以外(モブ・ドロップアイテム・矢など)はそのまま残ると
        // リセット後の状態と食い違うため、チャンク解放前に片付ける。
        world.entities.forEach { if (it !is Player) it.remove() }

        val keepSpawnInMemory = world.keepSpawnInMemory
        world.keepSpawnInMemory = false

        // 現在メモリ上に読み込まれているチャンクを、保存せずに全て解放する。
        // ここで解放しておかないと、後でファイルを差し替えてもサーバーはメモリ上の
        // (荒らされた)チャンクを使い続けてしまう。
        world.loadedChunks.toList().forEach { chunk ->
            if (chunk.isForceLoaded) {
                chunk.isForceLoaded = false
            }

            world.unloadChunk(chunk.x, chunk.z, false)
        }

        world.keepSpawnInMemory = keepSpawnInMemory

        val worldFolder = world.worldFolder

        RESETTABLE_SUBDIRECTORIES.forEach { sub ->
            val dst = File(worldFolder, sub)
            deleteRecursively(dst)

            val src = File(backupFolder, sub)
            if (src.exists()) {
                copyRecursively(src.toPath(), dst.toPath())
            }
        }

        // ここで初めて対象ワールドへ触れるので、スポーン付近のチャンクが
        // (差し替え後のファイルから)読み直される。
        displacedPlayers.forEach { it.teleport(world.spawnLocation) }

        isResetting = false
        onComplete(world)
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
