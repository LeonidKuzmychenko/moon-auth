# ---------- BUILD STAGE ----------
FROM gradle:9.1-jdk21 AS build

WORKDIR /app

# 1. Копируем только файлы зависимостей (кешируем)
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# 2. Прогреваем зависимости
RUN gradle dependencies --no-daemon || true

# 3. Теперь копируем исходники
COPY src ./src

# 4. Сборка
RUN gradle bootJar --no-daemon

# ---------- RUN STAGE ----------
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8383

ENTRYPOINT ["java", "-Duser.timezone=Europe/Kyiv", "-jar", "app.jar"]