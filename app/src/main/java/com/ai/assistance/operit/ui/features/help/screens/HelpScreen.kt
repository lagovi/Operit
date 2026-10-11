package com.ai.assistance.operit.ui.features.help.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ai.assistance.operit.ui.features.help.content.HelpTopic
import com.ai.assistance.operit.ui.features.help.content.HelpTopicHolder
import com.ai.assistance.operit.ui.features.help.content.HelpTopics
import com.ai.assistance.operit.ui.features.help.content.helpTopic

/**
 * Offline built-in help (fork): topic list + per-screen detail.
 * The "?" button sets [HelpTopicHolder.topic] before navigating here.
 */
@Composable
fun HelpScreen(onBackPressed: () -> Unit = {}) {
    var currentId by remember { mutableStateOf(HelpTopicHolder.topic.also { HelpTopicHolder.topic = "" }) }
    val topic = currentId.takeIf { it.isNotBlank() }?.let { helpTopic(it) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                if (currentId.isNotBlank()) currentId = "" else onBackPressed()
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = if (topic != null) topic.title else "Help",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (topic != null) {
            HelpDetail(topic)
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
                items(HelpTopics) { t ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            .clickable { currentId = t.id }
                    ) {
                        Text(
                            text = t.title,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun HelpDetail(topic: HelpTopic) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
        item {
            Text(
                text = topic.intro,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 10.dp)
            )
        }
        items(topic.sections) { s ->
            Text(
                text = s.header,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
            )
            Text(text = s.body, style = MaterialTheme.typography.bodyMedium)
        }
        item { Spacer(modifier = Modifier.height(90.dp)) }
    }
}
