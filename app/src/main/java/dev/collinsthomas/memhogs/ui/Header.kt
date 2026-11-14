package dev.collinsthomas.memhogs.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.collinsthomas.memhogs.R

@Composable
internal fun Header(snapshot: UiSnapshot?, gauge: Gauge?, refreshing: Boolean, motion: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Pac(size = 20.dp, chomping = motion && refreshing)
        Spacer(Modifier.width(8.dp))
        MemoryBits(active = refreshing, motion = motion)
        Spacer(Modifier.width(10.dp))
        Text(
            stringResource(R.string.app_name),
            fontFamily = Mono,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = Palette.Accent,
        )
        Spacer(Modifier.weight(1f))
        if (snapshot != null) {
            Text(
                stringResource(R.string.refresh_hint),
                fontFamily = Mono,
                fontSize = 11.sp,
                color = Palette.Dim,
            )
        }
    }

    val (usedFraction, line) = when {
        snapshot != null -> {
            val used by animateFloatAsState(
                targetValue = snapshot.usedKb.toFloat(),
                animationSpec = tween(if (motion) 900 else 0),
                label = "used",
            )
            snapshot.usedFraction to pluralStringResource(
                R.plurals.header_used_with_processes,
                snapshot.processCount,
                humanReadableKb(used.toLong()),
                snapshot.totalText,
                usagePercent(used.toLong(), snapshot.totalKb),
                snapshot.processCount,
            )
        }
        gauge != null -> {
            val used = gauge.totalBytes - gauge.availBytes
            val fraction = if (gauge.totalBytes > 0) used.toFloat() / gauge.totalBytes else 0f
            fraction to
                stringResource(
                    R.string.header_used,
                    humanReadableKb(used / 1024),
                    humanReadableKb(
                        gauge.totalBytes / 1024,
                    ),
                    usagePercent(used, gauge.totalBytes),
                )
        }
        else -> 0f to ""
    }
    if (line.isNotEmpty()) {
        RamGauge(usedFraction = usedFraction)
        Text(
            line,
            fontFamily = Mono,
            fontSize = 12.sp,
            color = Palette.Dim,
            modifier = Modifier.padding(top = 7.dp, bottom = 8.dp),
        )
    }
}
