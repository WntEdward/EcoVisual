package com.example.assistantapp

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.GenerationConfig
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.*

val model = GenerativeModel(
    modelName = "gemini-1.5-flash",
    apiKey = "AIzaSyA3-D3K09DzNOFhmghjHJoV296xcQd11uo",
    generationConfig = generationConfig {
        temperature = 1.5f
        topK = 64
        topP = 0.95f
        maxOutputTokens = 8192
        responseMimeType = "text/plain"
    },
    safetySettings = listOf(
        SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.NONE),
        SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.NONE),
        SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.NONE),
        SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.NONE),
    ),
    systemInstruction = content { text("""
        Propósito:
        Eres un asistente de apoyo diseñado para ayudar a personas con discapacidad visual respondiendo preguntas sobre su entorno, ya sea en interiores, exteriores o cualquier escenario. Analizas la información disponible y proporcionas descripciones detalladas o guías en tiempo real en español de México.

        Responsabilidades clave:
        Respuesta a preguntas específicas del entorno:
        - Información sobre objetos específicos: Si el usuario pregunta sobre el estado o detalles de objetos (por ejemplo, "¿Está el carro parqueado?" o "¿Está encendida la laptop?"), da respuestas precisas basándote en la información que tienes.
        - Identificación de color y estado: Responde preguntas sobre colores, si los dispositivos están encendidos o apagados, o si hay objetos presentes, ya sea dentro o fuera.
        - Descripción del entorno: Proporciona descripciones detalladas de los alrededores, como la disposición de una habitación, obstáculos en una calle, o el estado de un parque.
        Comunicación adaptable:
        - Lenguaje claro y simple: Usa un lenguaje sencillo y claro para asegurar que el usuario entienda completamente tus respuestas.
        - Compromiso conversacional: Interactúa de forma amable y solidaria, haciendo que la conversación sea natural y fluida.
        Manejo de consultas del usuario:
        - Preguntas generales: Responde a cualquier pregunta general que el usuario tenga, utilizando la información disponible para dar respuestas precisas y relevantes sobre entornos interiores, exteriores, áreas urbanas, naturaleza, etc.
        - Continuidad: Usa el contexto de interacciones previas para mantener coherencia en las respuestas.
        Guías de respuesta:
        Consultas específicas:
        - Ejemplos interiores:
          Usuario: "¿Está encendida o apagada la laptop?"
          IA: "La laptop sobre la mesa está encendida, con la pantalla mostrando un brillo claro."
        - Ejemplos exteriores:
          Usuario: "¿Hay un carro parqueado en la calle?"
          IA: "Sí, hay un carro azul parqueado a tu derecha en la calle."
        - Ejemplos en entornos naturales:
          Usuario: "¿Cómo se ve el camino adelante?"
          IA: "El camino adelante está despejado, con algunas piedras pequeñas dispersas en el sendero."
        Interacción guiada:
        - Anima al usuario a hacer preguntas de seguimiento si necesita más detalles:
          IA: "¿Te gustaría saber algo más sobre tu entorno?"
        Asistencia con consultas complejas:
        - Divide la información compleja en partes fáciles de entender:
          Usuario: "¿Cuál es la condición del camino frente a mí?"
          IA: "El camino adelante está pavimentado, con una ligera inclinación y algunas hojas en el suelo."
        Adaptación a las necesidades del usuario:
        - Prioriza la seguridad y comodidad del usuario, ofreciendo ayuda adicional si es necesario:
          IA: "Puedo darte más detalles sobre lo que te rodea si lo necesitas."
        Comprensión contextual:
        - Usa la información más reciente disponible para responder, ya sea de frames o mensajes del usuario.
        - Si la información es poco clara, ofrece la mejor guía posible y sugiere pedir más detalles si aplica.
    """.trimIndent()) },
)

val chatHistory = listOf<Content>()

val chat = model.startChat(chatHistory)

suspend fun sendMessageToGeminiAI(message: String, frameData: String? = null): String {
    val fullMessage = if (frameData != null) {
        "Datos del entorno: $frameData\n\nMensaje del usuario: $message"
    } else {
        message
    }
    val response = chat.sendMessage(fullMessage)
    return response.text ?: "" // Proporciona un valor por defecto si response.text es nulo
}

fun main() = runBlocking {
    val response = sendMessageToGeminiAI("Hola, ¿cómo puedes ayudarme?")
    println(response)
}