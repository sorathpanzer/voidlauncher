package app.voidlauncher.ui.composables

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.voidlauncher.data.AppModel
import app.voidlauncher.data.settings.AppSettings
import app.voidlauncher.helper.IconCache
import kotlinx.coroutines.launch

@Composable
fun HomeAppItem(
    modifier: Modifier = Modifier,
    app: AppModel,
    settings: AppSettings,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    appWidth: Dp,
    appHeight: Dp
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val iconCache = remember { IconCache(context) }
    var loadedIcon by remember(app.getKey()) { mutableStateOf(app.appIcon) }

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    val fontScale = settings.textSizeScale
    val fontWeight = remember(settings.fontWeight) {
        when (settings.fontWeight) {
            0 -> FontWeight.Thin
            1 -> FontWeight.Light
            2 -> FontWeight.Normal
            3 -> FontWeight.Medium
            4 -> FontWeight.Bold
            5 -> FontWeight.Black
            else -> FontWeight.Normal
        }
    }
    val iconCornerRadius = settings.iconCornerRadius.dp

}
