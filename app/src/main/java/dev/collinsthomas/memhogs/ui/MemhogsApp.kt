package dev.collinsthomas.memhogs.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.collinsthomas.memhogs.R
import dev.collinsthomas.memhogs.shizuku.ShizukuAccess

@Composable
fun MemhogsApp(
    state: MemhogsUiState,
    onRefresh: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenShizuku: () -> Unit,
    onGetShizuku: () -> Unit,
    onReclaim: (Set<String>) -> Unit,
    onForceStop: (Set<String>) -> Unit,
) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Palette.Accent,
            onPrimary = Palette.Surface,
            primaryContainer = Palette.AccentTint,
            onPrimaryContainer = Palette.Accent,
            secondary = Palette.Cyan,
            onSecondary = Palette.Surface,
            tertiary = Palette.Green,
            onTertiary = Palette.Surface,
            background = Palette.Background,
            onBackground = Palette.Text,
            surface = Palette.Surface,
            onSurface = Palette.Text,
            surfaceVariant = Palette.Cell,
            onSurfaceVariant = Palette.Dim,
            surfaceContainer = Palette.Surface,
            surfaceContainerLow = Palette.Background,
            surfaceContainerHigh = Palette.Cell,
            outline = Palette.Border,
            outlineVariant = Palette.Cell,
            error = Palette.Red,
            onError = Palette.Surface,
            errorContainer = Palette.RedTint,
            onErrorContainer = Palette.Red,
        ),
    ) {
        Surface(Modifier.fillMaxSize(), color = Palette.Background) {
            val access = state.access
            val motion = state.motion
            Column(
                Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(horizontal = 16.dp),
            ) {
                Header(
                    snapshot = state.snapshot,
                    gauge = state.gauge,
                    refreshing = state.refreshing,
                    motion = motion,
                )
                AnimatedVisibility(visible = state.reclaimResult != null) {
                    Text(
                        state.reclaimResult?.message() ?: "",
                        fontFamily = Mono,
                        fontSize = 12.sp,
                        color = Palette.Green,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                when (access) {
                    ShizukuAccess.NOT_RUNNING -> SetupScreen(
                        state.shizukuInstalled,
                        motion,
                        onOpenShizuku,
                        onGetShizuku,
                        onRefresh,
                    )
                    ShizukuAccess.NEEDS_PERMISSION -> PermissionScreen(motion, onRequestPermission)
                    ShizukuAccess.READY -> when {
                        state.error != null && state.snapshot == null -> ErrorScreen(
                            state.error.message(),
                            state.error.hint(),
                            motion,
                            onRefresh,
                        )
                        state.snapshot == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            EatingLoader(motion)
                        }
                        else -> {
                            state.error?.let { error ->
                                Text(
                                    error.message(),
                                    fontFamily = Mono,
                                    fontSize = 12.sp,
                                    color = Palette.Red,
                                    modifier = Modifier.padding(bottom = 8.dp),
                                )
                            }
                            GroupList(
                                snapshot = state.snapshot,
                                refreshing = state.refreshing,
                                motion = motion,
                                onRefresh = onRefresh,
                                onReclaim = onReclaim,
                                onForceStop = onForceStop,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReclaimResult.message(): String {
    val subject = appsSubject(labels)
    return when (this) {
        is ReclaimResult.Reclaimed -> stringResource(R.string.reclaim_freed, subject, humanReadableKb(freedKb))
        is ReclaimResult.NothingToReclaim -> stringResource(R.string.reclaim_nothing, subject)
    }
}

@Composable
internal fun appsSubject(labels: List<String>): String =
    labels.singleOrNull() ?: pluralStringResource(R.plurals.app_count, labels.size, labels.size)

@Composable
private fun LoadError.message(): String = when (this) {
    LoadError.EmptyMeminfo -> stringResource(R.string.error_empty_meminfo)
    LoadError.ShellDidNotStart -> stringResource(R.string.error_shell_did_not_start)
    LoadError.ShellDisconnected -> stringResource(R.string.error_shell_disconnected)
    is LoadError.Failed -> detail
}

@Composable
private fun LoadError.hint(): String? = when (this) {
    LoadError.ShellDidNotStart -> stringResource(R.string.error_shell_did_not_start_hint)
    LoadError.EmptyMeminfo, LoadError.ShellDisconnected, is LoadError.Failed -> null
}
