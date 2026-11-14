package com.student.amizoneplus.ui.kit

/** Little lines that give the app its character. Signed, lovingly, by Shynxo. */
object Quips {
    private val loadingLines = listOf(
        "Shynxo is fetching your stuff…",
        "Bribing the Amizone servers…",
        "Warming up the hamsters…",
        "Asking Amizone nicely…",
        "Shynxo pinky-promises this is fast…",
        "Untangling the internet…",
    )
    private val footerLines = listOf(
        "Handcrafted by Shynxo ✦",
        "Shynxo made this. Go attend a class.",
        "Built at 3 a.m. by Shynxo",
        "A Shynxo production",
        "Shynxo was here ✌",
        "Powered by chai and Shynxo",
    )
    private val emptyLines = listOf(
        "Nothing here. Shynxo checked twice.",
        "Empty as a Monday 8 a.m. class.",
        "Nothing yet. Enjoy the silence.",
    )
    private val tapLines = listOf(
        "Hey! That tickles.",
        "Shynxo says hi 👋",
        "You found the secret. There is no secret.",
        "Go drink some water 💧",
        "75% or bust.",
        "Bunking? Shynxo is watching 👀",
        "Okay okay, you can stop now.",
    )

    /** Funny faces for days with no classes. */
    val freeDay = listOf("😴", "🏖️", "🎮", "🍕", "🛌", "🦥", "🎧", "🍿", "🧋", "🐢")

    fun loading() = loadingLines.random()
    fun footer() = footerLines.random()
    fun empty() = emptyLines.random()
    fun tap(n: Int) = tapLines[n % tapLines.size]

    /** The same emoji for a given day, so circles don't reshuffle on every redraw. */
    fun freeDayFor(epochDay: Long) = freeDay[Math.floorMod(epochDay * 7 + 3, freeDay.size.toLong()).toInt()]
}
