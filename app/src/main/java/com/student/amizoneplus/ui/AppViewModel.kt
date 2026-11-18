package com.student.amizoneplus.ui

import android.app.Application
import android.webkit.CookieManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.student.amizoneplus.data.AmizoneData
import com.student.amizoneplus.data.AmizoneException
import com.student.amizoneplus.data.Credentials
import com.student.amizoneplus.data.Repository
import com.student.amizoneplus.data.Settings
import com.student.amizoneplus.logic.Badge
import com.student.amizoneplus.logic.earnedBadges
import com.student.amizoneplus.work.Notifier
import com.student.amizoneplus.work.RefreshWorker
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class Status { Booting, SignedOut, SignedIn }

data class UiState(
    val status: Status = Status.Booting,
    val data: AmizoneData? = null,
    val refreshing: Boolean = false,
    val error: String? = null,
    val needsWebLogin: Boolean = false,
    val webLoginOpen: Boolean = false,
    val settings: Settings = Settings(),
    val username: String? = null,
    val celebrate: Badge? = null,
)

/** One-off feedback the UI turns into haptics. */
enum class Feedback { Success, Warning, Error }

class AppViewModel(app: Application) : AndroidViewModel(app) {
    val repo = Repository.get(app)
    private val storage = repo.storage

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    private val _feedback = Channel<Feedback>(Channel.BUFFERED)
    val feedback = _feedback.receiveAsFlow()

    private var refreshJob: Job? = null
    private var pendingCreds: Credentials? = null

    init {
        val settings = storage.loadSettings()
        val creds = storage.loadCredentials()
        if (creds == null) {
            _state.value = UiState(status = Status.SignedOut, settings = settings)
        } else {
            _state.value = UiState(status = Status.SignedIn, data = storage.loadCache(), settings = settings, username = creds.username)
            refresh()
            RefreshWorker.schedule(app)
            com.student.amizoneplus.work.KeepAlive.schedule(app)
            com.student.amizoneplus.work.KeepAliveWorker.schedule(app)
        }
    }

    fun refresh(user: Boolean = false) {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            _state.update { it.copy(refreshing = true) }
            try {
                val fresh = repo.refreshAll()
                _state.update {
                    it.copy(data = fresh, refreshing = false, needsWebLogin = false,
                        error = if (fresh.errors.isNotEmpty()) "Couldn't load: ${fresh.errors.joinToString()}" else null)
                }
                if (user) _feedback.send(if (fresh.errors.isEmpty()) Feedback.Success else Feedback.Warning)
                checkBadges(fresh)
            } catch (e: AmizoneException.Network) {
                _state.update { it.copy(refreshing = false, error = e.message) }
                if (user) _feedback.send(Feedback.Error)
            } catch (e: AmizoneException) {
                _state.update { it.copy(refreshing = false, error = e.message, needsWebLogin = true) }
                if (user) _feedback.send(Feedback.Error)
            } catch (e: Exception) {
                _state.update { it.copy(refreshing = false, error = e.message ?: "Something went wrong.") }
                if (user) _feedback.send(Feedback.Error)
            }
        }
    }

    /** Throws a user-facing message on failure; opens the website login if Amizone wants it. */
    suspend fun signIn(c: Credentials) {
        try {
            repo.client.login(c)
        } catch (e: AmizoneException.NeedsWebLogin) {
            pendingCreds = c
            _state.update { it.copy(error = e.message, webLoginOpen = true) }
            return
        }
        enterApp(c)
    }

    private fun enterApp(c: Credentials) {
        storage.saveCredentials(c)
        _state.update { it.copy(status = Status.SignedIn, username = c.username, error = null) }
        RefreshWorker.schedule(getApplication())
        com.student.amizoneplus.work.KeepAlive.schedule(getApplication())
        com.student.amizoneplus.work.KeepAliveWorker.schedule(getApplication())
        refresh()
    }

    fun openWebLogin(c: Credentials? = null) {
        pendingCreds = c ?: storage.loadCredentials()
        _state.update { it.copy(webLoginOpen = true) }
    }

    fun webLoginFinished() {
        com.student.amizoneplus.data.Http.persistSession()
        val c = pendingCreds
        _state.update { it.copy(webLoginOpen = false, needsWebLogin = false, error = null) }
        if (_state.value.status != Status.SignedIn && c != null) enterApp(c) else refresh()
    }

    fun closeWebLogin() = _state.update { it.copy(webLoginOpen = false) }

    /** Used by in-app web pages when their session has expired. */
    suspend fun relogin(): Boolean = runCatching { repo.client.login() }.isSuccess

    fun pendingCredentials() = pendingCreds

    /** Your Amity email password (from the dashboard), for the "reveal" row in About. */
    fun emailPassword(): String? = repo.storage.loadSecret("email_pw")

    fun signOut() {
        RefreshWorker.cancel(getApplication())
        com.student.amizoneplus.work.KeepAlive.cancel(getApplication())
        com.student.amizoneplus.work.KeepAliveWorker.cancel(getApplication())
        storage.clearAll()
        CookieManager.getInstance().removeAllCookies(null)
        _state.value = UiState(status = Status.SignedOut)
    }

    fun updateSettings(transform: (Settings) -> Settings) {
        val next = transform(_state.value.settings)
        storage.saveSettings(next)
        _state.update { it.copy(settings = next) }
        _state.value.data?.let { d -> viewModelScope.launch { Notifier.process(getApplication(), d, next, storage) } }
    }

    private fun checkBadges(d: AmizoneData) {
        val earned = earnedBadges(d, _state.value.settings.threshold)
        val seen = storage.seenBadges
        val fresh = earned.filter { it.id !in seen }
        if (fresh.isNotEmpty()) {
            storage.seenBadges = seen + fresh.map { it.id }
            viewModelScope.launch {
                kotlinx.coroutines.delay(900)
                _state.update { it.copy(celebrate = fresh.first()) }
            }
        }
    }

    fun dismissCelebration() = _state.update { it.copy(celebrate = null) }
}
