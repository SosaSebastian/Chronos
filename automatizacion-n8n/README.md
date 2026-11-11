# Aviso de dosis omitida con n8n

Cuando suena el recordatorio de un medicamento y el paciente **no** toca "Marcar como tomada" en los siguientes 30 minutos, la app envía los datos a n8n y n8n le manda un correo al usuario registrado.

```
App (recordatorio) ──30 min──► ¿tomada?
                                 │ no
                                 ▼
          POST http://<pc>:5678/webhook/dosis-omitida
                                 │
                n8n: Webhook → ¿Datos válidos? → Enviar correo (SMTP) → Responder 200
                                         └─ no → Responder 400
```

## Archivos

| Archivo | Para qué |
| --- | --- |
| `HealthyChronos_DosisOmitida.json` | El flujo para importar en n8n |
| `ejemplo_dosis_omitida.json` | Un aviso de ejemplo para probar sin la app |

## 1. n8n en Docker

Si ya lo tienes corriendo, abre http://localhost:5678. Si no:

```bash
docker run -d --name n8n -p 5678:5678 -e GENERIC_TIMEZONE=America/Bogota -e TZ=America/Bogota -v n8n_data:/home/node/.n8n docker.n8n.io/n8nio/n8n
```

Lo importante es `-p 5678:5678`: sin eso, la app no llega al webhook.

## 2. Credencial de correo (Gmail)

1. En tu cuenta de Google activa la **verificación en 2 pasos**.
2. Entra a https://myaccount.google.com/apppasswords y crea una **contraseña de aplicación** (16 letras).
3. En n8n: **Credentials → Add credential → SMTP**, con estos datos:

| Campo | Valor |
| --- | --- |
| User | tu correo de Gmail |
| Password | la contraseña de aplicación (no tu contraseña normal) |
| Host | `smtp.gmail.com` |
| Port | `465` |
| SSL/TLS | activado |

4. Ponle de nombre **SMTP Healthy Chronos** y guarda.

## 3. Importar y activar el flujo

1. En n8n: **Workflows → Add workflow → ⋯ → Import from File** y elige `HealthyChronos_DosisOmitida.json`.
2. Abre el nodo **Enviar correo**:
   - En *Credential* elige **SMTP Healthy Chronos**.
   - En *From Email* cambia `TU_CORREO@gmail.com` por tu correo de Gmail.
3. Guarda y activa el flujo con el interruptor **Active** (arriba a la derecha).

Con el flujo activo, la URL de producción es `http://localhost:5678/webhook/dosis-omitida`.

## 4. Probar sin la app

Cambia `TU_CORREO@gmail.com` en `ejemplo_dosis_omitida.json` por el correo donde quieres recibirlo y, desde esta carpeta:

**PowerShell (Windows):**

```powershell
Invoke-RestMethod -Uri http://localhost:5678/webhook/dosis-omitida -Method Post -ContentType "application/json; charset=utf-8" -InFile ejemplo_dosis_omitida.json
```

**macOS / Linux / Git Bash:**

```bash
curl -X POST http://localhost:5678/webhook/dosis-omitida -H "Content-Type: application/json" --data-binary @ejemplo_dosis_omitida.json
```

Respuesta esperada: `{"ok": true, "enviadoA": "...", "medicamento": "Losartán"}` y llega un correo con el asunto **[PRUEBA] Dosis sin confirmar: Losartán de las 08:00 AM**.

Si envías un JSON sin `paciente.correo`, n8n responde **400** y no manda nada.

## 5. Conectar la app

La URL del webhook se define en el archivo `.env` de la raíz del proyecto:

```
N8N_WEBHOOK_URL=http://10.0.2.2:5678/webhook/dosis-omitida
```

- **Emulador de Android Studio:** deja `10.0.2.2`; así ve el emulador al computador.
- **Celular físico:** usa la IP del computador en el wifi (ej. `http://192.168.1.20:5678/...`), agrega esa IP en `app/src/main/res/xml/network_security_config.xml` y permite el puerto 5678 en el firewall de Windows.

Después de cambiar `.env`, haz **Build → Rebuild Project**.

## 6. Demo en vivo (1 minuto)

1. Inicia sesión en la app con una cuenta cuyo correo puedas abrir.
2. Ve a **Mi Perfil → Probar Alarma en 3s**.
3. Cuando llegue la notificación, **no** toques "Marcar como tomada".
4. En 1 minuto llega el correo con **[PRUEBA]** en el asunto. En n8n, la pestaña **Executions** muestra la ejecución.

Con los medicamentos reales la espera es de 30 minutos (`MINUTOS_ESPERA` en `DosisOmitidaNotifier.kt`).

## Si no llega el correo

| Síntoma | Qué revisar |
| --- | --- |
| No aparece nada en Executions | El flujo no está activo, la URL de `.env` está mal o falta `-p 5678:5678` en Docker |
| Logcat (filtro `DosisOmitida`) dice "Cleartext HTTP traffic not permitted" | Falta la IP en `network_security_config.xml` |
| Logcat dice "No hay sesión iniciada con correo" | Inicia sesión en la app antes de la prueba |
| La ejecución falla en "Enviar correo" | Contraseña de aplicación de Gmail o puerto 465 con SSL |
| El correo llega a spam | Márcalo como "No es spam" una vez |
