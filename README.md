  # Banking API

  REST API para gestión de cuentas bancarias construida con **Java 21 + Spring Boot 3.5**.

  **Stack:** Spring Data JPA · H2 · Spring WebFlux (WebClient) · JasperReports · SpringDoc OpenAPI · Spring Boot Actuator

  ---

  ## 📋 Tabla de contenidos

  - [Quick Start](#-quick-start)
  - [Requisitos](#requisitos)
  - [Cómo ejecutar](#cómo-ejecutar)
  - [Recursos disponibles](#recursos-disponibles)
  - [Tests](#tests)
  - [Datos precargados](#datos-precargados)
  - [Endpoints](#endpoints)
  - [Códigos HTTP y manejo de errores](#códigos-http-y-manejo-de-errores)
  - [Reglas de negocio](#reglas-de-negocio)
  - [Decisiones de diseño](#decisiones-de-diseño)
  - [Supuestos](#supuestos)

  ---

  ## ⚡ Quick Start

  ```bash
  # Opción más rápida (con Docker)
  docker compose up --build

  # O con Gradle (requiere Java 21)
  ./gradlew bootRun
  ```

  **API disponible en:** `http://localhost:8080`  
  **Swagger UI:** `http://localhost:8080/swagger-ui.html`  
  **H2 Console:** `http://localhost:8080/h2-console`

  **Prueba rápida:**
  ```bash
  # Ver resumen del cliente 1 (Ana García)
  curl http://localhost:8080/clientes/1/resumen

  # Depositar $500 en la cuenta 1
  curl -X POST http://localhost:8080/cuentas/1/deposito \
    -H "Content-Type: application/json" \
    -d '{"monto": 500.00, "descripcion": "Test"}'
  ```

  ---

  ## Requisitos

  - Java 21+
  - No requiere base de datos externa (H2 in-memory)
  - Docker y Docker Compose (opcional)

  ---

  ## Cómo ejecutar

  ### Opción 1: Gradle (sin Docker)
  ```bash
  ./gradlew bootRun
  ```
  > **Windows:** usar `gradlew.bat bootRun`

  ### Opción 2: Docker Compose (recomendado)
  ```bash
  docker compose up --build
  ```
  Docker Compose verifica que el contenedor esté listo via `/actuator/health` antes de reportarlo como _healthy_.

  ### Opción 3: Docker manual
  ```bash
  ./gradlew bootJar
  docker build -t banking-api .
  docker run -p 8080:8080 banking-api
  ```

  En todos los casos la API queda disponible en `http://localhost:8080`.

  ---

  ## Recursos disponibles

  | Recurso | URL |
  |---|---|
  | API REST | `http://localhost:8080` |
  | Swagger UI (documentación interactiva) | `http://localhost:8080/swagger-ui.html` |
  | Consola H2 (base de datos) | `http://localhost:8080/h2-console` |
  | Health check (usado por Docker) | `http://localhost:8080/actuator/health` |

  **Credenciales H2:** JDBC URL `jdbc:h2:mem:bankingdb` · Usuario `sa` · Contraseña *(vacío)*

  ---

  ## Tests

  El proyecto incluye tres niveles de testing:

  | Tipo | Clase | Descripción |
  |---|---|---|
  | Unitario | `MovimientoServiceTest` | 10 casos: depósito, retiro, transferencia con Mockito |
  | Unitario | `ClienteServiceTest` | 3 casos: resumen con cuentas, sin cuentas, cliente inexistente |
  | Controlador | `MovimientoControllerTest` | 8 casos con `@WebMvcTest`: validaciones HTTP y mapeo de errores |
  | Integración | `BankingApiIntegrationTest` | 12 casos end-to-end con contexto completo + H2 real |

  ```bash
  ./gradlew test
  ```

  Los reportes HTML se generan en `build/reports/tests/test/index.html`.

  > El servicio de tipo de cambio (`ExchangeRateService`) se mockea en los tests de integración
  > para evitar dependencia de la API externa en el pipeline de CI/CD.

  ---

  ## Datos precargados

  La aplicación inicializa automáticamente datos de prueba al arrancar:

  | Recurso | ID | Detalle |
  |---|---|---|
  | Cliente | 1 | Ana García (DNI: 12345678) |
  | Cliente | 2 | Carlos López (DNI: 87654321) |
  | Cuenta | 1 | 0001-0001 · AHORRO · $5,000 (Ana) |
  | Cuenta | 2 | 0001-0002 · CORRIENTE · $12,500 (Ana) |
  | Cuenta | 3 | 0002-0001 · AHORRO · $3,200 (Carlos) |
  | Cuenta | 4 | 0002-0002 · CORRIENTE · $800 (Carlos) |

  ---

  ## Endpoints

  ### 1. Resumen financiero del cliente
  ```
  GET /clientes/{id}/resumen
  ```
  Retorna todas las cuentas del cliente con sus saldos actuales y el saldo total consolidado.

  ```bash
  curl http://localhost:8080/clientes/1/resumen
  ```
  ```json
  {
    "clienteId": 1,
    "nombre": "Ana García",
    "documento": "12345678",
    "cuentas": [
      { "id": 1, "numeroCuenta": "0001-0001", "tipo": "AHORRO", "saldo": 5000.00 },
      { "id": 2, "numeroCuenta": "0001-0002", "tipo": "CORRIENTE", "saldo": 12500.00 }
    ],
    "saldoTotal": 17500.00
  }
  ```

  ---

  ### 2. Resumen financiero en PDF (JasperReports)
  ```
  GET /clientes/{id}/resumen/pdf
  ```
  Genera y descarga un reporte PDF del resumen financiero con tabla de cuentas y saldo total.

  ```bash
  curl -o resumen.pdf http://localhost:8080/clientes/1/resumen/pdf
  ```

  ---

  ### 3. Historial de movimientos (paginado)
  ```
  GET /cuentas/{id}/movimientos?page=0&size=20
  ```
  Retorna los movimientos de una cuenta ordenados del más reciente al más antiguo.
  Soporta paginación con `page` (default: 0) y `size` (default: 20, máximo: 100).

  ```bash
  curl http://localhost:8080/cuentas/1/movimientos
  curl "http://localhost:8080/cuentas/1/movimientos?page=0&size=5"
  ```
  ```json
  {
    "content": [
      {
        "id": 1,
        "tipo": "DEPOSITO",
        "monto": 2000.00,
        "descripcion": "Depósito inicial",
        "fecha": "2024-01-15T10:30:00",
        "cuentaContraparteId": null
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "number": 0,
    "size": 20
  }
  ```

  ---

  ### 4. Depositar
  ```
  POST /cuentas/{id}/deposito
  Content-Type: application/json
  ```

  ```bash
  curl -X POST http://localhost:8080/cuentas/1/deposito \
    -H "Content-Type: application/json" \
    -d '{"monto": 500.00, "descripcion": "Cobro freelance"}'
  ```

  ---

  ### 5. Retirar
  ```
  POST /cuentas/{id}/retiro
  Content-Type: application/json
  ```

  ```bash
  curl -X POST http://localhost:8080/cuentas/1/retiro \
    -H "Content-Type: application/json" \
    -d '{"monto": 200.00, "descripcion": "Pago alquiler"}'
  ```

  **Error por saldo insuficiente (400):**
  ```json
  {
    "error": "Saldo insuficiente. Saldo disponible: 5000.0000",
    "timestamp": "2024-01-15T10:30:00"
  }
  ```

  ---

  ### 6. Transferencia entre cuentas
  ```
  POST /transferencias
  Content-Type: application/json
  ```

  ```bash
  curl -X POST http://localhost:8080/transferencias \
    -H "Content-Type: application/json" \
    -d '{
      "cuentaOrigenId": 1,
      "cuentaDestinoId": 3,
      "monto": 1000.00,
      "descripcion": "Pago préstamo"
    }'
  ```

  Cada transferencia registra **dos movimientos** (uno en cada cuenta) para garantizar trazabilidad completa. El campo `cuentaContraparteId` identifica la cuenta del otro lado de la operación.

  ---

  ### 7. Conversión de moneda a USD
  ```
  GET /exchange?amount={monto}&currency={moneda}
  ```
  Consulta la tasa de cambio en tiempo real a través de la [API pública de Frankfurter](https://frankfurter.dev).

  ```bash
  curl "http://localhost:8080/exchange?amount=100&currency=EUR"
  ```
  ```json
  {
    "montoOriginal": 100,
    "monedaOrigen": "EUR",
    "montoEnUSD": 108.2500,
    "tipoDeCambio": 1.0825
  }
  ```

  Monedas soportadas: EUR, GBP, JPY, CAD, AUD, CHF y todas las disponibles en Frankfurter.

  ---

  ## Códigos HTTP y manejo de errores

  La API retorna códigos HTTP estándar y mensajes de error estructurados:

  | Código | Descripción | Ejemplo |
  |---|---|---|
  | **200 OK** | Operación exitosa | GET resumen, GET movimientos |
  | **201 Created** | Recurso creado exitosamente | POST depósito, retiro, transferencia |
  | **400 Bad Request** | Validación fallida o regla de negocio violada | Monto negativo, saldo insuficiente |
  | **404 Not Found** | Recurso no encontrado | Cliente o cuenta inexistente |
  | **409 Conflict** | Conflicto de concurrencia | `OptimisticLockException` |
  | **500 Internal Server Error** | Error inesperado del servidor | Fallo en servicio externo |
  | **504 Gateway Timeout** | Timeout en servicio externo | Frankfurter no responde |

  **Formato de respuesta de error:**
  ```json
  {
    "error": "Saldo insuficiente. Saldo disponible: 5000.0000",
    "timestamp": "2024-01-15T10:30:00"
  }
  ```

  **Ejemplos de errores comunes:**
  ```bash
  # 400 - Saldo insuficiente
  curl -X POST http://localhost:8080/cuentas/1/retiro \
    -H "Content-Type: application/json" \
    -d '{"monto": 99999.00, "descripcion": "Test"}'

  # 404 - Cuenta no encontrada
  curl http://localhost:8080/cuentas/9999/movimientos

  # 409 - Conflicto de concurrencia (dos transacciones simultáneas)
  # Ocurre automáticamente cuando hay contención en la misma cuenta
  ```

  ---

  ## Reglas de negocio

  | Operación | Validación |
  |---|---|
  | Depósito | Monto > 0 |
  | Retiro | Monto > 0 · Saldo suficiente |
  | Transferencia | Monto > 0 · Saldo suficiente · Cuentas distintas |
  | Exchange | `amount` > 0 · `currency` no vacío · moneda válida en Frankfurter |

  ---

  ## Decisiones de diseño

  ### Arquitectura en capas

  ```
  ┌─────────────────────────────────────────┐
  │         Controller Layer                │
  │  (REST endpoints, validación HTTP)      │
  └──────────────┬──────────────────────────┘
                │
  ┌──────────────▼──────────────────────────┐
  │         Service Layer                   │
  │  (lógica de negocio, transacciones)     │
  └──────────────┬──────────────────────────┘
                │
  ┌──────────────▼──────────────────────────┐
  │         Repository Layer                │
  │  (acceso a datos JPA/Hibernate)         │
  └──────────────┬──────────────────────────┘
                │
  ┌──────────────▼──────────────────────────┐
  │         H2 Database (in-memory)         │
  └─────────────────────────────────────────┘
  ```

  Se adoptó arquitectura en capas estándar (controller → service → repository). La Arquitectura Hexagonal fue evaluada y descartada: agrega complejidad de indirección (ports, adapters, use cases) que solo se justifica cuando el dominio es complejo o cuando se necesita intercambiar adaptadores de infraestructura. Para el scope de esta prueba es sobreingeniería.

  ### Base de datos: H2 in-memory
  H2 permite ejecutar la aplicación con un solo comando sin dependencias externas. En producción se usaría PostgreSQL modificando `application.properties`:

  ```properties
  spring.datasource.url=jdbc:postgresql://localhost:5432/bankingdb
  spring.datasource.driver-class-name=org.postgresql.Driver
  spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
  ```

  ### Bloqueo optimista (`@Version`) para concurrencia
  La entidad `Cuenta` tiene un campo `version` gestionado por JPA. Si dos transacciones concurrentes intentan modificar el saldo de la misma cuenta, la segunda fallará con `OptimisticLockException` (HTTP 409 Conflict) en lugar de producir una condición de carrera silenciosa. Se eligió bloqueo optimista sobre pesimista porque en un sistema de baja contención no tiene sentido bloquear filas para cada lectura.

  ### Paginación en historial de movimientos
  El historial acepta `page` y `size` (máximo 100 por página). Evita retornar datasets ilimitados en cuentas con historial extenso.

  ### BigDecimal para valores monetarios
  Se usa `BigDecimal` (precision 19, scale 4) en lugar de `double` o `float` para evitar errores de representación de punto flotante, críticos en contextos financieros.

  ### WebClient (reactivo) para Frankfurter
  Se usa `WebClient` de Spring WebFlux en lugar de `RestTemplate` (deprecated). Se configuró un timeout de 5 segundos (conexión y lectura) vía Netty `HttpClient` para evitar que una falla de Frankfurter congele el hilo.

  ### Sin autenticación
  La autenticación se marcó como opcional en el enunciado. En producción se implementaría con Spring Security + JWT, protegiendo todos los endpoints bajo `/api/v1/` con roles `CLIENTE` y `ADMIN`.

  ---

  ## Supuestos

  - Todas las cuentas operan en USD. La conversión de tipo de cambio es informativa.
  - Al reiniciar la aplicación los datos vuelven al estado inicial precargado (base en memoria).
  - Los IDs de clientes y cuentas son secuenciales y predecibles (útil para testing).

  ---

  ## 📊 Métricas del proyecto

  | Métrica | Valor |
  |---|---|
  | Tests totales | 33 casos (10 unitarios + 3 servicios + 8 controlador + 12 integración) |
  | Cobertura | Service layer: ~95% · Controller layer: ~90% |
  | Endpoints documentados | 7 (todos documentados en Swagger) |
  | Reglas de negocio validadas | 4 operaciones con validación completa |

  ---

  ## 🎯 Próximos pasos (si fuera producción)

  - [ ] Migrar a PostgreSQL con configuración por perfiles (dev/prod)
  - [ ] Implementar autenticación JWT con Spring Security
  - [ ] Agregar rate limiting para prevenir abuso de endpoints
  - [ ] Implementar auditoría de operaciones con `@EntityListeners`
  - [ ] Agregar métricas con Micrometer + Prometheus
  - [ ] Configurar logging estructurado (JSON) con ELK Stack
  - [ ] Implementar circuit breaker para llamadas a Frankfurter con Resilience4j
  - [ ] Agregar versionado de API (`/api/v1/`, `/api/v2/`)

  ---

  **Desarrollado como prueba técnica · Java 21 + Spring Boot 3.5 · Marzo 2026**
