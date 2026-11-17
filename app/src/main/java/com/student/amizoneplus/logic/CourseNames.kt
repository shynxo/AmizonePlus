package com.student.amizoneplus.logic

/**
 * Short, familiar names for courses ("Object Oriented Programming Using C++" → "OOPs",
 * "Applied Mathematics- III" → "Maths III"). Known courses use the names students
 * actually say; anything else long becomes an acronym. Roman-numeral parts are kept.
 */
private val KNOWN = listOf(
    "object oriented programming" to "OOPs",
    "data structure" to "DSA",
    "design and analysis of algorithm" to "DAA",
    "applied mathematics" to "Maths",
    "engineering mathematics" to "Maths",
    "discrete mathematics" to "Discrete Maths",
    "probability and statistics" to "Prob & Stats",
    "basic electronics" to "Electronics",
    "digital electronics and computer organi" to "DECO",
    "digital electronics" to "Digital Elec.",
    "machine learning: theory" to "ML",
    "electronic devices" to "EDC",
    "elementary german" to "German",
    "elementary french" to "French",
    "elementary spanish" to "Spanish",
    "elementary japanese" to "Japanese",
    "operating system" to "OS",
    "database management" to "DBMS",
    "computer network" to "CN",
    "computer organi" to "COA",
    "computer architecture" to "COA",
    "theory of computation" to "TOC",
    "compiler" to "Compilers",
    "software engineering" to "SE",
    "artificial intelligence" to "AI",
    "machine learning" to "ML",
    "deep learning" to "DL",
    "web development" to "Web Dev",
    "environmental studies" to "EVS",
    "environmental science" to "EVS",
    "communication skill" to "Comm. Skills",
    "professional communication" to "Prof. Comm.",
    "behavioural science" to "Behavioural Sci.",
    "building self-reliance" to "BSR",
    "human values" to "Human Values",
    "engineering physics" to "Physics",
    "engineering chemistry" to "Chemistry",
)

private val SKIP = setOf("of", "and", "&", "using", "in", "for", "the", "to", "with", "a", "an", "on")

private val NUMERAL = Regex("""(?:-|\s)\s*(I{1,3}|IV|V|VI{0,3}|IX|X)\s*$""", RegexOption.IGNORE_CASE)

fun shortCourseName(name: String, max: Int = 16): String {
    val clean = name.replace(Regex("""\s+"""), " ").trim()
    val roman = NUMERAL.find(clean)?.groupValues?.get(1)?.uppercase()
    val base = if (roman != null) clean.substring(0, NUMERAL.find(clean)!!.range.first).trim().trimEnd('-', ' ') else clean
    val known = KNOWN.firstOrNull { base.lowercase().contains(it.first) }?.second
    val short = when {
        known != null -> known
        base.length <= max -> base
        else -> base.split(' ', '-', '/').filter { it.isNotBlank() && it.lowercase() !in SKIP }
            .joinToString("") { w -> if (w.all { it.isUpperCase() || it.isDigit() || it == '+' }) w else w.first().uppercase() }
    }
    return if (roman != null) "$short $roman" else short
}
