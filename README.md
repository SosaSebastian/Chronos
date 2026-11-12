# Healthy Chronos 💊

**Sistema de gestión de medicamentos y recordatorios para adultos mayores y pacientes con tratamientos crónicos.**

Aplicación nativa para Android desarrollada en Kotlin con Jetpack Compose, Firebase, un asistente virtual de texto y voz con inteligencia artificial (Gemini) y una automatización en n8n que avisa por correo cuando una dosis queda sin tomar.

Proyecto integrador — **Optativa II: Desarrollo de Aplicaciones para Dispositivos Móviles**
Universidad de San Buenaventura, Bogotá · Prof. Jairo A. Salcedo Aranda

| Integrantes |
|---|
| Samuel Barato Benite |
| Juan Sebastián Sosa Martínez |

---

## Tabla de contenido

1. [Problema y alcance](#1-problema-y-alcance)
2. [Requisitos](#2-requisitos)
3. [Tecnologías](#3-tecnologías)
4. [Arquitectura](#4-arquitectura)
5. [Estructura del proyecto](#5-estructura-del-proyecto)
6. [Instalación y ejecución](#6-instalación-y-ejecución)
7. [Base de datos y seguridad](#7-base-de-datos-y-seguridad)
8. [Agente de inteligencia artificial](#8-agente-de-inteligencia-artificial)
9. [Automatización de dosis omitida (n8n)](#9-automatización-de-dosis-omitida-n8n)
10. [Pruebas](#10-pruebas)
11. [Manual de usuario](#11-manual-de-usuario)
12. [Dificultades y mejoras futuras](#12-dificultades-y-mejoras-futuras)

---

## 1. Problema y alcance

La adherencia a tratamientos crónicos ronda el 50 % (OMS). Los pacientes, en especial adultos mayores con varios medicamentos, olvidan dosis, no tienen visibilidad de su historial y sus cuidadores no se enteran cuando una toma se omite.

**Healthy Chronos** permite:

- Registrar medicamentos con dosis, hora y categoría (por formulario o por voz).
- Recibir alarmas a la hora exacta de cada toma, con acciones directas desde la notificación.
- Consultar un calendario y un historial de cumplimiento con porcentaje de adherencia.
- Exportar un reporte PDF para el médico tratante.
- Hacer preguntas por texto o por voz a un asistente IA que responde con base en los medicamentos reales del paciente.
- Recibir un correo cuando una dosis no se confirma a tiempo (automatización con n8n).

**Fuera del alcance de esta versión:** versión iOS (se plantea con Kotlin Multiplatform), vinculación de cuidadores dentro de la app y registro de citas médicas.

## 2. Requisitos

### Funcionales

| ID | Requisito |
|---|---|
| RF01 | Registro de paciente con datos personales, médico tratante y contacto de emergencia. |
| RF02 | Inicio y cierre de sesión con correo y contraseña. |
| RF03 | Registrar un medicamento (nombre, dosis, hora, categoría, indicaciones). |
| RF04 | Listar y filtrar los medicamentos por categoría y estado de toma. |
| RF05 | Editar, activar / desactivar y eliminar medicamentos. |
| RF06 | Programar alarmas por cada toma y reprogramarlas tras reiniciar el teléfono. |
| RF07 | Marcar una dosis como tomada o posponerla desde la notificación. |
| RF08 | Ver calendario e historial de cumplimiento con % de adherencia. |
| RF09 | Generar y compartir un reporte PDF de cumplimiento. |
| RF10 | Asistente IA de texto y voz para consultas sobre el tratamiento (escucha la pregunta y lee la respuesta en voz alta). |
| RF11 | Dictado por voz del nombre y las indicaciones al registrar un medicamento. |
| RF12 | Aviso por correo al usuario registrado cuando una dosis no se confirma en 30 minutos. |

### No funcionales

| ID | Requisito |
|---|---|
| RNF01 | Interfaz accesible para adultos mayores: textos grandes, alto contraste, botones amplios. |
| RNF02 | Cada usuario solo puede leer y escribir sus propios datos (reglas de Firestore). |
| RNF03 | La app no se cierra ante errores de red, de IA o de datos inválidos. |
| RNF04 | Las claves de API no se suben al repositorio. |
| RNF05 | Compatible con Android 7.0 (API 24) en adelante. |

## 3. Tecnologías

| Componente | Tecnología |
|---|---|
| Lenguaje | Kotlin 2.2 |
| IDE | Android Studio |
| Interfaz | Jetpack Compose + Material Design 3 |
| Arquitectura | MVVM (ViewModel + StateFlow) |
| Autenticación | Firebase Authentication (correo/contraseña) |
| Base de datos remota | Cloud Firestore |
| Base de datos local | Room (SQLite) |
| Asincronía | Kotlin Coroutines y Flow |
| Recordatorios | AlarmManager + BroadcastReceiver + notificaciones |
| Inteligencia artificial | Firebase AI Logic → Gemini (`gemini-3.8-flash`) |
| Seguridad | Firebase App Check, reglas de Firestore, Secrets Gradle Plugin |
| Reportes | `PdfDocument` de Android + FileProvider |
| Automatización | n8n (webhook → validación → correo SMTP) |
| Pruebas | JUnit 4, Robolectric, Compose Testing, Roborazzi, Firebase Rules Unit Testing |

## 4. Arquitectura

```
┌─────────────────────────────────────────────────────────┐
│  UI — Jetpack Compose (ui/screens)                       │
│  Login · Registro · Dashboard · Calendario · Historial   │
│  Registrar medicamento · Perfil · Asistente IA           │
└──────────────────────────┬──────────────────────────────┘
                           │ observa StateFlow / envía eventos
┌──────────────────────────▼──────────────────────────────┐
│  ViewModels (ui/viewmodel)                               │
│  MedicationViewModel · PacienteViewModel ·               │
│  RegistrarMedicamentoViewModel · CalendarViewModel ·     │
│  HistorialCumplimientoViewModel · DashboardMedicamentos… │
└──────────────────────────┬──────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────┐
│  Repositorios (data/repository) + Managers               │
│  FirebaseMedicationRepository · MedicationRepository ·   │
│  FirebaseAuthManager · AsistenteMedicacionManager        │
└───────┬──────────────────┬──────────────────┬───────────┘
        │                  │                  │
   Firestore / Auth     Room (local)     Gemini (Firebase AI)
```

- **Decisión de diseño:** MVVM separa la interfaz de la lógica y de las fuentes de datos, lo que facilita las pruebas y el mantenimiento.
- **Estados de la interfaz:** se modelan con clases selladas (`Idle`, `Loading`, `Success`, `Error`) y el tipo `Resource<T>`, así un error nunca deja la pantalla en un estado nulo.
- **Navegación:** controlada por el enum `AppScreen` en `MedicationViewModel` con transiciones animadas en `MainActivity`.

## 5. Estructura del proyecto

```
app/src/main/java/com/example/
├── MainActivity.kt                 # Punto de entrada y navegación
├── ai/
│   ├── AsistenteMedicacionManager.kt   # Asistente (Gemini + filtros + respaldo local), para texto y voz
│   └── voz/                        # Asistente de voz: ReconocedorVoz (escuchar), LectorVoz (hablar), VozUtils (reglas)
├── auth/
│   └── FirebaseAuthManager.kt      # Login, registro y cierre de sesión
├── data/
│   ├── local/                      # Room: entidad, DAO y base de datos
│   ├── model/                      # Modelos de Firestore, historial y Resource<T>
│   └── repository/                 # Acceso a Firestore y Room
├── notifications/                  # Alarmas, acciones de notificación y aviso de dosis omitida (n8n)
├── ui/
│   ├── calendar/                   # Modelos del calendario
│   ├── screens/                    # Pantallas en Compose
│   ├── theme/                      # Colores, tipografía y tema
│   └── viewmodel/                  # ViewModels
└── util/                           # PDF, dictado por voz, validación de dosis y App Check
app/src/test/java/com/example/      # Pruebas unitarias y de interfaz (Robolectric)
automatizacion-n8n/                 # Flujo de n8n para el correo de dosis omitida + guía
pruebas-firestore/                  # Pruebas de las reglas de seguridad de Firestore
docs/                               # Manual de usuario y mockups en PDF
firestore.rules                     # Reglas de seguridad de Firestore
```

## 6. Instalación y ejecución

### Requisitos previos

- Android Studio (versión reciente, compatible con AGP 9.1)
- JDK 17 o superior
- Un emulador o teléfono con Android 7.0 (API 24) o superior
- Un proyecto de Firebase propio

### Pasos

1. **Clonar el repositorio**

   ```bash
   git clone https://github.com/SosaSebastian/Chronos.git
   ```

2. **Configurar Firebase**
   - En la consola de Firebase, crear un proyecto y agregar una app Android con el paquete `com.chronos.app`.
   - Descargar `google-services.json` y copiarlo en la carpeta `app/`. Este archivo no se sube al repositorio.
   - Activar **Authentication → Correo/contraseña**.
   - Crear la base de datos **Cloud Firestore** y publicar las reglas del archivo `firestore.rules`.
   - Activar **Firebase AI Logic** (Gemini Developer API) para el asistente.

3. **Variables de entorno**
   Copiar `.env.example` como `.env` en la raíz del proyecto y poner la clave real:

   ```
   GEMINI_API_KEY=tu_clave
   N8N_WEBHOOK_URL=http://10.0.2.2:5678/webhook/dosis-omitida
   ```

   `10.0.2.2` es el computador visto desde el emulador. En un celular físico se usa la IP del computador.

4. **Keystore de depuración (opcional)**
   Si existe `debug.keystore` en la raíz del proyecto, el build de debug firma con él; si no, Android Studio usa su llave de depuración por defecto. Para generarlo:

   ```bash
   keytool -genkey -v -keystore debug.keystore -storepass android \
     -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 \
     -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
   ```

5. **Abrir y ejecutar**
   Abrir la carpeta en Android Studio, esperar la sincronización de Gradle y presionar **Run ▶** con el emulador o teléfono seleccionado.

6. **Firebase App Check (si está en modo obligatorio)**
   Al ejecutar en debug, buscar `debug secret` en Logcat y registrar ese token en la consola de Firebase: *App Check → Apps → Administrar tokens de depuración*.

7. **Correo de dosis omitida (opcional)**
   Seguir la guía de `automatizacion-n8n/README.md` para levantar n8n en Docker, configurar el correo e importar el flujo.

## 7. Base de datos y seguridad

### Modelo de datos en Firestore

```
users/{uid}
  ├── uid, email, nombre, edad, tipoSangre,
  │   medicoNombre, contactoNombre, contactoTelefono
  └── medicamentos/{medicamentoId}
        └── nombre, dosis, horaToma, categoria, indicaciones, activo
```

### Operaciones implementadas

| Operación | Método |
|---|---|
| Crear usuario y perfil | `registerUserAndCreateProfile` |
| Consultar perfil | `getPacienteProfile` |
| Crear medicamento | `addMedicamento` |
| Consultar medicamentos | `getActiveMedicamentos`, `observeActiveMedicamentos` (tiempo real) |
| Actualizar medicamento | `updateMedicamento` (nombre, dosis, hora, categoría e indicaciones) |
| Actualizar estado | `toggleMedicamentoStatus` |
| Eliminar medicamento | `deleteMedicamento` |

### Protección de credenciales y datos

- **Reglas de Firestore:** solo el usuario autenticado dueño del `uid` puede leer o modificar su documento y sus medicamentos. Además se valida que los campos obligatorios existan y tengan el tipo correcto.
- **Claves fuera del código:** `.env`, `google-services.json`, `local.properties` y los keystores están en `.gitignore`.
- **Firebase App Check:** verifica que las peticiones vengan de la app real.
- **Contraseñas:** las gestiona Firebase Authentication; la app nunca las guarda.

## 8. Agente de inteligencia artificial

**Propósito:** asistente de texto y voz experto y empático para el seguimiento de los medicamentos del usuario. Responde sobre los medicamentos registrados, da recomendaciones generales y seguras de uso (por ejemplo, con alimentos o precauciones con el sol) y ayuda a organizar los horarios de las tomas. No reemplaza al médico.

**Funcionamiento** (`AsistenteMedicacionManager.kt`):

1. El usuario escribe su pregunta en la pantalla **Asistente IA** (máximo 600 caracteres) o la dice con el micrófono.
2. **Filtro de emergencias local:** si describe síntomas graves (dolor en el pecho, falta de aire, reacción alérgica severa, desmayo, sobredosis), responde de inmediato con la línea de emergencias 123 y el contacto de emergencia, sin esperar a la IA.
3. **Filtro de alcance local:** temas ajenos a la salud reciben siempre la frase definida para fuera de alcance.
4. **Gemini (`gemini-3.8-flash`) con instrucciones de sistema:** recibe el contexto del asistente, las reglas, la fecha y hora de Colombia, los medicamentos registrados (RAG) y los últimos 6 mensajes de la conversación. La pregunta va separada de las instrucciones, para que el usuario no pueda cambiar las reglas.
5. **Motor local de respaldo:** si Gemini falla, no hay internet o tarda más de 35 s, responde con los datos registrados (qué falta por tomar, la próxima toma, dosis e indicaciones).
6. **Aviso médico obligatorio:** va en el primer mensaje y lo agrega el código a cada recomendación sobre un medicamento o un síntoma.

**Asistente de voz** (`ai/voz/`): la voz no es otra IA, es otra forma de hablar con el mismo asistente.

1. **Escuchar** (`ReconocedorVoz`): reconocedor de voz de Android en español de Colombia; muestra en pantalla lo que el paciente va diciendo.
2. **Pensar:** el texto reconocido pasa por los mismos pasos 2 a 6 de arriba (emergencias, alcance, Gemini, respaldo y aviso médico).
3. **Hablar** (`LectorVoz`): lee la respuesta con la voz de Android, sin etiquetas ni emojis y con las unidades completas ("50 miligramos"). Entre las voces del celular escoge la de acento más cercano al colombiano (Colombia → latino de EE. UU. → México → España) y de mejor calidad, incluidas las voces mejoradas de Google.
4. **Modo conversación** (opcional): después de responder vuelve a escuchar solo. Se detiene ante una alerta de emergencia, si no entiende o al salir de la pantalla.
5. **Fallos controlados:** sin permiso de micrófono, sin conexión, sin reconocedor o si no entendió, muestra un mensaje claro y el paciente puede escribir. No usa servicios pagos ni claves adicionales.

**Controles de seguridad del agente:**

| Escenario | Respuesta del agente |
|---|---|
| Síntomas de emergencia | Alerta roja, línea 123 y contacto de emergencia; no espera a la IA |
| Pregunta fuera del alcance | "Soy un asistente diseñado exclusivamente para el seguimiento de tus medicamentos y temas de salud. No puedo responder consultas sobre otras temáticas." |
| Medicamento que no está en la lista | "No tengo registro de ese medicamento en tu plan actual de tratamiento." |
| Citas médicas | Indica que la app no registra citas; no inventa fechas |
| Recomendación sobre un medicamento o síntoma | Respuesta + aviso médico obligatorio |
| Error del servicio o sin conexión | Motor de respaldo local con los datos registrados |

## 9. Automatización de dosis omitida (n8n)

Cuando suena un recordatorio, la app programa una verificación 30 minutos después (1 minuto para la alarma de prueba de *Mi Perfil*). Si la dosis sigue sin confirmarse, envía un JSON al webhook de n8n, que valida los datos y manda un correo al usuario registrado. El flujo importable y la guía de instalación están en [`automatizacion-n8n/`](automatizacion-n8n/README.md).

## 10. Pruebas

Ejecutar desde Android Studio (clic derecho en `app/src/test` → *Run Tests*) o con:

```bash
./gradlew testDebugUnitTest
```

El reporte queda en `app/build/reports/tests/testDebugUnitTest/index.html`.

| Archivo | Pruebas | Qué cubre |
|---|---|---|
| `ExampleRobolectricTest.kt` | 20 | Modelos, notificaciones, calendario (con días de la semana), adherencia, alarmas y agente IA |
| `PruebasFuncionamientoTest.kt` | 8 | Persistencia en Room (PF01–PF04) y casos del plan CP01, CP02, CP03 y CP05 |
| `FormulariosYBotonesTest.kt` | 8 | Botones y formularios de la pantalla de inicio de sesión (Compose Testing) |
| `ValidacionFormulariosTest.kt` | 12 | Validación de los formularios de paciente y de medicamento, y modo edición |
| `DosisOmitidaTest.kt` | 3 | Aviso de dosis omitida hacia n8n (CP07) |
| `AsistenteIATest.kt` | 16 | Alcance, aviso médico, emergencias, respaldo sin internet e historial del asistente |
| `VozAsistenteTest.kt` | 11 | Asistente de voz: limpieza del texto, errores, modo conversación, elección de la voz y pantalla (VZ01–VZ11) |
| `pruebas-firestore/` | 9 | Reglas de seguridad de Firestore (requiere Node 18+ y Java 11+: `npm install` y `npm run test:rules`) |

**Datos de prueba sugeridos para la demostración:**

- Paciente: María Gómez, 72 años, tipo de sangre O+
- Medicamento: Losartán 50 mg · 08:00 · Cardiovascular
- Medicamento: Metformina 850 mg · 13:00 · Diabetes

## 11. Manual de usuario

El manual completo en PDF está en [`docs/Manual_Usuario_HealthyChronos.pdf`](docs/Manual_Usuario_HealthyChronos.pdf) y los mockups en [`docs/Mockups_HealthyChronos.pdf`](docs/Mockups_HealthyChronos.pdf).

### Crear una cuenta
1. Abrir la app y tocar **Crear Cuenta**.
2. Llenar correo, contraseña, nombre, edad, tipo de sangre, médico tratante y contacto de emergencia. Los campos con (*) son obligatorios.
3. Tocar **Crear Cuenta y Guardar Perfil**. La app entra directamente al panel principal.

### Iniciar sesión
Escribir correo y contraseña y tocar **Iniciar Sesión**. Si los datos son incorrectos, la app muestra el error en pantalla.

### Agregar un medicamento
1. En el panel principal tocar el botón **+**.
2. Escribir el nombre, la dosis, la hora y elegir la categoría. También se puede usar el botón del **micrófono** para dictar.
3. Tocar **Guardar Medicamento**. La alarma queda programada automáticamente.

### Recibir y responder recordatorios
A la hora de la toma llega una notificación con dos botones:
- **Marcar como tomada:** registra la dosis en el historial.
- **Posponer 10 min:** vuelve a avisar 10 minutos después.

> La primera vez, Android pide permiso para mostrar notificaciones y alarmas exactas. Hay que aceptarlos para que los recordatorios funcionen.

### Ver el calendario y el historial
- **Calendario:** muestra las tomas por día y su estado.
- **Cumplimiento:** muestra el porcentaje de adherencia y permite **Exportar y Compartir PDF** para compartirlo con el médico.

### Editar o eliminar un medicamento
En el panel principal, cada tarjeta tiene dos íconos:
- **Lápiz (Editar medicamento):** abre el formulario con los datos actuales. Se cambian y se toca **Guardar Cambios**; el recordatorio se ajusta a la hora nueva.
- **Papelera (Eliminar medicamento):** pide confirmación y lo borra de la app y de Firestore.

### Consultar al asistente IA (por texto o por voz)
1. Tocar **Asistente IA** en el panel principal.
2. **Por texto:** escribir la pregunta y tocar Enviar. Ejemplos: *"¿Qué me falta por tomar hoy?"*, *"¿Puedo tomar el Losartán con café?"*
3. **Por voz:** con el campo vacío, tocar el **micrófono** (la primera vez pide permiso), hablar y tocar **Ya terminé** (o esperar a terminar de hablar). La respuesta se lee en voz alta.
4. Arriba están el **parlante** (activa o silencia las respuestas en voz alta) y el **modo conversación** (después de responder vuelve a escuchar solo). Cada respuesta tiene un botón **Escuchar**.
5. El asistente responde con base en los medicamentos registrados. **No reemplaza la consulta médica.**

En el emulador, para usar el micrófono del computador: menú **⋮ → Extended Controls → Microphone → "Virtual microphone uses host audio input"**.

### Perfil y ajustes
En **Mi Perfil** se ven los datos del paciente, el botón para **llamar al contacto de emergencia**, opciones de sonido y alto contraste, una prueba de alarma y el botón **Cerrar Sesión**.

## 12. Dificultades y mejoras futuras

**Dificultades**
- Que las alarmas exactas sigan funcionando con las restricciones de batería de Android y después de reiniciar el teléfono.
- Mantener las respuestas del agente IA seguras y ligadas a los datos del paciente, sin inventar información médica.
- Configurar Firebase (Auth, Firestore, App Check y AI Logic) de forma segura.

**Mejoras futuras**
- Vinculación de cuidadores, para que el aviso de dosis omitida también les llegue a ellos.
- Registro de citas médicas para que el asistente también las organice.
- Pruebas instrumentadas de interfaz con Compose Testing.
- Versión para iOS con Kotlin Multiplatform.
- Mover todos los textos a `strings.xml` para soportar varios idiomas.
