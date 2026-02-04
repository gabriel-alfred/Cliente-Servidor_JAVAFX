# Cliente-Servidor JavaFX - Guía Rápida

## Inicio Rápido

### Opción 1: Scripts Batch (Recomendado)

1. **Ejecutar Servidor**: Doble click en `run-server.bat`
2. **Ejecutar Cliente**: Doble click en `run-client.bat`

### Opción 2: Comandos Maven

**Servidor**:
```bash
mvn exec:java -Dexec.mainClass="server.ServerApp"
```

**Cliente**:
```bash
mvn javafx:run
```

## Credenciales

- **Admin**: `admin` / `admin`
- **User**: `user` / `user`


## Comandos Útiles

```bash
# Compilar
mvn clean compile

# Crear JAR
mvn package

# Ver dependencias
mvn dependency:tree
```