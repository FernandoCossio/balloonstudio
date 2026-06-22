1. Gestión de Usuarios y Accesos
Tabla: usuario
Atributos:
id (Clave Primaria)
activo
email
nombre_completo
password_hash
telefono
username

Tabla: rol
Atributos:
id (Clave Primaria)
descripcion
nombre

Tabla: rol_usuario
Atributos:
id (Clave Primaria)
rol_id (Clave Foránea -> rol.id)
usuario_id (Clave Foránea -> usuario.id)

Tabla: auth_token
Atributos:
id (Clave Primaria)
expiresAt
isRevoked
tipo
token
usedAt
usuario_id (Clave Foránea -> usuario.id)

Tabla: suscripcion
Atributos:
id (Clave Primaria)
almacenamiento_usado_mb
estado
fecha_inicio
fecha_vencimiento
limite_almacenamiento_mb
max_borradores
max_proyectos
plan
precio_mensual
usuario_id (Clave Foránea -> usuario.id)

2. Módulo de Inteligencia Artificial (Búsqueda)
Tabla: busqueda_ia
Atributos:
id (Clave Primaria)
descripcion_texto
embedding_consulta
fecha_busqueda
imagen_uri
modelo_version
proyecto_id (Clave Foránea -> proyecto_design.id, Opcional)
tipo_entrada
usuario_id (Clave Foránea -> usuario.id)

Tabla: busqueda_resultado
Atributos:
id (Clave Primaria)
articulo_id (Clave Foránea -> articulo_inventario.id)
busqueda_id (Clave Foránea -> busqueda_ia.id)
posicion_ranking
similitud_score

3. Core del Negocio: Proyectos y Diseños
Tabla: proyecto_diseno
Atributos:
id (Clave Primaria)
nombre
estado
fecha_creacion
fecha_evento
fecha_ultima_modificacion
lugar_evento
numero_invitados
distancia_km
costo_real_total
costo_consumibles
escenario_base_id (Clave Foránea -> escenario_base.id, Opcional)
usuario_id (Clave Foránea -> usuario.id)

Tabla: escenario_base
Atributos:
id (Clave Primaria)
nombre
descripcion
categoria
activo
escala
dimensiones_alto_px
dimensiones_ancho_px
imagen_url

Tabla: elemento_lienzo
Atributos:
id (Clave Primaria)
articulo_id (Clave Foránea -> articulo_inventario.id)
cantidad
escala
escenario_base (Clave Foránea o identificador de referencia)
layer
pos_x
pos_y
proyecto_id (Clave Foránea -> proyecto_diseno.id)
rotacion_deg

z_index

4. Inventario y Catálogo
Tabla: articulo_inventario
Atributos:
id (Clave Primaria)
nombre
descripcion
costo_adquisicion
dias_limpieza_posteriores
dias_preparacion_previos
embedding_visual
estado
mantenimiento_promedio_bs
nivel_complejidad
porcentaje_ganancia
peso_kg
stock_total
tiempo_armado_min
tipo_articulo
valor_residual
vida_util_anos
vida_util_usos
volumen_m3

Tabla: categoria
Atributos:
id (Clave Primaria)
nombre
descripcion

Tabla: categoria_inventario
Atributos:
id (Clave Primaria)
categoria_id (Clave Foránea -> categoria.id)
inventario_id (Clave Foránea -> articulo_inventario.id)

Tabla: imagen_article (o imagen_articulo)
Atributos:
id (Clave Primaria)
articulo_id (Clave Foránea -> articulo_inventario.id)
es_principal
fecha_subida
orden
procesado_ia
url

5. Cotizaciones, Reservas y Pagos
Tabla: cotizacion
Atributos:
id (Clave Primaria)
version
estado
fecha_generacion
costo_armado
costo_articulos
costo_flete
tasa_overhead_aplicada
total
proyecto_id (Clave Foránea -> proyecto_diseno.id)

Tabla: reserva
Atributos:
id (Clave Primaria)
cotizacion_id (Clave Foránea -> cotizacion.id)
empleado_asignado_id
estado
fecha_confirmacion
fecha_reserva
monto_anticipo
usuario_id (Clave Foránea -> usuario.id)

Tabla: pago
Atributos:
id (Clave Primaria)
reserva_id (Clave Foránea -> reserva.id)
monto
metodo
estado
fecha_pago
referencia_externa
tipo_pago

6. Operaciones, Bloqueos e Incidencias
Tabla: bloqueo_inventario
Atributos:
id (Clave Primaria)
articulo_id (Clave Foránea -> articulo_inventario.id)
reserva_id (Clave Foránea -> reserva.id)
cantidad
fecha_inicio
fecha_fin
tipo_bloqueo

Tabla: incidencia_articulo
Atributos:
id (Clave Primaria)
articulo_id (Clave Foránea -> articulo_inventario.id)
reserva_id (Clave Foránea -> reserva.id, Opcional)
reportado_por_id (Clave Foránea -> usuario.id)
descripcion
estado
fecha
fecha_resolucion
costo_reparacion
tipo

7. Parámetros de Configuración Global
Tabla: parametro_negocio
Atributos:
id (Clave Primaria)
costos_indirectos_mensuales
promedio_eventos_mes

Tabla: factor_estacional
Atributos:
id (Clave Primaria)
mes
descripcion
factor_estacional

🔗 Resumen de Relaciones (Cardinalidad)
usuario a auth_token: 1 a muchos (1 -> 0..*)
usuario a rol_usuario: 1 a muchos (1 -> *)
rol a rol_usuario: 1 a muchos (1 -> *)
usuario a suscripcion: 1 a opcional (1 -> 0..1)
usuario a proyecto_diseno: 1 a muchos (1 -> 0..*)
usuario a busqueda_ia: 1 a muchos (1 -> 0..*)
proyecto_diseno a busqueda_ia: 1 a muchos (1 -> 0..*)
busqueda_ia a busqueda_resultado: 1 a muchos (1 -> 0..*)
articulo_inventario a busqueda_resultado: 1 a muchos (1 -> 0..*)
articulo_inventario a imagen_articulo: 1 a muchos (1 -> 0..*)
articulo_inventario a categoria_inventario: 1 a muchos (1 -> *)
categoria a categoria_inventario: 1 a muchos (1 -> *)
proyecto_diseno a escenario_base: Muchos a muchos o referencial (1 -> 0..*)
escenario_base a elemento_lienzo: 1 a muchos (1 -> 0..*)
proyecto_diseno a elemento_lienzo: 1 a muchos (1 -> 0..*)
articulo_inventario a elemento_lienzo: 1 a muchos (1 -> 0..*)
proyecto_diseno a cotizacion: 1 a muchos (1 -> 0..*)
cotizacion a reserva: 1 a opcional (1 -> 0..1)
reserva a pago: 1 a muchos (1 -> 0..*)
reserva a bloqueo_inventario: 1 a muchos (1 -> 0..*)
articulo_inventario a bloqueo_inventario: 1 a muchos (1 -> 0..*)
articulo_inventario a incidencia_article: 1 a muchos (1 -> 0..*)
reserva a incidencia_article: 1 a muchos (1 -> 0..*)
usuario a incidencia_article: 1 a muchos (1 -> 0..* - reportado por)