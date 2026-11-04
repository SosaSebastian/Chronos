package com.example.util

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.theme.MintInteractive
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavySurfaceVariant
import com.example.ui.theme.TextWhite
import java.util.Locale

/**
 * Lanza el diálogo del reconocedor de voz de Android en español.
 */
fun createSpeechIntent(prompt: String = "Habla ahora..."): Intent {
    return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.forLanguageTag("es-ES").toLanguageTag())
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "es-ES")
        putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "es-ES")
        putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
    }
}

/**
 * Botón con ícono de micrófono y etiqueta accesible para adultos mayores,
 * que gestiona permisos de audio y llama a la API de reconocimiento de voz.
 */
@Composable
fun VoiceDictationButton(
    modifier: Modifier = Modifier,
    label: String? = "Dictar",
    promptText: String = "Habla para dictar...",
    testTag: String = "btn_dictar_voz",
    containerColor: Color = MintInteractive,
    contentColor: Color = NavyDark,
    onSpeechResult: (String) -> Unit
) {
    val context = LocalContext.current

    // Launcher de reconocimiento de voz
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenMatches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = spokenMatches?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                onSpeechResult(spokenText.trim())
            }
        }
    }

    // Launcher de solicitud de permiso de micrófono
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                speechLauncher.launch(createSpeechIntent(promptText))
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(
                    context,
                    "Reconocimiento de voz de Android no disponible",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            Toast.makeText(
                context,
                "Se requiere permiso de micrófono para dictar",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun startListening() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            try {
                speechLauncher.launch(createSpeechIntent(promptText))
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(
                    context,
                    "Reconocimiento de voz de Android no disponible",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    if (label != null) {
        FilledTonalButton(
            onClick = { startListening() },
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = containerColor,
                contentColor = contentColor
            ),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = modifier
                .defaultMinSize(minHeight = 44.dp)
                .testTag(testTag)
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Dictar por voz",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    } else {
        IconButton(
            onClick = { startListening() },
            modifier = modifier
                .size(48.dp)
                .testTag(testTag)
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Dictar por voz",
                tint = containerColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
