package com.example.psm_app_proyect

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

object ContratoStorage {

    private const val FILE_NAME = "contratos.json"

    fun guardar(context: Context, contratos: List<Contrato>) {
        val json = Gson().toJson(contratos)
        val file = File(context.filesDir, FILE_NAME)
        file.writeText(json)
    }

    fun cargar(context: Context): MutableList<Contrato> {
        val file = File(context.filesDir, FILE_NAME)

        if (!file.exists()) return mutableListOf()

        val json = file.readText()

        val type = object : TypeToken<MutableList<Contrato>>() {}.type
        return Gson().fromJson(json, type) ?: mutableListOf()
    }
}