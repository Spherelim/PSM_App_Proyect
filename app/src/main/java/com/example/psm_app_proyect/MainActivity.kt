package com.example.psm_app_proyect

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.*
import com.example.psm_app_proyect.ui.theme.PSM_App_ProyectTheme
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.lazy.itemsIndexed
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.Instant
import java.time.ZoneId
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import android.view.animation.OvershootInterpolator
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Alignment
import java.time.Month
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.core.app.NotificationCompat
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.blur
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.KeyboardType

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                1
            )
        }
        setContent {
            PSM_App_ProyectTheme {
                val navController = rememberNavController()

                val contratos = remember {
                    mutableStateListOf(
                        Contrato("Juan Pérez", "Departamento", "2026-12-01", "2026-12-01", 5000.0, "Activo"),
                        Contrato("Ana López", "Local", "2026-12-01", "2026-12-01", 3000.0, "Proximo")
                    )
                }

                val context = this

                LaunchedEffect(Unit) {
                    revisarContratos(context, contratos)
                }

                val isDark = isSystemInDarkTheme()

                val gradient = if (isDark) {
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF0F172A),
                            Color(0xFF1E293B)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFFe3f2fd),
                            Color(0xFF2A5298)
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            gradient
                        )
                ) {
                    Scaffold(
                        containerColor = Color.Transparent,
                        floatingActionButton = {
                            Column {
                                FloatingActionButton(onClick = {
                                    navController.navigate("form/-1")
                                }) {
                                    Text("+")
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                FloatingActionButton(onClick = {
                                    navController.navigate("grafica")
                                }) {
                                    Text("📊")
                                }
                            }
                        }
                    ) { innerPadding ->

                        NavHost(
                            navController = navController,
                            startDestination = "lista",
                            modifier = Modifier.padding(innerPadding)
                        ) {

                            composable("lista") {
                                ListaContratos(navController, contratos)
                            }

                            composable("grafica") {
                                PantallaGrafica(contratos)
                            }

                            composable("form/{index}") { backStackEntry ->

                                val index =
                                    backStackEntry.arguments?.getString("index")?.toIntOrNull()

                                FormContrato(navController, contratos, index)
                            }
                        }
                    }
                }
            }
        }
    }
}

fun mostrarNotificacion(context: Context, titulo: String, mensaje: String) {

    val channelId = "contratos_channel"

    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    val channel = NotificationChannel(
        channelId,
        "Contratos",
        NotificationManager.IMPORTANCE_HIGH
    )

    manager.createNotificationChannel(channel)

    val notification = NotificationCompat.Builder(context, channelId)
        .setContentTitle(titulo)
        .setContentText(mensaje)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .build()

    manager.notify(System.currentTimeMillis().toInt(), notification)
}

fun revisarContratos(context: Context, contratos: List<Contrato>) {

    contratos.forEach {

        val dias = calcularDiasRestantes(it.fechaFin)

        if (dias in 0..3) {
            mostrarNotificacion(
                context,
                "Contrato por vencer",
                "${it.nombre} vence en $dias días"
            )
        }

        if (dias < 0) {
            mostrarNotificacion(
                context,
                "Contrato vencido",
                "${it.nombre} ya venció"
            )
        }
    }
}

//////////////////////////////////////////////////////////
// MODELO
//////////////////////////////////////////////////////////

data class Contrato(
    val nombre: String,
    val tipo: String,
    val fechaInicio: String,
    val fechaFin: String,
    val monto: Double,
    val estado: String
)

//////////////////////////////////////////////////////////
// CARD
//////////////////////////////////////////////////////////

@Composable
fun ContratoCard(
    contrato: Contrato,
    onClick: () -> Unit
) {

    val diasRestantes = calcularDiasRestantes(contrato.fechaFin)
    val estado = obtenerEstado(diasRestantes)

    val color = when (estado) {
        "Activo" -> Color(0xFF4CAF50)
        "Proximo" -> Color(0xFFFFC107)
        else -> Color.Red
    }

    var pressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(),
        label = "scale"
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                onClick = {
                    pressed = true
                    onClick()
                    pressed = false
                }
            )
    ){
        Row(modifier = Modifier.height(IntrinsicSize.Min)){

            // Barra lateral
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .fillMaxHeight()
                    .background(color)
            )

            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = contrato.nombre,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(text = "Tipo: ${contrato.tipo}")
                Text(text = "Inicio: ${contrato.fechaInicio}")
                Text(text = "Fin: ${contrato.fechaFin}")
                Text(
                    text = if (diasRestantes >= 0)
                        "Vence en $diasRestantes días"
                    else
                        "Vencido hace ${-diasRestantes} días"
                )
                Text(text = "Pago: $${contrato.monto}")
            }
        }
    }
}

//////////////////////////////////////////////////////////
// LISTA
//////////////////////////////////////////////////////////

@Composable
fun ListaContratos(
    navController: NavController,
    contratos: MutableList<Contrato>
) {
    if (contratos.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No hay contratos",
                color = Color.White,
                fontSize = 18.sp
            )
        }
    } else {
        LazyColumn {
            itemsIndexed(contratos) { index, contrato ->
                ContratoCard(contrato) {
                    navController.navigate("form/$index")
                }
            }
        }
    }
}

//////////////////////////////////////////////////////////
// FORMULARIO (AGREGAR / EDITAR)
//////////////////////////////////////////////////////////
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormContrato(
    navController: NavController,
    contratos: MutableList<Contrato>,
    index: Int?
) {

    var showDatePicker by remember { mutableStateOf(false) }
    var seleccionandoInicio by remember { mutableStateOf(true) }

    val esEdicion = index != null && index >= 0 && index < contratos.size
    val contrato = if (esEdicion) contratos[index!!] else null

    var fechaInicio by remember { mutableStateOf(contrato?.fechaInicio ?: "") }
    var fechaFin by remember { mutableStateOf(contrato?.fechaFin ?: "") }

    var nombre by remember { mutableStateOf(contrato?.nombre ?: "") }
    var tipo by remember { mutableStateOf(contrato?.tipo ?: "") }
    var monto by remember { mutableStateOf(contrato?.monto?.toString() ?: "") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        Text(
            text = if (esEdicion) "Editar Contrato" else "Nuevo Contrato",
            fontSize = 20.sp
        )

        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Nombre", color = Color.DarkGray) },
            modifier = Modifier.fillMaxWidth()
        )

        var expanded by remember { mutableStateOf(false) }
        val opciones = listOf("Local", "Departamento")

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = tipo,
                onValueChange = {},
                readOnly = true,
                label = { Text("Tipo", color = Color.DarkGray) },
                textStyle = LocalTextStyle.current.copy(color = Color.DarkGray),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.DarkGray,
                    unfocusedTextColor = Color.DarkGray
                ),
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                opciones.forEach {
                    DropdownMenuItem(
                        text = { Text(it) },
                        onClick = {
                            tipo = it
                            expanded = false
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = monto,
            onValueChange = {
                if (it.matches(Regex("^\\d*\\.?\\d*$"))) {
                    monto = it
                }
            },
            label = { Text("Monto", color = Color.DarkGray) },
            textStyle = LocalTextStyle.current.copy(color = Color.DarkGray),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.DarkGray,
                unfocusedTextColor = Color.DarkGray
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                seleccionandoInicio = true
                showDatePicker = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (fechaInicio.isEmpty()) "Seleccionar Fecha Inicio" else fechaInicio)
        }

        Button(
            onClick = {
                seleccionandoInicio = false
                showDatePicker = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (fechaFin.isEmpty()) "Seleccionar Fecha Fin" else fechaFin)
        }
        if (showDatePicker) {

            val datePickerState = rememberDatePickerState(
                selectableDates = object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                        val hoy = LocalDate.now()
                        val date = Instant.ofEpochMilli(utcTimeMillis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()

                        return !date.isBefore(hoy)
                    }
                }
            )

            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {

                        val millis = datePickerState.selectedDateMillis

                        if (millis != null) {
                            val date = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()

                            if (seleccionandoInicio) {
                                fechaInicio = date.toString()
                            } else {
                                fechaFin = date.toString()
                            }
                        }

                        showDatePicker = false
                    }) {
                        Text("OK")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        var error by remember { mutableStateOf("") }

        Button(
            onClick = {

                when {
                    nombre.isBlank() -> error = "El nombre es obligatorio"
                    tipo !in listOf("Local", "Departamento") -> error = "Selecciona un tipo válido"
                    monto.toDoubleOrNull() == null -> error = "Monto inválido"
                    fechaInicio.isBlank() || fechaFin.isBlank() -> error = "Selecciona fechas"
                    else -> {

                        val nuevoContrato = Contrato(
                            nombre,
                            tipo,
                            fechaInicio,
                            fechaFin,
                            monto.toDouble(),
                            "Activo"
                        )

                        if (esEdicion) {
                            contratos[index!!] = nuevoContrato
                        } else {
                            contratos.add(nuevoContrato)
                        }

                        navController.popBackStack()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (esEdicion) "Actualizar" else "Guardar")
        }
        if (esEdicion) {
            Button(
                onClick = {
                    contratos.removeAt(index!!)
                    navController.popBackStack()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Eliminar")
            }
        }

        if (error.isNotEmpty()) {
            Text(
                text = error,
                color = Color.Red,
                fontSize = 14.sp
            )
        }
    }
}

fun calcularDiasRestantes(fechaFin: String): Long {
    return try {
        val hoy = LocalDate.now()
        val fin = LocalDate.parse(fechaFin)
        ChronoUnit.DAYS.between(hoy, fin)
    } catch (e: Exception) {
        0
    }
}

fun obtenerEstado(dias: Long): String {
    return when {
        dias < 0 -> "Vencido"
        dias <= 7 -> "Proximo"
        else -> "Activo"
    }
}

//////////////////////////////////////////////////////////
// GRAFICA
//////////////////////////////////////////////////////////
val meses = listOf(
    "Enero", "Febrero", "Marzo", "Abril",
    "Mayo", "Junio", "Julio", "Agosto",
    "Septiembre", "Octubre", "Noviembre", "Diciembre"
)
@Composable
fun PantallaGrafica(contratos: List<Contrato>) {

    var mesSeleccionado by remember { mutableStateOf(LocalDate.now().monthValue) }

    val contratosFiltrados = filtrarPorMes(contratos, mesSeleccionado)

    val total = contratosFiltrados.sumOf { it.monto }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {


        Text("Filtrar por mes", fontSize = 16.sp,color = Color.DarkGray)

        Spacer(modifier = Modifier.height(10.dp))

        SelectorMes(mesSeleccionado) {
            mesSeleccionado = it
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Ingresos Totales: $${total}",
            fontSize = 20.sp,
            color = Color.DarkGray
        )
        Text("${meses[mesSeleccionado-1]}", fontSize = 30.sp,
            color = Color.DarkGray)

        Spacer(modifier = Modifier.height(20.dp))

        DonaChart(contratosFiltrados)

        Spacer(modifier = Modifier.height(20.dp))

        val colores = listOf(
            Color(0xFF4CAF50),
            Color(0xFFFFC107),
            Color(0xFF2196F3),
            Color(0xFFF44336),
            Color(0xFF9C27B0)
        )

        contratosFiltrados.forEachIndexed { index, contrato ->

            val porcentaje = if (total > 0)
                ((contrato.monto / total) * 100).toInt()
            else 0

            ItemGraficaCard(
                nombre = contrato.nombre,
                porcentaje = porcentaje,
                color = colores[index % colores.size]
            )
        }
    }
}

@Composable
fun DonaChart(contratos: List<Contrato>) {

    val total = contratos.sumOf { it.monto }

    val colores = listOf(
        Color(0xFF4CAF50),
        Color(0xFFFFC107),
        Color(0xFF2196F3),
        Color(0xFFF44336),
        Color(0xFF9C27B0)
    )

    var animationPlayed by remember { mutableStateOf(false) }

    val animatedProgress by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "animation"
    )

    LaunchedEffect(true) {
        animationPlayed = true
    }

    // Box para superponer texto
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(250.dp)
    ) {

        val sinDatos = total == 0.0
        // DONA
        Canvas(modifier = Modifier.matchParentSize()) {

            var startAngle = -90f

            if (sinDatos) {
                drawArc(
                    color = Color.White.copy(alpha = 0.7f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 50f)
                )
            } else {
                contratos.forEachIndexed { index, contrato ->

                    val sweepAngle = ((contrato.monto / total) * 360f * animatedProgress).toFloat()

                    drawArc(
                        color = colores[index % colores.size],
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = 50f)
                    )

                    startAngle += sweepAngle
                }
            }
        }

        // TEXTO EN EL CENTRO
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            Text(
                text = "Total",
                fontSize = 14.sp,
                color = Color.DarkGray
            )

            Text(
                text = "$${"%,.0f".format(total)}",
                fontSize = 22.sp,
                color = Color.DarkGray
            )
        }
    }
}

@Composable
fun ItemGraficaCard(
    nombre: String,
    porcentaje: Int,
    color: Color
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.35f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // 🔵 Indicador de color (como la dona)
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(color, shape = CircleShape)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = nombre,
                    color = Color.White,
                    fontSize = 14.sp
                )
            }

            Text(
                text = "$porcentaje%",
                color = Color.White,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun SelectorMes(selectedMes: Int, onMesChange: (Int) -> Unit) {

    var expanded by remember { mutableStateOf(false) }

    Box {
        Button(onClick = { expanded = true }) {
            Text(meses[selectedMes - 1])
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            meses.forEachIndexed { index, mes ->
                DropdownMenuItem(
                    text = { Text(mes) },
                    onClick = {
                        onMesChange(index + 1)
                        expanded = false
                    }
                )
            }
        }
    }
}

fun filtrarPorMes(contratos: List<Contrato>, mes: Int): List<Contrato> {
    return contratos.filter {
        try {
            val fecha = LocalDate.parse(it.fechaFin)
            fecha.monthValue == mes
        } catch (e: Exception) {
            false
        }
    }
}