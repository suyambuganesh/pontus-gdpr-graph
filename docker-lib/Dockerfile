FROM pontusvisiongdpr/pontus-jpostal-lib as jpostal
FROM pontusvisiongdpr/pontus-natty-lib as natty

FROM maven:3.6-jdk-8-alpine as builder
RUN apk add git

COPY --from=jpostal /root/.m2 /root/.m2
COPY --from=natty /root/.m2 /root/.m2


RUN git clone  --depth 1 --single-branch  --branch master https://github.com/pontusvision/pontus-gdpr-graph.git && \
     cd pontus-gdpr-graph && \
    mvn clean package install -DskipTests

RUN cd /root/.m2/repository && for i in *; do if [[ "com" == $i || "pv-gdpr" == $i ]]; then echo skip ; else rm -r $i; fi; done

FROM alpine

COPY --from=builder /root/.m2/ /root/.m2/


