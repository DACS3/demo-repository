package com.example.eduqizpro

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

class EduQizApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            FirebaseApp.initializeApp(this)
            setupSystemProperties()
            Log.d("EduQizApplication", "Initialization successful")
        } catch (t: Throwable) {
            Log.e("EduQizApplication", "Initialization failed", t)
        }
    }

    private fun setupSystemProperties() {
        try {
            // Critical XML parsing fixes for Android to avoid library conflicts
            System.setProperty("org.apache.xmlbeans.impl.store.Locale.SaxLoader.canSetProperty", "false")
            System.setProperty("org.apache.xmlbeans.impl.store.Locale.SaxLoader.canSetLexicalHandler", "false")
            
            // Fixed factory discovery for Apache POI on Android
            val factory = "com.ctc.wstx.stax.WstxInputFactory"
            System.setProperty("javax.xml.stream.XMLInputFactory", factory)
            System.setProperty("org.apache.poi.javax.xml.stream.XMLInputFactory", factory)
            
            // Thread context classloader fix for ServiceLoader
            Thread.currentThread().contextClassLoader = EduQizApplication::class.java.classLoader
        } catch (t: Throwable) {
            Log.e("EduQizApplication", "Failed to set system properties", t)
        }
    }
}
