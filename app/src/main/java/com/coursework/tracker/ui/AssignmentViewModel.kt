package com.coursework.tracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.coursework.tracker.data.AssignmentRepository
import com.coursework.tracker.data.PhotoStore
import com.coursework.tracker.model.Assignment
import com.coursework.tracker.notify.ReminderScheduler
import com.coursework.tracker.util.toLocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class FilterMode(val label: String) {
    ACTIVE("进行中"),
    ALL("全部"),
    DONE("已完成"),
}

data class HomeStats(
    val active: Int = 0,
    val dueToday: Int = 0,
    val overdue: Int = 0,
    val done: Int = 0,
)

class AssignmentViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = AssignmentRepository(app)

    private val _items = MutableStateFlow<List<Assignment>>(emptyList())
    val items: StateFlow<List<Assignment>> = _items.asStateFlow()

    private val _selectedDate = MutableStateFlow<LocalDate?>(null)
    val selectedDate: StateFlow<LocalDate?> = _selectedDate.asStateFlow()

    private val _filter = MutableStateFlow(FilterMode.ACTIVE)
    val filter: StateFlow<FilterMode> = _filter.asStateFlow()

    /** 每 20 秒推进一次，让「还剩多久」和进度条保持新鲜 */
    private val _now = MutableStateFlow(System.currentTimeMillis())
    val now: StateFlow<Long> = _now.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages = _messages.asSharedFlow()

    val visibleItems: StateFlow<List<Assignment>> =
        combine(_items, _selectedDate, _filter) { items, date, mode ->
            items.asSequence()
                .filter { item ->
                    when (mode) {
                        FilterMode.ACTIVE -> !item.done
                        FilterMode.DONE -> item.done
                        FilterMode.ALL -> true
                    }
                }
                .filter { item -> date == null || item.reportAt.toLocalDate() == date }
                .let { seq ->
                    when (mode) {
                        FilterMode.DONE -> seq.sortedByDescending { it.reportAt }
                        else -> seq.sortedWith(compareBy({ it.reportAt }, { it.createdAt }))
                    }
                }
                .toList()
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val stats: StateFlow<HomeStats> =
        combine(_items, _now) { items, now ->
            val today = LocalDate.now()
            HomeStats(
                active = items.count { !it.done },
                dueToday = items.count { !it.done && it.reportAt.toLocalDate() == today },
                overdue = items.count { it.isOverdue(now) },
                done = items.count { it.done },
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, HomeStats())

    init {
        viewModelScope.launch {
            val loaded = repository.load()
            _items.value = loaded
            // 应用可能在闹钟被系统清理后重新打开，这里统一补排一次
            ReminderScheduler.syncAll(getApplication<Application>(), loaded)
        }
        viewModelScope.launch {
            while (true) {
                delay(TICK_INTERVAL_MS)
                _now.value = System.currentTimeMillis()
            }
        }
    }

    fun itemById(id: String?): Assignment? =
        if (id == null) null else _items.value.firstOrNull { it.id == id }

    fun upsert(item: Assignment) {
        val current = _items.value
        val index = current.indexOfFirst { it.id == item.id }
        val next = if (index >= 0) {
            current.toMutableList().also { it[index] = item }
        } else {
            current + item
        }
        persist(next)
        ReminderScheduler.sync(getApplication<Application>(), item)
        _messages.tryEmit(if (index >= 0) "已保存「${item.name}」" else "已添加「${item.name}」")
    }

    fun delete(item: Assignment) {
        persist(_items.value.filterNot { it.id == item.id })
        ReminderScheduler.cancel(getApplication<Application>(), item)
        // 照片文件按文件名去重：只删「删完之后没人再引用」的那些，
        // 免得将来出现两条作业共用同一张图时把别人的图一起删了
        val stillReferenced = _items.value.flatMap { it.photos }.toSet()
        deletePhotos(item.photos.filterNot { it in stillReferenced })
        _messages.tryEmit("已删除「${item.name}」")
    }

    /** 删除照片文件。放在 ViewModel 里执行，避免表单关闭时协程被取消导致删不干净 */
    fun deletePhotos(names: List<String>) {
        if (names.isEmpty()) return
        viewModelScope.launch { PhotoStore.delete(getApplication<Application>(), names) }
    }

    fun toggleDone(item: Assignment) {
        val updated = item.copy(done = !item.done)
        persist(_items.value.map { if (it.id == item.id) updated else it })
        ReminderScheduler.sync(getApplication<Application>(), updated)
        _messages.tryEmit(
            if (updated.done) "「${item.name}」已完成" else "已把「${item.name}」恢复为进行中"
        )
    }

    fun selectDate(date: LocalDate?) {
        _selectedDate.value = date
    }

    fun setFilter(mode: FilterMode) {
        _filter.value = mode
    }

    private fun persist(list: List<Assignment>) {
        _items.value = list
        viewModelScope.launch { repository.save(list) }
    }

    private companion object {
        const val TICK_INTERVAL_MS = 20_000L
    }
}
