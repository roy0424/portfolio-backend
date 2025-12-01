package dev.kyhan.common.util

import java.text.Normalizer
import java.util.Locale

object SlugUtils {
    private val SLUG_REGEX = "[^a-z0-9-]".toRegex()

    fun generate(text: String): String =
        Normalizer
            .normalize(text, Normalizer.Form.NFD)
            .lowercase(Locale.getDefault())
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .replace("\\s+".toRegex(), "-")
            .replace(SLUG_REGEX, "")
            .replace("-+".toRegex(), "-")
            .trim('-')

    fun generateUnique(
        text: String,
        existingCheck: (String) -> Boolean,
    ): String {
        var slug = generate(text)
        var counter = 1

        while (existingCheck(slug)) {
            slug = "${generate(text)}-${counter++}"
        }

        return slug
    }
}
