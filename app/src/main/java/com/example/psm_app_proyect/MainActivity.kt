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
import android.R
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
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
                        Contrato("Carlos Ruiz", "Departamento", "2026-04-10", "2027-04-10", 4500.0, "Nuevo", "Mensual", 10),

                        Contrato("Juan Pérez", "Departamento", "2026-01-01", "2026-12-01", 1200.0, "Activo", "Semanal", 1),

                        Contrato("Ana López", "Local", "2025-03-30", "2026-03-30", 1500.0, "Proximo", "Quincenal", 15),

                        Contrato("Don Camerino", "Local", "2025-03-20", "2026-03-20", 8500.0, "Vencido", "Mensual", 5),

                        Contrato("Coppel", "Local", "2025-03-25", "2026-03-27", 12000.0, "Vence Hoy", "Mensual", 27),

                        Contrato("Gimnasio Muscle", "Local", "2026-06-01", "2027-06-01", 7000.0, "Nuevo", "Mensual", 1),

                        Contrato("Sofía Castro", "Departamento", "2026-02-15", "2026-08-15", 2500.0, "Activo", "Semanal", 3),

                        Contrato("Tacos El Güero", "Local", "2025-12-01", "2026-12-01", 3500.0, "Activo", "Quincenal", 15)
                    )
                }

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val mostrarBotones = currentRoute == "lista"

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

                Box(modifier = Modifier.fillMaxSize().background(gradient)) {
                    Scaffold(
                        containerColor = Color.Transparent,
                        floatingActionButton = {
                            if (mostrarBotones) {
                                Column {
                                    FloatingActionButton(onClick = { navController.navigate("form/-1") }) { Text("+") }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    FloatingActionButton(onClick = { navController.navigate("grafica") }) { Text("📊") }
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

fun calcularProximoPago(diaPago: Int, frecuencia: String): LocalDate {
    val hoy = LocalDate.now()
    var proximo = hoy.withDayOfMonth(1)

    val maxDia = proximo.lengthOfMonth()
    proximo = proximo.withDayOfMonth(if (diaPago > maxDia) maxDia else diaPago)

    if (proximo.isBefore(hoy)) {
        proximo = when (frecuencia) {
            "Semanal" -> hoy.plusWeeks(1)
            "Quincenal" -> if (hoy.dayOfMonth < 15) hoy.withDayOfMonth(15) else hoy.plusMonths(1).withDayOfMonth(diaPago)
            else -> hoy.plusMonths(1).withDayOfMonth(if (diaPago > hoy.plusMonths(1).lengthOfMonth()) hoy.plusMonths(1).lengthOfMonth() else diaPago)
        }
    }
    return proximo
}

fun revisarContratos(context: Context, contratos: List<Contrato>) {
    contratos.forEach {
        val diasVencimiento = calcularDiasRestantes(it.fechaFin)
        val fechaPago = calcularProximoPago(it.diaPago, it.frecuenciaPago)
        val diasParaPago = ChronoUnit.DAYS.between(LocalDate.now(), fechaPago)

        if (diasVencimiento in 0..3) {
            mostrarNotificacion(context, "Contrato por vencer", "${it.nombre} vence en $diasVencimiento días")
        }

        if (diasParaPago in 0..2) {
            mostrarNotificacion(context, "¡Cobro Cercano!", "En $diasParaPago días toca cobrar a ${it.nombre}")
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
    val estado: String,
    val frecuenciaPago: String,
    val diaPago: Int
)

//////////////////////////////////////////////////////////
// CARD
//////////////////////////////////////////////////////////
@Composable
fun ContratoCard(contrato: Contrato, onClick: () -> Unit) {
    val diasRestantes = calcularDiasRestantes(contrato.fechaFin)
    val proximoPago = calcularProximoPago(contrato.diaPago, contrato.frecuenciaPago)
    val diasParaPago = ChronoUnit.DAYS.between(LocalDate.now(), proximoPago)

    val colorLateral = when {
        calcularDiasParaInicio(contrato.fechaInicio) > 0 -> Color(0xFF2196F3)
        diasRestantes < 0 -> Color.Red
        diasRestantes <= 7 -> Color(0xFFFFC107)
        else -> Color(0xFF4CAF50)
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(6.dp),
        modifier = Modifier.fillMaxWidth().padding(8.dp).clickable { onClick() }
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(modifier = Modifier.width(8.dp).fillMaxHeight().background(colorLateral))

            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(text = contrato.nombre, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(text = "✎", fontSize = 18.sp)//hay que cambiar luego el icono por una imagen
                }

                Text(text = "${contrato.fechaInicio} / ${contrato.fechaFin}", fontSize = 13.sp, color = Color.Gray)

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Column {
                        Text(
                            text = "Próximo pago: $proximoPago",
                            fontSize = 14.sp,
                            color = if (diasParaPago <= 3) Color.Red else Color.DarkGray,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Faltan $diasParaPago días",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "$${contrato.monto} MXN",
                            color = Color.Black,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = when(contrato.frecuenciaPago){
                                "Semanal" -> "Cada semana (Día ${contrato.diaPago})"
                                "Quincenal" -> "Cada quincena (Día ${contrato.diaPago})"
                                else -> "Cada mes (Día ${contrato.diaPago})"
                            },
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
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
fun FormContrato(navController: NavController, contratos: MutableList<Contrato>, index: Int?) {
    val esEdicion = index != null && index >= 0
    val contrato = if (esEdicion) contratos[index!!] else null

    var nombre by remember { mutableStateOf(contrato?.nombre ?: "") }
    var tipo by remember { mutableStateOf(contrato?.tipo ?: "Departamento") }
    var monto by remember { mutableStateOf(contrato?.monto?.toString() ?: "") }
    var frecuencia by remember { mutableStateOf(contrato?.frecuenciaPago ?: "Mensual") }
    var diaPago by remember { mutableStateOf(contrato?.diaPago?.toString() ?: "1") }
    var fechaInicio by remember { mutableStateOf(contrato?.fechaInicio ?: "") }
    var fechaFin by remember { mutableStateOf(contrato?.fechaFin ?: "") }

    var error by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var seleccionandoInicio by remember { mutableStateOf(true) }

    val maxDia = when (frecuencia) {
        "Semanal" -> 7
        "Quincenal" -> 14
        else -> 28
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (esEdicion) "Modificar Contrato" else "Crear Nuevo Contrato",
            fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f))
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

                OutlinedTextField(
                    value = nombre, onValueChange = { nombre = it },
                    label = { Text("Nombre del cliente") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    var expFrec by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expFrec,
                        onExpandedChange = { expFrec = !expFrec },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = frecuencia, onValueChange = {}, readOnly = true,
                            label = { Text("Frecuencia") },
                            modifier = Modifier.menuAnchor(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expFrec) }
                        )
                        ExposedDropdownMenu(expanded = expFrec, onDismissRequest = { expFrec = false }) {
                            listOf("Semanal", "Quincenal", "Mensual").forEach {
                                DropdownMenuItem(text = { Text(it) }, onClick = { frecuencia = it; diaPago = "1"; expFrec = false })
                            }
                        }
                    }

                    OutlinedTextField(
                        value = diaPago,
                        onValueChange = { if (it.all { c -> c.isDigit() }) diaPago = it },
                        label = { Text("Día") },
                        modifier = Modifier.weight(0.6f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        supportingText = { Text("Máx: $maxDia", fontSize = 10.sp) }
                    )
                }

                OutlinedTextField(
                    value = monto, onValueChange = { if (it.matches(Regex("^\\d*\\.?\\d*$"))) monto = it },
                    label = { Text("Monto de pago") },
                    modifier = Modifier.fillMaxWidth(),
                    prefix = { Text("$ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                Text("Vigencia del Contrato", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { seleccionandoInicio = true; showDatePicker = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (fechaInicio.isEmpty()) "📅 Inicio" else fechaInicio, fontSize = 12.sp)
                    }

                    Button(
                        onClick = { seleccionandoInicio = false; showDatePicker = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        enabled = fechaInicio.isNotEmpty() // BLOQUEADO si no hay inicio
                    ) {
                        Text(if (fechaFin.isEmpty()) "📅 Fin" else fechaFin, fontSize = 12.sp)
                    }
                }
            }
        }

        if (error.isNotEmpty()) {
            Text(error, color = Color(0xFFFFCDD2), fontWeight = FontWeight.Bold)
        }

        Button(
            onClick = {
                val dInt = diaPago.toIntOrNull() ?: 0
                val mD = monto.toDoubleOrNull()

                when {
                    nombre.isBlank() || monto.isBlank() -> error = "Llena los campos básicos"
                    dInt !in 1..maxDia -> error = "El día debe ser entre 1 y $maxDia para $frecuencia"
                    fechaInicio.isEmpty() || fechaFin.isEmpty() -> error = "Faltan las fechas"
                    LocalDate.parse(fechaFin).isBefore(LocalDate.parse(fechaInicio)) -> error = "La fecha fin debe ser mayor al inicio"
                    else -> {
                        val nuevo = Contrato(nombre, tipo, fechaInicio, fechaFin, mD ?: 0.0, "Activo", frecuencia, dInt)
                        if (esEdicion) contratos[index!!] = nuevo else contratos.add(nuevo)
                        navController.popBackStack()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("GUARDAR CONTRATO", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        if (esEdicion) {
            TextButton(onClick = { contratos.removeAt(index!!); navController.popBackStack() }) {
                Text("Eliminar este contrato", color = Color(0xFFFF8A80))
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val date = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                        if (seleccionandoInicio) {
                            fechaInicio = date.toString()
                            fechaFin = ""
                        } else {
                            fechaFin = date.toString()
                        }
                    }
                    showDatePicker = false
                }) { Text("Confirmar") }
            }
        ) { DatePicker(state = datePickerState) }
    }
}


fun calcularDiasRestantes(fechaFin: String): Long {
    return try {
        val hoy = LocalDate.now()
        val fin = LocalDate.parse(fechaFin)
        ChronoUnit.DAYS.between(hoy, fin)
    } catch (e: Exception) { 0 }
}

fun calcularDiasParaInicio(fechaInicio: String): Long {
    return try {
        val hoy = LocalDate.now()
        val inicio = LocalDate.parse(fechaInicio)
        ChronoUnit.DAYS.between(hoy, inicio)
    } catch (e: Exception) { 0 }
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