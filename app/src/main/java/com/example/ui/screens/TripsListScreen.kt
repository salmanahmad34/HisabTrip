package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.ExpenseEntity
import com.example.data.local.TripEntity
import com.example.data.local.TripMemberEntity
import com.example.domain.money.MoneyUtils
import com.example.domain.util.DateUtils
import com.example.ui.components.EmptyStateView
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.HisabTripViewModel

enum class ViewMode {
  GRID,
  LIST
}

enum class CoverDisplayMode {
  SCENIC_PHOTOS,
  IMAGE_PLACEHOLDERS
}

@Composable
fun TripsListScreen(
  viewModel: HisabTripViewModel,
  onCreateTripClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val trips by viewModel.trips.collectAsState()
  val allMembers by viewModel.allTripMembers.collectAsState()
  val allExpenses by viewModel.allExpenses.collectAsState()

  var searchQuery by remember { mutableStateOf("") }
  var selectedFilter by remember { mutableStateOf("ALL") } // ALL, ACTIVE, COMPLETED
  var viewMode by remember { mutableStateOf(ViewMode.GRID) }
  var coverDisplayMode by remember { mutableStateOf(CoverDisplayMode.SCENIC_PHOTOS) }

  val filteredTrips = trips.filter { trip ->
    val matchesFilter = when (selectedFilter) {
      "ACTIVE" -> trip.status == "ACTIVE"
      "COMPLETED" -> trip.status == "COMPLETED"
      else -> true
    }
    val matchesSearch = searchQuery.isBlank() ||
      trip.name.contains(searchQuery, ignoreCase = true) ||
      trip.id.contains(searchQuery, ignoreCase = true) ||
      trip.description.contains(searchQuery, ignoreCase = true) ||
      trip.startDate.contains(searchQuery, ignoreCase = true) ||
      trip.endDate.contains(searchQuery, ignoreCase = true)

    matchesFilter && matchesSearch
  }

  // Pre-calculate per-trip metrics
  val memberCountsByTrip = remember(allMembers) {
    allMembers.groupBy { it.tripId }.mapValues { it.value.size }
  }

  val totalSpentByTrip = remember(allExpenses) {
    allExpenses.filter { it.status == "APPROVED" }
      .groupBy { it.tripId }
      .mapValues { (_, expList) -> expList.sumOf { it.amountMinor } }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MainBackground)
      .statusBarsPadding()
  ) {
    // Top App Bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(
          text = "My Trips",
          fontSize = 24.sp,
          fontWeight = FontWeight.Bold,
          color = TextPrimary
        )

        Surface(
          shape = RoundedCornerShape(12.dp),
          color = SurfaceNeutral,
          border = androidx.compose.foundation.BorderStroke(0.8.dp, BorderSubtle)
        ) {
          Text(
            text = "${trips.size}",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
          )
        }
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // View Switcher (Grid vs List)
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = CardBackground,
          border = androidx.compose.foundation.BorderStroke(0.8.dp, BorderSubtle)
        ) {
          Row(modifier = Modifier.padding(2.dp)) {
            IconButton(
              onClick = { viewMode = ViewMode.GRID },
              modifier = Modifier
                .size(32.dp)
                .background(
                  if (viewMode == ViewMode.GRID) SurfaceNeutral else Color.Transparent,
                  RoundedCornerShape(8.dp)
                )
            ) {
              Icon(
                imageVector = Icons.Default.GridView,
                contentDescription = "Grid View",
                tint = if (viewMode == ViewMode.GRID) TextPrimary else TextTertiary,
                modifier = Modifier.size(16.dp)
              )
            }

            IconButton(
              onClick = { viewMode = ViewMode.LIST },
              modifier = Modifier
                .size(32.dp)
                .background(
                  if (viewMode == ViewMode.LIST) SurfaceNeutral else Color.Transparent,
                  RoundedCornerShape(8.dp)
                )
            ) {
              Icon(
                imageVector = Icons.Default.ViewList,
                contentDescription = "List View",
                tint = if (viewMode == ViewMode.LIST) TextPrimary else TextTertiary,
                modifier = Modifier.size(16.dp)
              )
            }
          }
        }

        // Circular Add Trip Action Button
        IconButton(
          onClick = onCreateTripClick,
          modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(PrimaryAction)
            .shadow(4.dp, CircleShape, spotColor = Color(0x33191919))
            .testTag("trips_list_add_button")
        ) {
          Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Create Trip",
            tint = PureWhite,
            modifier = Modifier.size(20.dp)
          )
        }
      }
    }

    // Search Bar
    OutlinedTextField(
      value = searchQuery,
      onValueChange = { searchQuery = it },
      placeholder = {
        Text("Search trips by name, location, code...", fontSize = 13.sp, color = TextTertiary)
      },
      leadingIcon = {
        Icon(
          Icons.Default.Search,
          contentDescription = null,
          tint = TextSecondary,
          modifier = Modifier.size(18.dp)
        )
      },
      trailingIcon = {
        if (searchQuery.isNotEmpty()) {
          IconButton(onClick = { searchQuery = "" }) {
            Icon(
              Icons.Default.Clear,
              contentDescription = "Clear",
              tint = TextSecondary,
              modifier = Modifier.size(16.dp)
            )
          }
        }
      },
      singleLine = true,
      shape = RoundedCornerShape(14.dp),
      colors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = CardBackground,
        unfocusedContainerColor = CardBackground,
        focusedBorderColor = PrimaryAction,
        unfocusedBorderColor = BorderSubtle
      ),
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)
        .testTag("trips_search_input")
    )

    Spacer(modifier = Modifier.height(10.dp))

    // Filter Chips & Cover Display Mode Switcher
    LazyRow(
      modifier = Modifier.fillMaxWidth(),
      contentPadding = PaddingValues(horizontal = 20.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      val statusFilters = listOf(
        "ALL" to "All (${trips.size})",
        "ACTIVE" to "Ongoing (${trips.count { it.status == "ACTIVE" }})",
        "COMPLETED" to "Completed (${trips.count { it.status == "COMPLETED" }})"
      )

      items(statusFilters) { (filterKey, label) ->
        val isSelected = selectedFilter == filterKey
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = if (isSelected) PrimaryAction else SurfaceNeutral,
          modifier = Modifier
            .clickable { selectedFilter = filterKey }
            .border(
              0.8.dp,
              if (isSelected) PrimaryAction else BorderSubtle,
              RoundedCornerShape(12.dp)
            )
        ) {
          Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) PureWhite else TextSecondary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
          )
        }
      }

      item {
        Box(
          modifier = Modifier
            .height(28.dp)
            .width(1.dp)
            .background(BorderSubtle)
            .padding(vertical = 2.dp)
        )
      }

      // Visual Style Mode Toggle: Photos vs Image Placeholders
      item {
        val isPhotos = coverDisplayMode == CoverDisplayMode.SCENIC_PHOTOS
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = if (isPhotos) CardBackground else SurfaceNeutral,
          border = androidx.compose.foundation.BorderStroke(
            0.8.dp,
            if (isPhotos) TextPrimary else BorderSubtle
          ),
          modifier = Modifier.clickable {
            coverDisplayMode = if (isPhotos) {
              CoverDisplayMode.IMAGE_PLACEHOLDERS
            } else {
              CoverDisplayMode.SCENIC_PHOTOS
            }
          }
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
          ) {
            Icon(
              imageVector = if (isPhotos) Icons.Default.PhotoLibrary else Icons.Default.Image,
              contentDescription = null,
              tint = if (isPhotos) TextPrimary else TextSecondary,
              modifier = Modifier.size(13.dp)
            )
            Text(
              text = if (isPhotos) "Scenic Photos" else "Image Placeholders",
              fontSize = 11.sp,
              fontWeight = FontWeight.Medium,
              color = if (isPhotos) TextPrimary else TextSecondary
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Main Content: Grid or List
    if (filteredTrips.isEmpty()) {
      EmptyStateView(
        title = if (searchQuery.isNotEmpty()) "No matching trips" else "No trips found",
        message = if (searchQuery.isNotEmpty())
          "No trips match '$searchQuery'. Try checking spelling or resetting filters."
        else
          "Start your first shared journey with member budgets, contributions, and settlements.",
        icon = Icons.Default.FlightTakeoff,
        actionLabel = if (searchQuery.isEmpty()) "+ Create Trip" else "Reset Search",
        onAction = if (searchQuery.isEmpty()) onCreateTripClick else {
          {
            searchQuery = ""
            selectedFilter = "ALL"
          }
        },
        modifier = Modifier.weight(1f)
      )

      // Helpful one-tap button if fresh database has no trips
      if (trips.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 90.dp),
          contentAlignment = Alignment.Center
        ) {
          TextButton(
            onClick = { viewModel.seedSampleTrips() }
          ) {
            Text(
              text = "⚡ Load Sample Itineraries (Manali, Goa & Rajasthan)",
              fontSize = 12.sp,
              fontWeight = FontWeight.Medium,
              color = TextSecondary
            )
          }
        }
      }
    } else {
      if (viewMode == ViewMode.GRID) {
        LazyVerticalGrid(
          columns = GridCells.Fixed(2),
          contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 96.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.fillMaxSize()
        ) {
          items(filteredTrips, key = { it.id }) { trip ->
            val memberCount = memberCountsByTrip[trip.id] ?: 1
            val spentMinor = totalSpentByTrip[trip.id] ?: 0L

            TripGridCard(
              trip = trip,
              memberCount = memberCount,
              totalSpentMinor = spentMinor,
              displayMode = coverDisplayMode,
              onClick = { viewModel.openTrip(trip.id) }
            )
          }
        }
      } else {
        LazyColumn(
          contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 96.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.fillMaxSize()
        ) {
          items(filteredTrips, key = { it.id }) { trip ->
            val memberCount = memberCountsByTrip[trip.id] ?: 1
            val spentMinor = totalSpentByTrip[trip.id] ?: 0L

            TripListCard(
              trip = trip,
              memberCount = memberCount,
              totalSpentMinor = spentMinor,
              displayMode = coverDisplayMode,
              onClick = { viewModel.openTrip(trip.id) }
            )
          }
        }
      }
    }
  }
}

/**
 * Modern 2-column Grid Card with graphite and warm off-white design
 */
@Composable
fun TripGridCard(
  trip: TripEntity,
  memberCount: Int,
  totalSpentMinor: Long,
  displayMode: CoverDisplayMode,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val formattedDateRange = remember(trip.startDate, trip.endDate) {
    DateUtils.formatDateRange(trip.startDate, trip.endDate)
  }
  val durationBadge = remember(trip.startDate, trip.endDate) {
    DateUtils.formatDurationBadge(trip.startDate, trip.endDate)
  }

  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = CardBackground),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = modifier
      .fillMaxWidth()
      .border(0.8.dp, BorderSubtle, RoundedCornerShape(18.dp))
      .clickable(onClick = onClick)
      .testTag("trip_grid_card_${trip.id}")
  ) {
    Column {
      // Header Cover (Scenic Image or Image Placeholder)
      TripCardCoverHeader(
        trip = trip,
        durationBadge = durationBadge,
        displayMode = displayMode,
        height = 118.dp
      )

      // Card Content Body
      Column(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
      ) {
        // Date Formatting Badge Row
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(
            imageVector = Icons.Default.CalendarMonth,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(11.dp)
          )
          Text(
            text = formattedDateRange,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Trip Name
        Text(
          text = trip.name,
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          color = TextPrimary,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          lineHeight = 18.sp
        )

        // Traveler companions indicator
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Group,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(11.dp)
          )
          Text(
            text = if (memberCount > 1) "$memberCount travelers" else "1 traveler",
            fontSize = 10.sp,
            color = TextSecondary
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Financial & Budget Details
        val hasBudget = trip.budgetMinor > 0L
        val budgetProgress = if (hasBudget) {
          (totalSpentMinor.toFloat() / trip.budgetMinor.toFloat()).coerceIn(0f, 1f)
        } else 0f

        Column(
          modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceNeutral, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "BUDGET",
              fontSize = 8.sp,
              fontWeight = FontWeight.Bold,
              color = TextTertiary,
              letterSpacing = 0.5.sp
            )
            Text(
              text = MoneyUtils.formatPaise(trip.budgetMinor),
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
          }

          if (totalSpentMinor > 0L) {
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
              progress = { budgetProgress },
              modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp)),
              color = if (budgetProgress >= 0.95f) StatusAmber else PrimaryAction,
              trackColor = BorderSubtle
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = "Spent",
                fontSize = 8.sp,
                color = TextTertiary
              )
              Text(
                text = MoneyUtils.formatPaise(totalSpentMinor),
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
              )
            }
          }
        }
      }
    }
  }
}

/**
 * Spacious Full-Width List Card
 */
@Composable
fun TripListCard(
  trip: TripEntity,
  memberCount: Int,
  totalSpentMinor: Long,
  displayMode: CoverDisplayMode,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val formattedDateRange = remember(trip.startDate, trip.endDate) {
    DateUtils.formatDateRange(trip.startDate, trip.endDate)
  }
  val durationBadge = remember(trip.startDate, trip.endDate) {
    DateUtils.formatDurationBadge(trip.startDate, trip.endDate)
  }

  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = CardBackground),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = modifier
      .fillMaxWidth()
      .border(0.8.dp, BorderSubtle, RoundedCornerShape(18.dp))
      .clickable(onClick = onClick)
      .testTag("trip_list_card_${trip.id}")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(10.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      // Thumbnail / Cover Header
      Box(
        modifier = Modifier
          .size(width = 110.dp, height = 115.dp)
          .clip(RoundedCornerShape(14.dp))
      ) {
        TripCardCoverHeader(
          trip = trip,
          durationBadge = durationBadge,
          displayMode = displayMode,
          height = 115.dp
        )
      }

      // Content Details
      Column(
        modifier = Modifier
          .weight(1f)
          .height(115.dp),
        verticalArrangement = Arrangement.SpaceBetween
      ) {
        Column {
          // Date Formatting
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Icon(
              imageVector = Icons.Default.CalendarMonth,
              contentDescription = null,
              tint = TextSecondary,
              modifier = Modifier.size(11.dp)
            )
            Text(
              text = formattedDateRange,
              fontSize = 11.sp,
              fontWeight = FontWeight.Medium,
              color = TextSecondary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }

          Spacer(modifier = Modifier.height(2.dp))

          // Title
          Text(
            text = trip.name,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )

          if (trip.description.isNotEmpty()) {
            Text(
              text = trip.description,
              fontSize = 11.sp,
              color = TextSecondary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        // Bottom row: budget and travelers
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.Bottom
        ) {
          Column {
            Text(
              text = "BUDGET",
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              color = TextTertiary,
              letterSpacing = 0.5.sp
            )
            Text(
              text = MoneyUtils.formatPaise(trip.budgetMinor),
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
              .background(SurfaceNeutral, RoundedCornerShape(8.dp))
              .padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Group,
              contentDescription = null,
              tint = TextSecondary,
              modifier = Modifier.size(12.dp)
            )
            Text(
              text = "$memberCount",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
          }
        }
      }
    }
  }
}

/**
 * Cover Header Composable:
 * Renders either curated scenic travel photography or an elegant graphite image placeholder
 */
@Composable
fun TripCardCoverHeader(
  trip: TripEntity,
  durationBadge: String?,
  displayMode: CoverDisplayMode,
  height: androidx.compose.ui.unit.Dp,
  modifier: Modifier = Modifier
) {
  val destinationTheme = remember(trip.name, trip.description) {
    getDestinationTheme(trip.name, trip.description)
  }

  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(height)
  ) {
    if (displayMode == CoverDisplayMode.SCENIC_PHOTOS) {
      // Select appropriate curated scenic drawable
      val drawableRes = when (destinationTheme.themeType) {
        ThemeType.MOUNTAIN -> R.drawable.img_mountain_retreat
        ThemeType.COASTAL -> R.drawable.img_coastal_road
        else -> R.drawable.scenic_travel_cover
      }

      Image(
        painter = painterResource(id = drawableRes),
        contentDescription = trip.name,
        contentScale = ContentScale.Crop,
        modifier = Modifier.matchParentSize()
      )

      // Dual-tone gradient overlay for contrast
      Box(
        modifier = Modifier
          .matchParentSize()
          .background(
            Brush.verticalGradient(
              colors = listOf(
                Color(0x38000000),
                Color(0x10000000),
                Color(0x80191919)
              )
            )
          )
      )
    } else {
      // Premium Graphite & Warm Off-White Graphic Image Placeholder
      val gradientBrush = when (destinationTheme.themeType) {
        ThemeType.MOUNTAIN -> Brush.linearGradient(
          colors = listOf(Color(0xFF262C34), Color(0xFF14171A))
        )
        ThemeType.COASTAL -> Brush.linearGradient(
          colors = listOf(Color(0xFF2E3842), Color(0xFF1C2228))
        )
        ThemeType.ROAD_TRIP -> Brush.linearGradient(
          colors = listOf(Color(0xFF33302C), Color(0xFF1C1A18))
        )
        ThemeType.WILDERNESS -> Brush.linearGradient(
          colors = listOf(Color(0xFF243028), Color(0xFF131A15))
        )
        ThemeType.CITY -> Brush.linearGradient(
          colors = listOf(Color(0xFF2D2A32), Color(0xFF18161A))
        )
      }

      Box(
        modifier = Modifier
          .matchParentSize()
          .background(gradientBrush)
      ) {
        // Subtle geometric background watermark circle
        Box(
          modifier = Modifier
            .size(80.dp)
            .align(Alignment.CenterEnd)
            .background(Color(0x08FFFFFF), CircleShape)
        )

        // Center stylized travel placeholder emblem
        Column(
          modifier = Modifier.align(Alignment.Center),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .background(Color(0x22FFFFFF))
              .border(0.8.dp, Color(0x33FFFFFF), CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = destinationTheme.icon,
              contentDescription = destinationTheme.label,
              tint = PureWhite,
              modifier = Modifier.size(18.dp)
            )
          }

          Spacer(modifier = Modifier.height(4.dp))

          Text(
            text = destinationTheme.label.uppercase(),
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xAAFFFFFF),
            letterSpacing = 0.8.sp
          )
        }
      }
    }

    // Top Row: Duration Chip (Start) and Status Badge (End)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      if (durationBadge != null) {
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = Color(0xAA191919),
          border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0x33FFFFFF))
        ) {
          Text(
            text = durationBadge,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            color = PureWhite,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
          )
        }
      } else {
        Spacer(modifier = Modifier.width(1.dp))
      }

      StatusBadge(status = trip.status)
    }

    // Bottom-End: Unique Trip Code Pill
    Surface(
      shape = RoundedCornerShape(6.dp),
      color = Color(0x99191919),
      border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0x22FFFFFF)),
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(8.dp)
    ) {
      Text(
        text = "#${trip.id.takeLast(4).uppercase()}",
        fontSize = 8.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xDDFFFFFF),
        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
      )
    }
  }
}

enum class ThemeType {
  MOUNTAIN,
  COASTAL,
  ROAD_TRIP,
  WILDERNESS,
  CITY
}

data class DestinationTheme(
  val themeType: ThemeType,
  val icon: ImageVector,
  val label: String
)

fun getDestinationTheme(name: String, desc: String): DestinationTheme {
  val combined = "$name $desc".lowercase()
  return when {
    combined.contains("mountain") || combined.contains("manali") ||
      combined.contains("himalaya") || combined.contains("trek") ||
      combined.contains("snow") || combined.contains("peak") ||
      combined.contains("ladakh") -> DestinationTheme(
      ThemeType.MOUNTAIN,
      Icons.Default.Landscape,
      "Mountain Expedition"
    )

    combined.contains("beach") || combined.contains("goa") ||
      combined.contains("coast") || combined.contains("sea") ||
      combined.contains("ocean") || combined.contains("island") -> DestinationTheme(
      ThemeType.COASTAL,
      Icons.Default.BeachAccess,
      "Coastal Journey"
    )

    combined.contains("road") || combined.contains("drive") ||
      combined.contains("highway") || combined.contains("bike") -> DestinationTheme(
      ThemeType.ROAD_TRIP,
      Icons.Default.DirectionsCar,
      "Road Trip"
    )

    combined.contains("camp") || combined.contains("forest") ||
      combined.contains("jungle") || combined.contains("wild") ||
      combined.contains("safari") -> DestinationTheme(
      ThemeType.WILDERNESS,
      Icons.Default.Explore,
      "Wilderness Camp"
    )

    else -> DestinationTheme(
      ThemeType.CITY,
      Icons.Default.FlightTakeoff,
      "Travel Itinerary"
    )
  }
}
