FROM openjdk:8-jre-alpine

ENV APPLICATION_USER kweb
RUN adduser -D -g '' $APPLICATION_USER

EXPOSE 16097

RUN mkdir /app
RUN chown -R $APPLICATION_USER /app

USER $APPLICATION_USER

COPY ./build/libs/GitHubStatismics.jar /app/GitHubStatismics.jar
WORKDIR /app

CMD ["java", "-server", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseCGroupMemoryLimitForHeap", "-XX:InitialRAMFraction=2", "-XX:MinRAMFraction=2", "-XX:MaxRAMFraction=2", "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=100", "-XX:+UseStringDeduplication", "-jar", "GitHubStatismics.jar"]