package com.openscan.scanner.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.openscan.scanner.ui.theme.InkBlack
import com.openscan.scanner.ui.theme.PaperWhite

/** The inverse ink masthead colors, shared by every screen's top bar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun editorialBarColors(): TopAppBarColors = TopAppBarDefaults.topAppBarColors(
    containerColor = InkBlack,
    scrolledContainerColor = InkBlack,
    navigationIconContentColor = PaperWhite,
    titleContentColor = PaperWhite,
    actionIconContentColor = PaperWhite
)

/** A mono-caps eyebrow label with a trailing orange dot, like a printed section index. */
@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(8.dp))
        OrangeDot(size = 4)
    }
}

/** A 1px hairline rule. */
@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Divider(modifier = modifier, thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
}

/** A heavier 1.5px ink rule for top-level joints. */
@Composable
fun InkRule(modifier: Modifier = Modifier) {
    Divider(modifier = modifier, thickness = 1.5.dp, color = MaterialTheme.colorScheme.onBackground)
}
