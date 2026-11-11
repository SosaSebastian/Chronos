package com.example.util

/**
 * Se lanza cuando la dosis de un medicamento no es válida (vacía, sin cantidad, cero o negativa).
 * Caso de prueba CP01 del plan de pruebas.
 */
class DosisInvalidaException(mensaje: String) : IllegalArgumentException(mensaje)

/**
 * Valida el texto de la dosis que escribe el usuario, por ejemplo "50 mg", "0,5 mg" o "600 mg / 400 UI".
 * Toma la primera cantidad numérica del texto y exige que sea mayor que cero.
 */
object ValidadorDosis {

    private val primeraCantidad = Regex("""-?\s*\d+(?:[.,]\d+)?""")

    /**
     * @return la cantidad numérica de la dosis.
     * @throws DosisInvalidaException si la dosis está vacía, no tiene cantidad o es cero/negativa.
     */
    @Throws(DosisInvalidaException::class)
    fun validar(dosis: String): Double {
        val texto = dosis.trim()
        if (texto.isBlank()) {
            throw DosisInvalidaException("La dosis o concentración es obligatoria (*).")
        }
        val cantidad = primeraCantidad.find(texto)?.value
            ?.replace(" ", "")
            ?.replace(',', '.')
            ?.toDoubleOrNull()
            ?: throw DosisInvalidaException("La dosis debe incluir una cantidad, por ejemplo 50 mg (*).")
        if (cantidad <= 0.0) {
            throw DosisInvalidaException("La dosis debe ser mayor que cero (*).")
        }
        return cantidad
    }
}
