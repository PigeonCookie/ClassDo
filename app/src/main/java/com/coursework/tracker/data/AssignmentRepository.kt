package com.coursework.tracker.data

import android.content.Context
import com.coursework.tracker.model.Assignment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

/**
 * 用应用私有目录下的一个 JSON 文件存放全部作业记录。
 * 数据量很小（几百条以内），全量读写足够快，且不引入数据库依赖。
 */
class AssignmentRepository(context: Context) {

    private val file = File(context.filesDir, "assignments.json")
    private val serializer = ListSerializer(Assignment.serializer())
    private val json = Json {
        ignoreUnknownKeys = true
        // 遇到不认识的枚举值（比如以后加了新的时间类型）时回落到默认值，而不是整个文件读不出来
        coerceInputValues = true
        encodeDefaults = true
        prettyPrint = false
    }

    suspend fun load(): List<Assignment> = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext emptyList()
        runCatching { json.decodeFromString(serializer, file.readText()) }
            .getOrElse { emptyList() }
    }

    suspend fun save(items: List<Assignment>) = withContext(Dispatchers.IO) {
        // 先写临时文件再替换，避免写入过程中被杀导致数据损坏
        val tmp = File(file.parentFile, file.name + ".tmp")
        runCatching {
            tmp.writeText(json.encodeToString(serializer, items))
            if (file.exists()) file.delete()
            tmp.renameTo(file)
        }
    }
}
