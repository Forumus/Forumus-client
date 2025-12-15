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
        // Lấy thời gian hiện tại
        val currentDate = Calendar.getInstance()

        // Định dạng thời gian theo yêu cầu (yyyyMMdd_HHmmss)
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val formattedDate = dateFormat.format(currentDate.time)

        // Tạo số ngẫu nhiên từ 1000 đến 9999
        val randomPart = (1000..9999).random()

        // Kết hợp lại thành ID với định dạng "POST_yyyyMMdd_HHmmss_random"
        return "REPORT" + "_" + "$formattedDate" + "_" + "$randomPart"
    }

    // Save report to Firebase
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