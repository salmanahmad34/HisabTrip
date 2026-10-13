package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.money.MoneyUtils
import com.example.ui.components.MemberAvatar
import com.example.ui.theme.*
import com.example.ui.viewmodel.HisabTripViewModel
import kotlinx.coroutines.launch

data class SupportedCurrency(
  val code: String,
  val symbol: String,
  val name: String,
  val flagEmoji: String
)

val PRESET_CURRENCIES = listOf(
  SupportedCurrency("INR", "₹", "Indian Rupee", "🇮🇳"),
  SupportedCurrency("USD", "$", "US Dollar", "🇺🇸"),
  SupportedCurrency("EUR", "€", "Euro", "🇪🇺"),
  SupportedCurrency("GBP", "£", "British Pound", "🇬🇧"),
  SupportedCurrency("AED", "AED", "UAE Dirham", "🇦🇪"),
  SupportedCurrency("JPY", "¥", "Japanese Yen", "🇯🇵"),
  SupportedCurrency("SGD", "S$", "Singapore Dollar", "🇸🇬"),
  SupportedCurrency("THB", "฿", "Thai Baht", "🇹🇭"),
  SupportedCurrency("CAD", "CA$", "Canadian Dollar", "🇨🇦"),
  SupportedCurrency("AUD", "A$", "Australian Dollar", "🇦🇺")
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateTripScreen(
  viewModel: HisabTripViewModel,
  onBack: () -> Unit,
  onTripCreated: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  BackHandler {
    onBack()
  }

  val allProfiles by viewModel.allProfiles.collectAsState()
  val scope = rememberCoroutineScope()

  var title by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  var selectedCurrency by remember { mutableStateOf(PRESET_CURRENCIES.first()) }
  var isCustomCurrency by remember { mutableStateOf(false) }
  var customCurrencyInput by remember { mutableStateOf("") }

  var startDate by remember { mutableStateOf("15 Oct 2026") }
  var endDate by remember { mutableStateOf("22 Oct 2026") }
  var budgetInput by remember { mutableStateOf("25000") }

  val selectedMemberIds = remember { mutableStateListOf<String>() }

  var titleError by remember { mutableStateOf<String?>(null) }
  var isSaving by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  val effectiveCurrencyCode = if (isCustomCurrency && customCurrencyInput.isNotBlank()) {
    customCurrencyInput.trim().uppercase()
  } else {
    selectedCurrency.code
  }

  val effectiveCurrencySymbol = MoneyUtils.getCurrencySymbol(effectiveCurrencyCode)

  fun handleSave() {
    val cleanTitle = title.trim()
    if (cleanTitle.isEmpty()) {
      titleError = "Trip title is required"
      return
    }

    titleError = null
    errorMessage = null
    isSaving = true

    val budgetMinor = MoneyUtils.parseToPaise(budgetInput) ?: 0L

    scope.launch {
      val result = viewModel.initializeTrip(
        title = cleanTitle,
        description = description.trim(),
        currency = effectiveCurrencyCode,
        startDate = startDate.trim(),
        endDate = endDate.trim(),
        budgetMinor = budgetMinor,
        memberIds = selectedMemberIds.toList()
      )

      isSaving = false
      result.onSuccess { tripId ->
        onTripCreated(tripId)
      }.onFailure { error ->
        errorMessage = error.localizedMessage ?: "Failed to save trip to Firestore. Please try again."
      }
    }
  }

  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .testTag("create_trip_screen"),
    containerColor = MainBackground,
    topBar = {
      TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = PureWhite,
          titleContentColor = Graphite900
        ),
        title = {
          Column {
            Text(
              text = "Initialize New Trip",
              fontSize = 19.sp,
              fontWeight = FontWeight.Bold,
              color = Graphite900
            )
            Text(
              text = "Creates cloud group in Firestore",
              fontSize = 12.sp,
              color = Graphite500
            )
          }
        },
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier = Modifier.testTag("back_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
              tint = Graphite900
            )
          }
        },
        actions = {
          // Live Firestore Sync Indicator
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = StatusGreenBg,
            modifier = Modifier.padding(end = 12.dp)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Icon(
                imageVector = Icons.Default.CloudDone,
                contentDescription = "Cloud Enabled",
                tint = StatusGreen,
                modifier = Modifier.size(14.dp)
              )
              Text(
                text = "Firestore Sync",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = StatusGreen
              )
            }
          }
        }
      )
    }
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
      // Hero Header Card
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          Box(
            modifier = Modifier
              .size(52.dp)
              .clip(CircleShape)
              .background(PrimaryAction.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.FlightTakeoff,
              contentDescription = null,
              tint = PrimaryAction,
              modifier = Modifier.size(28.dp)
            )
          }

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Plan & Track Expenses",
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold,
              color = Graphite900
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "Set title, description, and currency to initialize your group ledger directly in Firestore.",
              fontSize = 13.sp,
              color = Graphite600,
              lineHeight = 18.sp
            )
          }
        }
      }

      // Title Section (Required)
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text(
              text = "Trip Title",
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold,
              color = Graphite900
            )
            Text(
              text = "*",
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFFDC2626)
            )
          }

          OutlinedTextField(
            value = title,
            onValueChange = {
              title = it
              if (titleError != null) titleError = null
            },
            placeholder = { Text("e.g., Goa Beach Holiday, Manali Road Trip") },
            leadingIcon = {
              Icon(
                imageVector = Icons.Default.FlightTakeoff,
                contentDescription = null,
                tint = Graphite500
              )
            },
            trailingIcon = {
              if (title.isNotEmpty()) {
                IconButton(onClick = { title = "" }) {
                  Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Clear Title",
                    tint = Graphite400
                  )
                }
              }
            },
            singleLine = true,
            isError = titleError != null,
            supportingText = {
              if (titleError != null) {
                Text(
                  text = titleError!!,
                  color = Color(0xFFDC2626),
                  fontSize = 12.sp
                )
              } else {
                Text(
                  text = "Choose a recognizable name for your travel group",
                  color = Graphite400,
                  fontSize = 11.sp
                )
              }
            },
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = PrimaryAction,
              unfocusedBorderColor = Graphite300,
              focusedContainerColor = PureWhite,
              unfocusedContainerColor = PureWhite
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("trip_title_input")
          )
        }
      }

      // Description Section (Required / Detailed)
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text(
            text = "Description & Purpose",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Graphite900
          )

          OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            placeholder = {
              Text("Describe your trip: itinerary highlights, hotel details, destination plans, or shared rules...")
            },
            leadingIcon = {
              Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = Graphite500
              )
            },
            minLines = 3,
            maxLines = 6,
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = PrimaryAction,
              unfocusedBorderColor = Graphite300,
              focusedContainerColor = PureWhite,
              unfocusedContainerColor = PureWhite
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("trip_description_input")
          )

          Text(
            text = "Stored in Firestore for all invited trip companions to see",
            fontSize = 11.sp,
            color = Graphite500
          )
        }
      }

      // Currency Selector Section (Required)
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("trip_currency_selector")
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Text(
                text = "Trip Currency",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Graphite900
              )
              Text(
                text = "*",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFDC2626)
              )
            }

            Surface(
              shape = RoundedCornerShape(8.dp),
              color = StatusGreenBg
            ) {
              Text(
                text = "Selected: $effectiveCurrencyCode ($effectiveCurrencySymbol)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = StatusGreen,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }

          Text(
            text = "All shared expenses, contributions, and settlements will be calculated in this currency.",
            fontSize = 12.sp,
            color = Graphite500
          )

          // Currency Chips Grid
          FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            PRESET_CURRENCIES.forEach { curr ->
              val isSelected = !isCustomCurrency && selectedCurrency.code == curr.code
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) Graphite900 else Graphite100,
                contentColor = if (isSelected) PureWhite else Graphite800,
                modifier = Modifier
                  .clickable {
                    selectedCurrency = curr
                    isCustomCurrency = false
                  }
                  .testTag("trip_currency_chip_${curr.code.lowercase()}")
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Text(text = curr.flagEmoji, fontSize = 14.sp)
                  Text(
                    text = "${curr.code} (${curr.symbol})",
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                  )
                  if (isSelected) {
                    Icon(
                      imageVector = Icons.Default.Check,
                      contentDescription = "Selected",
                      tint = PureWhite,
                      modifier = Modifier.size(14.dp)
                    )
                  }
                }
              }
            }
          }

          // Custom Currency Option
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            TextButton(
              onClick = {
                isCustomCurrency = !isCustomCurrency
                if (isCustomCurrency && customCurrencyInput.isBlank()) {
                  customCurrencyInput = "EUR"
                }
              }
            ) {
              Text(
                text = if (isCustomCurrency) "← Choose from standard currencies" else "+ Other ISO currency...",
                fontSize = 12.sp,
                color = PrimaryAction
              )
            }
          }

          if (isCustomCurrency) {
            OutlinedTextField(
              value = customCurrencyInput,
              onValueChange = { customCurrencyInput = it.take(4).uppercase() },
              label = { Text("3-Letter Currency Code (e.g., CHF, CAD)") },
              singleLine = true,
              keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryAction,
                unfocusedBorderColor = Graphite300
              ),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth()
            )
          }
        }
      }

      // Additional Trip Details (Dates & Target Budget)
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          Text(
            text = "Trip Dates & Budget",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Graphite900
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            OutlinedTextField(
              value = startDate,
              onValueChange = { startDate = it },
              label = { Text("Start Date") },
              leadingIcon = {
                Icon(
                  imageVector = Icons.Default.CalendarToday,
                  contentDescription = null,
                  tint = Graphite500,
                  modifier = Modifier.size(18.dp)
                )
              },
              singleLine = true,
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(12.dp),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryAction,
                unfocusedBorderColor = Graphite300
              )
            )

            OutlinedTextField(
              value = endDate,
              onValueChange = { endDate = it },
              label = { Text("End Date") },
              leadingIcon = {
                Icon(
                  imageVector = Icons.Default.CalendarToday,
                  contentDescription = null,
                  tint = Graphite500,
                  modifier = Modifier.size(18.dp)
                )
              },
              singleLine = true,
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(12.dp),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryAction,
                unfocusedBorderColor = Graphite300
              )
            )
          }

          // Budget Input with dynamic currency symbol
          OutlinedTextField(
            value = budgetInput,
            onValueChange = { budgetInput = it },
            label = { Text("Planned Group Budget ($effectiveCurrencySymbol)") },
            leadingIcon = {
              Text(
                text = effectiveCurrencySymbol,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Graphite700,
                modifier = Modifier.padding(start = 12.dp)
              )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = PrimaryAction,
              unfocusedBorderColor = Graphite300
            )
          )
        }
      }

      // Invite Initial Companions (Optional)
      if (allProfiles.isNotEmpty()) {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = PureWhite),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Group,
                contentDescription = null,
                tint = PrimaryAction,
                modifier = Modifier.size(20.dp)
              )
              Text(
                text = "Add Trip Members (Optional)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Graphite900
              )
            }

            Text(
              text = "Tap to include companions into this group right away:",
              fontSize = 12.sp,
              color = Graphite500
            )

            FlowRow(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              allProfiles.forEach { profile ->
                val isSelected = selectedMemberIds.contains(profile.userId)
                Surface(
                  shape = RoundedCornerShape(20.dp),
                  color = if (isSelected) StatusGreenBg else Graphite100,
                  border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, StatusGreen) else null,
                  modifier = Modifier.clickable {
                    if (isSelected) {
                      selectedMemberIds.remove(profile.userId)
                    } else {
                      selectedMemberIds.add(profile.userId)
                    }
                  }
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    MemberAvatar(
                      avatarKey = profile.avatarKey,
                      name = profile.displayName,
                      size = 24.dp
                    )
                    Text(
                      text = profile.displayName,
                      fontSize = 12.sp,
                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                      color = if (isSelected) Graphite900 else Graphite700
                    )
                    if (isSelected) {
                      Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = StatusGreen,
                        modifier = Modifier.size(14.dp)
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }

      // Error message banner if save fails
      AnimatedVisibility(visible = errorMessage != null) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = Color(0xFFFEE2E2),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Info,
              contentDescription = "Error",
              tint = Color(0xFFDC2626)
            )
            Text(
              text = errorMessage ?: "",
              fontSize = 13.sp,
              color = Color(0xFF991B1B)
            )
          }
        }
      }

      // Save to Firestore Submit Button
      Button(
        onClick = { handleSave() },
        enabled = !isSaving,
        colors = ButtonDefaults.buttonColors(
          containerColor = PrimaryAction,
          contentColor = PureWhite,
          disabledContainerColor = PrimaryAction.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(54.dp)
          .shadow(6.dp, RoundedCornerShape(14.dp), spotColor = PrimaryAction.copy(alpha = 0.25f))
          .testTag("save_trip_button")
      ) {
        if (isSaving) {
          CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            color = PureWhite,
            strokeWidth = 2.5.dp
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = "Saving to Firestore...",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
          )
        } else {
          Icon(
            imageVector = Icons.Default.CloudUpload,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = "Initialize Trip & Save to Firestore",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }

      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}
