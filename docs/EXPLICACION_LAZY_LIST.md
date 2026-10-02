# Explicación de la Lazy List

## Objetivo de la pantalla

La pantalla `Forms` muestra una colección de formularios creados por el usuario. Para este ejercicio usamos diez registros locales definidos en `FormSampleData`, sin API ni `ViewModel`. Cada registro es un `FormSummary` inmutable que contiene un identificador, título, cantidad de respuestas, fecha de actualización, estado y URL de imagen.

## Por qué elegimos una lista vertical

Elegimos `LazyColumn` porque los formularios son elementos de información que el usuario necesita revisar uno después de otro. Cada tarjeta contiene texto, estado, imagen y varias acciones, por lo que una sola columna conserva suficiente espacio para leer todo con claridad. La colección puede crecer conforme el usuario crea más formularios y el desplazamiento vertical es el comportamiento más natural para recorrerla. Un grid reduciría el espacio disponible y haría más difícil comparar los títulos, fechas y cantidades de respuestas.

## Qué hace LazyColumn

`LazyColumn` es el componente de Jetpack Compose para presentar una lista con desplazamiento vertical. A diferencia de una `Column` normal, una lista lazy compone y posiciona principalmente los elementos que necesita mostrar dentro del área visible. Esto evita crear de una vez la interfaz completa de una colección grande y permite usar mejor la memoria y el tiempo de procesamiento.

En nuestra pantalla se utiliza de esta forma:

```kotlin
LazyColumn(
    contentPadding = PaddingValues(
        horizontal = 16.dp,
        vertical = 24.dp
    ),
    verticalArrangement = Arrangement.spacedBy(16.dp)
) {
    items(
        items = visibleForms,
        key = { form -> form.id }
    ) { form ->
        FormListItem(form = form)
    }
}
```

`items()` recibe la colección que debe mostrar y ejecuta el contenido para los formularios requeridos por la lista. El componente de cada elemento está separado en `FormListItem`, lo que mantiene la pantalla organizada y permite reutilizar la tarjeta.

## Composición y recomposición

La composición inicial ocurre cuando Compose ejecuta por primera vez los Composables y construye la descripción de la interfaz. La recomposición sucede cuando cambia un estado que fue leído por un Composable. Compose vuelve a ejecutar las partes que podrían depender del nuevo valor y puede omitir las partes cuyos datos no cambiaron.

El filtro seleccionado se guarda en `FormLinkApp`, que es el componente que contiene las dos pantallas:

```kotlin
var selectedFilter by rememberSaveable {
    mutableStateOf(FormFilter.ALL)
}

Forms(
    selectedFilter = selectedFilter,
    onFilterSelected = { selectedFilter = it }
)
```

Este manejo se conoce como elevación de estado: `Forms` muestra el filtro que recibe y avisa mediante un callback cuando el usuario selecciona otro. El flujo es el siguiente:

1. El usuario presiona `Todos`, `Activos` o `Borradores`
2. `Forms` ejecuta el callback `onFilterSelected`
3. `FormLinkApp` actualiza `selectedFilter`
4. Se vuelve a calcular `visibleForms` con el filtro seleccionado
5. `LazyColumn` recibe la colección actualizada y muestra los elementos correspondientes

Como el estado permanece en `FormLinkApp`, no se pierde cuando el usuario entra a la pantalla de plantillas y regresa a Formularios. `rememberSaveable` también permite restaurarlo si Android necesita recrear la actividad. Esto no significa que Android reconstruya toda la aplicación con cada selección: Compose actualiza las partes que dependen del valor y puede omitir los Composables cuyos parámetros siguen iguales.

El filtro de diez elementos es una operación pequeña y se prepara antes de entrar al cuerpo de cada tarjeta, por lo que no estamos realizando cálculos pesados dentro de `FormListItem`.

## Por qué usamos una key estable

La lista usa la siguiente key:

```kotlin
key = { form -> form.id }
```

Cada `FormSummary` tiene un `id` único que representa la identidad del formulario y no depende de su posición. Cuando la colección se filtra, un formulario puede cambiar de posición, pero conserva el mismo `id`. Esto ayuda a Compose a reconocer que sigue siendo el mismo elemento y a asociar correctamente cualquier estado recordado con su tarjeta.

Usar el índice sería menos seguro porque el índice cambia cuando se elimina, agrega, filtra o reordena contenido. Dos formularios diferentes podrían ocupar la misma posición en momentos distintos. El `id` evita esa ambigüedad y cumple la recomendación de usar una key única, estable y compatible con `Bundle`.

## Content padding y separación

`contentPadding` agrega espacio alrededor del contenido de la lista. En nuestro caso aplica `16.dp` a los lados y `24.dp` arriba y abajo. Este padding pertenece al contenido: el primer y último elemento reciben el espacio correspondiente sin reducir manualmente cada tarjeta.

`verticalArrangement = Arrangement.spacedBy(16.dp)` mantiene una separación uniforme entre los elementos. De esta manera no necesitamos agregar un `Spacer` distinto después de cada formulario y se evita dejar espacio adicional después del último elemento. Todas estas medidas son múltiplos de `4.dp`, como solicita el wireframe.

## Modelo inmutable y componente separado

`FormSummary` usa una `data class` con propiedades `val`. Al no modificar sus propiedades internamente, los datos son más predecibles y Compose puede razonar mejor sobre si los parámetros cambiaron. Si un formulario necesita actualizarse, se crea una nueva instancia y se entrega una nueva lista a la interfaz.

`FormListItem` recibe un formulario y callbacks para las acciones. No conoce la navegación ni controla la colección completa. Esta separación permite probar, reutilizar y modificar la tarjeta sin mezclarla con los filtros o con la estructura general de `Forms`.

## Imágenes e interacciones

Cada tarjeta usa Coil mediante `AsyncImage` para cargar una imagen remota. Mientras la imagen se descarga se muestra un `ColorPainter` como placeholder; también existe un color de respaldo si la carga falla. El tamaño de la imagen está limitado a `72.dp` y `ContentScale.Crop` evita que deforme la tarjeta.

El `OutlinedCard` recibe un `onClick`, y las acciones Compartir, Editar y Ver reciben callbacks independientes. Por ahora la pantalla muestra un `Snackbar` para que cada interacción produzca un resultado observable. El botón Nuevo formulario además cambia el destino actual y abre la pantalla `ChooseTemplate`.

## Explicación corta

Una `LazyColumn` describe una colección vertical sin mantener compuestos todos sus elementos al mismo tiempo. Cuando cambia `selectedFilter`, `FormLinkApp` conserva el nuevo valor y Compose vuelve a ejecutar las partes dependientes para entregar una nueva colección visible a la lista. La key `form.id` mantiene la identidad de cada formulario aunque su posición cambie. `contentPadding` controla el espacio exterior del contenido y `Arrangement.spacedBy` conserva una separación uniforme entre tarjetas.

## Fuentes oficiales verificadas

- [Lazy lists and lazy grids - Android Developers](https://developer.android.com/develop/ui/compose/lists)
- [Thinking in Compose - Android Developers](https://developer.android.com/develop/ui/compose/mental-model)
- [State and Jetpack Compose - Android Developers](https://developer.android.com/develop/ui/compose/state)
- [Lifecycle of composables - Android Developers](https://developer.android.com/develop/ui/compose/lifecycle)

Consulta realizada el 8 de agosto de 2026
