package com.keepsy.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keepsy.app.R
import com.keepsy.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun SplashScreenView(onAnimationFinished: () -> Unit = {}) {
    var stage by remember { mutableIntStateOf(0) }
    
    val logoScale by animateFloatAsState(
        targetValue = if (stage >= 1) 1f else 0.8f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "logoScale"
    )
    val logoAlpha by animateFloatAsState(
        targetValue = if (stage >= 1) 1f else 0f,
        animationSpec = tween(600),
        label = "logoAlpha"
    )
    
    val nameAlpha by animateFloatAsState(
        targetValue = if (stage >= 2) 1f else 0f,
        animationSpec = tween(500),
        label = "nameAlpha"
    )
    val nameTranslation by animateFloatAsState(
        targetValue = if (stage >= 2) 0f else 20f,
        animationSpec = tween(500, easing = EaseOutQuad),
        label = "nameTranslation"
    )
    
    val taglineAlpha by animateFloatAsState(
        targetValue = if (stage >= 3) 1f else 0f,
        animationSpec = tween(500),
        label = "taglineAlpha"
    )
    val taglineBlur by animateDpAsState(
        targetValue = if (stage >= 3) 0.dp else 12.dp,
        animationSpec = tween(800),
        label = "taglineBlur"
    )
    
    val loaderAlpha by animateFloatAsState(
        targetValue = if (stage >= 4) 1f else 0f,
        animationSpec = tween(600),
        label = "loaderAlpha"
    )

    LaunchedEffect(Unit) {
        delay(300)
        stage = 1 // Logo entrance
        delay(600)
        stage = 2 // App name
        delay(500)
        stage = 3 // Tagline
        delay(500)
        stage = 4 // Loader
        delay(1500) // Ensure users experience the premium brand reveal
        onAnimationFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        contentAlignment = Alignment.Center
    ) {
        // Background Effects
        KeepsyBackgroundEffects()
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // 1. Logo
            Image(
                painter = painterResource(id = R.drawable.ic_keepsy_logo),
                contentDescription = null,
                modifier = Modifier
                    .size(100.dp)
                    .graphicsLayer(
                        scaleX = logoScale,
                        scaleY = logoScale,
                        alpha = logoAlpha
                    )
                    .clip(RoundedCornerShape(24.dp))
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 2. App Name
            Text(
                text = "Keepsy",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = TextPrimary,
                modifier = Modifier
                    .graphicsLayer(
                        alpha = nameAlpha,
                        translationY = nameTranslation
                    )
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 3. Tagline
            Text(
                text = stringResource(id = R.string.splash_tagline),
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .graphicsLayer(alpha = taglineAlpha)
                    .blur(taglineBlur)
            )
            
            Spacer(modifier = Modifier.height(64.dp))
            
            // 4. Subtle Loader
            Box(modifier = Modifier.graphicsLayer(alpha = loaderAlpha)) {
                KeepsyGradientLoader(size = 32.dp)
            }
        }
    }
}

@Composable
fun KeepsyBackgroundEffects() {
    val infiniteTransition = rememberInfiniteTransition(label = "bg_effects")
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    
    // Floating particles
    val particleOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particleOffset"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        
        // Faint bottom wave lines
        val path = Path().apply {
            moveTo(0f, height * 0.85f)
            quadraticTo(width * 0.25f, height * 0.82f + (particleOffset * 20f), width * 0.5f, height * 0.85f)
            quadraticTo(width * 0.75f, height * 0.88f - (particleOffset * 20f), width, height * 0.85f)
        }
        
        drawPath(
            path = path,
            color = (if (isLight) Color.LightGray else PrimaryAccent).copy(alpha = if (isLight) 0.3f else 0.08f),
            style = Stroke(width = 2f)
        )
        
        // Random particles
        val random = Random(42)
        repeat(15) {
            val startX = random.nextFloat() * width
            val startY = random.nextFloat() * height
            val moveY = (particleOffset * 60f) * (if (it % 2 == 0) 1f else -1f)
            
            drawCircle(
                color = if (it % 3 == 0) {
                    (if (isLight) Color.LightGray else PrimaryAccent).copy(alpha = if (isLight) 0.5f else 0.1f)
                } else {
                    (if (isLight) Color.Gray else PrimaryPurple).copy(alpha = if (isLight) 0.4f else 0.08f)
                },
                radius = 2.dp.toPx(),
                center = Offset(startX, startY + moveY)
            )
        }
    }
}

@Composable
fun KeepsyGradientLoader(size: Dp = 48.dp, strokeWidth: Dp = 3.dp) {
    val infiniteTransition = rememberInfiniteTransition(label = "loader")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )

    Canvas(modifier = Modifier.size(size)) {
        val sweepGradient = Brush.sweepGradient(
            colors = listOf(
                Color.Transparent,
                PrimaryPurple.copy(alpha = 0.3f),
                PrimaryAccent
            )
        )
        
        rotate(angle) {
            drawCircle(
                brush = sweepGradient,
                radius = (size.toPx() / 2) - (strokeWidth.toPx() / 2),
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
fun LoadingStateScreen(text: String = "Syncing your memories...") {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        contentAlignment = Alignment.Center
    ) {
        KeepsyBackgroundEffects()
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.ic_keepsy_logo),
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .alpha(0.7f)
            )
            Spacer(modifier = Modifier.height(32.dp))
            KeepsyGradientLoader(size = 40.dp)
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun TrustFeatureCard(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
fun PrimaryGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingIcon: ImageVector? = Icons.AutoMirrored.Filled.ArrowForward
) {
    val gradient = Brush.horizontalGradient(
        colors = listOf(PrimaryPurple, PrimaryAccent)
    )
    
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (trailingIcon != null) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        imageVector = trailingIcon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TrustIndicator(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(SuccessGreen.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = SuccessGreen,
                modifier = Modifier.size(14.dp)
            )
        }
        Text(
            text = text, 
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}

@Composable
fun KeepsySearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        leadingIcon = { 
            Icon(
                imageVector = Icons.Default.Search, 
                contentDescription = null, 
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            ) 
        },
        trailingIcon = {
            AnimatedVisibility(
                visible = query.isNotEmpty(),
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Close, 
                        contentDescription = "Clear", 
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        placeholder = { 
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
            ) 
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = if (isLight) 0.8f else 0.4f),
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(20.dp),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (action != null) {
                action()
            }
        }
        if (subtitle != null && subtitle != "") {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun EmptyState(
    icon: @Composable () -> Unit,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null
) {
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = if (isLight) 0.8f else 0.5f)), RoundedCornerShape(32.dp)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = if (isLight) 0.15f else 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        if (action != null) {
            Spacer(modifier = Modifier.height(32.dp))
            action()
        }
    }
}
