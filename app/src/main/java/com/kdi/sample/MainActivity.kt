package com.kdi.sample

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.kdi.sample.ui.theme.SampleTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SampleTheme {
                Scaffold { paddingValues ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        PermissionRequestScreen {
                            setContent {
                                LauncherScreen(Modifier.padding(paddingValues), packageManager)
                            }
                        }
                    } else {
                        LauncherScreen(
                            modifier = Modifier.padding(paddingValues),
                            packageManager = packageManager
                        )
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun PermissionRequestScreen(onPermissionGranted: () -> Unit) {
    var hasPermission by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasPermission = isGranted
        if (isGranted) {
            onPermissionGranted()
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(android.Manifest.permission.QUERY_ALL_PACKAGES)
    }

    if (!hasPermission) {
        Text("Permission required to display apps.")
    }
}

@Composable
fun LauncherScreen(modifier: Modifier, packageManager: PackageManager) {
    val installedApps = getInstalledApps(packageManager)
    val sortedApps = installedApps.sortedBy { it.loadLabel(packageManager).toString() }
    val apps by remember {
        mutableStateOf(sortedApps)
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier
    ) {
        items(apps.size) { index ->
            val app = apps[index]
            AppItem(app, packageManager)
        }
    }
}

@Composable
fun AppItem(app: ResolveInfo, packageManager: PackageManager) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable {
                val launchIntent =
                    packageManager.getLaunchIntentForPackage(app.activityInfo.packageName)
                launchIntent?.let { context.startActivity(it) }
            }
    ) {
        val drawable = app.loadIcon(packageManager)
        val appName = app.loadLabel(packageManager).toString()
        Image(
            bitmap = drawable.toBitmap().asImageBitmap(),
            modifier = Modifier.size(64.dp),
            contentDescription = appName
        )
        Text(
            text = appName,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp)
        )
    }
}

private fun getInstalledApps(packageManager: PackageManager): List<ResolveInfo> {
    val mainIntent = Intent(Intent.ACTION_MAIN, null)
    mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
    return packageManager.queryIntentActivities(mainIntent, 0)
}