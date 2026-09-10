# 🎫 AetherDesk
### Sistema de Gestión de Tickets Cliente-Servidor Multihilo
*Lightweight, multi-threaded client-server technical support ticket management system built with Java 22, JavaFX, and TCP Sockets.*

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Java 22](https://img.shields.io/badge/Java-22-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![JavaFX 22](https://img.shields.io/badge/JavaFX-22-2C2D30?logo=oracle&logoColor=white)](https://openjfx.io/)
[![Maven 3.8+](https://img.shields.io/badge/Maven-3.8+-C71A36?logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![JasperReports](https://img.shields.io/badge/JasperReports-6.21.0-008080)](https://community.jaspersoft.com/)
[![UI: Brutalist](https://img.shields.io/badge/UI-Brutalist%20Monochrome-black)](https://github.com/alfredgabriel/AetherDesk-Ticket-Management-System)

---

## 🇪🇸 Español

### 1. Visión y Propósito

**AetherDesk** es una solución de escritorio cliente-servidor para soporte técnico y gestión de incidentes. Permite a operadores y administradores coordinar la apertura, diagnóstico, escalado y cierre de incidencias técnicas en tiempo real mediante un protocolo binario/texto personalizado sobre sockets TCP.

Diseñado para ser **autónomo, rápido y libre de dependencias complejas**:
- No requiere instalar bases de datos relacionales externas (persistencia rápida serializada thread-safe).
- Servidor multihilo desacoplado con `ThreadPoolExecutor`.
- Exportación analítica en PDF mediante JasperReports.
- Interfaz gráfica moderna en **estética Brutalista blanco y negro**.

---

### 2. Arquitectura del Sistema

```
[ CLIENTE JAVAFX GUI ] <====== Sockets TCP (Puerto 9000) ======> [ SERVIDOR AETHERDESK CORE ]
         │                                                                   │
         ├── Interfaz Brutalista B&N                                        ├── ThreadPoolExecutor (Multihilo)
         ├── Dashboard & Filtros                                            ├── AuthService (Roles ADMIN/USER)
         └── Formularios de Tickets                                         ├── TicketService (CRUD Incidencias)
                                                                            ├── ReportService (JasperReports PDF)
                                                                            └── DataStore Concurrente (DataStore.dat)
```

---

### 🚀 Uso y Puesta en Marcha

#### Requisitos Previos
- [Java JDK 22+](https://adoptium.net/)
- [Apache Maven](https://maven.apache.org/)

#### 1. Iniciar el Servidor
Puedes ejecutar el script rápido en Windows:
```cmd
run-server.bat
```
O mediante Maven:
```bash
mvn exec:java -Dexec.mainClass="server.ServerApp"
```

#### 2. Iniciar el Cliente GUI
En otra terminal o mediante doble clic:
```cmd
run-client.bat
```
O mediante Maven:
```bash
mvn exec:java -Dexec.mainClass="client.ClientApp"
```

#### Credenciales por defecto:
- **Administrador**: `admin` / `admin123`
- **Operador**: `user` / `user123`

---

### 🛠️ Compilación con Maven

Para compilar y empaquetar el proyecto completo en `target/`:
```bash
mvn clean package
```

---

## 🇬🇧 English

### 1. Overview & Purpose

**AetherDesk** is a lightweight, decoupled client-server technical support ticket management system designed for speed, reliability, and high-concurrency environments.

Featuring a **custom TCP protocol**, role-based access control, embedded data persistence without external database engines, JasperReports PDF generation, and a **Brutalist Monochrome desktop interface**.

---

### 2. Quick Start

```bash
# Build
mvn clean package

# Start Server
run-server.bat
# or: mvn exec:java -Dexec.mainClass="server.ServerApp"

# Start Client
run-client.bat
# or: mvn exec:java -Dexec.mainClass="client.ClientApp"
```

---

## 📜 License

Distributed under the **MIT License**. See `LICENSE` for details.
