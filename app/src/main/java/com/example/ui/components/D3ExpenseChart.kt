package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.domain.analytics.ChartDataItem
import com.example.domain.analytics.ChartDisplayType

class D3ChartBridge(
  private val onItemSelected: (String) -> Unit
) {
  private val handler = Handler(Looper.getMainLooper())

  @JavascriptInterface
  fun onSliceSelected(id: String) {
    handler.post {
      onItemSelected(id)
    }
  }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun D3ExpenseChart(
  data: List<ChartDataItem>,
  displayType: ChartDisplayType,
  selectedItemId: String?,
  onItemSelected: (String) -> Unit,
  centerTitle: String = "Total Spend",
  centerAmountFormatted: String = "",
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val bridge = remember(onItemSelected) { D3ChartBridge(onItemSelected) }

  val htmlContent = remember(data, displayType, selectedItemId, centerTitle, centerAmountFormatted) {
    generateD3Html(
      data = data,
      displayType = displayType,
      selectedItemId = selectedItemId,
      centerTitle = centerTitle,
      centerAmountFormatted = centerAmountFormatted
    )
  }

  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(280.dp)
      .clip(RoundedCornerShape(16.dp))
      .background(Color.White)
      .testTag("d3_expense_chart")
  ) {
    AndroidView(
      modifier = Modifier
        .fillMaxWidth()
        .height(280.dp),
      factory = { ctx ->
        WebView(ctx).apply {
          setBackgroundColor(0x00000000)
          isVerticalScrollBarEnabled = false
          isHorizontalScrollBarEnabled = false
          settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            cacheMode = WebSettings.LOAD_DEFAULT
          }
          addJavascriptInterface(bridge, "AndroidD3")
          loadDataWithBaseURL("https://local.hisabtrip", htmlContent, "text/html", "UTF-8", null)
        }
      },
      update = { webView ->
        webView.loadDataWithBaseURL("https://local.hisabtrip", htmlContent, "text/html", "UTF-8", null)
      }
    )
  }
}

/**
 * Builds the interactive HTML containing D3.js and an SVG chart renderer.
 * Designed to render offline and online, with animated arcs, tooltips, and touch handling.
 */
private fun generateD3Html(
  data: List<ChartDataItem>,
  displayType: ChartDisplayType,
  selectedItemId: String?,
  centerTitle: String,
  centerAmountFormatted: String
): String {
  val itemsJson = buildString {
    append("[")
    data.forEachIndexed { index, item ->
      if (index > 0) append(",")
      val safeLabel = item.label.replace("\"", "\\\"")
      val safeFormatted = item.formattedAmount.replace("\"", "\\\"")
      val safeSubtitle = item.subtitle.replace("\"", "\\\"")
      append("{\"id\":\"${item.id}\",\"label\":\"$safeLabel\",\"amount\":${item.amountMinor},\"formatted\":\"$safeFormatted\",\"pct\":${item.percentage},\"color\":\"${item.colorHex}\",\"subtitle\":\"$safeSubtitle\"}")
    }
    append("]")
  }

  val isDonut = displayType == ChartDisplayType.DONUT_CHART

  return """
  <!DOCTYPE html>
  <html>
  <head>
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
    <style>
      * {
        box-sizing: border-box;
        margin: 0;
        padding: 0;
        -webkit-tap-highlight-color: transparent;
        user-select: none;
      }
      body {
        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
        background-color: transparent;
        overflow: hidden;
        width: 100%;
        height: 100%;
        display: flex;
        align-items: center;
        justify-content: center;
      }
      #chart-container {
        width: 100%;
        height: 100%;
        display: flex;
        align-items: center;
        justify-content: center;
        position: relative;
      }
      svg {
        width: 100%;
        height: 100%;
        overflow: visible;
      }
      .arc-path {
        cursor: pointer;
        transition: transform 0.2s cubic-bezier(0.4, 0, 0.2, 1), opacity 0.2s ease, stroke-width 0.2s ease;
      }
      .arc-path:hover, .arc-path:active {
        opacity: 0.9;
      }
      .arc-path.selected {
        filter: drop-shadow(0px 4px 8px rgba(0,0,0,0.25));
        stroke: #ffffff;
        stroke-width: 3px;
      }
      .center-title {
        font-size: 11px;
        font-weight: 600;
        fill: #777777;
        text-transform: uppercase;
        letter-spacing: 0.5px;
        text-anchor: middle;
      }
      .center-value {
        font-size: 17px;
        font-weight: 700;
        fill: #191919;
        text-anchor: middle;
      }
      .center-subtitle {
        font-size: 11px;
        font-weight: 500;
        fill: #2E7D32;
        text-anchor: middle;
      }
      /* Bar chart styles */
      .bar-group {
        cursor: pointer;
      }
      .bar-group:active .bar-rect {
        opacity: 0.8;
      }
      .bar-label {
        font-size: 12px;
        font-weight: 600;
        fill: #191919;
      }
      .bar-value {
        font-size: 11px;
        font-weight: 700;
        fill: #555555;
        text-anchor: end;
      }
      .bar-rect {
        rx: 5px;
        ry: 5px;
        transition: width 0.4s cubic-bezier(0.4, 0, 0.2, 1), opacity 0.2s ease;
      }
      .bar-bg {
        rx: 5px;
        ry: 5px;
        fill: #F0EEEA;
      }
      .empty-state {
        font-size: 13px;
        font-weight: 500;
        fill: #999999;
        text-anchor: middle;
      }
    </style>
  </head>
  <body>
    <div id="chart-container">
      <svg id="chart-svg" viewBox="0 0 360 270" preserveAspectRatio="xMidYMid meet"></svg>
    </div>

    <!-- Load D3 v7 with fallback inline renderer -->
    <script src="https://cdn.jsdelivr.net/npm/d3@7"></script>
    <script>
      (function() {
        var rawData = $itemsJson;
        var selectedId = "$selectedItemId";
        var isDonut = $isDonut;
        var centerTitleText = "$centerTitle";
        var centerAmountText = "$centerAmountFormatted";

        var svg = document.getElementById('chart-svg');

        if (!rawData || rawData.length === 0) {
          svg.innerHTML = '<text class="empty-state" x="180" y="135">No expenses recorded yet</text>';
          return;
        }

        if (isDonut) {
          renderDonutChart(rawData, selectedId);
        } else {
          renderBarChart(rawData, selectedId);
        }

        function notifySelection(id) {
          if (window.AndroidD3 && typeof window.AndroidD3.onSliceSelected === 'function') {
            window.AndroidD3.onSliceSelected(id);
          }
        }

        // --- Custom D3 / SVG Donut Renderer ---
        function renderDonutChart(data, selected) {
          var cx = 180;
          var cy = 135;
          var outerRadius = 110;
          var innerRadius = 66;

          var total = data.reduce(function(acc, item) { return acc + item.amount; }, 0);
          if (total <= 0) {
            svg.innerHTML = '<text class="empty-state" x="180" y="135">Total spend is zero</text>';
            return;
          }

          var selectedItem = data.find(function(d) { return d.id === selected; });

          var currentAngle = -Math.PI / 2;
          var html = '<g id="donut-slices">';

          data.forEach(function(item) {
            var sliceAngle = (item.amount / total) * (Math.PI * 2);
            var startAngle = currentAngle;
            var endAngle = currentAngle + sliceAngle;
            currentAngle = endAngle;

            var isSelected = (item.id === selected);
            var oR = isSelected ? outerRadius + 6 : outerRadius;
            var iR = innerRadius;

            // Compute SVG arc path
            var x1 = cx + oR * Math.cos(startAngle);
            var y1 = cy + oR * Math.sin(startAngle);
            var x2 = cx + oR * Math.cos(endAngle);
            var y2 = cy + oR * Math.sin(endAngle);

            var x3 = cx + iR * Math.cos(endAngle);
            var y3 = cy + iR * Math.sin(endAngle);
            var x4 = cx + iR * Math.cos(startAngle);
            var y4 = cy + iR * Math.sin(startAngle);

            var largeArcFlag = sliceAngle > Math.PI ? 1 : 0;

            var pathD = [
              'M', x1, y1,
              'A', oR, oR, 0, largeArcFlag, 1, x2, y2,
              'L', x3, y3,
              'A', iR, iR, 0, largeArcFlag, 0, x4, y4,
              'Z'
            ].join(' ');

            var selectedClass = isSelected ? ' arc-path selected' : ' arc-path';
            html += '<path class="' + selectedClass + '" d="' + pathD + '" fill="' + item.color + '" data-id="' + item.id + '" stroke="#FFFFFF" stroke-width="2" />';
          });

          html += '</g>';

          // Center Text
          var displayTitle = selectedItem ? selectedItem.label : centerTitleText;
          var displayValue = selectedItem ? selectedItem.formatted : centerAmountText;
          var displaySub = selectedItem ? (selectedItem.pct + '% of total') : '';

          html += '<g id="center-readout">';
          html += '<text class="center-title" x="' + cx + '" y="' + (cy - 16) + '">' + escapeXml(displayTitle) + '</text>';
          html += '<text class="center-value" x="' + cx + '" y="' + (cy + 7) + '">' + escapeXml(displayValue) + '</text>';
          if (displaySub) {
            html += '<text class="center-subtitle" x="' + cx + '" y="' + (cy + 24) + '">' + escapeXml(displaySub) + '</text>';
          }
          html += '</g>';

          svg.innerHTML = html;

          // Attach click listeners to paths
          var paths = svg.querySelectorAll('.arc-path');
          paths.forEach(function(path) {
            path.addEventListener('click', function(e) {
              e.stopPropagation();
              var id = this.getAttribute('data-id');
              notifySelection(id);
            });
          });
        }

        // --- Custom D3 / SVG Horizontal Bar Renderer ---
        function renderBarChart(data, selected) {
          var topItems = data.slice(0, 6); // Top 6 items for clean vertical density
          var maxAmount = Math.max.apply(null, topItems.map(function(d) { return d.amount; })) || 1;

          var startY = 22;
          var rowHeight = 38;
          var barLeft = 14;
          var barWidthMax = 332;
          var barH = 12;

          var html = '<g id="bar-chart-groups">';

          topItems.forEach(function(item, idx) {
            var y = startY + (idx * rowHeight);
            var isSelected = (item.id === selected);
            var width = Math.max(12, (item.amount / maxAmount) * barWidthMax);

            html += '<g class="bar-group" data-id="' + item.id + '">';
            // Label & percentage
            html += '<text class="bar-label" x="' + barLeft + '" y="' + (y + 11) + '">' + escapeXml(item.label) + ' <tspan fill="#888" font-size="11" font-weight="normal">(' + item.pct + '%)</tspan></text>';
            // Formatted Amount
            html += '<text class="bar-value" x="' + (barLeft + barWidthMax) + '" y="' + (y + 11) + '">' + escapeXml(item.formatted) + '</text>';
            // Background track
            html += '<rect class="bar-bg" x="' + barLeft + '" y="' + (y + 17) + '" width="' + barWidthMax + '" height="' + barH + '" />';
            // Value Bar
            var strokeAttr = isSelected ? ' stroke="#191919" stroke-width="2"' : '';
            html += '<rect class="bar-rect" x="' + barLeft + '" y="' + (y + 17) + '" width="' + width + '" height="' + barH + '" fill="' + item.color + '"' + strokeAttr + ' />';
            html += '</g>';
          });

          html += '</g>';
          svg.innerHTML = html;

          var groups = svg.querySelectorAll('.bar-group');
          groups.forEach(function(group) {
            group.addEventListener('click', function(e) {
              e.stopPropagation();
              var id = this.getAttribute('data-id');
              notifySelection(id);
            });
          });
        }

        function escapeXml(unsafe) {
          if (!unsafe) return '';
          return unsafe.replace(/[<>&'"]/g, function (c) {
            switch (c) {
              case '<': return '&lt;';
              case '>': return '&gt;';
              case '&': return '&amp;';
              case '\'': return '&apos;';
              case '"': return '&quot;';
            }
          });
        }
      })();
    </script>
  </body>
  </html>
  """.trimIndent()
}
