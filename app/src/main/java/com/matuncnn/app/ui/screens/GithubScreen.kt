package com.matuncnn.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class RepoInfo(
    val name: String = "MatUnCNN",
    val description: String = "",
    val stars: Int = 0,
    val forks: Int = 0,
    val openIssues: Int = 0,
    val license: String = "",
    val owner: String = "Celesth",
    val ownerAvatar: String = "",
    val htmlUrl: String = "https://github.com/Celesth/MatUnCNN",
    val releasesUrl: String = "",
    val latestRelease: String = "",
    val loading: Boolean = true,
    val error: String = ""
)

@Composable
fun GithubScreen() {
    var repoInfo by remember { mutableStateOf(RepoInfo()) }

    LaunchedEffect(Unit) {
        repoInfo = repoInfo.copy(loading = true, error = "")
        try {
            val result = withContext(Dispatchers.IO) {
                val conn = URL("https://api.github.com/repos/Celesth/MatUnCNN").openConnection() as HttpURLConnection
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                val code = conn.responseCode
                if (code == 200) {
                    val body = conn.inputStream.bufferedReader().readText()
                    val json = JSONObject(body)
                    val licenseObj = json.optJSONObject("license")
                    RepoInfo(
                        name = json.optString("name", "MatUnCNN"),
                        description = json.optString("description", ""),
                        stars = json.optInt("stargazers_count", 0),
                        forks = json.optInt("forks_count", 0),
                        openIssues = json.optInt("open_issues_count", 0),
                        license = licenseObj?.optString("spdx_id", "") ?: "",
                        owner = json.getJSONObject("owner").optString("login", "Celesth"),
                        ownerAvatar = json.getJSONObject("owner").optString("avatar_url", ""),
                        htmlUrl = json.optString("html_url", "https://github.com/Celesth/MatUnCNN"),
                        releasesUrl = json.optString("releases_url", "").replace("{/id}", ""),
                        loading = false
                    )
                } else {
                    RepoInfo(loading = false, error = "HTTP $code")
                }
            }
            repoInfo = result
        } catch (e: Exception) {
            repoInfo = RepoInfo(loading = false, error = e.message ?: "Unknown error")
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (repoInfo.loading) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text("Fetching repo info...")
                }
            }
        } else if (repoInfo.error.isNotBlank()) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.BugReport, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        Text("Could not fetch repo info", style = MaterialTheme.typography.titleSmall)
                        Text(repoInfo.error, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Text("Visit: github.com/Celesth/MatUnCNN",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        } else {
            item {
                Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Repository", style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(repoInfo.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        if (repoInfo.description.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(repoInfo.description, style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3, overflow = TextOverflow.Ellipsis)
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            StatItem(Icons.Default.Star, "${repoInfo.stars}", "Stars")
                            StatItem(Icons.Filled.Person, "${repoInfo.forks}", "Forks")
                            StatItem(Icons.Default.BugReport, "${repoInfo.openIssues}", "Issues")
                        }
                        if (repoInfo.license.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text("License: ${repoInfo.license}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.AccountCircle, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Developer", style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(repoInfo.owner, style = MaterialTheme.typography.bodyLarge)
                        Text("GitHub", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            item {
                val context = LocalContext.current
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(repoInfo.htmlUrl))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Info, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Open on GitHub")
                }
            }
        }
    }
}

@Composable
private fun StatItem(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
