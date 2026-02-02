package com.hcmus.forumus_client.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.hcmus.forumus_client.data.model.Report
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ReportRepository (
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    fun generateReportId(): String {
        val currentDate = Calendar.getInstance()

        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val formattedDate = dateFormat.format(currentDate.time)

        val randomPart = (1000..9999).random()

        return "REPORT" + "_" + "$formattedDate" + "_" + "$randomPart"
    }

    suspend fun saveReport(report: Report) {
        val user = auth.currentUser ?: throw IllegalStateException("User not logged in")

        val generatedId = generateReportId()

        val reportRef = firestore.collection("reports")
            .document(generatedId)

        val updatedReport = report.copy(
            id = generatedId,
            authorId = user.uid,
        )

        reportRef.set(updatedReport).await()
    }
}