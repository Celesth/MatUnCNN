package com.matuncnn.app

import android.app.Application
import android.content.Context
import java.io.File

class MatUnCnnApp : Application() {

    lateinit var workDir: String
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        workDir = filesDir.absolutePath + File.separator + "realsr"
    }

    fun ensureWorkDir() {
        val dir = File(workDir)
        if (!dir.exists()) dir.mkdirs()
    }

    companion object {
        lateinit var instance: MatUnCnnApp
            private set

        fun getContext(): Context = instance.applicationContext
    }
}
