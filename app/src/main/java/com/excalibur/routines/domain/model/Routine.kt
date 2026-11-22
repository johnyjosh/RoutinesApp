package com.excalibur.routines.domain.model

import java.util.UUID

data class Routine(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val icon: String = "🦊",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
