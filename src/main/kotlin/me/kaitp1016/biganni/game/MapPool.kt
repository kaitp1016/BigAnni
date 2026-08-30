package me.kaitp1016.biganni.game

import me.kaitp1016.biganni.config.Config

/**
 * 「同じマップのきれいなコピーを複数のディメンションとして用意しておき、試合ごとに
 * 順番に切り替えて使う」ための連続試合化の仕組み。
 *
 * ワールド自体を書き換えてリセットするのではなく、そもそも荒らされていない次のマップへ
 * 切り替えるだけなので、リセット処理特有のリスク(サーバーがメモリに持っているチャンク情報と
 * ディスクのファイルがズレる等)が発生しない。
 *
 * 使い方(運用側):
 *   1. plugins/BigAnni/maps/ に、同じマップ構成を指すJSONを複数用意する
 *      (例: coastal_1.json, coastal_2.json, ... coastal_10.json。ワールド名も
 *      それぞれ別のディメンションを指すようにしておくこと)
 *   2. 試合前に `/anni nextmap coastal_` のように、共通の接頭辞(プレフィックス)を指定して実行する
 *      -> まだ使っていないマップの中から1つ選ばれ、Game.map / Game.mapId に自動でセットされる
 *   3. そのまま `/anni startimmediately` などで試合開始
 *   4. 用意した分(例: 10試合)を使い切ったら、`/anni poolstatus` で状況を確認できる。
 *      イベントが終わったあと、今まで通りの手順(バックアップを戻して再起動)で
 *      まとめて掃除すればよい
 *   5. サーバーを再起動せずに「使用済み」の記録だけをリセットしたい場合は `/anni poolreset` を使う
 *      (ワールド自体は荒れたままなので、掃除していないマップを再利用しないよう注意すること)
 */
object MapPool {
    private val usedMapIds = mutableSetOf<String>()

    /**
     * [prefix] で始まるマップ設定(plugins/BigAnni/maps 配下のJSONファイル)のうち、まだ使っていないものを
     * 1つ選んで返す。見つからなければ null。
     */
    fun candidates(prefix: String): List<String> {
        return Config.getMapNames().filter { it.startsWith(prefix) }.sorted()
    }

    fun unusedCandidates(prefix: String): List<String> {
        return candidates(prefix).filter { it !in usedMapIds }
    }

    fun markUsed(mapId: String) {
        usedMapIds.add(mapId)
    }

    fun isUsed(mapId: String): Boolean {
        return mapId in usedMapIds
    }

    fun reset() {
        usedMapIds.clear()
    }

    fun usedMapIdsSorted(): List<String> {
        return usedMapIds.sorted()
    }
}
