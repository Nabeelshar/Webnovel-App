package my.webnovels.composableActions

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import my.webnovels.services.RestoreDataService

@Composable
fun onDoRestore(): () -> Unit
{
    val context = LocalContext.current
    val fileExplorer = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            if (uri != null)
                RestoreDataService.start(ctx = context, uri = uri)
        }
    )

    val permissions = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted)
            fileExplorer.launch("application/*")
    }

    return { permissions.launch(Manifest.permission.READ_EXTERNAL_STORAGE) }
}