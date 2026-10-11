package com.servicedesk.server.analytics

import com.servicedesk.server.config.AppConfig
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.notExists

class AnalyticsReportStorage(
    private val root: Path = Path.of(AppConfig.analyticsStoragePath),
) {
    init {
        if (root.notExists()) {
            Files.createDirectories(root)
        }
    }

    fun resolveFileName(fileName: String): Path? {
        if (!FILE_NAME_REGEX.matches(fileName)) return null
        val resolved = root.resolve(fileName).normalize()
        if (!resolved.startsWith(root)) return null
        return if (resolved.toFile().exists()) resolved else null
    }

    fun targetPath(fileName: String): Path {
        if (!FILE_NAME_REGEX.matches(fileName)) {
            throw IllegalArgumentException("invalid file name")
        }
        return root.resolve(fileName)
    }

    companion object {
        private val FILE_NAME_REGEX = Regex("^[a-zA-Z0-9._\\-()а-яА-ЯёЁ ]+\\.xlsx$")
    }
}
