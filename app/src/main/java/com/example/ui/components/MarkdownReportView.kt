package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ForestOutline
import com.example.ui.theme.ForestSlateDeep
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarmGold

@Composable
fun MarkdownReportView(
    markdown: String,
    modifier: Modifier = Modifier
) {
    val lines = remember(markdown) { markdown.lines() }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        var i = 0
        while (i < lines.size) {
            val rawLine = lines[i]
            val trimmed = rawLine.trim()

            when {
                trimmed.isEmpty() -> {
                    // Spacing handled by Arrangement.spacedBy
                }
                trimmed == "---" || trimmed == "***" -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .height(1.dp)
                            .background(ForestOutline.copy(alpha = 0.6f))
                    )
                }
                trimmed.startsWith("### ") -> {
                    Text(
                        text = parseInlineFormatting(trimmed.removePrefix("### ").trim(), WarmGold),
                        style = MaterialTheme.typography.titleMedium,
                        color = EmeraldPrimary,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                trimmed.startsWith("## ") -> {
                    Text(
                        text = parseInlineFormatting(trimmed.removePrefix("## ").trim(), WarmGold),
                        style = MaterialTheme.typography.titleLarge,
                        color = WarmGold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                trimmed.startsWith("# ") -> {
                    Text(
                        text = parseInlineFormatting(trimmed.removePrefix("# ").trim(), WarmGold),
                        style = MaterialTheme.typography.headlineSmall,
                        color = WarmGold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                trimmed.startsWith("|") && trimmed.endsWith("|") -> {
                    val tableBlock = StringBuilder()
                    while (i < lines.size && lines[i].trim().startsWith("|")) {
                        val tLine = lines[i].trim()
                        if (!tLine.replace("|", "").replace("-", "").replace(":", "").isBlank()) {
                            tableBlock.appendLine(tLine)
                        }
                        i++
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ForestSlateDeep, RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = tableBlock.toString().trim(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFontFamily
                            ),
                            color = TextSecondary
                        )
                    }
                    continue
                }
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    val content = trimmed.drop(2).trim()
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyMedium,
                            color = WarmGold,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = parseInlineFormatting(content, TextPrimary),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                else -> {
                    Text(
                        text = parseInlineFormatting(trimmed, TextPrimary),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                }
            }
            i++
        }
    }
}

private fun parseInlineFormatting(input: String, boldColor: Color): AnnotatedString {
    return buildAnnotatedString {
        var idx = 0
        while (idx < input.length) {
            val boldStart = input.indexOf("**", idx)
            if (boldStart == -1) {
                append(input.substring(idx))
                break
            }
            if (boldStart > idx) {
                append(input.substring(idx, boldStart))
            }
            val boldEnd = input.indexOf("**", boldStart + 2)
            if (boldEnd == -1) {
                append(input.substring(boldStart))
                break
            }
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = boldColor)) {
                append(input.substring(boldStart + 2, boldEnd))
            }
            idx = boldEnd + 2
        }
    }
}
