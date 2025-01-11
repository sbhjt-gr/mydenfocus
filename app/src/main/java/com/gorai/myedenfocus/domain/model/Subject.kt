package com.gorai.myedenfocus.domain.model

import com.gorai.myedenfocus.presentation.theme.gradient1
import com.gorai.myedenfocus.presentation.theme.gradient2
import com.gorai.myedenfocus.presentation.theme.gradient3
import com.gorai.myedenfocus.presentation.theme.gradient4
import com.gorai.myedenfocus.presentation.theme.gradient5
import androidx.compose.ui.graphics.Color
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Subject(
    val name: String,
    val goalHours: Float,
    val colors: List<Int>,
    @PrimaryKey(autoGenerate = true)
    val subjectId: Int? = null
) {
    companion object {
        val subjectCardColors = listOf(
            // Original colors
            listOf(Color(0xFF6B4DE6), Color(0xFF9900F0)),
            listOf(Color(0xFFFF5F6D), Color(0xFFFFC371)),
            listOf(Color(0xFF11998E), Color(0xFF38EF7D)),
            listOf(Color(0xFF536976), Color(0xFF292E49)),
            listOf(Color(0xFFFF4B2B), Color(0xFFFF416C)),
            
            // New modern gradient combinations
            listOf(Color(0xFF4158D0), Color(0xFFC850C0)),  // Purple dream
            listOf(Color(0xFF0093E9), Color(0xFF80D0C7)),  // Ocean breeze
            listOf(Color(0xFFFF3CAC), Color(0xFF784BA0)),  // Pink sunset
            listOf(Color(0xFF00B4DB), Color(0xFF0083B0)),  // Blue lagoon
            listOf(Color(0xFFFBB034), Color(0xFFFF0099)),  // Citrus punch
            listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0)),  // Deep violet
            listOf(Color(0xFF56CCF2), Color(0xFF2F80ED)),  // Sky blue
            listOf(Color(0xFFFF6B6B), Color(0xFF556270)),  // Coral gray
            listOf(Color(0xFF20BF55), Color(0xFF01BAEF)),  // Mint splash
            listOf(Color(0xFFFF9966), Color(0xFFFF5E62))   // Peach sunset
        )
    }
}