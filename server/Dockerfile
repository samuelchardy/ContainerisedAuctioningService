FROM java:8-jdk-alpine AS buildtime

WORKDIR /usr/src

COPY . ./

RUN apk --no-cache add make wget \
 && make

FROM java:8-jdk-alpine AS runtime

WORKDIR /usr/src

COPY /src/*.txt ./src/
COPY --from=buildtime /usr/src/out/*.class ./app/
COPY --from=buildtime /usr/src/libs/*.jar ./libs/

EXPOSE 8080

CMD ["java", "-cp", "./libs/*:./app", "Server"]
