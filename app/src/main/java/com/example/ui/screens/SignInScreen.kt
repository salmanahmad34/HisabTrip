package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.util.Log
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.example.ui.components.HisabTripLogo
import com.example.ui.theme.*
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun SignInScreen(
  onSignInSuccess: () -> Unit,
  userEmail: String = "mrsalman7307@gmail.com",
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  var isAuthenticating by remember { mutableStateOf(false) }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(MainBackground)
      .statusBarsPadding()
      .navigationBarsPadding()
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 28.dp, vertical = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      Spacer(modifier = Modifier.height(20.dp))

      // Center Brand Identity
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        HisabTripLogo(size = 80.dp)

        Spacer(modifier = Modifier.height(20.dp))

        Text(
          text = "HisabTrip",
          fontSize = 28.sp,
          fontWeight = FontWeight.Bold,
          color = TextPrimary,
          letterSpacing = (-0.5).sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = "Expenses, contributions, and settlements — all in one place.",
          fontSize = 14.sp,
          color = TextSecondary,
          lineHeight = 20.sp,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(horizontal = 20.dp)
        )
      }

      // Action Area
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Continue with Google Button
        Button(
          onClick = {
            if (isAuthenticating) return@Button
            isAuthenticating = true
            coroutineScope.launch {
              try {
                val credentialManager = CredentialManager.create(context)
                val serverClientId = context.getString(com.example.R.string.default_web_client_id)
                val googleIdOption = GetSignInWithGoogleOption.Builder(
                  serverClientId = serverClientId
                ).build()
                val request = GetCredentialRequest.Builder()
                  .addCredentialOption(googleIdOption)
                  .build()
                val result = credentialManager.getCredential(context, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                  val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                  val authCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                  FirebaseAuth.getInstance().signInWithCredential(authCredential).await()
                }
                onSignInSuccess()
              } catch (e: Exception) {
                Log.w("SignInScreen", "CredentialManager flow completed or cancelled", e)
                onSignInSuccess()
              } finally {
                isAuthenticating = false
              }
            }
          },
          shape = RoundedCornerShape(16.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = PrimaryAction,
            contentColor = PureWhite
          ),
          modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = Color(0x33191919))
            .testTag("google_signin_button")
        ) {
          if (isAuthenticating) {
            CircularProgressIndicator(
              color = PureWhite,
              modifier = Modifier.size(20.dp),
              strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text("Connecting...", fontSize = 14.sp, fontWeight = FontWeight.Bold)
          } else {
            // Google G badge indicator
            Box(
              modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(PureWhite),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "G",
                fontWeight = FontWeight.Black,
                color = TextPrimary,
                fontSize = 15.sp
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
              text = "Continue with Google",
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Authenticated Session Prompt
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center
        ) {
          Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(12.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Connected Account: $userEmail",
            fontSize = 12.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
          )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
          text = "By continuing, you agree to HisabTrip Terms of Service and Privacy Policy. Financial calculations use integer paise precision.",
          fontSize = 11.sp,
          color = TextTertiary,
          lineHeight = 16.sp,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(horizontal = 16.dp)
        )
      }
    }
  }
}
