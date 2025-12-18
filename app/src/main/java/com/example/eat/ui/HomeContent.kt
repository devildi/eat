package com.example.eat.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeContent(
    viewModel: MainViewModel,
    onArticleClick: (com.example.eat.data.ArticleEntity) -> Unit,
    onArticleLongClick: (com.example.eat.data.ArticleEntity) -> Unit
) {
    val uiState by viewModel.articleUiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = uiState) {
            is ArticleUiState.Loading -> {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is ArticleUiState.Success -> {
                val articles = state.articles
                if (articles.isEmpty()) {
                    Text(
                        text = "No articles yet. Click + to add.",
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                    ) {
                        items(articles) { article ->
                            androidx.compose.material3.Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = { onArticleClick(article) },
                                        onLongClick = { onArticleLongClick(article) }
                                    ),
                                elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 2.dp),
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    androidx.compose.foundation.layout.Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = article.title,
                                            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = SimpleDateFormat("yy/MM/dd", Locale.getDefault()).format(Date(article.timestamp)),
                                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                            color = androidx.compose.ui.graphics.Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
