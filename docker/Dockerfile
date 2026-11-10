FROM eclipse-temurin:17-jdk AS builder
WORKDIR /application
ARG ARTIFACT_NAME
COPY ${ARTIFACT_NAME}.jar application.jar
RUN java -Djarmode=layertools -jar application.jar extract

FROM eclipse-temurin:17-jre

# Utilisateur non privilégié (constat C7)
RUN groupadd --system app && useradd --system --gid app --no-create-home app

WORKDIR /application

ARG EXPOSED_PORT
EXPOSE ${EXPOSED_PORT}

ENV SPRING_PROFILES_ACTIVE=docker

COPY --from=builder --chown=app:app /application/dependencies/ ./
# contournement btrfs (conservé du fichier d'origine)
RUN true
COPY --from=builder --chown=app:app /application/spring-boot-loader/ ./
RUN true
COPY --from=builder --chown=app:app /application/snapshot-dependencies/ ./
RUN true
COPY --from=builder --chown=app:app /application/application/ ./

USER app
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
