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

                LaunchedEffect(Unit) {
                    revisarContratos(this@MainActivity, contratos)
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

fun calcularProximoPago(diaPago: Int, frecuencia: String, fechaInicio: String, fechaFin: String): LocalDate? {
    try {
        val hoy = LocalDate.now()
        val inicio = LocalDate.parse(fechaInicio)
        val fin = LocalDate.parse(fechaFin)

        if (hoy.isAfter(fin)) {
            return null
        }

        val puntoPartida = if (hoy.isBefore(inicio)) inicio else hoy

        var proximo = puntoPartida.withDayOfMonth(1)
        val maxDia = proximo.lengthOfMonth()
        proximo = proximo.withDayOfMonth(if (diaPago > maxDia) maxDia else diaPago)

        if (proximo.isBefore(puntoPartida)) {
            proximo = when (frecuencia) {
                "Semanal" -> puntoPartida.plusWeeks(1)
                "Quincenal" -> if (puntoPartida.dayOfMonth < 15) puntoPartida.withDayOfMonth(15) else puntoPartida.plusMonths(1).withDayOfMonth(diaPago)
                else -> puntoPartida.plusMonths(1).withDayOfMonth(if (diaPago > puntoPartida.plusMonths(1).lengthOfMonth()) puntoPartida.plusMonths(1).lengthOfMonth() else diaPago)
            }
        }

        if (proximo.isAfter(fin)) {
            return null
        }

        return proximo

    } catch (e: Exception) {
        return null
    }
}

fun revisarContratos(context: Context, contratos: SnapshotStateList<Contrato>) {
    val hoy = LocalDate.now()

    for (i in contratos.indices) {
        val it = contratos[i]

        val diasParaInicio = calcularDiasParaInicio(it.fechaInicio)
        val diasParaFin = calcularDiasRestantes(it.fechaFin)
        val fechaPago = calcularProximoPago(it.diaPago, it.frecuenciaPago, it.fechaInicio, it.fechaFin)

        var avisoUnDiaInicio = it.notificadoUnDiaInicio
        var avisoInicioHoy = it.notificadoInicioHoy
        var avisoUnDiaFin = it.notificadoUnDiaFin
        var avisoFinHoy = it.notificadoFinHoy

        if (diasParaInicio == 1L && !it.notificadoUnDiaInicio) {
            mostrarNotificacion(context, "Contrato por iniciar", "Mañana inicia el contrato de ${it.nombre}")
            avisoUnDiaInicio = true
        }

        if (diasParaInicio == 0L && !it.notificadoInicioHoy) {
            mostrarNotificacion(context, "Contrato Iniciado", "Hoy ha entrado en vigor el contrato de ${it.nombre}")
            avisoInicioHoy = true
        }

        if (diasParaFin == 1L && !it.notificadoUnDiaFin) {
            mostrarNotificacion(context, "Contrato por finalizar", "Mañana llega a su fin el contrato de ${it.nombre}")
            avisoUnDiaFin = true
        }

        if (diasParaFin == 0L && !it.notificadoFinHoy) {
            mostrarNotificacion(context, "Contrato Finalizado", "Hoy ha vencido formalmente el contrato de ${it.nombre}")
            avisoFinHoy = true
        }

        if (fechaPago != null) {
            val diasParaPago = ChronoUnit.DAYS.between(hoy, fechaPago)
            if (diasParaPago in 0..2) {
                mostrarNotificacion(
                    context,
                    "¡Cobro Cercano!",
                    "En $diasParaPago días toca cobrar a ${it.nombre}"
                )
            }
        }

        if (avisoUnDiaInicio != it.notificadoUnDiaInicio ||
            avisoInicioHoy != it.notificadoInicioHoy ||
            avisoUnDiaFin != it.notificadoUnDiaFin ||
            avisoFinHoy != it.notificadoFinHoy) {

            contratos[i] = it.copy(
                notificadoUnDiaInicio = avisoUnDiaInicio,
                notificadoInicioHoy = avisoInicioHoy,
                notificadoUnDiaFin = avisoUnDiaFin,
                notificadoFinHoy = avisoFinHoy
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
    val estado: String,
    val frecuenciaPago: String,
    val diaPago: Int,

    val notificadoUnDiaInicio: Boolean = false,
    val notificadoInicioHoy: Boolean = false,
    val notificadoUnDiaFin: Boolean = false,
    val notificadoFinHoy: Boolean = false
)

//////////////////////////////////////////////////////////
// CARD
//////////////////////////////////////////////////////////
@Composable
fun ContratoCard(contrato: Contrato, onClick: () -> Unit) {

    val hoy by produceState(initialValue = LocalDate.now()) {
        while (true) {
            kotlinx.coroutines.delay(60000)
            value = LocalDate.now()
        }
    }

    val diasRestantes = calcularDiasRestantes(contrato.fechaFin)
    val diasParaInicio = calcularDiasParaInicio(contrato.fechaInicio)
    val proximoPago = calcularProximoPago(contrato.diaPago, contrato.frecuenciaPago, contrato.fechaInicio, contrato.fechaFin)

    val colorLateral = when {
        diasParaInicio > 0 -> Color(0xFF2196F3) // Azul: No ha iniciado
        diasRestantes < 0 -> Color.Red         // Rojo: Vencido
        diasRestantes <= 7 -> Color(0xFFFFC107) // Amarillo: Por vencer
        else -> Color(0xFF4CAF50)              // Verde: Activo
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
                }

                Text(text = "${contrato.fechaInicio} / ${contrato.fechaFin}", fontSize = 13.sp, color = Color.Gray)

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {

                    Column {
                        if (diasParaInicio > 0) {
                            Text(
                                text = "Sin iniciar",
                                fontSize = 14.sp,
                                color = Color(0xFF2196F3),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Inicia en $diasParaInicio días",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        } else if (proximoPago == null) {
                            Text(
                                text = "Contrato Finalizado",
                                fontSize = 14.sp,
                                color = Color.Red,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Vencido hace ${kotlin.math.abs(diasRestantes)} días",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        } else {
                            val diasParaPago = ChronoUnit.DAYS.between(hoy, proximoPago)
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
                                "Semanal" -> "Cada semana"
                                "Quincenal" -> "Cada quincena"
                                else -> "Cada mes"
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

    var showHelpModal by remember { mutableStateOf(false) }

    val hoy = LocalDate.now()

    val maxDia = when (frecuencia) {
        "Semanal" -> 7
        "Quincenal" -> 14
        else -> 30
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

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {

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

                    Column(modifier = Modifier.weight(0.7f)) {
                        OutlinedTextField(
                            value = diaPago,
                            onValueChange = {
                                if (it.all { c -> c.isDigit() }) diaPago = it
                            },
                            label = { Text("Día") },
                            trailingIcon = {
                                // Botón de ayuda interactivo tipo "?"
                                IconButton(onClick = { showHelpModal = true }) {
                                    Text("❔", fontSize = 16.sp)
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "Rango: 1-$maxDia",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

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

        if (showHelpModal) {
            AlertDialog(
                onDismissRequest = { showHelpModal = false },
                title = { Text("¿Cómo configurar el Día?", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "📅 Semanal (1 al 7):\nSelecciona el día de la semana que se cobrará (1 = Lunes, 7 = Domingo).",
                            fontSize = 14.sp
                        )
                        Text(
                            text = "📅 Quincenal (1 al 14):\nSelecciona el día relativo de cobro:\n• 1 a 7: Lunes a Domingo de la primera semana.\n• 8 a 14: Lunes a Domingo de la segunda semana.",
                            fontSize = 14.sp
                        )
                        Text(
                            text = "📅 Mensual (1 al 30):\nSelecciona el día calendario exacto de cada mes para realizar el cobro (Días 1 al 30).",
                            fontSize = 14.sp
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showHelpModal = false }) {
                        Text("Entendido")
                    }
                },
                shape = RoundedCornerShape(16.dp)
            )
        }

        if (error.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = { error = "" },
                title = { Text(text = "Error de Validación", fontWeight = FontWeight.Bold) },
                text = { Text(text = error) },
                confirmButton = {
                    TextButton(onClick = { error = "" }) {
                        Text("OK")
                    }
                },
                shape = RoundedCornerShape(16.dp)
            )
        }

        Button(
            onClick = {
                val dInt = diaPago.toIntOrNull() ?: 0
                val mD = monto.toDoubleOrNull()

                val inicio = try { LocalDate.parse(fechaInicio) } catch (e: Exception) { null }
                val fin = try { LocalDate.parse(fechaFin) } catch (e: Exception) { null }

                when {
                    nombre.isBlank() -> error = "Nombre vacío"
                    mD == null -> error = "Monto inválido"
                    dInt !in 1..maxDia -> error = "Día fuera del rango permitido (1-$maxDia)"
                    inicio == null || fin == null -> error = "Fechas inválidas"
                    !esEdicion && inicio.isBefore(hoy) -> error = "La fecha inicio no puede ser pasada"
                    fin.isBefore(inicio) -> error = "La fecha fin debe ser mayor a la fecha de inicio"
                    else -> {
                        val nuevo = Contrato(
                            nombre = nombre,
                            tipo = tipo,
                            fechaInicio = fechaInicio,
                            fechaFin = fechaFin,
                            monto = mD,
                            estado = contrato?.estado ?: "Activo",
                            frecuenciaPago = frecuencia,
                            diaPago = dInt,
                            notificadoUnDiaInicio = contrato?.notificadoUnDiaInicio ?: false,
                            notificadoInicioHoy = contrato?.notificadoInicioHoy ?: false,
                            notificadoUnDiaFin = contrato?.notificadoUnDiaFin ?: false,
                            notificadoFinHoy = contrato?.notificadoFinHoy ?: false
                        )
                        if (esEdicion) {
                            contratos.removeAt(index!!)
                            contratos.add(index, nuevo)
                        } else {
                            contratos.add(nuevo)
                        }

                        navController.popBackStack()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("GUARDAR")
        }

        if (esEdicion) {
            TextButton(
                onClick = {
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
                Text(
                    "ELIMINAR CONTRATO",
                    color = Color.Red.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

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

                        if (!esEdicion && date.isBefore(hoy)) {
                            error = "No puedes elegir fechas pasadas para contratos nuevos"
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

    val totalMes = calcularIngresosPorMes(contratos, mesSeleccionado, anioSeleccionado)

    val colores = listOf(
        Color(0xFF4CAF50),
        Color(0xFFFFC107),
        Color(0xFF2196F3),
        Color(0xFFF44336),
        Color(0xFF9C27B0)
    )

    val itemsGrafica = remember(contratos, mesSeleccionado, anioSeleccionado) {
        contratos.mapIndexed { index, contrato ->
            val montoEnMes = calcularMontoDeContratoEnMes(contrato, mesSeleccionado, anioSeleccionado)
            ItemGrafica(
                nombre = contrato.nombre.ifBlank { "Sin nombre" },
                montoGeneradoEnMes = montoEnMes,
                color = colores[index % colores.size]
            )
        }.filter { it.montoGeneradoEnMes > 0.0 }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Text(
            text = "Análisis de Ingresos",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .background(
                    color = Color.Black.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    SelectorMes(mesSeleccionado) {
                        mesSeleccionado = it
                    }
                }

                Box(
                    modifier = Modifier
                        .width(1.5.dp)
                        .height(24.dp)
                        .background(Color.White.copy(alpha = 0.2f))
                )

                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { anioSeleccionado-- },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("◀", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                    }

                    Text(
                        text = "$anioSeleccionado",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    IconButton(
                        onClick = { anioSeleccionado++ },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("▶", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(25.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Ingresos Totales",
                    fontSize = 15.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Text(
                    text = "$${"%,.2f".format(totalMes)} MXN",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = "${meses[mesSeleccionado - 1]} $anioSeleccionado",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                DonaChart(itemsGrafica, totalMes)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        itemsGrafica.forEach { item ->
            val porcentaje = if (totalMes > 0) (item.montoGeneradoEnMes / totalMes) * 100 else 0.0

            ItemGraficaCard(
                nombre = item.nombre,
                porcentaje = porcentaje,
                color = item.color
            )
        }
    }
}

@Composable
fun DonaChart(items: List<ItemGrafica>, totalMes: Double) {
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

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(220.dp)
    ) {
        val sinDatos = totalMes == 0.0

        Canvas(modifier = Modifier.matchParentSize()) {
            var startAngle = -90f

            if (sinDatos) {
                drawArc(
                    color = Color.White.copy(alpha = 0.2f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 40f)
                )
            } else {
                items.forEach { item ->
                    val sweepAngle = ((item.montoGeneradoEnMes / totalMes) * 360f * animatedProgress).toFloat()

                    drawArc(
                        color = item.color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = 40f)
                    )
                    startAngle += sweepAngle
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Total Periodo",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
            Text(
                text = "$${"%,.2f".format(totalMes)}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
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

fun calcularMontoDeContratoEnMes(contrato: Contrato, mes: Int, anio: Int): Double {
    var totalContrato = 0.0
    try {
        val inicio = LocalDate.parse(contrato.fechaInicio)
        val fin = LocalDate.parse(contrato.fechaFin)
        var fecha = inicio

        while (!fecha.isAfter(fin)) {
            if (fecha.monthValue == mes && fecha.year == anio) {
                totalContrato += contrato.monto
            }
            fecha = when (contrato.frecuenciaPago) {
                "Semanal" -> fecha.plusWeeks(1)
                "Quincenal" -> fecha.plusDays(15)
                else -> fecha.plusMonths(1)
            }
        }
    } catch (_: Exception) {}
    return totalContrato
}

@Composable
fun ItemGraficaCard(
    nombre: String,
    porcentaje: Double,
    color: Color
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.35f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(color, shape = CircleShape)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = nombre,
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "${"%.1f".format(porcentaje)}%",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun SelectorMes(selectedMes: Int, onMesChange: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .background(
                    color = Color.White.copy(alpha = 0.12f), // Fondo translúcido suave
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable { expanded = true }
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = meses[selectedMes - 1],
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "▼",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color(0xFF1E293B))
        ) {
            meses.forEachIndexed { index, mes ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = mes,
                            color = Color.White,
                            fontWeight = if (index + 1 == selectedMes) FontWeight.Bold else FontWeight.Normal
                        )
                    },
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

data class ItemGrafica(
    val nombre: String,
    val montoGeneradoEnMes: Double,
    val color: Color
)