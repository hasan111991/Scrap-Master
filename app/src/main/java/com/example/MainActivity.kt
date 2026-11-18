package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.firebase.configureAppCheck
import com.example.model.PortalRoute
import com.example.ui.CollectionHubApp
import com.example.ui.theme.CollectionHubTheme
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    configureAppCheck(this, intent)
    enableEdgeToEdge()
    val initialRoute = when (intent?.data?.host?.lowercase()) {
      "collector" -> PortalRoute.COLLECTOR_PORTAL
      "hub-admin" -> PortalRoute.ADMIN_PORTAL
      "public" -> PortalRoute.PUBLIC_HOME
      else -> PortalRoute.CUSTOMER_PORTAL
    }
    setContent {
      CollectionHubTheme {
        CollectionHubApp(initialRoute = initialRoute)
      }
    }
  }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
  Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
  MyApplicationTheme { Greeting("Android") }
}
