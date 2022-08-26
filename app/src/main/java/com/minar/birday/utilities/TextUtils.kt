package com.minar.birday.utilities

import java.util.*

// Format a name, considering other uppercase letter, multiple words, the apostrophe and inner spaces
@ExperimentalStdlibApi
fun String.smartFixName(forceCapitalize: Boolean = false): String {
    return replace(Regex("(\\s)+"), " ")
        .trim()
        .split(" ").joinToString(" ") { it ->
            if (forceCapitalize) {
                it.lowercase(Locale.ROOT)
                it.replaceFirstChar {
                    if (it.isLowerCase())
                        it.titlecase(Locale.ROOT)
                    else it.toString()
                }
            } else it
        }
        .split("'").joinToString("'") { it ->
            if (forceCapitalize) {
                it.lowercase(Locale.ROOT)
                it.replaceFirstChar {
                    if (it.isLowerCase())
                        it.titlecase(Locale.ROOT)
                    else it.toString()
                }
            } else it
        }
        .split("-").joinToString("-") { it ->
            if (forceCapitalize) {
                it.lowercase(Locale.ROOT)
                it.replaceFirstChar {
                    if (it.isLowerCase())
                        it.titlecase(Locale.ROOT)
                    else it.toString()
                }
            } else it
        }
}

// Check if the string is written using letters, numbers, emoticons and only particular symbols
fun checkName(submission: String): Boolean {
    var apostropheFound = false
    var hyphenFound = false
    var ampersandFound = false

    if (submission == "\'") return false
    if (submission.startsWith('-')) return false
    if (submission.contains("-\'")) return false
    loop@ for (s in submission.replace("\\s".toRegex(), "")) {
        when {
            s.isSurrogate() -> continue@loop
            // Seems like numbers are allowed in certain countries!
            s.isDigit() -> continue@loop
            s.isLetter() -> continue@loop
            s == '-' && !hyphenFound -> hyphenFound = true
            s == '\'' && !apostropheFound -> apostropheFound = true
            s == '&' && !ampersandFound -> ampersandFound = true
            else -> return false
        }
    }
    return true
}