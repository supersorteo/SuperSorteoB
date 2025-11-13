# Usar una imagen base de Eclipse Temurin Java 23
FROM eclipse-temurin:23-jdk


# Crear el directorio /uploads y asignar permisos

#RUN mkdir -p /app/uploads
#RUN chmod -R 777 /app/uploads
#RUN mkdir -p /app/videos
#RUN chmod -R 777 /app/uploads /app/videos
# Copiar el archivo JAR de la aplicación al contenedor

RUN mkdir -p /app/uploads /app/videos
RUN chmod -R 777 /app/uploads /app/videos

COPY target/rifa-0.0.1-SNAPSHOT.jar java-app.jar


# Exponer el puerto 8080
EXPOSE 8080

# Configurar el comando de entrada para ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "java-app.jar"]

