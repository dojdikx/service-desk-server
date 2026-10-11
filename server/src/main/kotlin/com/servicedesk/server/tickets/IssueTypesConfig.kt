package com.servicedesk.server.tickets

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class IssueTypeHierarchyItem(
    val issueType: String,
    val subIssueTypes: List<String> = emptyList(),
)

@Serializable
data class IssueTypesHierarchyFile(
    val hierarchy: List<IssueTypeHierarchyItem> = emptyList(),
    val thirdIssueTypes: List<String> = emptyList(),
)

@Serializable
data class IssueTypesResponse(
    val lang: String,
    val hierarchy: List<IssueTypeHierarchyItem>,
    val thirdIssueTypes: List<String>,
    val labels: Map<String, String>,
)

object IssueTypesConfig {
    private val json = Json { ignoreUnknownKeys = true }

    private val hierarchy: IssueTypesHierarchyFile by lazy { loadHierarchy() }
    private val labelsRu: Map<String, String> by lazy { loadLabels("ticket/issue_type_labels_ru.json") }
    private val labelsZh: Map<String, String> by lazy { loadLabels("ticket/issue_type_labels_zh.json") }

    fun response(lang: String): IssueTypesResponse {
        val normalized = when (lang.lowercase()) {
            "zh", "cn", "chinese" -> "zh"
            else -> "ru"
        }
        val labels = when (normalized) {
            "zh" -> labelsZh
            else -> labelsRu
        }
        return IssueTypesResponse(
            lang = normalized,
            hierarchy = hierarchy.hierarchy,
            thirdIssueTypes = hierarchy.thirdIssueTypes,
            labels = labels,
        )
    }

    private fun loadHierarchy(): IssueTypesHierarchyFile {
        val text = readResource("ticket/issue_types_hierarchy.json")
        return json.decodeFromString(IssueTypesHierarchyFile.serializer(), text)
    }

    private fun loadLabels(path: String): Map<String, String> {
        val text = readResource(path)
        val element = json.parseToJsonElement(text)
        return element.jsonObject.mapValues { (_, value) ->
            value.jsonPrimitive.content
        }
    }

    private fun readResource(path: String): String =
        IssueTypesConfig::class.java.classLoader.getResourceAsStream(path)
            ?.bufferedReader()
            ?.use { it.readText() }
            ?: error("$path not found")
}
