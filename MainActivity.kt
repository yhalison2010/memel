package com.exemplo.arcelular

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.ar.core.Frame
import com.google.ar.core.TrackingFailureReason
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.arcore.createAnchorOrNull
import io.github.sceneview.ar.arcore.isValid
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.ar.rememberARCameraNode
import io.github.sceneview.math.Position
import io.github.sceneview.math.Size
import io.github.sceneview.node.CubeNode
import io.github.sceneview.rememberCollisionSystem
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import io.github.sceneview.rememberOnGestureListener
import io.github.sceneview.rememberView

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Tela()
            }
        }
    }
}

@Composable
fun Tela() {
    val context = LocalContext.current
    var temCamera by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val pedir = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        temCamera = it
    }
    LaunchedEffect(Unit) {
        if (!temCamera) pedir.launch(Manifest.permission.CAMERA)
    }

    if (temCamera) {
        CenaAR()
    } else {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Permita o uso da câmera para usar a realidade aumentada.")
        }
    }
}

@Composable
fun CenaAR() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val cameraNode = rememberARCameraNode(engine)
    val childNodes = rememberNodes()
    val view = rememberView(engine)
    val collisionSystem = rememberCollisionSystem(view)

    var frame by remember { mutableStateOf<Frame?>(null) }
    var falha by remember { mutableStateOf<TrackingFailureReason?>(null) }

    val aviso = when (falha) {
        TrackingFailureReason.INSUFFICIENT_LIGHT -> "Pouca luz. Vá para um lugar mais claro."
        TrackingFailureReason.EXCESSIVE_MOTION -> "Movimento rápido demais. Mova devagar."
        TrackingFailureReason.INSUFFICIENT_FEATURES -> "Superfície sem textura. Aponte para algo com detalhes."
        else -> if (childNodes.isEmpty()) "Mova o celular devagar e toque no chão ou na mesa." else "Toque para colocar mais cubos."
    }

    Box(Modifier.fillMaxSize()) {
        ARScene(
            modifier = Modifier.fillMaxSize(),
            childNodes = childNodes,
            engine = engine,
            view = view,
            modelLoader = modelLoader,
            collisionSystem = collisionSystem,
            cameraNode = cameraNode,
            planeRenderer = true,
            onTrackingFailureChanged = { falha = it },
            onSessionUpdated = { _, quadro -> frame = quadro },
            onGestureListener = rememberOnGestureListener(
                onSingleTapConfirmed = { evento, no ->
                    if (no == null) {
                        val acerto = frame
                            ?.hitTest(evento.x, evento.y)
                            ?.firstOrNull { it.isValid(depthPoint = false, point = false) }
                        acerto?.createAnchorOrNull()?.let { ancora ->
                            val cor = Color.hsv((0..359).random().toFloat(), 0.7f, 0.9f)
                            val ancoraNo = AnchorNode(engine = engine, anchor = ancora)
                            ancoraNo.addChildNode(
                                CubeNode(
                                    engine = engine,
                                    size = Size(0.1f),
                                    center = Position(y = 0.05f),
                                    materialInstance = materialLoader.createColorInstance(cor)
                                )
                            )
                            childNodes += ancoraNo
                        }
                    }
                }
            )
        )

        Surface(
            color = Color.Black.copy(alpha = 0.6f),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp, start = 16.dp, end = 16.dp)
        ) {
            Text(aviso, color = Color.White, modifier = Modifier.padding(12.dp))
        }

        Button(
            onClick = { childNodes.clear() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
        ) {
            Text("Limpar cubos")
        }
    }
}
