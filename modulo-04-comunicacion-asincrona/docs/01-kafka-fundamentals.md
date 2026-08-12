# 1. Apache Kafka Quick Start

## Objetivo

Levantar Kafka en local y probar el flujo básico:

1. crear un topic
2. escribir eventos
3. leer eventos
4. opcionalmente probar Kafka Connect y Kafka Streams

> Basado en la guía oficial de Apache Kafka 4.3: https://kafka.apache.org/43/getting-started/quickstart/

## Requisitos

- Java 17+
- Kafka descargado y descomprimido
- Terminal con acceso a `bin/`

## 1. Obtener Kafka

Descarga la última versión estable y descomprímela:

```bash
tar -xzf kafka_2.13-4.3.1.tgz
cd kafka_2.13-4.3.1
```

## 2. Arrancar el entorno Kafka

Kafka 4.3 usa **KRaft** por defecto. Si trabajas con los archivos descargados:

```bash
KAFKA_CLUSTER_ID="$(bin/kafka-storage.sh random-uuid)"
bin/kafka-storage.sh format --standalone -t "$KAFKA_CLUSTER_ID" -c config/server.properties
bin/kafka-server-start.sh config/server.properties
```

Deja este proceso corriendo en una terminal.

## 3. Crear un topic

Abre otra terminal en la carpeta de Kafka y crea el topic de ejemplo:

```bash
bin/kafka-topics.sh --create \
  --topic quickstart-events \
  --bootstrap-server localhost:9092
```

## 4. Escribir eventos

Inicia el productor de consola:

```bash
bin/kafka-console-producer.sh --topic quickstart-events --bootstrap-server localhost:9092
```

Escribe una línea por evento. Cada Enter crea un mensaje nuevo.

Ejemplo:

```text
hola kafka
evento 2
evento 3
```

Para salir, usa `Ctrl-C`.

## 5. Leer eventos

En otra terminal, abre el consumidor:

```bash
bin/kafka-console-consumer.sh --topic quickstart-events --from-beginning --bootstrap-server localhost:9092
```

Verás los mensajes que escribiste en el productor.

## 6. Probar Kafka Connect

La guía también muestra un flujo con `connect-test`:

```bash
more test.txt
bin/connect-standalone.sh config/connect-standalone.properties \
  config/connect-file-source.properties \
  config/connect-file-sink.properties
```

Después puedes revisar la salida con:

```bash
more test.sink.txt
```

Y también consumir directamente desde Kafka:

```bash
bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic connect-test --from-beginning
```

## 7. Probar Kafka Streams

La quickstart incluye un ejemplo de `WordCount` con Kafka Streams. La idea es leer desde `quickstart-events` y transformar el stream en una app Java/Scala.

Ejemplo conceptual:

```java
KStream<String, String> textLines = builder.stream("quickstart-events");
```

## 8. Detener Kafka

Cuando termines, para el servidor con `Ctrl-C` en la terminal donde corre Kafka.

## Resumen

| Paso | Comando principal |
|------|-------------------|
| Obtener Kafka | `tar -xzf ...` |
| Formatear storage | `bin/kafka-storage.sh format --standalone ...` |
| Iniciar servidor | `bin/kafka-server-start.sh config/server.properties` |
| Crear topic | `bin/kafka-topics.sh --create ...` |
| Producir mensajes | `bin/kafka-console-producer.sh ...` |
| Consumir mensajes | `bin/kafka-console-consumer.sh ...` |

---

**Siguiente**: [02-event-sourcing.md](02-event-sourcing.md)
