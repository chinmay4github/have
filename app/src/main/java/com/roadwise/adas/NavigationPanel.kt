package com.roadwise.adas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NavigationPanel(
    isMonitoring: Boolean,
    routeStatus: String,
    onOpenRoute: (String) -> Unit,
) {
    var destination by rememberSaveable { mutableStateOf("") }
    val foreground = MaterialTheme.colorScheme.onSurface
    val muted = Color(0xFF98AAB4)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(23.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, Color(0xFF223640)),
    ) {
        Column(Modifier.padding(17.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
                    Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("ROUTE PLANNER", color = foreground, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                    Text("Send a destination to your maps app", color = muted, fontSize = 10.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = destination,
                onValueChange = { destination = it },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isMonitoring,
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                placeholder = { Text("Destination, address or place", color = muted, fontSize = 12.sp) },
            )
            Spacer(Modifier.height(9.dp))
            Button(
                onClick = { onOpenRoute(destination) },
                enabled = !isMonitoring && destination.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(45.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(7.dp))
                Text("Open turn-by-turn route", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(7.dp))
            Text(
                text = if (isMonitoring) {
                    "Stop monitoring before opening navigation. Roadwise pauses its camera in the background."
                } else {
                    routeStatus
                },
                color = muted,
                fontSize = 10.sp,
                lineHeight = 14.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Routes are provided by Google Maps or the installed map app; no map API key or embedded map is used.",
                color = muted.copy(alpha = 0.8f),
                fontSize = 9.sp,
                lineHeight = 13.sp,
            )
        }
    }
}
