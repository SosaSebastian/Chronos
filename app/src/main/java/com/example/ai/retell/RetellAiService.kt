package com.example.ai.retell

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Cliente Retrofit para la API REST de Retell AI (Voz Externa y Chat Directo con el Agente).
 * Permite tanto llamadas de voz WebRTC como comunicación de chat directamente con el agente configurado.
 */
interface RetellAiService {

    @POST("v2/create-web-call")
    suspend fun createWebCall(
        @Header("Authorization") authorization: String,
        @Body request: CreateWebCallRequest
    ): Response<CreateWebCallResponse>

    @GET("v2/get-agent/{agent_id}")
    suspend fun getAgent(
        @Header("Authorization") authorization: String,
        @Path("agent_id") agentId: String
    ): Response<RetellAgentResponse>

    @POST("create-chat")
    suspend fun createChat(
        @Header("Authorization") authorization: String,
        @Body request: CreateChatRequest
    ): Response<CreateChatResponse>

    @POST("create-chat-completion")
    suspend fun createChatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: CreateChatCompletionRequest
    ): Response<CreateChatCompletionResponse>

    @GET("v2/get-call/{call_id}")
    suspend fun getCall(
        @Header("Authorization") authorization: String,
        @Path("call_id") callId: String
    ): Response<GetCallResponse>
}
