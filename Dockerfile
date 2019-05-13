FROM ubuntu:18.04

RUN apt-get update && apt-get install -y openjdk-8-jdk nano less git maven
RUN mkdir /morphcsv
RUN mkdir /configs
RUN mkdir /morphcsv/output
RUN mkdir /queries
COPY . /morphcsv

RUN cd /morphcsv && mvn clean compile assembly:single && cp target/morph-csv-1.0-jar-with-dependencies.jar morph-csv.jar


CMD ["tail", "-f", "/dev/null"]
