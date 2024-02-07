package com.femi.joketeller

data class Joke(
    val category: String?,
    val delivery: String?,
    val error: Boolean?,
    val flags: Flags?,
    val id: Int?,
    val lang: String?,
    val safe: Boolean?,
    val setup: String?,
    val joke: String?,
    val type: String?
)