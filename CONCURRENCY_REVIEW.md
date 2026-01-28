# Análisis de Concurrencia - Sistema de Gestión de Incidencias

## Resumen Ejecutivo

Este documento analiza los mecanismos de seguridad de hilos (thread-safety) implementados en el sistema cliente-servidor de gestión de incidencias. El sistema está diseñado para manejar múltiples clientes concurrentes de forma segura.

## Arquitectura de Concurrencia

### Modelo de Hilos

```
ServerApp (Main Thread)
    │
    ├─► ExecutorService (CachedThreadPool)
    │       │
    │       ├─► ClientHandler Thread 1
    │       ├─► ClientHandler Thread 2
    │       ├─► ClientHandler Thread 3
    │       └─► ClientHandler Thread N
    │
    └─► DataStore (Singleton compartido)
            ├─► List<User> (Synchronized)
            └─► List<Ticket> (Synchronized)
```

## Componentes Thread-Safe

### 1. DataStore (Singleton)

**Ubicación**: `server/datastore/DataStore.java`

#### Mecanismos de Seguridad

✅ **Singleton Thread-Safe**
```java
public static synchronized DataStore getInstance() {
    if (instance == null) {
        instance = new DataStore();
    }
    return instance;
}
```
- El método `getInstance()` está sincronizado
- Garantiza una única instancia compartida entre todos los hilos

✅ **Listas Sincronizadas**
```java
users = Collections.synchronizedList(new ArrayList<>());
tickets = Collections.synchronizedList(new ArrayList<>());
```
- Todas las operaciones básicas (add, get, set) son atómicas
- Protege contra corrupción de datos en acceso concurrente

✅ **Bloques Sincronizados para Operaciones Complejas**
```java
public synchronized Ticket addTicket(Ticket ticket) {
    synchronized (tickets) {
        int maxId = tickets.stream().mapToInt(Ticket::getId).max().orElse(0);
        ticket.setId(maxId + 1);
        tickets.add(ticket);
        saveData();
        return ticket;
    }
}
```
- Doble sincronización: método + lista
- Garantiza atomicidad en operaciones multi-paso
- Previene race conditions en generación de IDs

✅ **Retorno de Copias Defensivas**
```java
public List<Ticket> getAllTickets() {
    synchronized (tickets) {
        return new ArrayList<>(tickets);
    }
}
```
- Evita ConcurrentModificationException
- Los clientes reciben una copia, no la lista original
- Permite iteración segura sin bloquear la lista original

### 2. ClientHandler (Aislamiento por Thread)

**Ubicación**: `server/core/ClientHandler.java`

#### Características de Seguridad

✅ **Aislamiento de Estado**
- Cada `ClientHandler` se ejecuta en su propio hilo
- Variables de instancia (`currentUser`, `socket`, `in`, `out`) son privadas a cada hilo
- No hay estado compartido entre handlers

✅ **Acceso Controlado a Recursos Compartidos**
```java
DataStore.getInstance().addTicket(newTicket);  // Thread-safe
DataStore.getInstance().getAllTickets();       // Thread-safe
```
- Solo accede a `DataStore` a través de métodos sincronizados
- No mantiene referencias a listas compartidas

### 3. PersistenceService (Singleton)

**Ubicación**: `server/persistence/PersistenceService.java`

#### Mecanismos de Seguridad

✅ **Métodos Sincronizados**
```java
public synchronized void saveData(List<User> users, List<Ticket> tickets)
public synchronized ServerData loadData()
```
- Previene escrituras/lecturas concurrentes al archivo
- Garantiza consistencia de datos en disco

✅ **Copias Defensivas**
```java
ServerData data = new ServerData(
    new ArrayList<>(users),
    new ArrayList<>(tickets)
);
```
- Crea copias antes de serializar
- Evita que cambios durante serialización corrompan el archivo

### 4. ServerLogger (Singleton)

**Ubicación**: `server/util/ServerLogger.java`

#### Mecanismos de Seguridad

✅ **Métodos Sincronizados**
```java
public synchronized void info(String message)
public synchronized void warning(String message)
public synchronized void error(String message)
```
- Previene entrelazado de mensajes de log
- Garantiza escrituras atómicas al archivo

✅ **Auto-flush**
```java
fileWriter.println(logEntry);
fileWriter.flush();
```
- Asegura que los logs se escriban inmediatamente
- Previene pérdida de datos en caso de crash

## Escenarios de Concurrencia Analizados

### Escenario 1: Múltiples Clientes Creando Tickets Simultáneamente

**Situación**: 3 clientes intentan crear tickets al mismo tiempo

**Protección**:
1. Cada `ClientHandler` procesa su request en su propio hilo
2. `DataStore.addTicket()` está sincronizado
3. La generación de ID y la inserción son atómicas
4. `saveData()` se ejecuta de forma sincronizada

**Resultado**: ✅ Seguro - No hay race conditions, IDs únicos garantizados

### Escenario 2: Cliente Listando Mientras Otro Crea

**Situación**: Cliente A lista tickets mientras Cliente B crea uno nuevo

**Protección**:
1. `getAllTickets()` retorna una copia
2. Cliente A itera sobre su copia sin bloquear
3. Cliente B puede modificar la lista original
4. Ambas operaciones son thread-safe

**Resultado**: ✅ Seguro - Sin bloqueos innecesarios, sin excepciones

### Escenario 3: Shutdown Durante Operaciones

**Situación**: Servidor recibe señal de apagado mientras hay operaciones activas

**Protección**:
```java
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    logger.info("Señal de apagado recibida. Guardando datos...");
    DataStore.getInstance().saveData();
    shutdownPool();
    logger.close();
}));
```
- Shutdown hook garantiza guardado de datos
- `pool.awaitTermination()` espera a que terminen los hilos
- Timeout de 5 segundos antes de forzar shutdown

**Resultado**: ✅ Seguro - Datos se guardan antes de cerrar

### Escenario 4: Múltiples Guardados Simultáneos

**Situación**: Varios hilos modifican datos y disparan `saveData()`

**Protección**:
- `PersistenceService.saveData()` está sincronizado
- Solo un hilo puede escribir al archivo a la vez
- Otros hilos esperan su turno

**Resultado**: ✅ Seguro - Archivo no se corrompe

## Posibles Mejoras (Opcionales)

### 1. ReadWriteLock para DataStore

**Problema Actual**: Lecturas bloquean otras lecturas
**Solución**:
```java
private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

public List<Ticket> getAllTickets() {
    rwLock.readLock().lock();
    try {
        return new ArrayList<>(tickets);
    } finally {
        rwLock.readLock().unlock();
    }
}
```
**Beneficio**: Múltiples lecturas concurrentes sin bloqueo

### 2. Debouncing de Guardado

**Problema Actual**: Cada modificación guarda inmediatamente
**Solución**: Guardar cada N segundos o cada M cambios
**Beneficio**: Reduce I/O de disco

### 3. AtomicInteger para IDs

**Problema Actual**: Búsqueda de max ID en cada inserción
**Solución**:
```java
private final AtomicInteger nextTicketId = new AtomicInteger(1);

public Ticket addTicket(Ticket ticket) {
    ticket.setId(nextTicketId.getAndIncrement());
    tickets.add(ticket);
    saveData();
    return ticket;
}
```
**Beneficio**: Generación de ID más eficiente

## Plan de Pruebas de Concurrencia

### Prueba 1: Múltiples Clientes Simultáneos

**Objetivo**: Verificar que el servidor maneja 5+ clientes concurrentes

**Pasos**:
1. Iniciar servidor
2. Ejecutar 5 instancias del cliente
3. Hacer login con diferentes usuarios
4. Cada cliente crea 10 tickets
5. Verificar que se crearon 50 tickets únicos

**Criterio de Éxito**: 
- 50 tickets con IDs únicos (1-50)
- Sin excepciones en logs
- Archivo `server_data.dat` contiene todos los tickets

### Prueba 2: Lectura/Escritura Concurrente

**Objetivo**: Verificar que lecturas no bloquean escrituras

**Pasos**:
1. Cliente A lista tickets en loop (cada 100ms)
2. Cliente B crea tickets en loop (cada 200ms)
3. Ejecutar durante 30 segundos

**Criterio de Éxito**:
- Cliente A recibe listas actualizadas
- Cliente B crea tickets sin errores
- Sin ConcurrentModificationException

### Prueba 3: Stress Test de Persistencia

**Objetivo**: Verificar integridad de datos bajo carga

**Pasos**:
1. 10 clientes crean tickets simultáneamente
2. Detener servidor abruptamente (Ctrl+C)
3. Reiniciar servidor
4. Verificar que todos los tickets se recuperaron

**Criterio de Éxito**:
- Archivo `server_data.dat` no está corrupto
- Todos los tickets creados están presentes
- Logs muestran guardado exitoso

### Prueba 4: Shutdown Graceful

**Objetivo**: Verificar que shutdown hook funciona

**Pasos**:
1. Iniciar servidor con 3 clientes conectados
2. Clientes crean tickets
3. Enviar señal de shutdown (Ctrl+C)
4. Verificar logs

**Criterio de Éxito**:
- Log muestra "Señal de apagado recibida"
- Log muestra "Datos guardados correctamente"
- Pool de hilos se cierra ordenadamente

## Conclusión

### ✅ Puntos Fuertes

1. **DataStore completamente thread-safe** con sincronización adecuada
2. **Aislamiento de hilos** en ClientHandler
3. **Persistencia sincronizada** sin race conditions
4. **Logging thread-safe** sin entrelazado de mensajes
5. **Shutdown graceful** con guardado de datos

### ⚠️ Consideraciones

1. El rendimiento puede degradarse con muchos clientes (>100) debido a sincronización
2. Guardado automático en cada operación puede ser costoso
3. No hay límite en el pool de hilos (CachedThreadPool)

### 📊 Veredicto

**El sistema es THREAD-SAFE** para el uso esperado (5-20 clientes concurrentes). Los mecanismos de sincronización son correctos y suficientes para prevenir race conditions, deadlocks y corrupción de datos.

Para escenarios de alta concurrencia (>100 clientes), se recomiendan las mejoras opcionales mencionadas.
