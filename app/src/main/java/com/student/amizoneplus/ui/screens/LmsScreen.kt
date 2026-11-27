package com.student.amizoneplus.ui.screens

import com.student.amizoneplus.ui.kit.SquircleShape
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Slideshow
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.student.amizoneplus.data.LmsCourse
import com.student.amizoneplus.data.LmsFile
import com.student.amizoneplus.data.LmsItem
import com.student.amizoneplus.data.LmsSection
import com.student.amizoneplus.data.LmsSemester
import com.student.amizoneplus.data.Lms
import com.student.amizoneplus.ui.AppViewModel
import com.student.amizoneplus.ui.UiState
import com.student.amizoneplus.ui.kit.Caps
import com.student.amizoneplus.ui.kit.Chip
import com.student.amizoneplus.ui.kit.Feel
import com.student.amizoneplus.ui.kit.OutlineRow
import com.student.amizoneplus.ui.kit.Rise
import com.student.amizoneplus.ui.kit.SectionLabel
import com.student.amizoneplus.ui.kit.T
import com.student.amizoneplus.ui.kit.Tone
import com.student.amizoneplus.ui.kit.UnfoldCard
import com.student.amizoneplus.ui.kit.press
import com.student.amizoneplus.ui.shell.HeroPage
import com.student.amizoneplus.ui.kit.StatTile
import com.student.amizoneplus.ui.theme.LocalHaptics
import com.student.amizoneplus.ui.theme.Motion
import com.student.amizoneplus.ui.theme.P
import com.student.amizoneplus.ui.theme.Type
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.coroutines.launch

private fun modIcon(m: String): ImageVector = when (m) {
    "resource" -> Icons.AutoMirrored.Outlined.InsertDriveFile
    "folder" -> Icons.Outlined.Folder
    "url" -> Icons.Outlined.Link
    "page" -> Icons.Outlined.Description
    "book" -> Icons.Outlined.Book
    "forum" -> Icons.Outlined.Forum
    "assign" -> Icons.Outlined.Assignment
    "quiz" -> Icons.Outlined.HelpOutline
    else -> Icons.Outlined.ViewInAr
}

private fun fileIcon(name: String): ImageVector {
    val e = name.substringAfterLast('.', "").lowercase()
    return when {
        e == "pdf" -> Icons.Outlined.Description
        e.startsWith("ppt") -> Icons.Outlined.Slideshow
        e.startsWith("xls") || e == "csv" -> Icons.Outlined.GridOn
        e in setOf("zip", "rar", "7z") -> Icons.Outlined.Archive
        e in setOf("png", "jpg", "jpeg", "gif") -> Icons.Outlined.Image
        e in setOf("mp4", "mkv", "mov") -> Icons.Outlined.Videocam
        else -> Icons.AutoMirrored.Outlined.InsertDriveFile
    }
}

private fun labelFor(m: String) = mapOf("resource" to "File", "url" to "Link", "page" to "Page", "book" to "Book", "forum" to "Forum",
    "assign" to "Assignment", "quiz" to "Quiz", "lesson" to "Lesson")[m] ?: m.replaceFirstChar { it.uppercase() }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LmsScreen(vm: AppViewModel, s: UiState, focusCode: String?, onFocusHandled: () -> Unit, openWeb: (String, String) -> Unit) {
    val lms = vm.repo.lms
    val storage = vm.repo.storage
    val context = LocalContext.current
    val haptics = LocalHaptics.current
    val scope = rememberCoroutineScope()

    var sems by remember { mutableStateOf(listOf<LmsSemester>()) }
    var semIdx by remember { mutableIntStateOf(0) }
    var courses by remember { mutableStateOf<List<LmsCourse>?>(null) }
    var course by remember { mutableStateOf<LmsCourse?>(null) }
    var content by remember { mutableStateOf<List<LmsSection>?>(null) }
    val folders = remember { mutableStateMapOf<String, List<LmsFile>?>() } // null = loading
    var busy by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val sem = sems.getOrNull(semIdx)
    fun key(k: String) = "lms2/${sem?.label?.replace(Regex("[^A-Za-z0-9]"), "_")}_$k.json"

    LaunchedEffect(Unit) {
        try { sems = lms.semesters() } catch (e: Exception) { error = e.message ?: "Could not reach Amizone." }
    }

    suspend fun loadCourses(user: Boolean) {
        val s0 = sem ?: return
        courses = storage.readJson(key("courses"), ListSerializer(LmsCourse.serializer())) ?: courses
        loading = true
        try {
            val c = lms.courses(s0); courses = c; storage.writeJson(key("courses"), ListSerializer(LmsCourse.serializer()), c); error = null
            if (user) haptics.success()
        } catch (e: Exception) { error = e.message ?: "LMS unavailable."; if (user) haptics.error() } finally { loading = false }
    }

    suspend fun openCourse(c: LmsCourse, user: Boolean) {
        val s0 = sem ?: return
        course = c; folders.clear()
        content = storage.readJson(key("c${c.id}"), ListSerializer(LmsSection.serializer()))
        loading = true
        try {
            val x = lms.content(s0, c.id); content = x; storage.writeJson(key("c${c.id}"), ListSerializer(LmsSection.serializer()), x); error = null
            if (user) haptics.success()
        } catch (e: Exception) { error = e.message ?: "Could not load this course."; if (user) haptics.error() } finally { loading = false }
    }

    LaunchedEffect(sem?.label) { course = null; courses = null; loadCourses(false) }
    LaunchedEffect(focusCode, courses) {
        val code = focusCode ?: return@LaunchedEffect
        val match = courses?.firstOrNull { it.code.equals(code, true) } ?: return@LaunchedEffect
        openCourse(match, false); onFocusHandled()
    }
    if (course != null) BackHandler { course = null; content = null }

    fun subjectName(c: LmsCourse) = s.data?.courses?.firstOrNull { it.code.equals(c.code, true) }?.name ?: c.fullname

    fun toggleFolder(item: LmsItem) {
        val s0 = sem ?: return
        if (folders.containsKey(item.id)) { haptics.collapse(); folders.remove(item.id); return }
        haptics.expand()
        folders[item.id] = storage.readJson(key("f${item.id}"), ListSerializer(LmsFile.serializer()))
        scope.launch {
            try {
                val f = lms.folderFiles(s0, item); folders[item.id] = f; storage.writeJson(key("f${item.id}"), ListSerializer(LmsFile.serializer()), f)
            } catch (e: Exception) { haptics.error(); if (folders[item.id] == null) folders[item.id] = emptyList() }
        }
    }

    fun open(url: String, name: String, modname: String = "resource") {
        val s0 = sem ?: return
        if (busy != null) return
        haptics.press()
        if (modname in setOf("forum", "assign", "quiz", "page", "book", "lesson", "h5pactivity", "scorm")) { openWeb(url, name); return }
        busy = url
        scope.launch {
            try {
                when (val r = lms.open(context, s0, url, name)) {
                    is Lms.OpenResult.Web -> if (modname == "url") context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(r.url))) else openWeb(r.url, name)
                    Lms.OpenResult.Opened -> haptics.success()
                }
            } catch (e: Exception) { haptics.error(); error = e.message ?: "Couldn't open that file." } finally { busy = null }
        }
    }

    AnimatedContent(
        course,
        transitionSpec = {
            (fadeIn(tween(300, easing = Motion.emphasized)) + slideInHorizontally(tween(420, easing = Motion.emphasized)) { if (targetState != null) it / 8 else -it / 8 }) togetherWith fadeOut(tween(120))
        },
        label = "lms",
    ) { current ->
        HeroPage(
            heroColor = P.sky,
            refreshing = loading,
            onRefresh = { scope.launch { if (current != null) openCourse(current, true) else loadCourses(true) } },
            hero = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (current != null) com.student.amizoneplus.ui.kit.CircleButton(Icons.AutoMirrored.Outlined.ArrowBack, { course = null; content = null }, 48.dp)
                    T(if (current != null) "COURSE" else "LMS", Type.display, color = P.onHero, modifier = Modifier.weight(1f))
                    if (current == null && sems.size > 1) com.student.amizoneplus.ui.kit.DropdownPill(
                        sems.getOrNull(semIdx)?.label?.substringBefore(" ")?.ifEmpty { null } ?: (sems.getOrNull(semIdx)?.label ?: "semester"),
                        { semIdx = (semIdx + 1) % sems.size },
                    )
                    com.student.amizoneplus.ui.shell.MenuButton()
                }
                if (current != null) {
                    T(subjectName(current).uppercase(), Type.display, color = P.onHero, maxLines = 3, modifier = Modifier.padding(top = 16.dp))
                    Box(Modifier.padding(top = 10.dp)) { Chip(current.code.ifEmpty { current.shortname }, Tone.Cream) }
                } else {
                    T("Slides, notes and assignments from Amizone LMS — arranged the way each teacher uploaded them.", Type.body, color = P.onHero.copy(alpha = 0.75f),
                        modifier = Modifier.padding(top = 14.dp))
                    Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile("${courses?.size ?: 0}", "courses", P.onHero.copy(alpha = 0.08f))
                        StatTile("${s.data?.notices?.size ?: 0}", "documents", P.onHero.copy(alpha = 0.08f))
                    }
                }
            },
        ) {
            error?.let { Box(Modifier.fillMaxWidth().background(P.badSoft, SquircleShape(22.dp)).padding(16.dp)) { T(it, Type.caption) } }
            if (current != null) {
                val c = content
                if (c == null) Spinner()
                else if (c.all { it.items.isEmpty() }) Empty("Nothing has been uploaded for this subject yet.")
                var idx = 0
                c?.forEachIndexed { si, sec ->
                    if (sec.items.isEmpty()) return@forEachIndexed
                    Rise(idx++) { SectionLabel(sec.title.ifEmpty { "Section ${si + 1}" }) }
                    sec.items.forEach { it ->
                        Rise(idx++) {
                            when (it.modname) {
                                "label" -> T(it.name, Type.body, sub = true, modifier = Modifier.padding(horizontal = 20.dp))
                                "folder" -> FolderBlock(it, folders.containsKey(it.id), folders[it.id], { toggleFolder(it) }, { f -> open(f.url, f.name) }, busy)
                                else -> OutlineRow(it.name, labelFor(it.modname), { open(lms.itemUrl(it), it.name, it.modname) }) {
                                    if (busy == lms.itemUrl(it)) CircularProgressIndicator(color = P.ink, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    else Icon(modIcon(it.modname), null, tint = P.text)
                                }
                            }
                        }
                    }
                }
            } else {
                val list = courses
                if (list == null) Spinner() else if (list.isEmpty()) Empty("No LMS courses for this semester.")
                list?.forEachIndexed { i, c ->
                    Rise(i) {
                        com.student.amizoneplus.ui.kit.Card(onClick = { scope.launch { openCourse(c, false) } }) {
                            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                com.student.amizoneplus.ui.kit.Avatar(c.code.ifEmpty { c.fullname }, listOf(P.sky, P.sage, P.sun, P.lilac)[i % 4], 52.dp)
                                Column(Modifier.weight(1f)) {
                                    T(subjectName(c).uppercase(), Type.title, maxLines = 2)
                                    T(c.code.ifEmpty { c.shortname }, Type.caption, sub = true, maxLines = 1, modifier = Modifier.padding(top = 4.dp))
                                }
                                com.student.amizoneplus.ui.kit.ArrowButton({ scope.launch { openCourse(c, false) } })
                            }
                        }
                    }
                }
                val notices = s.data?.notices.orEmpty()
                if (notices.isNotEmpty()) {
                    Rise((list?.size ?: 0) + 1) { SectionLabel("University documents") }
                    notices.forEachIndexed { i, d ->
                        Rise((list?.size ?: 0) + 2 + i) {
                            OutlineRow(d.title, d.description, { haptics.tap(); context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(d.url))) }) {
                                Icon(Icons.Outlined.Download, null, tint = P.text)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderBlock(item: LmsItem, open: Boolean, files: List<LmsFile>?, onToggle: () -> Unit, onOpen: (LmsFile) -> Unit, busy: String?) {
    UnfoldCard(
        open = open,
        onToggle = onToggle,
        head = { onInk ->
            Icon(if (onInk) Icons.Outlined.Folder else Icons.Outlined.Folder, null, tint = if (onInk) P.lav else P.text, modifier = Modifier.size(22.dp))
            T(item.name, Type.label, ink = onInk, maxLines = 2, modifier = Modifier.weight(1f))
            if (!onInk && files != null) T("${files.size}", Type.caption, sub = true)
        },
    ) {
        Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
            when {
                files == null -> Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = P.lav) }
                files.isEmpty() -> T("This folder is empty.", Type.body, ink = true, sub = true, modifier = Modifier.padding(vertical = 12.dp))
                else -> files.groupBy { it.path }.forEach { (path, list) ->
                    if (path.isNotEmpty()) Caps(path, P.onInkFaint, Modifier.padding(top = 12.dp, bottom = 4.dp))
                    list.forEach { f ->
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 60.dp).press({ onOpen(f) }, Feel.None, 0.98f).padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Icon(fileIcon(f.name), null, tint = P.onInkSub, modifier = Modifier.size(24.dp))
                            T(f.name.substringBeforeLast('.'), Type.body, ink = true, maxLines = 2, modifier = Modifier.weight(1f))
                            if (busy == f.url) CircularProgressIndicator(color = P.lav, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            else f.name.substringAfterLast('.', "").takeIf { it.isNotEmpty() && it.length <= 5 }?.let { Chip(it.uppercase(), Tone.Ghost) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Empty(text: String) {
    val wink = androidx.compose.runtime.remember { com.student.amizoneplus.ui.kit.Quips.empty() }
    androidx.compose.foundation.layout.Column(Modifier.fillMaxWidth().padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        T(text, Type.body, sub = true, align = TextAlign.Center)
        T(wink, Type.caption, faint = true, align = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
    }
}
