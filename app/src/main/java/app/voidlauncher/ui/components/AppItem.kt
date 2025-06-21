package app.voidlauncher.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.voidlauncher.data.AppModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AppItem(
    modifier: Modifier = Modifier,
    app: AppModel,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                ).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        app.appIcon?.let { icon ->
            Image(
                bitmap = icon,
                contentDescription = null,
                modifier =
                    Modifier
                        .size(40.dp)
                        .padding(end = 16.dp),
            )
        }

        Text(
            text = app.appLabel,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
    }
}
