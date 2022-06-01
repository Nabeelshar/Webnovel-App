package my.webnovels.uiViews

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import my.webnovels.ui.theme.ImageBorderRadius
import my.webnovels.R

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookImageButtonView(
    title: String,
    coverImageUrl: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
)
{
    MyButton(
        text = title,
        onClick = onClick,
        onLongClick = onLongClick,
        radius = ImageBorderRadius,
        borderWidth = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) { _, radius, _ ->
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1 / 1.45f)
                .clip(RoundedCornerShape(radius))
        ) {
            ImageView(
                imageModel = coverImageUrl,
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                error = R.drawable.default_book_cover,
            )
            Text(
                text = title,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth(1f)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            0f to MaterialTheme.colors.primary.copy(alpha = 0f),
                            0.44f to MaterialTheme.colors.primary.copy(alpha = 0.5f),
                            1f to MaterialTheme.colors.primary.copy(alpha = 0.85f),
                        )
                    )
                    .padding(top = 30.dp, bottom = 8.dp)
                    .padding(horizontal = 8.dp)
            )
        }
    }
}