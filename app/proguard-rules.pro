# Apache POI + XMLBeans + OOXML schemas rely on reflection and resource lookups.
-keep class org.apache.poi.** { *; }
-keep class org.apache.xmlbeans.** { *; }
-keep class org.openxmlformats.schemas.** { *; }
-keep class com.microsoft.schemas.** { *; }
-keep class schemaorg_apache_xmlbeans.** { *; }
-keep class org.apache.commons.** { *; }
-keep class org.apache.logging.log4j.** { *; }
# Aalto StAX is loaded by class name through system properties.
-keep class com.fasterxml.aalto.** { *; }
-keep class org.codehaus.stax2.** { *; }
-keep class javax.xml.stream.** { *; }
# Desktop-only references POI never reaches on Android.
-dontwarn java.awt.**
-dontwarn javax.swing.**
-dontwarn javax.imageio.**
-dontwarn org.apache.batik.**
-dontwarn org.osgi.**
-dontwarn aQute.bnd.annotation.**
-dontwarn net.sf.saxon.**
-dontwarn org.bouncycastle.**
-dontwarn org.apache.logging.log4j.**
-dontwarn org.slf4j.**
-dontwarn com.github.luben.**
-dontwarn org.tukaani.**
-dontwarn org.brotli.**
-dontwarn org.w3c.dom.events.**
-dontwarn org.w3c.dom.ls.**
-dontwarn javax.xml.stream.**
-dontwarn org.apache.xmlbeans.**
# Room and Hilt ship their own consumer rules.
# Unused POI parts (Word/PowerPoint/Visio/signing/PDF) that don't exist on Android
-dontwarn com.microsoft.schemas.**
-dontwarn org.openxmlformats.schemas.**
-dontwarn de.rototor.**
-dontwarn javax.xml.crypto.**
-dontwarn org.apache.jcp.**
-dontwarn org.apache.pdfbox.**
-dontwarn org.apache.xml.security.**
-dontwarn org.ietf.jgss.**
-dontwarn org.objectweb.asm.**
-dontwarn org.w3c.dom.svg.**
-dontwarn org.w3c.dom.traversal.**
-ignorewarnings
