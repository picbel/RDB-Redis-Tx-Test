package com.picbel.distributedtx.redis

import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
data class Session(
    val sessionId: String,
    val userId: Long,
    val createdAt: Long
)