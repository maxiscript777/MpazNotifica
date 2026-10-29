# MPAZ Notifica

App Android para que Inspectoría General y los profesores jefes de la Escuela Escritora Marcela Paz envíen comunicados a los apoderados y queden registrados. El envío por correo, SMS y WhatsApp es simulado y todos los datos son ficticios.

DSY1105, sección 007, Evaluación Parcial 2.

Integrante: Maximiliano Alexander Améstica Piña
GitHub: (pegar enlace)

## Cómo ejecutar

1. Abrir en Android Studio la carpeta que tiene el archivo `settings.gradle.kts`.
2. Esperar que termine el Sync de Gradle (necesita internet la primera vez).
3. Correr la app en un emulador o teléfono con Android 7.0 o superior.

Si Android Studio pide elegir el JDK, usar el 21.

## Usuarios de prueba

- Inspectoría General: inspectoria@mpaznotifica.cl / inspectoria123
- Profesor Jefe 4° Básico A: profesor.4basicoA@mpaznotifica.cl / profesor123

## Qué hace la app

- Inicio de sesión por perfil (RF-01) y opciones de envío según el perfil (RF-02).
- Redactar un comunicado con asunto, cuerpo, tipo y PDF opcional (RF-03).
- Elegir destinatarios: apoderado, curso, taller o colegio completo (RF-04 a RF-06).
- Enviar el comunicado con un envío simulado (RF-07).
- Guardar si cada envío salió despachado o fallido, por apoderado (RF-08).
- Historial de comunicados con filtros por tipo, emisor y fecha, y detalle de cada uno (RF-09 a RF-11).
- Autorización con firma física: solo avisa, la firma es en papel (RF-12).
- Cambiar el canal preferente de un apoderado, solo Inspectoría.

Los comunicados enviados no se pueden editar ni borrar. Los datos quedan en Room y siguen ahí al cerrar la app.

## Estructura

La app va siempre por este camino: pantalla, ViewModel, repositorio, DAO y Room.

- `ui`: pantallas y componentes.
- `viewmodel`: estado, login y envío.
- `reglas`: validaciones y reglas del colegio.
- `data`: repositorio, Room y el servicio de mensajería simulado.
- `model`: clases de datos.
- `recursosnativos`: selector de PDF y vibración.

## Recursos nativos

El caso no permite cámara, micrófono, GPS ni biometría, así que usé dos que no piden esos permisos:

1. Selector de documentos del sistema, para adjuntar el PDF del comunicado. No pide permisos.
2. Vibración cuando termina el envío. Usa el permiso VIBRATE, que no se le pide al usuario.
