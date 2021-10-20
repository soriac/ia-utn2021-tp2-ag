Este repositorio contiene el código fuente y los datos de salida del trabajo práctico 2 para la materia Inteligencia Artificial de la Universidad Tecnológica Nacional Facultad Regional Buenos Aires (UTN FRBA).

# Cómo generar un build
Requisitos: Java 11 o superior.

```bash
./gradlew jar
```

Esto generará un archivo .jar en la carpeta `build/libs`.
También adjuntamos un build en la [sección de releases](https://github.com/soriac/ia-utn2021-tp2-ag/releases).

# Cómo correr el programa

```bash
java -jar build/libs/ia_tp2_jenetics-1.0.jar
```

Esto correrá 8 experimentos en 8 hilos en paralelo. Para ajustar esto ver el `executor` de la función `main` del archivo `Solver.kt`
