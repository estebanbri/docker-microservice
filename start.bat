mvn clean install
docker build --tag estebanbri/docker-app:1.0 .
docker run --name rest-app -d -p 8081:8080 -e SERVER_PORT=8080 -e EUREKA_URI=http://eureka-server:8761/eureka --network mi_red estebanbri/docker-app:1.0
docker run --name rest-app-b -d -p 8082:8080 -e SERVER_PORT=8080 -e EUREKA_URI=http://eureka-server:8761/eureka --network mi_red estebanbri/docker-app-b:1.0
