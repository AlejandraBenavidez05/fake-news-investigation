# ================================
# STAGE 1: DEPENDENCIES
# ================================
FROM eclipse-temurin:17-jdk-alpine AS deps

WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

RUN chmod +x mvnw
RUN ./mvnw -B dependency:go-offline

# ================================
# STAGE 2: BUILD
# ================================
FROM deps AS build

WORKDIR /app

COPY src ./src

RUN ./mvnw -B package -DskipTests

# ================================
# STAGE 3: RUNTIME (DISTROLESS)
# ================================
FROM gcr.io/distroless/java17-debian11

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","/app/app.jar"]