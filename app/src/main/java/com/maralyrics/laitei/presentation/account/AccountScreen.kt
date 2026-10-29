package com.maralyrics.laitei.presentation.account

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.maralyrics.laitei.R
import com.maralyrics.laitei.domain.model.Account
import com.maralyrics.laitei.domain.model.AuthFailure
import com.maralyrics.laitei.presentation.common.components.ProfileImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    onBackClick: () -> Unit,
    onOpenWebsite: (AccountWebMode) -> Unit,
    viewModel: AccountViewModel = hiltViewModel()
) {
    val account by viewModel.account.collectAsState()
    val signIn by viewModel.signIn.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    // The in-app language lives in the context the composition provides, so read strings through it.
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
            when (message) {
                AccountMessage.SignedOutEverywhere ->
                    snackbar.showSnackbar(context.getString(R.string.account_signed_out_everywhere_done))
                is AccountMessage.Failed -> snackbar.showSnackbar(failureText(context, message.failure))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.account_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.btn_back))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            val current = account
            if (current == null) {
                SignInForm(
                    state = signIn,
                    onSignIn = viewModel::signIn,
                    onEdit = viewModel::clearFailure,
                    onOpenWebsite = { onOpenWebsite(AccountWebMode.SIGN_IN) }
                )
            } else {
                SignedIn(
                    account = current,
                    onManage = { onOpenWebsite(AccountWebMode.MANAGE) },
                    onSignOut = viewModel::signOut,
                    onSignOutEverywhere = viewModel::signOutEverywhere
                )
            }
        }
    }
}

@Composable
private fun SignInForm(
    state: SignInUiState,
    onSignIn: (String, String) -> Unit,
    onEdit: () -> Unit,
    onOpenWebsite: () -> Unit
) {
    var identifier by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val canSubmit = identifier.isNotBlank() && password.isNotEmpty() && !state.isLoading

    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.account_sign_in_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.account_sign_in_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = identifier,
            onValueChange = { identifier = it; onEdit() },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.account_identifier)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) })
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; onEdit() },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.account_password)) },
            singleLine = true,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (canSubmit) onSignIn(identifier, password) }),
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = stringResource(
                            if (showPassword) R.string.account_hide_password else R.string.account_show_password
                        )
                    )
                }
            }
        )

        state.failure?.let { failure ->
            Text(
                text = failureText(LocalContext.current, failure),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Button(
            onClick = { onSignIn(identifier, password) },
            enabled = canSubmit,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text(stringResource(R.string.account_sign_in_button))
            }
        }

        // The security check, account creation and password reset all live on the website.
        if (state.failure == AuthFailure.ChallengeRequired) {
            Button(onClick = onOpenWebsite, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.account_continue_on_website))
            }
        }
        TextButton(onClick = onOpenWebsite, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.account_create_or_forgot))
        }
    }
}

/** The reader-facing text for why a sign-in (or another account call) failed. */
private fun failureText(context: Context, failure: AuthFailure): String = when (failure) {
    AuthFailure.InvalidCredentials -> context.getString(R.string.account_err_invalid)
    AuthFailure.ChallengeRequired -> context.getString(R.string.account_err_challenge)
    is AuthFailure.TooManyAttempts ->
        if (failure.minutes != null) context.getString(R.string.account_err_too_many, failure.minutes)
        else context.getString(R.string.account_err_too_many_generic)
    AuthFailure.Offline -> context.getString(R.string.account_err_offline)
    is AuthFailure.Server -> context.getString(R.string.account_err_server)
}

@Composable
private fun SignedIn(
    account: Account,
    onManage: () -> Unit,
    onSignOut: () -> Unit,
    onSignOutEverywhere: () -> Unit
) {
    var confirm by rememberSaveable { mutableStateOf<Confirm?>(null) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ProfileImage(imageUrl = null, name = account.label, size = 88.dp)
        Spacer(Modifier.height(12.dp))
        Text(
            text = account.label,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = "@${account.username}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        account.email?.let { email ->
            Spacer(Modifier.height(8.dp))
            Text(text = email, style = MaterialTheme.typography.bodyMedium)
            Surface(
                modifier = Modifier.padding(top = 6.dp),
                shape = MaterialTheme.shapes.large,
                color = if (account.emailVerified) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.errorContainer
            ) {
                Text(
                    text = stringResource(
                        if (account.emailVerified) R.string.account_email_verified else R.string.account_email_unverified
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }

    HorizontalDivider()
    ListItem(
        headlineContent = { Text(stringResource(R.string.account_manage)) },
        supportingContent = { Text(stringResource(R.string.account_manage_desc)) },
        leadingContent = { Icon(Icons.Default.Language, contentDescription = null) },
        modifier = Modifier.clickableRow(onManage)
    )
    ListItem(
        headlineContent = { Text(stringResource(R.string.account_sign_out_everywhere)) },
        leadingContent = { Icon(Icons.Default.Devices, contentDescription = null) },
        modifier = Modifier.clickableRow { confirm = Confirm.EVERYWHERE }
    )
    ListItem(
        headlineContent = { Text(stringResource(R.string.account_sign_out)) },
        leadingContent = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) },
        modifier = Modifier.clickableRow { confirm = Confirm.SIGN_OUT }
    )
    HorizontalDivider()
    ListItem(
        headlineContent = {
            Text(stringResource(R.string.account_delete), color = MaterialTheme.colorScheme.error)
        },
        supportingContent = { Text(stringResource(R.string.account_delete_desc)) },
        leadingContent = {
            Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error)
        },
        modifier = Modifier.clickableRow(onManage)
    )

    confirm?.let { which ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = {
                Text(stringResource(if (which == Confirm.SIGN_OUT) R.string.account_sign_out_confirm_title else R.string.account_sign_out_everywhere))
            },
            text = {
                Text(stringResource(if (which == Confirm.SIGN_OUT) R.string.account_sign_out_confirm else R.string.account_sign_out_everywhere_confirm))
            },
            confirmButton = {
                TextButton(onClick = {
                    confirm = null
                    if (which == Confirm.SIGN_OUT) onSignOut() else onSignOutEverywhere()
                }) { Text(stringResource(R.string.account_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirm = null }) { Text(stringResource(R.string.btn_cancel)) }
            }
        )
    }
}

private enum class Confirm { SIGN_OUT, EVERYWHERE }

private fun Modifier.clickableRow(onClick: () -> Unit): Modifier =
    this.then(Modifier.clickable(onClick = onClick))
