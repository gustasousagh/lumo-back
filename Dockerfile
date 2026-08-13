# =============================================================================
# Lumo API — imagem de produção (Spring Boot / Java 21)
# =============================================================================

# --- build -------------------------------------------------------------------
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# gradle.properties fixa org.gradle.java.home num caminho da máquina de dev,
# que não existe aqui. O -D na linha de comando sobrescreve isso.
ENV GRADLE_OPTS="-Dorg.gradle.java.home=/opt/java/openjdk"

# Primeiro só os arquivos de build: com o código fora, a layer de dependências
# fica em cache e não rebaixa a cada commit.
COPY gradlew settings.gradle build.gradle gradle.properties ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies || true

COPY src src
RUN ./gradlew --no-daemon bootJar -x test \
    && cp build/libs/*.jar app.jar

# --- runtime -----------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -g 1001 -S spring && adduser -u 1001 -S spring -G spring

COPY --from=build --chown=spring:spring /app/app.jar app.jar

# Tudo que o usuário envia mora em /app/data: avatares em uploads/, e o acervo
# de música e livros em library/. MONTE UM VOLUME AQUI — sem isso o acervo
# inteiro some no próximo redeploy.
#
# As subpastas são criadas já na imagem, com a dona certa, e não só em runtime.
# O motivo: quando o Docker cria um volume NOMEADO vazio, ele copia o conteúdo
# que existe nesse caminho na imagem, ownership incluído. Assim o volume nasce
# gravável pelo usuário 'spring' (uid 1001). Se essas pastas só nascessem em
# runtime, o primeiro upload num volume novo dependeria da permissão do ponto de
# montagem — que num bind mount pertence ao root, e o upload falharia.
RUN mkdir -p /app/data/uploads \
             /app/data/library/music \
             /app/data/library/books \
             /app/data/library/covers \
    && chown -R spring:spring /app/data
VOLUME ["/app/data"]

USER spring
EXPOSE 8080

# MaxRAMPercentage deixa a JVM respeitar o limite de memória do container
# em vez de enxergar a RAM do host inteiro.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
