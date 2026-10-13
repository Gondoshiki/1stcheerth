package com.example.gallery

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.example.gallery.data.FolderTagCrossRef
import com.example.gallery.data.GalleryDatabase
import com.example.gallery.data.MediaItem
import com.example.gallery.data.NaturalOrder
import com.example.gallery.data.TagEntity
import com.example.gallery.data.loadDeviceMedia
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** วิธีเรียงโฟลเดอร์ (ตรงกับกล่อง Sort by ของ Right Gallery ยกเว้น Custom) */
enum class FolderSort { NAME, PATH, SIZE, COUNT, MODIFIED, TAKEN, RANDOM }

enum class PhotoSort { NAME, SIZE, MODIFIED, TAKEN, RANDOM }

/** ตัวกรอง + การเรียงลำดับของหนึ่งรายการ (โฟลเดอร์ / รูปทั้งหมด / รูปในโฟลเดอร์) */
data class ListFilter<S>(
    val query: String = "",
    val tagIds: Set<Long> = emptySet(),
    val sort: S,
    val ascending: Boolean = false,
    /** true = เรียงตัวเลขในชื่อตามค่าจริง (1, 2, 10) แทนทีละตัวอักษร (1, 10, 2) */
    val natural: Boolean = true,
    /** ใช้กับการเรียงแบบสุ่ม เปลี่ยนค่าเมื่อเลือก Random ใหม่ */
    val seed: Long = 0L
)

/** ค่าการเรียงรูปที่จำไว้ (ใช้ทั้งแบบทุกโฟลเดอร์ และแบบเฉพาะโฟลเดอร์) */
data class SortSetting(
    val sort: PhotoSort,
    val ascending: Boolean,
    val natural: Boolean
)

data class FolderItem(
    val bucketId: Long,
    val name: String,
    val path: String,
    val count: Int,
    val coverUri: String?,
    val totalSize: Long,
    /** เวลาถ่ายล่าสุดของรูปในโฟลเดอร์ */
    val latestTaken: Long,
    /** เวลาแก้ไขล่าสุดของรูปในโฟลเดอร์ */
    val latestModified: Long,
    val tags: List<TagEntity>
)

data class FolderDetail(
    val folder: FolderItem,
    val photos: List<MediaItem>
)

data class ViewerState(
    val items: List<MediaItem>,
    val index: Int
)

internal fun buildFolders(
    media: List<MediaItem>,
    tags: List<TagEntity>,
    refs: List<FolderTagCrossRef>
): List<FolderItem> {
    val tagById = tags.associateBy { it.id }
    val tagIdsByBucket = refs.groupBy({ it.bucketId }, { it.tagId })
    return media.groupBy { it.bucketId }.map { (bucketId, items) ->
        val newest = items.maxByOrNull { it.date }
        FolderItem(
            bucketId = bucketId,
            name = items.first().bucketName,
            path = items.first().path,
            count = items.size,
            coverUri = newest?.uri,
            totalSize = items.sumOf { it.size },
            latestTaken = newest?.date ?: 0L,
            latestModified = items.maxOf { it.modified },
            tags = tagIdsByBucket[bucketId].orEmpty()
                .mapNotNull { tagById[it] }
                .sortedBy { it.name.lowercase() }
        )
    }
}

private fun textOrder(natural: Boolean): Comparator<String> =
    if (natural) NaturalOrder else String.CASE_INSENSITIVE_ORDER

internal fun sortFolders(list: List<FolderItem>, f: ListFilter<FolderSort>): List<FolderItem> {
    if (f.sort == FolderSort.RANDOM) return list.shuffled(Random(f.seed))
    val text = textOrder(f.natural)
    val base: Comparator<FolderItem> = when (f.sort) {
        FolderSort.NAME -> compareBy<FolderItem, String>(text) { it.name }
        FolderSort.PATH -> compareBy<FolderItem, String>(text) { it.path }
        FolderSort.SIZE -> compareBy<FolderItem> { it.totalSize }
        FolderSort.COUNT -> compareBy<FolderItem> { it.count }
        FolderSort.MODIFIED -> compareBy<FolderItem> { it.latestModified }
        FolderSort.TAKEN -> compareBy<FolderItem> { it.latestTaken }
        FolderSort.RANDOM -> compareBy<FolderItem> { it.bucketId }
    }
    val comparator = base.thenBy { it.bucketId }
    return list.sortedWith(if (f.ascending) comparator else comparator.reversed())
}

internal fun sortPhotos(list: List<MediaItem>, f: ListFilter<PhotoSort>): List<MediaItem> {
    if (f.sort == PhotoSort.RANDOM) return list.shuffled(Random(f.seed))
    val text = textOrder(f.natural)
    val base: Comparator<MediaItem> = when (f.sort) {
        PhotoSort.NAME -> compareBy<MediaItem, String>(text) { it.name }
        PhotoSort.SIZE -> compareBy<MediaItem> { it.size }
        PhotoSort.MODIFIED -> compareBy<MediaItem> { it.modified }
        PhotoSort.TAKEN -> compareBy<MediaItem> { it.date }
        PhotoSort.RANDOM -> compareBy<MediaItem> { it.id }
    }
    val comparator = base.thenBy { it.id }
    return list.sortedWith(if (f.ascending) comparator else comparator.reversed())
}

class GalleryViewModel(app: Application) : AndroidViewModel(app) {

    private val db = GalleryDatabase.get(app)
    private val dao = db.dao()
    private val prefs = app.getSharedPreferences("gallery_prefs", Context.MODE_PRIVATE)

    private inline fun <reified E : Enum<E>> loadEnum(key: String, default: E): E =
        runCatching { enumValueOf<E>(prefs.getString(key, "") ?: "") }.getOrDefault(default)

    /** อ่านค่าการเรียงรูปที่จำไว้ตามชื่อกลุ่ม (prefix) */
    private fun loadSetting(prefix: String): SortSetting = SortSetting(
        sort = loadEnum("${prefix}_sort", PhotoSort.TAKEN),
        ascending = prefs.getBoolean("${prefix}_asc", false),
        natural = prefs.getBoolean("${prefix}_natural", true)
    )

    private fun saveSetting(prefix: String, s: SortSetting) {
        prefs.edit()
            .putString("${prefix}_sort", s.sort.name)
            .putBoolean("${prefix}_asc", s.ascending)
            .putBoolean("${prefix}_natural", s.natural)
            .apply()
    }

    private fun encode(s: SortSetting) =
        "${s.sort.name}|${if (s.ascending) 1 else 0}|${if (s.natural) 1 else 0}"

    private fun decode(text: String): SortSetting? {
        val parts = text.split("|")
        if (parts.size != 3) return null
        val sort = runCatching { PhotoSort.valueOf(parts[0]) }.getOrNull() ?: return null
        return SortSetting(sort, parts[1] == "1", parts[2] == "1")
    }

    /** ค่าเรียงของทุกโฟลเดอร์ (ยกเว้นโฟลเดอร์ที่ตั้งเฉพาะตัว) และค่าเฉพาะแต่ละโฟลเดอร์ (key = bucketId) */
    private var globalFolderSort: SortSetting = loadSetting("photo")
    private val folderOverrides = HashMap<Long, SortSetting>()

    init {
        prefs.all.forEach { (key, value) ->
            if (key.startsWith("fo_") && value is String) {
                val id = key.removePrefix("fo_").toLongOrNull()
                val setting = decode(value)
                if (id != null && setting != null) folderOverrides[id] = setting
            }
        }
    }

    // ---------- สถานะหน้าจอ (อยู่รอดตอนสลับภาษา/หมุนจอ) ----------
    /** null = ยังโหลดอยู่ */
    val media = MutableStateFlow<List<MediaItem>?>(null)

    /** true = กำลังดูหน้ารูปทั้งหมด */
    val showAll = MutableStateFlow(false)
    val openBucketId = MutableStateFlow<Long?>(null)
    val viewer = MutableStateFlow<ViewerState?>(null)

    /** จำนวนคอลัมน์ของกริดโฟลเดอร์ และมุมมองแบบรายการ (จำค่าไว้ข้ามการเปิดแอพ) */
    val columns = MutableStateFlow(prefs.getInt("columns", 2).coerceIn(1, 5))
    val listView = MutableStateFlow(prefs.getBoolean("list_view", false))

    val folderFilter = MutableStateFlow(
        ListFilter(
            sort = loadEnum("folder_sort", FolderSort.MODIFIED),
            ascending = prefs.getBoolean("folder_asc", false),
            natural = prefs.getBoolean("folder_natural", true),
            seed = System.nanoTime()
        )
    )
    private val allSetting = loadSetting("all")
    val photoFilter = MutableStateFlow(
        ListFilter(
            sort = allSetting.sort,
            ascending = allSetting.ascending,
            natural = allSetting.natural,
            seed = System.nanoTime()
        )
    )
    val inFolderFilter = MutableStateFlow(
        ListFilter(
            sort = globalFolderSort.sort,
            ascending = globalFolderSort.ascending,
            natural = globalFolderSort.natural,
            seed = System.nanoTime()
        )
    )

    /** true = โฟลเดอร์ที่เปิดอยู่ใช้ค่าเรียงเฉพาะตัว (ติ๊ก "ใช้กับโฟลเดอร์นี้เท่านั้น") */
    val inFolderCustom = MutableStateFlow(false)

    // ---------- ข้อมูล ----------
    private val tagRefs = dao.observeFolderTags()

    val tags: StateFlow<List<TagEntity>> = dao.observeTags()
        .map { list -> list.sortedBy { it.name.lowercase() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val folders: StateFlow<List<FolderItem>> = combine(
        media, tags, tagRefs, folderFilter
    ) { m, t, r, f ->
        val needle = f.query.trim()
        val list = buildFolders(m.orEmpty(), t, r).filter { item ->
            val matchesText = needle.isEmpty() ||
                item.name.contains(needle, ignoreCase = true) ||
                item.tags.any { it.name.contains(needle, ignoreCase = true) }
            val matchesTags = f.tagIds.isEmpty() ||
                item.tags.map { it.id }.containsAll(f.tagIds)
            matchesText && matchesTags
        }
        sortFolders(list, f)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** รูปทั้งหมด กรองด้วยชื่อรูป / ชื่อโฟลเดอร์ / แท็กของโฟลเดอร์ */
    val photos: StateFlow<List<MediaItem>> = combine(
        media, tags, tagRefs, photoFilter
    ) { m, t, r, f ->
        val tagById = t.associateBy { it.id }
        val tagIdsByBucket = r.groupBy({ it.bucketId }, { it.tagId })
        val tagNamesByBucket = tagIdsByBucket.mapValues { (_, ids) ->
            ids.mapNotNull { tagById[it]?.name }
        }
        val needle = f.query.trim()
        val filtered = m.orEmpty().filter { item ->
            val ids = tagIdsByBucket[item.bucketId].orEmpty()
            val names = tagNamesByBucket[item.bucketId].orEmpty()
            val matchesText = needle.isEmpty() ||
                item.name.contains(needle, ignoreCase = true) ||
                item.bucketName.contains(needle, ignoreCase = true) ||
                names.any { it.contains(needle, ignoreCase = true) }
            val matchesTags = f.tagIds.isEmpty() || ids.containsAll(f.tagIds)
            matchesText && matchesTags
        }
        sortPhotos(filtered, f)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val detail: StateFlow<FolderDetail?> = combine(
        media, tags, tagRefs, openBucketId, inFolderFilter
    ) { m, t, r, id, f ->
        if (id == null) {
            null
        } else {
            val items = m.orEmpty().filter { it.bucketId == id }
            if (items.isEmpty()) {
                null
            } else {
                val folder = buildFolders(items, t, r).first()
                val needle = f.query.trim()
                val shown = items.filter {
                    needle.isEmpty() || it.name.contains(needle, ignoreCase = true)
                }
                FolderDetail(folder, sortPhotos(shown, f))
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // ---------- การกระทำ ----------
    /** อ่านรูปในเครื่องใหม่ (เรียกตอนเปิดแอพ / กลับมาที่แอพ / หลังลบรูป) */
    fun refresh() {
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) {
                runCatching { loadDeviceMedia(getApplication<Application>()) }
                    .getOrDefault(emptyList())
            }
            media.value = list
            val open = openBucketId.value
            if (open != null && list.none { it.bucketId == open }) {
                closeFolder()
            }
        }
    }

    /** เปิดโฟลเดอร์: ใช้ค่าเรียงเฉพาะโฟลเดอร์ถ้าเคยตั้งไว้ ไม่เช่นนั้นใช้ค่าของทุกโฟลเดอร์ */
    fun openFolder(bucketId: Long) {
        val own = folderOverrides[bucketId]
        val effective = own ?: globalFolderSort
        inFolderFilter.value = ListFilter(
            sort = effective.sort,
            ascending = effective.ascending,
            natural = effective.natural,
            seed = System.nanoTime()
        )
        inFolderCustom.value = own != null
        viewer.value = null
        openBucketId.value = bucketId
    }

    fun closeFolder() {
        viewer.value = null
        openBucketId.value = null
    }

    fun openViewer(items: List<MediaItem>, index: Int) {
        if (items.isEmpty()) return
        viewer.value = ViewerState(items, index.coerceIn(0, items.lastIndex))
    }

    fun closeViewer() {
        viewer.value = null
    }

    fun setColumns(count: Int) {
        columns.value = count.coerceIn(1, 5)
        prefs.edit().putInt("columns", columns.value).apply()
    }

    fun setListView(enabled: Boolean) {
        listView.value = enabled
        prefs.edit().putBoolean("list_view", enabled).apply()
    }

    fun setFolderSort(sort: FolderSort, ascending: Boolean, natural: Boolean) {
        folderFilter.update {
            it.copy(
                sort = sort,
                ascending = ascending,
                natural = natural,
                seed = if (sort == FolderSort.RANDOM) System.nanoTime() else it.seed
            )
        }
        prefs.edit()
            .putString("folder_sort", sort.name)
            .putBoolean("folder_asc", ascending)
            .putBoolean("folder_natural", natural)
            .apply()
    }

    /** ค่าเรียงของหน้ารูปทั้งหมด (จำไว้) */
    fun setPhotoSort(sort: PhotoSort, ascending: Boolean, natural: Boolean) {
        photoFilter.update {
            it.copy(
                sort = sort,
                ascending = ascending,
                natural = natural,
                seed = if (sort == PhotoSort.RANDOM) System.nanoTime() else it.seed
            )
        }
        saveSetting("all", SortSetting(sort, ascending, natural))
    }

    /**
     * ค่าเรียงรูปในโฟลเดอร์ (จำไว้)
     * folderOnly = true: ใช้เฉพาะโฟลเดอร์ที่เปิดอยู่ / false: ใช้กับทุกโฟลเดอร์ (และให้โฟลเดอร์นี้ตามค่ากลาง)
     */
    fun setInFolderSort(sort: PhotoSort, ascending: Boolean, natural: Boolean, folderOnly: Boolean) {
        val id = openBucketId.value
        val setting = SortSetting(sort, ascending, natural)
        if (folderOnly && id != null) {
            folderOverrides[id] = setting
            prefs.edit().putString("fo_$id", encode(setting)).apply()
        } else {
            globalFolderSort = setting
            saveSetting("photo", setting)
            if (id != null) {
                folderOverrides.remove(id)
                prefs.edit().remove("fo_$id").apply()
            }
        }
        inFolderCustom.value = folderOnly && id != null
        inFolderFilter.update {
            it.copy(
                sort = sort,
                ascending = ascending,
                natural = natural,
                seed = if (sort == PhotoSort.RANDOM) System.nanoTime() else it.seed
            )
        }
    }

    private fun <S> MutableStateFlow<ListFilter<S>>.toggleTag(id: Long) {
        update { it.copy(tagIds = if (id in it.tagIds) it.tagIds - id else it.tagIds + id) }
    }

    fun toggleFolderTag(id: Long) = folderFilter.toggleTag(id)

    fun togglePhotoTag(id: Long) = photoFilter.toggleTag(id)

    fun createTag(name: String, onDone: (Long) -> Unit = {}) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            val existing = dao.findTag(clean)
            val id = existing?.id ?: dao.insertTag(TagEntity(name = clean))
            onDone(id)
        }
    }

    fun deleteTag(id: Long) {
        folderFilter.update { it.copy(tagIds = it.tagIds - id) }
        photoFilter.update { it.copy(tagIds = it.tagIds - id) }
        viewModelScope.launch { dao.deleteTag(id) }
    }

    fun saveFolderTags(bucketId: Long, tagIds: Set<Long>) {
        viewModelScope.launch {
            db.withTransaction {
                dao.clearFolderTags(bucketId)
                dao.insertFolderTags(tagIds.map { FolderTagCrossRef(bucketId, it) })
            }
        }
    }
}
