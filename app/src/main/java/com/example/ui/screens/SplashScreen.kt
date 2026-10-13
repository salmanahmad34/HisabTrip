package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.HisabTripLogo
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
  onContinue: () -> Unit,
  userEmail: String = "mrsalman7307@gmail.com"
) {
  var startAnimation by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    startAnimation = true
    delay(1800)
    onContinue()
  }

  val logoScale by animateFloatAsState(
    targetValue = if (startAnimation) 1f else 0.75f,
    animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
    label = "logoScale"
  )

  Box(
    modifier = Modifier
      .fillMaxSize()
  ) {
    // Softly blurred travel background
    Image(
      painter = painterResource(id = R.drawable.scenic_travel_cover),
      contentDescription = null,
      contentScale = ContentScale.Crop,
      modifier = Modifier
        .fillMaxSize()
        .blur(16.dp)
    )

    // Warm gradient overlay
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors = listOf(
              Color(0x99191919),
              Color(0xCC191919),
              Color(0xE6121417)
            )
          )
        )
    )

    // Animated route line in background
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = size.width
      val h = size.height
      val path = Path().apply {
        moveTo(w * 0.1f, h * 0.28f)
        cubicTo(w * 0.35f, h * 0.32f, w * 0.2f, h * 0.52f, w * 0.5f, h * 0.58f)
        cubicTo(w * 0.8f, h * 0.64f, w * 0.65f, h * 0.82f, w * 0.9f, h * 0.86f)
      }
      drawPath(
        path = path,
        color = Color(0x22FFFFFF),
        style = Stroke(width = 3.dp.toPx())
      )
      drawCircle(Color(0x44FFFFFF), radius = 6.dp.toPx(), center = Offset(w * 0.1f, h * 0.28f))
      drawCircle(Color(0x44FFFFFF), radius = 6.dp.toPx(), center = Offset(w * 0.5f, h * 0.58f))
      drawCircle(Color(0x44FFFFFF), radius = 6.dp.toPx(), center = Offset(w * 0.9f, h * 0.86f))
    }

    Column(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding()
        .padding(32.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Box(
        modifier = Modifier
          .scale(logoScale)
          .padding(bottom = 20.dp)
      ) {
        HisabTripLogo(size = 90.dp)
      }

      Text(
        text = "HisabTrip",
        color = PureWhite,
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp
      )

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = "Expenses, contributions, and settlements — all in one place.",
        color = Color(0xCCFFFFFF),
        fontSize = 15.sp,
        lineHeight = 22.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 16.dp)
      )

      Spacer(modifier = Modifier.height(28.dp))

      AnimatedVisibility(
        visible = startAnimation,
        enter = fadeIn(tween(600, delayMillis = 300)) + slideInVertically(tween(600, delayMillis = 300))
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .background(Color(0x2BFFFFFF), RoundedCornerShape(20.dp))
            .border(0.8.dp, Color(0x40FFFFFF), RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 7.dp)
        ) {
          Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = StatusGreen,
            modifier = Modifier.size(15.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Google Session: $userEmail",
            color = PureWhite,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
          )
        }
      }
    }

    // Direct manual enter button
    Button(
      onClick = onContinue,
      shape = RoundedCornerShape(16.dp),
      colors = ButtonDefaults.buttonColors(
        containerColor = PureWhite,
        contentColor = TextPrimary
      ),
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth()
        .navigationBarsPadding()
        .padding(24.dp)
        .testTag("splash_continue_button")
    ) {
      Text(
        text = "Open HisabTrip",
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp
      )
      Spacer(modifier = Modifier.width(6.dp))
      Icon(
        imageVector = Icons.Default.ArrowForward,
        contentDescription = null,
        modifier = Modifier.size(18.dp)
      )
    }
  }
}
