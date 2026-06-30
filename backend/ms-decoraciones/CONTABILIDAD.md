Actúa como un Ingeniero de Software Principal y Consultor Financiero Experto. Tienes acceso completo a la base de código de este proyecto de Gestión de Eventos y Decoraciones. Tu objetivo es realizar una refactorización estructural profunda en el backend, específicamente en la lógica de negocio dentro de CotizacionService.java y su arquitectura de datos de soporte. Existe una brecha crítica entre la fundamentación teórica contable y la implementación actual, además de una falta de parametrización dinámica para el administrador.

Analiza todo el proyecto e identifica los puntos clave donde se guardan o manejan constantes rígidas. Debes preparar un diseño que mueva todos los valores de control a un modelo de configuración dinámica modificable por el administrador antes de proceder con la refactorización basándote en las siguientes directrices exactas:

PARTE 1. FORMULACIÓN MATEMÁTICA Y CONTABLE REQUERIDA (MODELO TEÓRICO)

Precio de Artículos Consumibles (PC):
El precio se basa en el costo de adquisición directo aplicando un margen de recargo comercial o Markup.
Fórmula: PC = Costo_Adquisicion * (1 + (Porcentaje_Markup / 100))

Precio de Alquiler de Artículos Reutilizables (PAR):
Busca recuperar la inversión evaluando el mayor impacto financiero entre el desgaste por uso ordinario (Fórmula PAU) y la obsolescencia por tiempo en depósito (Fórmula DLA), añadiendo además una tasa de riesgo por pérdidas o siniestros.
Fórmula PAU (Precio de Alquiler Unitario por uso): PAU = (Costo_Adquisicion / Vida_Util_Usos) + Mantenimiento_Promedio
Fórmula DLA (Depreciación Lineal Anualizada por tiempo): DLA = (Costo_Adquisicion - Valor_Residual) / (Vida_Util_Anios * 365) * (1 + Dias_Preparacion + Dias_Limpieza)
Fórmula de Provisión por Siniestro (PS): PS = Costo_Adquisicion * (Porcentaje_Siniestralidad / 100)
Fórmula Final PAR: PAR = (Maximo(PAU, DLA) + PS) * (1 + (Porcentaje_Ganancia / 100))

Costo Logístico de Transporte (CLT):
Debe ser escalable de forma tridimensional evaluando el volumen total del lienzo en metros cúbicos frente a la capacidad máxima del vehículo estándar del negocio. Si el volumen supera la capacidad de una unidad, el costo base debe multiplicarse por la cantidad de viajes requeridos.
Fórmula: CLT = (Tarifa_Base_Vehiculo * Numero_Viajes) + (Distancia_Km * Tarifa_Por_Km)

Costo de Mano de Obra / Armado (CMO):
Es un costo variable acumulativo basado en el tiempo y esfuerzo real. No debe ser una tarifa plana global. Se calcula sumando el tiempo de armado de cada artículo multiplicado por su cantidad y por la tarifa del nivel de complejidad correspondiente.
Fórmula: CMO = Sumatoria_De_Todos_Los_Items(Tiempo_Armado_Item_Minutos * Cantidad_Item * (Tarifa_Hora_Operario_Segun_Complejidad / 60))

Costo por Gastos Indirectos (Overhead):
Es una asignación fija representativa por evento calculada mediante el prorrateo de los costos operativos mensuales fijos del negocio divididos entre el volumen histórico ponderado de eventos.
Fórmula: Costo_Overhead_Fijo = Gastos_Fijos_Mensuales / Promedio_Mensual_Eventos

Presupuesto Total Final (PT):
Aplica el factor de ajuste estacional del mercado sobre el acumulado de todos los costos y márgenes anteriores.
Fórmula: PT = (Suma_De_Precios_Articulos + CLT + CMO + Costo_Overhead_Fijo) * Factor_Estacional_Mes

PARTE 2. EXPLICACIÓN DE LAS MALAS APLICACIONES ENCONTRADAS EN EL CÓDIGO ACTUAL

Analiza el archivo CotizacionService.java y notarás las siguientes deficiencias técnicas frente al modelo teórico:

En la Mano de Obra (CMO), el método calcularManoObra evalúa erróneamente la complejidad máxima de un solo ítem en el lienzo y asigna una tarifa plana única global (ej. 350, 150 o 50) para toda la orden. Esto rompe la contabilidad de costos porque penaliza órdenes pequeñas con un artículo de alta complejidad e infravalora eventos masivos con decenas de artículos que demandan mucho tiempo de montaje. Debe cambiarse a un cálculo acumulativo por tiempo de armado por ítem.

En la Logística (CLT), el método calcularFlete calcula tramos estáticos de volumen en metros cúbicos, pero no implementa la lógica de escalabilidad por número de viajes cuando el diseño excede la capacidad de transporte físico instalada.

El Overhead está mal conceptualizado. En el código aplicas un 10 por ciento variable sobre el subtotal directo. Esto desvirtúa la naturaleza de los costos indirectos fijos, provocando que un evento costoso subsidie de forma desproporcionada los gastos fijos del local comercial sobre un evento económico.

La Provisión por Pérdidas o Siniestros (PS) está completamente omitida en la lógica de cálculo para artículos reutilizables.

El Factor Estacional y el Overhead se aplican de forma rígida en código sin opción de ser alterados o desactivados por requerimiento comercial.

PARTE 3. REQUERIMIENTO DE PARAMETRIZACIÓN DINÁMICA (CONTROL DE ADMINISTRADOR)

Antes de alterar las fórmulas, escanea el proyecto y prepara una propuesta de estructura o clase de configuración (por ejemplo, una entidad global de base de datos) para que las siguientes variables dejen de estar en código duro y pasen a ser leídas dinámicamente desde los ajustes del administrador:

Interruptores de Reglas de Negocio (Feature Toggles):

Un control booleano para activar o desactivar el cálculo del Factor Estacional. Si se desactiva, el algoritmo debe omitir la búsqueda en base de datos y forzar un factor de 1.0 por defecto.

Un control booleano para activar o desactivar el recargo de la Provisión por Siniestro (PS) en los artículos reutilizables.

Configuración Operativa y de Costos:

Tasa de Overhead Global: Un campo numérico para definir el Monto Fijo de Overhead en bolivianos por evento, eliminando el cálculo porcentual del 10 por ciento.

Matriz de Logística: Valores editables para Capacidad Volumétrica del Vehículo (en metros cúbicos), Tarifa Base por Viaje y Tarifa Variable por Kilómetro.

Matriz de Mano de Obra: Valores editables de Tarifa por Hora para los tres niveles de complejidad (BAJO, MEDIO, ALTO).

Fallbacks de Amortización: Valores por defecto de Vida Útil en Usos, Vida Útil en Años, Porcentaje de Valor Residual y Porcentaje de Mantenimiento, para usarse automáticamente si un artículo del inventario no tiene estos datos definidos.

PARTE 4. DISEÑO DE LA CORRECTA IMPLEMENTACIÓN Y FLUJO LÓGICO DE DATOS

Estructura el flujo de datos dentro de CotizacionService.java exactamente en este orden secuencial:

Paso 1. Carga de Parámetros Dinámicos: Recupera la entidad o servicio de configuración global con todas las variables descritas en la Parte 3.

Paso 2. Inicialización de Métricas del Lienzo: Declara acumuladores en cero para volumen total en metros cúbicos, costo total de artículos consumibles, costo total de artículos reutilizables y costo acumulado de mano de obra.

Paso 3. Bucle de Procesamiento de Elementos del Lienzo: Itera sobre cada artículo solicitado. Por cada uno, consulta sus propiedades en la base de datos (ArticuloInventario).

Si es Consumible: Calcula su precio unitario con la fórmula PC (usando su costo de adquisición y el markup del artículo o el del sistema), multiplícalo por la cantidad requerida y súmalo al acumulador de consumibles.

Si es Reutilizable: Recupera sus valores de amortización. Si están vacíos, inyecta los Fallbacks de la configuración. Evalúa si el interruptor de Provisión por Siniestro está encendido; si es así, calcula PS, si no, establécelo en cero. Ejecuta las fórmulas PAU y DLA. Determina el mayor valor entre PAU y DLA, súmale PS, aplica el porcentaje de ganancia para obtener el PAR unitario, multiplícalo por la cantidad requerida y súmalo al acumulador de reutilizables.

Cálculo de Esfuerzo de Mano de Obra por Ítem: Extrae el tiempo estimado de armado en minutos de ese ítem y la tarifa horaria leída desde la configuración dinámica correspondiente a su nivel de complejidad. Calcula el costo de mano de obra de ese grupo de artículos e increméntalo en el acumulador global de CMO.

Acumulación Física: Multiplica el volumen unitario en metros cúbicos del artículo por su cantidad e increméntalo en el acumulador de volumen total.

Paso 4. Procesamiento Logístico Post-Bucle: Toma el volumen total en metros cúbicos acumulado. Determina cuántos viajes son necesarios dividiendo dicho volumen entre la Capacidad Volumétrica leída de la configuración. Con el número de viajes calculado, la distancia en kilómetros provista y las tarifas de logística dinámicas, ejecuta la fórmula CLT para obtener el Costo Logístico Final.

Paso 5. Inyección de Costos Fijos Estructurales: Recupera el valor del Costo Overhead Fijo desde la configuración dinámica del sistema.

Paso 6. Consolidación y Optimización de Ingresos: Suma el acumulador de artículos consumibles, el de artículos reutilizables, el costo logístico de transporte (CLT), el acumulador de mano de obra (CMO) y el Costo Overhead Fijo.
Evalúa el interruptor del Factor Estacional de la configuración. Si está activo, determina el mes de ejecución del evento a partir de la fecha de la reserva y busca su multiplicador en el repositorio; si está desactivado o no existe el registro, establece el factor en 1.0. Multiplica la suma total consolidada por el Factor Estacional para definir el Presupuesto Total Final (PT).

Paso 7. Retorno de Datos: Construye y retorna el objeto de respuesta CotizacionDetalleResponse mapeando con total precisión cada subtotal calculado individualmente para garantizar la transparencia del desglose de precios en la interfaz de usuario.

Por favor, revisa el archivo CotizacionService.java junto con las entidades relacionadas, identifica los puntos clave de constantes rígidas, diseña la integración de la configuración dinámica y reescribe los métodos de cálculo siguiendo estrictamente este flujo financiero y sus ecuaciones matemáticas.

FIN DEL PROMPT PARA LA IA DEL IDE