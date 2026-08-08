from pathlib import Path


def replace(path: str, old: str, new: str) -> None:
    p = Path(path)
    text = p.read_text()
    if old not in text:
        raise SystemExit(f"Expected pattern not found in {path}: {old[:120]!r}")
    p.write_text(text.replace(old, new, 1))


Path("app/src/main/java/com/wolferdwolf/drop/data/SavedSourceType.kt").write_text('''package com.wolferdwolf.drop.data

enum class SavedSourceType(val label: String) {
    TEXT("Text"),
    LINK("Link"),
    IMAGE("Image"),
    PDF("PDF"),
    UNKNOWN("Unknown");

    companion object {
        fun fromStored(value: String?): SavedSourceType =
            values().firstOrNull { it.name == value } ?: UNKNOWN
    }
}
''')

replace(
    "app/src/main/java/com/wolferdwolf/drop/data/SavedReference.kt",
    '''    val originalText: String,\n    val createdAtEpochMillis: Long,\n    val notes: String = ""\n)''',
    '''    val originalText: String,\n    val createdAtEpochMillis: Long,\n    val notes: String = "",\n    val sourceType: SavedSourceType = SavedSourceType.UNKNOWN\n)'''
)

replace(
    "app/src/main/java/com/wolferdwolf/drop/data/SavedReferenceCodec.kt",
    '''        encodePart(reference.originalText),\n        encodePart(reference.notes)\n    ).joinToString("|")''',
    '''        encodePart(reference.originalText),\n        encodePart(reference.notes),\n        reference.sourceType.name\n    ).joinToString("|")'''
)
replace(
    "app/src/main/java/com/wolferdwolf/drop/data/SavedReferenceCodec.kt",
    '''        val parts = value.split('|', limit = 5)\n        if (parts.size !in 4..5) return null''',
    '''        val parts = value.split('|', limit = 6)\n        if (parts.size !in 4..6) return null'''
)
replace(
    "app/src/main/java/com/wolferdwolf/drop/data/SavedReferenceCodec.kt",
    '''            createdAtEpochMillis = createdAt,\n            notes = parts.getOrNull(4)?.let(::decodePart).orEmpty()\n        )''',
    '''            createdAtEpochMillis = createdAt,\n            notes = parts.getOrNull(4)?.let(::decodePart).orEmpty(),\n            sourceType = SavedSourceType.fromStored(parts.getOrNull(5))\n        )'''
)

replace(
    "app/src/main/java/com/wolferdwolf/drop/data/SavedReferenceStore.kt",
    '''    fun save(title: String, originalText: String, now: Long = System.currentTimeMillis()): SavedReference {''',
    '''    fun save(\n        title: String,\n        originalText: String,\n        now: Long = System.currentTimeMillis(),\n        sourceType: SavedSourceType = SavedSourceType.UNKNOWN\n    ): SavedReference {'''
)
replace(
    "app/src/main/java/com/wolferdwolf/drop/data/SavedReferenceStore.kt",
    '''            originalText = originalText.trim(),\n            createdAtEpochMillis = now\n        )''',
    '''            originalText = originalText.trim(),\n            createdAtEpochMillis = now,\n            sourceType = sourceType\n        )'''
)

replace(
    "app/src/main/java/com/wolferdwolf/drop/history/HistorySearch.kt",
    '''package com.wolferdwolf.drop.history\n\nimport java.text.Normalizer''',
    '''package com.wolferdwolf.drop.history\n\nimport com.wolferdwolf.drop.data.SavedSourceType\nimport java.text.Normalizer'''
)
replace(
    "app/src/main/java/com/wolferdwolf/drop/history/HistorySearch.kt",
    '''enum class HistoryDateFilter {\n    ALL,\n    TODAY,\n    LAST_7_DAYS,\n    LAST_30_DAYS\n}\n''',
    '''enum class HistoryDateFilter {\n    ALL,\n    TODAY,\n    LAST_7_DAYS,\n    LAST_30_DAYS\n}\n\nenum class HistorySourceFilter {\n    ALL,\n    TEXT,\n    LINK,\n    IMAGE,\n    PDF\n}\n'''
)
replace(
    "app/src/main/java/com/wolferdwolf/drop/history/HistorySearch.kt",
    '''    fun includesReminders(filter: HistoryItemFilter): Boolean =\n        filter == HistoryItemFilter.ALL || filter == HistoryItemFilter.REMINDERS\n\n    fun matchesDate(''',
    '''    fun includesReminders(filter: HistoryItemFilter): Boolean =\n        filter == HistoryItemFilter.ALL || filter == HistoryItemFilter.REMINDERS\n\n    fun matchesSource(filter: HistorySourceFilter, sourceType: SavedSourceType): Boolean = when (filter) {\n        HistorySourceFilter.ALL -> true\n        HistorySourceFilter.TEXT -> sourceType == SavedSourceType.TEXT\n        HistorySourceFilter.LINK -> sourceType == SavedSourceType.LINK\n        HistorySourceFilter.IMAGE -> sourceType == SavedSourceType.IMAGE\n        HistorySourceFilter.PDF -> sourceType == SavedSourceType.PDF\n    }\n\n    fun matchesDate('''
)

main = "app/src/main/java/com/wolferdwolf/drop/MainActivity.kt"
replace(main, 'import com.wolferdwolf.drop.data.SavedReferenceStore\n', 'import com.wolferdwolf.drop.data.SavedReferenceStore\nimport com.wolferdwolf.drop.data.SavedSourceType\n')
replace(main, 'import com.wolferdwolf.drop.history.HistorySearch\n', 'import com.wolferdwolf.drop.history.HistorySearch\nimport com.wolferdwolf.drop.history.HistorySourceFilter\n')
replace(main, '    private var sourceText by mutableStateOf<String?>(null)\n', '    private var sourceText by mutableStateOf<String?>(null)\n    private var sourceType by mutableStateOf(SavedSourceType.UNKNOWN)\n')
replace(
    main,
    '''        sourceText = savedInstanceState?.getString(STATE_TEXT) ?: SharedTextParser.parse(intent)\n        editedResults = EditableExtractionState.decode(''',
    '''        sourceText = savedInstanceState?.getString(STATE_TEXT) ?: SharedTextParser.parse(intent)\n        sourceType = SavedSourceType.fromStored(\n            savedInstanceState?.getString(STATE_SOURCE_TYPE) ?: intent.getStringExtra(EXTRA_SOURCE_TYPE)\n        )\n        if (sourceType == SavedSourceType.UNKNOWN && sourceText != null) {\n            sourceType = inferSharedTextSource(sourceText.orEmpty())\n        }\n        editedResults = EditableExtractionState.decode('''
)
replace(main, '                    Screen.TEXT_ENTRY -> EntryScreen("Paste text", false, { screen = Screen.HOME }, ::beginFlow)\n                    Screen.LINK_ENTRY -> EntryScreen("Add link", true, { screen = Screen.HOME }, ::beginFlow)\n', '                    Screen.TEXT_ENTRY -> EntryScreen("Paste text", false, { screen = Screen.HOME }) { beginFlow(it, SavedSourceType.TEXT) }\n                    Screen.LINK_ENTRY -> EntryScreen("Add link", true, { screen = Screen.HOME }) { beginFlow(it, SavedSourceType.LINK) }\n')
replace(main, '                    Screen.PREVIEW -> if (text == null) reset() else PreviewScreen(text, ::reset) {\n', '                    Screen.PREVIEW -> if (text == null) reset() else PreviewScreen(text, sourceType.label, ::reset) {\n')
replace(main, '                        runCatching { referenceStore.save(title, text) }\n', '                        runCatching { referenceStore.save(title, text, sourceType = sourceType) }\n')
replace(main, '                        runCatching { referenceStore.save("Checklist", value) }\n', '                        runCatching { referenceStore.save("Checklist", value, sourceType = sourceType) }\n')
replace(main, '        SharedTextParser.parse(intent)?.let(::beginFlow)\n', '        SharedTextParser.parse(intent)?.let { beginFlow(it, resolveSourceType(intent, it)) }\n')
replace(main, '        outState.putString(STATE_TEXT, sourceText)\n', '        outState.putString(STATE_TEXT, sourceText)\n        outState.putString(STATE_SOURCE_TYPE, sourceType.name)\n')
replace(main, '                beginFlow(text)\n', '                beginFlow(text, SavedSourceType.IMAGE)\n')
replace(
    main,
    '''    private fun beginFlow(value: String) {\n        val clean = value.trim().take(SharedTextParser.MAX_SHARED_TEXT_LENGTH)\n        if (clean.isNotBlank()) {\n            sourceText = clean\n            editedResults = null\n            screen = Screen.PREVIEW\n        }\n    }''',
    '''    private fun beginFlow(value: String, type: SavedSourceType = SavedSourceType.UNKNOWN) {\n        val clean = value.trim().take(SharedTextParser.MAX_SHARED_TEXT_LENGTH)\n        if (clean.isNotBlank()) {\n            sourceText = clean\n            sourceType = if (type == SavedSourceType.UNKNOWN) inferSharedTextSource(clean) else type\n            editedResults = null\n            screen = Screen.PREVIEW\n        }\n    }\n\n    private fun resolveSourceType(intent: Intent?, text: String): SavedSourceType {\n        val explicit = SavedSourceType.fromStored(intent?.getStringExtra(EXTRA_SOURCE_TYPE))\n        return if (explicit != SavedSourceType.UNKNOWN) explicit else inferSharedTextSource(text)\n    }\n\n    private fun inferSharedTextSource(text: String): SavedSourceType {\n        val trimmed = text.trim()\n        val urls = RuleBasedExtractor.extract(trimmed).filter { it.type == ExtractionType.URL }\n        return if (urls.size == 1 && trimmed.replace(urls.first().value, "").isBlank()) {\n            SavedSourceType.LINK\n        } else {\n            SavedSourceType.TEXT\n        }\n    }'''
)
replace(main, '        sourceText = null\n        editedResults = null\n', '        sourceText = null\n        sourceType = SavedSourceType.UNKNOWN\n        editedResults = null\n')
replace(main, '        const val STATE_TEXT = "source_text"\n', '        const val EXTRA_SOURCE_TYPE = "drop_source_type"\n        const val STATE_TEXT = "source_text"\n        const val STATE_SOURCE_TYPE = "source_type"\n')
replace(main, '    private companion object {\n', '    companion object {\n')
replace(main, '    var dateFilter by rememberSaveable { mutableStateOf(HistoryDateFilter.ALL) }\n', '    var dateFilter by rememberSaveable { mutableStateOf(HistoryDateFilter.ALL) }\n    var sourceFilter by rememberSaveable { mutableStateOf(HistorySourceFilter.ALL) }\n')
replace(main, '                HistorySearch.matchesDate(dateFilter, it.createdAtEpochMillis)\n', '                HistorySearch.matchesDate(dateFilter, it.createdAtEpochMillis) &&\n                HistorySearch.matchesSource(sourceFilter, it.sourceType)\n')
replace(main, '    val filtering = searchQuery.isNotBlank() || itemFilter != HistoryItemFilter.ALL || dateFilter != HistoryDateFilter.ALL\n', '    val filtering = searchQuery.isNotBlank() || itemFilter != HistoryItemFilter.ALL || dateFilter != HistoryDateFilter.ALL || sourceFilter != HistorySourceFilter.ALL\n')
source_ui_anchor = '''            item {\n                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {\n                    Text("Filter by date", style = MaterialTheme.typography.labelLarge)'''
source_ui = '''            item {\n                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {\n                    Text("Filter saved items by source", style = MaterialTheme.typography.labelLarge)\n                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {\n                        SourceFilterButton("All sources", sourceFilter == HistorySourceFilter.ALL, Modifier.weight(1f)) { sourceFilter = HistorySourceFilter.ALL }\n                        SourceFilterButton("Text", sourceFilter == HistorySourceFilter.TEXT, Modifier.weight(1f)) { sourceFilter = HistorySourceFilter.TEXT }\n                        SourceFilterButton("Link", sourceFilter == HistorySourceFilter.LINK, Modifier.weight(1f)) { sourceFilter = HistorySourceFilter.LINK }\n                    }\n                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {\n                        SourceFilterButton("Image", sourceFilter == HistorySourceFilter.IMAGE, Modifier.weight(1f)) { sourceFilter = HistorySourceFilter.IMAGE }\n                        SourceFilterButton("PDF", sourceFilter == HistorySourceFilter.PDF, Modifier.weight(1f)) { sourceFilter = HistorySourceFilter.PDF }\n                    }\n                    Text("Older saved items without source metadata stay visible only under All sources.")\n                }\n            }\n            item {\n                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {\n                    Text("Filter by date", style = MaterialTheme.typography.labelLarge)'''
replace(main, source_ui_anchor, source_ui)
replace(main, '                        Text(reference.title, style = MaterialTheme.typography.titleMedium)\n                        Text(reference.originalText, maxLines = 3)\n', '                        Text(reference.title, style = MaterialTheme.typography.titleMedium)\n                        Text("Source: ${reference.sourceType.label}", style = MaterialTheme.typography.labelLarge)\n                        Text(reference.originalText, maxLines = 3)\n')
replace(main, '            item { Text("Saved reference", style = MaterialTheme.typography.labelLarge) }\n', '            item { Text("Source: ${reference.sourceType.label}", style = MaterialTheme.typography.labelLarge) }\n            item { Text("Saved reference", style = MaterialTheme.typography.labelLarge) }\n')
replace(
    main,
    '''private fun PreviewScreen(value: String, onDiscard: () -> Unit, onContinue: (String) -> Unit) {''',
    '''private fun PreviewScreen(value: String, sourceLabel: String, onDiscard: () -> Unit, onContinue: (String) -> Unit) {'''
)
replace(main, '            item { Text("Review before processing", style = MaterialTheme.typography.headlineSmall) }\n', '            item { Text("Review before processing", style = MaterialTheme.typography.headlineSmall) }\n            item { Text("Source: $sourceLabel", style = MaterialTheme.typography.labelLarge) }\n')
insert_anchor = '''@OptIn(ExperimentalMaterial3Api::class)\n@Composable\nprivate fun ReferenceDetailScreen('''
insert_button = '''@Composable\nprivate fun SourceFilterButton(\n    label: String,\n    selected: Boolean,\n    modifier: Modifier = Modifier,\n    onClick: () -> Unit\n) {\n    if (selected) {\n        FilledTonalButton(onClick = onClick, modifier = modifier) { Text(label) }\n    } else {\n        OutlinedButton(onClick = onClick, modifier = modifier) { Text(label) }\n    }\n}\n\n@OptIn(ExperimentalMaterial3Api::class)\n@Composable\nprivate fun ReferenceDetailScreen('''
replace(main, insert_anchor, insert_button)

for path, source_name in [
    ("app/src/main/java/com/wolferdwolf/drop/pdf/PdfImportActivity.kt", "PDF"),
    ("app/src/main/java/com/wolferdwolf/drop/timetable/TimetableReviewActivity.kt", "IMAGE"),
]:
    replace(
        path,
        '''                .setType("text/plain")\n                .putExtra(Intent.EXTRA_TEXT, text)''',
        f'''                .setType("text/plain")\n                .putExtra(Intent.EXTRA_TEXT, text)\n                .putExtra(MainActivity.EXTRA_SOURCE_TYPE, com.wolferdwolf.drop.data.SavedSourceType.{source_name}.name)'''
    )

replace(
    "app/src/main/java/com/wolferdwolf/drop/share/SharedStreamActivity.kt",
    '''                                    .setType("text/plain")\n                                    .putExtra(Intent.EXTRA_TEXT, clean)''',
    '''                                    .setType("text/plain")\n                                    .putExtra(Intent.EXTRA_TEXT, clean)\n                                    .putExtra(MainActivity.EXTRA_SOURCE_TYPE, com.wolferdwolf.drop.data.SavedSourceType.IMAGE.name)'''
)

replace(
    "app/src/test/java/com/wolferdwolf/drop/data/SavedReferenceCodecTest.kt",
    '''            createdAtEpochMillis = 123456L,\n            notes = "Follow up after review"\n        )''',
    '''            createdAtEpochMillis = 123456L,\n            notes = "Follow up after review",\n            sourceType = SavedSourceType.PDF\n        )'''
)
replace(
    "app/src/test/java/com/wolferdwolf/drop/data/SavedReferenceCodecTest.kt",
    '''            SavedReference(42L, "Wolf plan", "Original text", 1234L, ""),''',
    '''            SavedReference(42L, "Wolf plan", "Original text", 1234L, "", SavedSourceType.UNKNOWN),'''
)

replace(
    "app/src/test/java/com/wolferdwolf/drop/history/HistorySearchTest.kt",
    '''package com.wolferdwolf.drop.history\n\nimport org.junit.Assert.assertFalse''',
    '''package com.wolferdwolf.drop.history\n\nimport com.wolferdwolf.drop.data.SavedSourceType\nimport org.junit.Assert.assertFalse'''
)
replace(
    "app/src/test/java/com/wolferdwolf/drop/history/HistorySearchTest.kt",
    '''    @Test\n    fun dateFilterUsesLocalCalendarDayBoundaries() {''',
    '''    @Test\n    fun sourceFilterMatchesOnlyPersistedSourceType() {\n        assertTrue(HistorySearch.matchesSource(HistorySourceFilter.ALL, SavedSourceType.UNKNOWN))\n        assertTrue(HistorySearch.matchesSource(HistorySourceFilter.IMAGE, SavedSourceType.IMAGE))\n        assertTrue(HistorySearch.matchesSource(HistorySourceFilter.PDF, SavedSourceType.PDF))\n        assertFalse(HistorySearch.matchesSource(HistorySourceFilter.IMAGE, SavedSourceType.TEXT))\n        assertFalse(HistorySearch.matchesSource(HistorySourceFilter.TEXT, SavedSourceType.UNKNOWN))\n    }\n\n    @Test\n    fun dateFilterUsesLocalCalendarDayBoundaries() {'''
)

flow = "app/src/androidTest/java/com/wolferdwolf/drop/HistorySearchFlowTest.kt"
replace(flow, 'import com.wolferdwolf.drop.data.SavedReferenceStore\n', 'import com.wolferdwolf.drop.data.SavedReferenceStore\nimport com.wolferdwolf.drop.data.SavedSourceType\n')
replace(flow, '            now = 9_001L\n        )', '            now = 9_001L,\n            sourceType = SavedSourceType.TEXT\n        )')
replace(flow, '        val second = store.save("Supplier invoice", "Replacement bearings and machine oil.", now = 9_002L)\n', '        val second = store.save("Supplier invoice", "Replacement bearings and machine oil.", now = 9_002L, sourceType = SavedSourceType.PDF)\n')
replace(flow, '        val recent = store.save("Today field note", "Fresh maintenance note saved today.", now = System.currentTimeMillis())\n', '        val recent = store.save("Today field note", "Fresh maintenance note saved today.", now = System.currentTimeMillis(), sourceType = SavedSourceType.IMAGE)\n')
replace(flow, '                visible(device, "Reminders")\n', '                visible(device, "Reminders")\n                visible(device, "Filter saved items by source")\n                visible(device, "All sources")\n                visible(device, "Text")\n                visible(device, "Link")\n                visible(device, "Image")\n                visible(device, "PDF")\n')
replace(
    flow,
    '''                capture(device, "/data/local/tmp/drop-history-search-result.png")\n\n                search.text = ""''',
    '''                capture(device, "/data/local/tmp/drop-history-search-result.png")\n\n                search.text = ""\n                dismissKeyboard(device)\n                clickExactText(device, "PDF")\n                visible(device, "Supplier invoice")\n                visible(device, "Source: PDF")\n                assertTrue("PDF source filter must hide text references", device.wait(Until.gone(By.text("Café quarterly wolf strategy")), TIMEOUT))\n                assertTrue("PDF source filter must hide image references", device.wait(Until.gone(By.text("Today field note")), TIMEOUT))\n                capture(device, "/data/local/tmp/drop-history-filter-pdf-source.png")\n                clickExactText(device, "All sources")\n\n                search.text = ""'''
)

print("Source provenance integration applied")
