package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.DiaCumplimiento
import com.example.data.model.HistorialCumplimientoUiState
import com.example.ui.viewmodel.MedicamentoUiModel
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Datos clínicos del paciente requeridos para el informe en PDF.
 */
data class DatosPacienteInforme(
    val nombreCompleto: String = "Sr. Sebastian Sosa",
    val edad: Int = 73,
    val tipoSangre: String = "O+",
    val medicoTratante: String = "Dra. Sara Martinez",
    val especialidadMedico: String = "Cardióloga",
    val contactoEmergencia: String = "Isabela Sosa (Hija)",
    val telefonoEmergencia: String = "+57 310 456 7890"
)

/**
 * Generador nativo de informes médicos en formato PDF de alta calidad y legibilidad
 * para que el adulto mayor o su cuidador lo compartan con su médico tratante
 * (vía WhatsApp, Correo, Drive, Impresión, etc.).
 */
object PdfReportGenerator {

    // Dimensiones estándar A4 a 72 DPI (Puntos tipográficos)
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842

    /**
     * Genera el archivo PDF físico en la caché de la aplicación y retorna el File resultante.
     */
    fun generarReportePdf(
        context: Context,
        paciente: DatosPacienteInforme,
        estadoHistorial: HistorialCumplimientoUiState,
        medicamentosActivos: List<MedicamentoUiModel>
    ): File? {
        val pdfDocument = PdfDocument()

        try {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            dibujarContenidoReporte(canvas, paciente, estadoHistorial, medicamentosActivos)

            pdfDocument.finishPage(page)

            // Crear directorio en caché para reportes compartibles
            val reportsDir = File(context.cacheDir, "reports")
            if (!reportsDir.exists()) {
                reportsDir.mkdirs()
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val pdfFile = File(reportsDir, "Reporte_Adherencia_${paciente.nombreCompleto.replace(" ", "_")}_$timestamp.pdf")

            val outputStream = FileOutputStream(pdfFile)
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()

            return pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            pdfDocument.close()
        }
    }

    /**
     * Abre el diálogo de compartir de Android (Intent.ACTION_SEND) con el archivo PDF adjunto.
     */
    fun compartirReportePdf(
        context: Context,
        pdfFile: File,
        pacienteNombre: String,
        medicoNombre: String
    ) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(
                    Intent.EXTRA_SUBJECT,
                    "Reporte Clínico de Adherencia y Medicamentos - $pacienteNombre"
                )
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Estimada $medicoNombre,\n\nAdjunto el reporte semanal de cumplimiento farmacológico del paciente $pacienteNombre generado desde la aplicación Healty Chronos.\n\nPor favor revise las tomas registradas y las observaciones de salud adjuntas."
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Enviar reporte médico al profesional...")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                context,
                "Error al abrir opciones de envío: ${e.localizedMessage}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * Genera y comparte el PDF en un solo paso con retroalimentación visual al usuario.
     */
    fun exportarYCompartir(
        context: Context,
        paciente: DatosPacienteInforme,
        estadoHistorial: HistorialCumplimientoUiState,
        medicamentosActivos: List<MedicamentoUiModel>,
        onSuccess: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        val file = generarReportePdf(context, paciente, estadoHistorial, medicamentosActivos)
        if (file != null && file.exists()) {
            compartirReportePdf(context, file, paciente.nombreCompleto, paciente.medicoTratante)
            onSuccess?.invoke()
        } else {
            val errorMsg = "No se pudo generar el documento PDF"
            Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
            onError?.invoke(errorMsg)
        }
    }

    /**
     * Dibuja los elementos vectoriales, tipografías y tablas en el canvas del PDF.
     */
    private fun dibujarContenidoReporte(
        canvas: Canvas,
        paciente: DatosPacienteInforme,
        estadoHistorial: HistorialCumplimientoUiState,
        medicamentosActivos: List<MedicamentoUiModel>
    ) {
        val paint = Paint().apply { isAntiAlias = true }

        // Paleta médica profesional
        val colorNavyDark = Color.rgb(11, 25, 44)       // #0B192C
        val colorNavyMedium = Color.rgb(19, 42, 69)     // #132A45
        val colorTealPrimary = Color.rgb(0, 168, 150)   // #00A896
        val colorMintAccent = Color.rgb(2, 201, 159)    // #02C99F
        val colorLightBg = Color.rgb(246, 248, 252)     // #F6F8FC
        val colorCardBg = Color.rgb(255, 255, 255)      // Blanco
        val colorBorder = Color.rgb(222, 228, 238)      // Gris borde
        val colorTextDark = Color.rgb(20, 32, 48)       // Texto principal
        val colorTextMuted = Color.rgb(90, 105, 120)    // Texto secundario
        val colorAlert = Color.rgb(220, 53, 69)         // Rojo alerta
        val colorSuccess = Color.rgb(25, 135, 84)       // Verde éxito

        // 1. Fondo de página general
        paint.color = colorLightBg
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), paint)

        // 2. Banner de Encabezado Superior (Header)
        paint.color = colorNavyDark
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 78f, paint)

        // Línea de acento verde menta bajo el encabezado
        paint.color = colorMintAccent
        canvas.drawRect(0f, 78f, PAGE_WIDTH.toFloat(), 82f, paint)

        // Cruz médica / Icono en Header
        paint.color = colorMintAccent
        val crossX = 36f
        val crossY = 38f
        canvas.drawRect(crossX - 3f, crossY - 12f, crossX + 3f, crossY + 12f, paint)
        canvas.drawRect(crossX - 12f, crossY - 3f, crossX + 12f, crossY + 3f, paint)

        // Texto del Header
        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 17f
        canvas.drawText("HEALTY CHRONOS", 58f, 35f, paint)

        paint.color = Color.rgb(180, 210, 230)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 9.5f
        canvas.drawText("INFORME CLÍNICO DE ADHERENCIA Y SEGUIMIENTO FARMACOLÓGICO", 58f, 49f, paint)
        canvas.drawText("Sistema Integral de Gestión de Medicamentos para el Adulto Mayor", 58f, 61f, paint)

        // Fecha de emisión en la esquina superior derecha
        val fechaEmision = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "ES")).format(Date())
        paint.color = Color.rgb(200, 220, 240)
        paint.textSize = 8.5f
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Fecha de emisión: $fechaEmision", PAGE_WIDTH - 24f, 38f, paint)
        canvas.drawText("Documento Médico Oficial", PAGE_WIDTH - 24f, 52f, paint)
        paint.textAlign = Paint.Align.LEFT

        var curY = 96f

        // 3. Tarjeta de Datos del Paciente y Médico Tratante
        val cardMargin = 24f
        val cardWidth = PAGE_WIDTH - (cardMargin * 2)
        val infoCardHeight = 72f

        paint.color = colorCardBg
        val infoRect = RectF(cardMargin, curY, cardMargin + cardWidth, curY + infoCardHeight)
        canvas.drawRoundRect(infoRect, 8f, 8f, paint)

        paint.color = colorBorder
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(infoRect, 8f, 8f, paint)
        paint.style = Paint.Style.FILL

        // Banda lateral izquierda identificadora
        paint.color = colorTealPrimary
        canvas.drawRoundRect(RectF(cardMargin, curY, cardMargin + 5f, curY + infoCardHeight), 4f, 4f, paint)

        // Columna 1: Paciente
        paint.color = colorNavyDark
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 12f
        canvas.drawText("PACIENTE: ${paciente.nombreCompleto.uppercase()}", cardMargin + 14f, curY + 20f, paint)

        paint.color = colorTextMuted
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 9.5f
        canvas.drawText("Edad: ${paciente.edad} años  |  Grupo Sanguíneo: ${paciente.tipoSangre}", cardMargin + 14f, curY + 36f, paint)
        canvas.drawText("Emergencia: ${paciente.contactoEmergencia} - Tel: ${paciente.telefonoEmergencia}", cardMargin + 14f, curY + 52f, paint)

        // Columna 2: Médico
        val col2X = cardMargin + (cardWidth * 0.55f)
        paint.color = colorNavyDark
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 10.5f
        canvas.drawText("MÉDICO TRATANTE:", col2X, curY + 20f, paint)

        paint.color = colorTealPrimary
        paint.textSize = 11f
        canvas.drawText(paciente.medicoTratante, col2X, curY + 36f, paint)

        paint.color = colorTextMuted
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 9.5f
        canvas.drawText("Especialidad: ${paciente.especialidadMedico}", col2X, curY + 52f, paint)

        curY += infoCardHeight + 14f

        // 4. Bloque de Cumplimiento Semanal General (KPIs)
        paint.color = colorNavyMedium
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 12f
        canvas.drawText("RESUMEN GENERAL DE CUMPLIMIENTO (ÚLTIMA SEMANA)", cardMargin, curY + 2f, paint)

        curY += 10f

        val kpiWidth = (cardWidth - 24f) / 4f
        val kpiHeight = 52f

        // KPI 1: Adherencia Global %
        dibujarKpiBox(
            canvas, cardMargin, curY, kpiWidth, kpiHeight,
            "${estadoHistorial.porcentajeGeneralSemana}%",
            "Adherencia Global",
            if (estadoHistorial.porcentajeGeneralSemana >= 80) colorMintAccent else colorAlert,
            colorNavyDark
        )

        // KPI 2: Dosis Tomadas
        dibujarKpiBox(
            canvas, cardMargin + kpiWidth + 8f, curY, kpiWidth, kpiHeight,
            "${estadoHistorial.dosisTomadasTotal} / ${estadoHistorial.dosisPlanificadasTotal}",
            "Tomas a Tiempo",
            colorSuccess,
            colorNavyDark
        )

        // KPI 3: Dosis Omitidas / Retraso
        dibujarKpiBox(
            canvas, cardMargin + (kpiWidth + 8f) * 2, curY, kpiWidth, kpiHeight,
            "${estadoHistorial.dosisOmitidasTotal}",
            "Tomas Omitidas",
            if (estadoHistorial.dosisOmitidasTotal == 0) colorSuccess else colorAlert,
            colorNavyDark
        )

        // KPI 4: Racha Activa
        dibujarKpiBox(
            canvas, cardMargin + (kpiWidth + 8f) * 3, curY, kpiWidth, kpiHeight,
            "${estadoHistorial.rachaDiasConsecutivos} Días",
            "Racha Consecutiva",
            Color.rgb(255, 158, 0),
            colorNavyDark
        )

        curY += kpiHeight + 16f

        // 5. Tabla de Desglose de Cumplimiento de los 7 Días de la Semana
        paint.color = colorNavyMedium
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 11.5f
        canvas.drawText("HISTORIAL DIARIO DE TOMAS (ÚLTIMOS 7 DÍAS)", cardMargin, curY + 2f, paint)

        curY += 10f

        val tableHeaderHeight = 20f
        val tableRowHeight = 18f
        val diasAMostrar = estadoHistorial.diasHistorial.takeLast(7).ifEmpty {
            crearDiasFallback()
        }

        // Encabezado de la tabla de días
        paint.color = colorNavyDark
        canvas.drawRoundRect(RectF(cardMargin, curY, cardMargin + cardWidth, curY + tableHeaderHeight), 4f, 4f, paint)

        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 9f
        canvas.drawText("DÍA Y FECHA", cardMargin + 10f, curY + 13f, paint)
        canvas.drawText("TOMAS COMPLETADAS", cardMargin + 150f, curY + 13f, paint)
        canvas.drawText("PORCENTAJE", cardMargin + 310f, curY + 13f, paint)
        canvas.drawText("ESTADO CLÍNICO", cardMargin + 420f, curY + 13f, paint)

        curY += tableHeaderHeight

        // Filas de los 7 días
        diasAMostrar.forEachIndexed { index, dia ->
            val rowY = curY + (index * tableRowHeight)
            val isEven = index % 2 == 0
            paint.color = if (isEven) Color.WHITE else Color.rgb(240, 244, 250)
            canvas.drawRect(cardMargin, rowY, cardMargin + cardWidth, rowY + tableRowHeight, paint)

            // Línea separadora tenue
            paint.color = colorBorder
            paint.strokeWidth = 0.5f
            canvas.drawLine(cardMargin, rowY + tableRowHeight, cardMargin + cardWidth, rowY + tableRowHeight, paint)

            // Textos de fila
            paint.color = colorTextDark
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 8.5f
            val diaEtiqueta = "${dia.diaNombre} (${dia.fechaCorta})"
            canvas.drawText(diaEtiqueta, cardMargin + 10f, rowY + 12f, paint)

            canvas.drawText("${dia.dosisTomadas} de ${dia.dosisTotal} tomas", cardMargin + 150f, rowY + 12f, paint)

            // Barra miniatura de progreso
            val barX = cardMargin + 310f
            val barW = 70f
            val barH = 7f
            paint.color = Color.rgb(215, 222, 235)
            canvas.drawRoundRect(RectF(barX, rowY + 5f, barX + barW, rowY + 5f + barH), 3f, 3f, paint)

            val pct = dia.porcentaje.coerceIn(0f, 100f)
            paint.color = if (pct >= 80f) colorTealPrimary else if (pct >= 50f) Color.rgb(255, 158, 0) else colorAlert
            canvas.drawRoundRect(RectF(barX, rowY + 5f, barX + (barW * (pct / 100f)), rowY + 5f + barH), 3f, 3f, paint)

            paint.color = colorTextDark
            canvas.drawText("${pct.toInt()}%", barX + barW + 8f, rowY + 12f, paint)

            // Etiqueta de estado
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val (estadoTexto, estadoColor) = when {
                pct >= 80f -> Pair("Completado", colorSuccess)
                pct >= 50f -> Pair("Retraso / Parcial", Color.rgb(220, 140, 0))
                else -> Pair("Omitido / Atención", colorAlert)
            }
            paint.color = estadoColor
            canvas.drawText(estadoTexto, cardMargin + 420f, rowY + 12f, paint)
        }

        curY += (diasAMostrar.size * tableRowHeight) + 14f

        // 6. Medicamentos Activos y Esquema Posológico
        paint.color = colorNavyMedium
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 11.5f
        canvas.drawText("ESQUEMA POSOLÓGICO Y MEDICAMENTOS ACTIVOS REGISTRADOS", cardMargin, curY + 2f, paint)

        curY += 10f

        val medTableH = 20f
        val medRowH = 22f
        val medsList = if (medicamentosActivos.isNotEmpty()) medicamentosActivos else getMedsFallback()

        // Encabezado de tabla de medicamentos
        paint.color = colorNavyDark
        canvas.drawRoundRect(RectF(cardMargin, curY, cardMargin + cardWidth, curY + medTableH), 4f, 4f, paint)

        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 9f
        canvas.drawText("MEDICAMENTO", cardMargin + 10f, curY + 13f, paint)
        canvas.drawText("DOSIS", cardMargin + 120f, curY + 13f, paint)
        canvas.drawText("HORARIO", cardMargin + 180f, curY + 13f, paint)
        canvas.drawText("CATEGORÍA", cardMargin + 245f, curY + 13f, paint)
        canvas.drawText("INDICACIONES CLÍNICAS", cardMargin + 325f, curY + 13f, paint)

        curY += medTableH

        medsList.take(5).forEachIndexed { index, med ->
            val rowY = curY + (index * medRowH)
            val isEven = index % 2 == 0
            paint.color = if (isEven) Color.WHITE else Color.rgb(242, 246, 252)
            canvas.drawRect(cardMargin, rowY, cardMargin + cardWidth, rowY + medRowH, paint)

            paint.color = colorBorder
            paint.strokeWidth = 0.5f
            canvas.drawLine(cardMargin, rowY + medRowH, cardMargin + cardWidth, rowY + medRowH, paint)

            paint.color = colorTextDark
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 9f
            canvas.drawText(med.nombre, cardMargin + 10f, rowY + 14f, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(med.dosis, cardMargin + 120f, rowY + 14f, paint)

            paint.color = colorTealPrimary
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(med.horaToma, cardMargin + 180f, rowY + 14f, paint)

            paint.color = colorTextMuted
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(med.categoria, cardMargin + 245f, rowY + 14f, paint)

            paint.color = colorTextDark
            val indicacionesCortas = if (med.indicaciones.length > 40) {
                med.indicaciones.take(38) + "..."
            } else {
                med.indicaciones.ifBlank { "Sin indicaciones adicionales" }
            }
            canvas.drawText(indicacionesCortas, cardMargin + 325f, rowY + 14f, paint)
        }

        curY += (medsList.take(5).size * medRowH) + 14f

        // 7. Observación Clínica y Recomendación Geriátrica
        val obsBoxHeight = 44f
        paint.color = Color.rgb(238, 247, 246)
        val obsRect = RectF(cardMargin, curY, cardMargin + cardWidth, curY + obsBoxHeight)
        canvas.drawRoundRect(obsRect, 6f, 6f, paint)

        paint.color = colorTealPrimary
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(obsRect, 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        paint.color = colorTealPrimary
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 9.5f
        canvas.drawText("OBSERVACIONES DE ADHERENCIA GERIÁTRICA:", cardMargin + 10f, curY + 15f, paint)

        paint.color = colorTextDark
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 8.5f
        val obsTexto = estadoHistorial.consejoGeriátrico.ifBlank {
            "El paciente mantiene un alto nivel de cumplimiento (${estadoHistorial.porcentajeGeneralSemana}%). Se sugiere mantener la rutina matutina para asegurar la estabilidad hemodinámica."
        }
        val linea1 = if (obsTexto.length > 95) obsTexto.take(92) + "..." else obsTexto
        canvas.drawText(linea1, cardMargin + 10f, curY + 28f, paint)
        canvas.drawText("Medicamento más constante: ${estadoHistorial.medicamentoMasConstante}", cardMargin + 10f, curY + 39f, paint)

        curY += obsBoxHeight + 20f

        // 8. Sección de Firmas y Sello Médico
        val colWidth = (cardWidth - 40f) / 2f
        val sigLineY = curY + 38f

        // Firma Médico Tratante
        paint.color = Color.rgb(180, 190, 205)
        paint.strokeWidth = 1f
        canvas.drawLine(cardMargin + 20f, sigLineY, cardMargin + 20f + colWidth - 20f, sigLineY, paint)

        paint.color = colorTextDark
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 9f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("FIRMA Y SELLO MÉDICO TRATANTE", cardMargin + 20f + (colWidth - 20f) / 2f, sigLineY + 12f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 8f
        paint.color = colorTextMuted
        canvas.drawText("${paciente.medicoTratante} - ${paciente.especialidadMedico}", cardMargin + 20f + (colWidth - 20f) / 2f, sigLineY + 23f, paint)

        // Firma Paciente / Cuidador
        val col2SigX = cardMargin + colWidth + 40f
        paint.color = Color.rgb(180, 190, 205)
        canvas.drawLine(col2SigX + 20f, sigLineY, col2SigX + 20f + colWidth - 20f, sigLineY, paint)

        paint.color = colorTextDark
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 9f
        canvas.drawText("FIRMA DEL PACIENTE O CUIDADOR", col2SigX + 20f + (colWidth - 20f) / 2f, sigLineY + 12f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 8f
        paint.color = colorTextMuted
        canvas.drawText(paciente.contactoEmergencia, col2SigX + 20f + (colWidth - 20f) / 2f, sigLineY + 23f, paint)
        paint.textAlign = Paint.Align.LEFT

        // 9. Pie de Página (Footer)
        paint.color = colorNavyDark
        canvas.drawRect(0f, PAGE_HEIGHT - 24f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), paint)

        paint.color = Color.rgb(180, 205, 230)
        paint.textSize = 7.5f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(
            "Documento confidencial emitido por Healty Chronos. Válido para control clínico de medicación del adulto mayor. Página 1 de 1",
            PAGE_WIDTH / 2f,
            PAGE_HEIGHT - 9f,
            paint
        )
        paint.textAlign = Paint.Align.LEFT
    }

    private fun dibujarKpiBox(
        canvas: Canvas,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        value: String,
        label: String,
        valueColor: Int,
        bgContainerColor: Int
    ) {
        val paint = Paint().apply { isAntiAlias = true }

        // Fondo de tarjeta KPI
        paint.color = bgContainerColor
        val rect = RectF(x, y, x + width, y + height)
        canvas.drawRoundRect(rect, 8f, 8f, paint)

        // Borde fino
        paint.color = Color.rgb(35, 60, 95)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(rect, 8f, 8f, paint)
        paint.style = Paint.Style.FILL

        // Valor grande
        paint.color = valueColor
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 15f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(value, x + (width / 2f), y + 23f, paint)

        // Etiqueta pequeña
        paint.color = Color.rgb(200, 220, 240)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 8f
        canvas.drawText(label, x + (width / 2f), y + 40f, paint)
        paint.textAlign = Paint.Align.LEFT
    }

    private fun crearDiasFallback(): List<DiaCumplimiento> {
        val dias = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Hoy")
        val pcts = listOf(100f, 100f, 75f, 100f, 80f, 100f, 100f)
        return dias.mapIndexed { idx, d ->
            DiaCumplimiento(
                id = "dia_$idx",
                diaNombre = d,
                fechaCorta = "Día ${idx + 1}",
                fechaCompleta = "$d de la semana",
                porcentaje = pcts[idx],
                dosisTomadas = if (pcts[idx] >= 100f) 4 else 3,
                dosisTotal = 4,
                esHoy = idx == dias.size - 1
            )
        }
    }

    private fun getMedsFallback(): List<MedicamentoUiModel> {
        return listOf(
            MedicamentoUiModel("1", 1L, "Losartán", "50 mg", "08:00 AM", "Presión", "Tomar después del desayuno con agua.", true, true),
            MedicamentoUiModel("2", 2L, "Metformina", "850 mg", "01:00 PM", "Diabetes", "Tomar inmediatamente después del almuerzo.", true, true),
            MedicamentoUiModel("3", 3L, "Calcio D3", "600 mg", "08:00 PM", "Vitaminas", "Tomar antes de dormir con un poco de agua.", true, false),
            MedicamentoUiModel("4", 4L, "Aspirina 100", "100 mg", "08:00 AM", "Corazón", "Protección cardiovascular preventiva.", true, true)
        )
    }
}
