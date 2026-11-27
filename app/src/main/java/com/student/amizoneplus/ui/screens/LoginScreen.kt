package com.student.amizoneplus.ui.screens

import com.student.amizoneplus.ui.kit.SquircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.student.amizoneplus.data.AmizoneException
import com.student.amizoneplus.data.Credentials
import com.student.amizoneplus.ui.AppViewModel
import com.student.amizoneplus.ui.kit.Caps
import com.student.amizoneplus.ui.kit.Feel
import com.student.amizoneplus.ui.kit.T
import com.student.amizoneplus.ui.kit.press
import com.student.amizoneplus.ui.theme.LocalHaptics
import com.student.amizoneplus.ui.theme.P
import com.student.amizoneplus.ui.theme.Type
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(vm: AppViewModel, ctxError: String?) {
    val scope = rememberCoroutineScope()
    val haptics = LocalHaptics.current
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun submit() {
        if (user.isBlank() || pass.isEmpty()) { error = "Enter your Amizone ID and password."; haptics.warning(); return }
        busy = true; error = null
        scope.launch {
            try {
                vm.signIn(Credentials(user.trim(), pass))
            } catch (e: AmizoneException) {
                error = e.message; haptics.error()
            } catch (e: Exception) {
                error = e.message ?: "Sign in failed."; haptics.error()
            } finally {
                busy = false
            }
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        unfocusedBorderColor = P.hairline, focusedTextColor = P.text, unfocusedTextColor = P.text,
        cursorColor = P.text, focusedContainerColor = P.card, unfocusedContainerColor = P.card, focusedBorderColor = P.text,
    )

    Column(
        // Scrolls, so the password box and Sign in button stay reachable above the keyboard
        // on small screens; clears the system nav buttons at the bottom.
        Modifier.fillMaxSize().background(P.canvas).statusBarsPadding().navigationBarsPadding().imePadding()
            .verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(64.dp).background(P.sun, SquircleShape(20.dp)), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.School, null, tint = P.onHero, modifier = Modifier.size(30.dp))
        }
        T("WELCOME", Type.hero, modifier = Modifier.padding(top = 24.dp))
        T("a Shynxo production ✦", Type.caption, faint = true)
        T("Sign in with your Amizone account. You'll only need to do this once.", Type.body, sub = true, modifier = Modifier.padding(top = 6.dp, bottom = 32.dp))

        T("Amizone ID", Type.caption, sub = true, modifier = Modifier.padding(bottom = 8.dp))
        OutlinedTextField(
            user, { user = it }, Modifier.fillMaxWidth(), singleLine = true, shape = SquircleShape(18.dp), colors = fieldColors,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Next),
        )
        T("Password", Type.caption, sub = true, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
        OutlinedTextField(
            pass, { pass = it }, Modifier.fillMaxWidth(), singleLine = true, shape = SquircleShape(18.dp), colors = fieldColors,
            visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { submit() }),
            trailingIcon = {
                Icon(if (show) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, null, tint = P.sub,
                    modifier = Modifier.press({ show = !show }, Feel.Select))
            },
        )

        (error ?: ctxError)?.let { T(it, Type.body, color = P.bad, modifier = Modifier.padding(top = 16.dp)) }

        Row(
            Modifier.padding(top = 24.dp).fillMaxWidth().height(58.dp).press(if (busy) null else ::submit, Feel.Press)
                .background(P.cream, SquircleShape(50)),
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
        ) {
            if (busy) CircularProgressIndicator(color = P.onCream, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            else Caps("SIGN IN", P.onCream)
        }

        T(
            "Sign in through the Amizone website", Type.label.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium), color = P.text,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 20.dp)
                .press({
                    if (user.isBlank() || pass.isEmpty()) error = "Enter your Amizone ID and password first so the app can remember them."
                    else vm.openWebLogin(Credentials(user.trim(), pass))
                }),
        )

        Row(Modifier.padding(top = 36.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Lock, null, tint = P.faint, modifier = Modifier.size(14.dp))
            T("  Your password is encrypted on this phone (Android Keystore) and sent only to s.amizone.net.",
                Type.caption, faint = true)
        }
    }
}
