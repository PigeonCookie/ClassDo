package com.coursework.tracker.data

import android.content.Context
import com.coursework.tracker.model.Assignment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/**
 * 作业表单填了一半的内容自动存成草稿，新建和编辑都适用。
 *
 * 以前是退出就清空、什么都不留；现在改成自动保存：中途退出、误触返回都不会丢，
 * 5 分钟内回来还在，超过 5 分钟没再动过就自动作废（连它带进来的照片一起删掉）。
 *
 * 草稿只存一份，用 [KEY_BASE_ID] 记住它属于哪份表单：
 * 空 = 新建作业，否则 = 正在编辑的那条作业 id，这样两边不会串味。
 */
object DraftStore {

    private const val PREFS = "homework_butler_draft"
    private const val KEY_JSON = "draft_json"
    private const val KEY_SAVED_AT = "draft_saved_at"
    private const val KEY_BASE_ID = "draft_base_id"

    /** 多久没动过就算过期 */
    private const val TTL_MS = 5 * 60 * 1000L

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /**
     * 有效期内、且属于同一份表单（[baseId] 一致）时返回草稿；否则返回 null。
     * 属于别的表单的草稿只是「这次用不上」，不当成过期，所以不会顺手删掉。
     */
    fun load(context: Context, baseId: String?): Assignment? {
        val store = prefs(context)
        val raw = store.getString(KEY_JSON, null) ?: return null
        if (store.getString(KEY_BASE_ID, "").orEmpty() != baseId.orEmpty()) return null

        val savedAt = store.getLong(KEY_SAVED_AT, 0L)
        if (System.currentTimeMillis() - savedAt > TTL_MS) {
            discard(context)
            return null
        }
        return runCatching { json.decodeFromString(Assignment.serializer(), raw) }.getOrNull()
    }

    fun save(context: Context, baseId: String?, draft: Assignment) {
        runCatching {
            prefs(context).edit()
                .putString(KEY_JSON, json.encodeToString(Assignment.serializer(), draft))
                .putString(KEY_BASE_ID, baseId.orEmpty())
                .putLong(KEY_SAVED_AT, System.currentTimeMillis())
                .commit()
        }
    }

    /** 正常保存成作业之后调用：草稿清掉，但照片归这条作业了，不能删 */
    fun consume(context: Context) {
        prefs(context).edit()
            .remove(KEY_JSON)
            .remove(KEY_BASE_ID)
            .remove(KEY_SAVED_AT)
            .commit()
    }

    /** 草稿作废：连同它带进来的照片文件一起删掉 */
    fun discard(context: Context) {
        val store = prefs(context)
        val photos = store.getString(KEY_JSON, null)
            ?.let {
                runCatching { json.decodeFromString(Assignment.serializer(), it).photos }
                    .getOrNull()
            }
            .orEmpty()

        store.edit().remove(KEY_JSON).remove(KEY_BASE_ID).remove(KEY_SAVED_AT).commit()

        if (photos.isNotEmpty()) {
            // 用独立作用域删，别跟着某个正在关闭的界面一起被取消
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                PhotoStore.delete(context.applicationContext, photos)
            }
        }
    }

    /** 应用启动时调一次，把已经过期的草稿清掉 */
    fun discardIfExpired(context: Context) {
        val store = prefs(context)
        val savedAt = store.getLong(KEY_SAVED_AT, 0L)
        if (System.currentTimeMillis() - savedAt > TTL_MS) {
            discard(context)
        }
    }
}
