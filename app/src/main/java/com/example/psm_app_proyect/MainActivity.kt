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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import java.util.Collections.addAll

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
                    mutableStateListOf<Contrato>().apply {
                        addAll(ContratoStorage.cargar(this@MainActivity))
                    }
                }

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val mostrarBotones = currentRoute == "lista"

                val context = this

                LaunchedEffect(contratos) {
                    snapshotFlow { contratos.toList() }
                        .collect{
                            ContratoStorage.guardar(this@MainActivity, it)
                        }
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

    // Malditas seas Jona, no pusimos que se actualizara la tarjeta mediante la fecha.
    val hoy by produceState(initialValue = LocalDate.now()) {
        while (true) {
            kotlinx.coroutines.delay(60000) // cada minuto
            value = LocalDate.now()
        }
    }

    val diasRestantes = calcularDiasRestantes(contrato.fechaFin)
    val proximoPago = calcularProximoPago(contrato.diaPago, contrato.frecuenciaPago)
    val diasParaPago = ChronoUnit.DAYS.between(hoy, proximoPago)

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
                    // Text(text = "✎", fontSize = 18.sp) //Mejor lo quitamos
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
    contratos: SnapshotStateList<Contrato>
) {

    var filtroTipo by remember { mutableStateOf("Todos") }

    val contratosFiltrados by remember(filtroTipo, contratos) {
        mutableStateOf(
            when (filtroTipo) {
                "Local" -> contratos.filter { it.tipo == "Local" }
                "Departamento" -> contratos.filter { it.tipo == "Departamento" }
                else -> contratos
            }
        )
    }

    Column{

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("Todos", "Local", "Departamento").forEach {
                Button(
                    onClick = { filtroTipo = it },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (filtroTipo == it) Color(0xFF4CAF50) else Color.Gray
                    )
                ) {
                    Text(it)
                }
            }
        }

        if (contratosFiltrados.isEmpty()) {

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Sin contratos",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Agrega un nuevo contrato",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn {
                items(
                    items = contratosFiltrados,
                    key = { "${it.nombre}-${it.fechaInicio}" }
                ) { contrato ->

                    val indexReal = contratos.indexOf(contrato)

                    ContratoCard(contrato) {
                        navController.navigate("form/$indexReal")
                    }
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
fun FormContrato(navController: NavController, contratos: SnapshotStateList<Contrato>, index: Int?) {

    val esEdicion = index != null && index >= 0
    val contrato = if (esEdicion && index in contratos.indices) contratos[index!!] else null

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

    val hoy = LocalDate.now()

    val maxDia = when (frecuencia) {
        "Semanal" -> 7
        "Quincenal" -> 14
        else -> 28
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Text(
            text = if (esEdicion) "Modificar Contrato" else "Crear Nuevo Contrato",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Card {
            Column(modifier = Modifier.padding(16.dp)) {

                // NOMBRE
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                var expandTipo by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = expandTipo,
                    onExpandedChange = { expandTipo = !expandTipo }
                ) {
                    OutlinedTextField(
                        value = tipo,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandTipo)
                        },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = expandTipo,
                        onDismissRequest = { expandTipo = false }
                    ) {
                        listOf("Local", "Departamento").forEach {
                            DropdownMenuItem(
                                text = { Text(it) },
                                onClick = {
                                    tipo = it
                                    expandTipo = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // FRECUENCIA + DIA
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                    var expFrec by remember { mutableStateOf(false) }

                    ExposedDropdownMenuBox(
                        expanded = expFrec,
                        onExpandedChange = { expFrec = !expFrec },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = frecuencia,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Frecuencia") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expFrec)
                            },
                            modifier = Modifier.menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = expFrec,
                            onDismissRequest = { expFrec = false }
                        ) {
                            listOf("Semanal", "Quincenal", "Mensual").forEach {
                                DropdownMenuItem(
                                    text = { Text(it) },
                                    onClick = {
                                        frecuencia = it
                                        diaPago = "1"
                                        expFrec = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = diaPago,
                        onValueChange = {
                            if (it.all { c -> c.isDigit() }) diaPago = it
                        },
                        label = { Text("Día") },
                        modifier = Modifier.weight(0.5f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // MONTO
                OutlinedTextField(
                    value = monto,
                    onValueChange = {
                        if (it.matches(Regex("^\\d*\\.?\\d*$"))) monto = it
                    },
                    label = { Text("Monto") },
                    prefix = { Text("$ ") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // FECHAS
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                    Button(
                        onClick = {
                            seleccionandoInicio = true
                            showDatePicker = true
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(fechaInicio.ifEmpty { "Inicio" })
                    }

                    Button(
                        onClick = {
                            seleccionandoInicio = false
                            showDatePicker = true
                        },
                        enabled = fechaInicio.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(fechaFin.ifEmpty { "Fin" })
                    }
                }
            }
        }

        if (error.isNotEmpty()) {
            Text(error, color = Color.Red)
        }

        // GUARDAR
        Button(
            onClick = {

                val dInt = diaPago.toIntOrNull() ?: 0
                val mD = monto.toDoubleOrNull()

                val inicio = try { LocalDate.parse(fechaInicio) } catch (e: Exception) { null }
                val fin = try { LocalDate.parse(fechaFin) } catch (e: Exception) { null }

                when {
                    nombre.isBlank() -> error = "Nombre vacío"
                    mD == null -> error = "Monto inválido"
                    dInt !in 1..maxDia -> error = "Día inválido"
                    inicio == null || fin == null -> error = "Fechas inválidas"
                    inicio.isBefore(hoy) -> error = "La fecha inicio no puede ser pasada"
                    fin.isBefore(inicio) -> error = "La fecha fin debe ser mayor"
                    else -> {
                        val nuevo = Contrato(
                            nombre,
                            tipo,
                            fechaInicio,
                            fechaFin,
                            mD,
                            "Activo",
                            frecuencia,
                            dInt
                        )

                        if (esEdicion) {
                            contratos.removeAt(index!!)
                            contratos.add(index, nuevo)
                        }
                        else contratos.add(nuevo)

                        navController.popBackStack()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("GUARDAR")
        }

        // ELIMINAR
        if (esEdicion) {
            TextButton(onClick = {
                index?.let {
                    if (it in contratos.indices) {
                        contratos.removeAt(it)
                    }
                }
                navController.popBackStack()
            },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black.copy(alpha = 0.4f)
                )
            ) {
                Text("ELIMINAR CONTRATO",
                    color = Color.Red.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold)
            }
        }
    }

    // DATE PICKER
    if (showDatePicker) {
        val state = rememberDatePickerState()

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        val date = Instant.ofEpochMilli(it)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()

                        if (date.isBefore(hoy)) {
                            error = "No puedes elegir fechas pasadas"
                        } else {
                            if (seleccionandoInicio) {
                                fechaInicio = date.toString()
                                fechaFin = ""
                            } else {
                                fechaFin = date.toString()
                            }
                        }
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            }
        ) {
            DatePicker(state = state)
        }
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
    var anioSeleccionado by remember { mutableStateOf(LocalDate.now().year) }

    val contratosFiltrados = obtenerPagosDelMes(contratos, mesSeleccionado, anioSeleccionado)

    val total = calcularIngresosPorMes(contratos, mesSeleccionado, anioSeleccionado)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text("Filtrar por mes",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.DarkGray)

        Spacer(modifier = Modifier.height(10.dp))

        SelectorAnio(anioSeleccionado) {
            anioSeleccionado = it
        }

        Spacer(modifier = Modifier.height(10.dp))

        SelectorMes(mesSeleccionado) {
            mesSeleccionado = it
        }

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            shape = RoundedCornerShape(25.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.35f)
            )
        ){
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Ingresos Totales: $${total}",
                    fontSize = 20.sp,
                    color = Color.White
                )
                Text(
                    "${meses[mesSeleccionado - 1]} ${anioSeleccionado}", fontSize = 30.sp,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(20.dp))

                DonaChart(contratosFiltrados)
            }
        }
//        Text(
//            text = "Ingresos Totales: $${total}",
//            fontSize = 20.sp,
//            color = Color.DarkGray
//        )
//        Text("${meses[mesSeleccionado-1]}", fontSize = 30.sp,
//            color = Color.DarkGray)
//
//        Spacer(modifier = Modifier.height(20.dp))
//
//        DonaChart(contratosFiltrados)

        Spacer(modifier = Modifier.height(10.dp))

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
                color = Color.White
            )

            Text(
                text = "$${"%,.0f".format(total)}",
                fontSize = 22.sp,
                color = Color.White
            )
        }
    }
}

fun calcularIngresosPorMes(contratos: List<Contrato>, mes: Int, anio: Int): Double {
    var total = 0.0

    contratos.forEach { contrato ->
        try {
            val inicio = LocalDate.parse(contrato.fechaInicio)
            val fin = LocalDate.parse(contrato.fechaFin)

            var fecha = inicio

            while (!fecha.isAfter(fin)) {

                if (fecha.monthValue == mes && fecha.year == anio) {
                    total += contrato.monto
                }

                fecha = when (contrato.frecuenciaPago) {
                    "Semanal" -> fecha.plusWeeks(1)
                    "Quincenal" -> fecha.plusDays(15)
                    else -> fecha.plusMonths(1)
                }
            }

        } catch (_: Exception) {}
    }

    return total
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
            Text(meses[selectedMes - 1],)
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

@Composable
fun SelectorAnio(anio: Int, onChange: (Int) -> Unit) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = { onChange(anio - 1) },
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black.copy(alpha = 0.3f)
            )
        ) {
            Text("◀", color = Color.White)
        }

        Text(
            text = "$anio",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Button(
            onClick = { onChange(anio + 1) },
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black.copy(alpha = 0.3f)
            )
        ) {
            Text("▶", color = Color.White)
        }
    }
}

fun obtenerPagosDelMes(contratos: List<Contrato>, mes: Int, anio: Int): List<Contrato> {
    return contratos.filter { contrato ->
        try {
            val inicio = LocalDate.parse(contrato.fechaInicio)
            val fin = LocalDate.parse(contrato.fechaFin)

            var fecha = inicio

            while (!fecha.isAfter(fin)) {

                if (fecha.monthValue == mes && fecha.year == anio) {
                    return@filter true
                }

                fecha = when (contrato.frecuenciaPago) {
                    "Semanal" -> fecha.plusWeeks(1)
                    "Quincenal" -> fecha.plusDays(15)
                    else -> fecha.plusMonths(1)
                }
            }

            false
        } catch (_: Exception) {
            false
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