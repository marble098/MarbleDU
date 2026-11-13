package ir.marbledu.datausage.data

import java.util.Locale

/** Label/package search, tolerant of Arabic/Persian keyboard variants. */
object AppListFilter {
    private fun normalize(value: String): String = value.trim().lowercase(Locale.ROOT)
        .replace('ي', 'ی').replace('ك', 'ک')

    fun matches(label: String, packageName: String, query: String): Boolean {
        val needle = normalize(query)
        return normalize(label).contains(needle) || normalize(packageName).contains(needle)
    }
}
