package com.matuncnn.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.matuncnn.app.ui.theme.AppColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.matuncnn.app.util.DownloadProgress

@Composable
fun DownloadScreen(
    progress: DownloadProgress,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (progress.error.isNotBlank()) {
            Icon(
                Icons.Filled.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = AppColors.statusError
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Download Failed",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                progress.error,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.textSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onRetry) {
                Icon(Icons.Filled.Refresh, null, Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Retry")
            }
        } else {
            Icon(
                Icons.Filled.CloudDownload,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = AppColors.accent
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Setting up MatUnCNN",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                progress.message,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.textSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    val isExtracting = progress.message.startsWith("Extracting")
                    val hasTotal = progress.totalBytes > 0

                    if (hasTotal) {
                        val animatedProgress by animateFloatAsState(
                            targetValue = progress.bytesDownloaded.toFloat() / progress.totalBytes,
                            label = "progress"
                        )
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.fillMaxWidth().height(8.dp)
                        )
                    } else {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(8.dp)
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    if (hasTotal) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                formatBytes(progress.bytesDownloaded),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                formatBytes(progress.totalBytes),
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.textSecondary
                            )
                        }
                        val pct = (progress.bytesDownloaded * 100 / progress.totalBytes).toInt()
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "$pct%",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }

                    if (isExtracting) {
                        Text(
                            progress.message,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            color = AppColors.textPrimary
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "Downloading model assets from GitHub Releases...\nThis may take a few minutes.",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.textSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "%.1f MB".format(bytes.toDouble() / (1024 * 1024))
    }
}
