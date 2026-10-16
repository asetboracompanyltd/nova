package com.example.excelmanager

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ExcelApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Apache POI's OOXML reader needs a StAX implementation, which Android lacks.
        // Pointing POI at Aalto (bundled in the APK) makes .xlsx parsing work on-device.
        System.setProperty("org.apache.poi.javax.xml.stream.XMLInputFactory", "com.fasterxml.aalto.stax.InputFactoryImpl")
        System.setProperty("org.apache.poi.javax.xml.stream.XMLOutputFactory", "com.fasterxml.aalto.stax.OutputFactoryImpl")
        System.setProperty("org.apache.poi.javax.xml.stream.XMLEventFactory", "com.fasterxml.aalto.stax.EventFactoryImpl")
    }
}
