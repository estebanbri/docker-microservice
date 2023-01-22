# Docker microservice example

## Importante: 
Acordate de anotar con @LoadBalanced al bean del RestTemplate para que funcione las llamadas entre microservicios
por el valor de "spring.application.name" en vez de hacerla via "ip:port", para probar que esta balanceando la carga correctamente
levanta 1 instancia de docker-app y 2 instancias de docker-app-b y desde la instancia de docker llama desde el browser el endpoint
/call-docker-app-b y vas a visulizar hiteando varias veces que te va a dar el output de la IP de las dos intancias de docker-app-b
cuando va balanceando la carga.

## Porque es importante setear una network para la comunicacion de los container/microservicios
Fijate que nosotros en vez de pasarle la IP del eureka server en el docker run de los eureka client le pasamos el nombre del container eureka-server
> -e EUREKA_URI=http://eureka-server:8761/eureka"

¿Como lo hacemos?
Gracias a definir una user-defined network podés usar el nombre del container en vez de la IP, para que la comunicacion intra-container
sea por nombre de container y no por IP, en nuestro caso creamos previamente una network llamada mi_red.

## Crear una user defined network
> docker network create mi_red

Recordando que la network por defecto autoasignada a cada container es BRIDGE, dicha network lo que hace es autogenerar una 
ip nueva random por container levantado, es decir por eso no podes confiarte de manejarte via IP porque si levantas un container A, docker
te va a asignar una IP y si lo frenas y levantas de nuevo al container A despues de un rato docker puede asignarle otra IP distinta a la inicial.

Teniendo en cuenta lo del parrafo anterior, una network user-defined por debajo usa una BRIDGE pero ademas te va a permitir 
que los containers dentro de docker (comunicacion intra-container) (ojo no por fuera es decir si queres acceder desde chrome
a un container tenes que hacerlo via la IP del HOST y el port definido en el mapeo de puertos con -p) se puedan comunicar
por nombre de container en vez de por IP.

Recorda que los containers que estan en redes distintas no se pueden comunicar. Es decir si tenes dos containers A  y B
dentro de una user-defined network llamada mi_red no se van a poder comunicar de ninguna manera con un container C 
que se esta ejecutando sobre la network default Bridge de docker.

## Eureka
Primero configura el eureka server y ejecuta un container
> docker run --name eureka-server -d -p 8761:8761 --network mi_red estebanbri/eureka-server:1.0

## Creacion de una imagen 
> docker build --tag estebanbri/docker-app:1.0 .

El nombre de la imagen es estebanbri/docker-app y el tag es 1.0
Nota importante: el ultimo parametro del comando es el llamado Build-Context, este build context que en el comando
al especificar "." significa el directorio actual en el cual se encuentra el archivo Dockerfile, indica los
archivos o carpetas includias en el build context van a ser enviados al Docker Daemon para poder generar la imagen
al momento de hacer el docker build.
Es muy importante setear el Build-Context correctamente, porque si lo especificamos mal por ejemplo
en el Dockerfile unicamente usamos como en este caso un COPY del .jar y es decir no necesitamos ni el codigo fuente,
ni los .class, etc. Entonces vamos a estar enviando al Docker Daemon archivos y carpetas que ni si quiera
son necesarios para generar la imagen y luego van a guardarse en la imagen final ocupando mas espacio dicha imagen.
Solución 1: usa un archivo .dockerignore para excluir todas las carpetas y archivos que no son necesarias de ser enviados
al DockerDaemon para que genere mi imagen final.
Solución 2: En guru lo que hacemos es
1) En el server de Jenkins generamos el .jar con el el mvn clean install
2) En el server de Jenkins via ssh creamos una carpeta component-version en el server de desarrollo para el componente y tag en especifico
3) En el server de jenkins copiamos y enviamos por la red via scp las unicas dos cosas necesarias segun nuestro Dockerfile que son el: el archivo Dockerfile y el .jar
4) En el server de desarrollo ejecutamos docker build --tag image_name /path/to/component-version
Como ves no usamos .dockerignore porque definimos un BuildContext /path/to/component-version que es una carpeta que tiene el Dockerfile y el .jar y ningun archivo o carpeta de mas que sobre.

## Ejecucion de container
> docker run --name rest-app -d --rm -p 8081:8080 -e SERVER_PORT=8080 -e EUREKA_URI=http://eureka-server:8761/eureka --network mi_red estebanbri/docker-app:1.0

Nota 0: NUNCA DE LOS JAMASES LE DES NOMBRES CON GUION BAJO A LOS NOMBRES DE LOS CONTAINERS porque si usas una user-defined network (--network mi_red) cuando te comuniques de un contenedor a otro el tomcat del contenedor destino te va a chillar y te va a tirar un 400 error que dice: java.lang.IllegalArgumentException: The character [_] is never valid in a domain name.
Nota 1: para ejecutar multiples containers de una misma imagen es obligatorio que sean distintos tanto los nombres de los container y los ports.
Nota 2: docker run no te permite crear y ejecutar un contenedor llamado por ej rest-app si ya existe un contenedor ejecutandose inclusive stopped. (solucion: si esta stopped hacele un docker rm container_name para eliminarlo) 
Nota 3: El flag -rm es para que se borre el container automaticamente una vez que se detiene el contenedor.

## Spring boot tiene un plugin de maven para que te genere la imagen automaticamente dentro del lifecycle por defecto sin necesidad de usar comandos docker
./mvnw spring-boot:build-image -Dspring-boot.build-image.imageName=springio/gs-spring-boot-docker

## Dockerfile instructions

### EXPOSE
La instruccion EXPOSE realmente no publica el puerto. Funciona como un tipo de documentación entre la persona
que contruyó la imagen y la persona que ejecuta un contenedor de dicha imagen sobre que puertos
están destinados a ser publicados. Para realmente publicar el puerto cuando se ejecute un container usa el flag -p
sobre el comando docker run para publicar uno o mas puertos, o el flat -P para publicar todos los puertos exposed.
Por defecto, EXPOSE asume TCP, es decir esto es equivalente
EXPOSE 80/tcp
EXPOSE 80
Si queres UDP:
EXPOSE 80/udp

### WORKDIR
En criollo lo usas para definir el directorio de trabajo es decir siguiendo el diseño de mi Dockerfile
el path donde vas a querer que se ubique el .jar dentro del sistema de archivos dentro de container, es decir
al .jar lo va a meter en una carpeta /app. 

### COPY versus ADD
Sirven para lo mismo copiar contenido del workspace para generar mi imagen pero ADD puede ademas manejar tar y URL remotas.
Si no necesitas ni manejar ni tar ni url remotas, usa COPY que es mas performante.

### CMD versus ENTRYPOINT
Both CMD and ENTRYPOINT instructions define what command gets executed when running a container. There are few rules that describe their co-operation.
- Dockerfile should specify at least one of CMD or ENTRYPOINT commands.
- ENTRYPOINT should be defined when using the container as an executable.
- CMD should be used as a way of defining default arguments for an ENTRYPOINT command or for executing an ad-hoc command in a container.
- CMD will be overridden when running the container with alternative arguments.

## Que son las imagenes con REPOSITORY=<none> y TAG=<none>
Aparecen cuando vos creas una imagen que ya existia es decir ya existia su nombre y tag previamente y cuyo contenido 
de la imagen nueva cambió respecto a la anterior, entonces lo que hace docker no la pisa con la nueva version sino
que te la deja "dangling" con nombre de imagen y tag <none>.
Solucion: docker system prune (ojo es peligro porque te borra incluso las network user defined en caso de que esten todos removidos los container que la usan)

## Don't use a random port for the Spring app (No uses server.port=0)
1) Para el caso de que tu entorno de trabajo sea 100% docker es decir en local las app las vas a ejecutar dentro de contenedores docker
Don't use a random port for the Spring app. Use the default port and have Docker expose that as whatever port you like.
Don't use a random port for the Spring app. Specify it via an environment variable and have Docker provide that.

Containers are isolated and independent. You can have N number of Container instances of your Spring Boot app running, all using the same port INTERNALLY
If you use docker to host your spring application, just don't use a random port! Use a fixed port because every container gets his own IP anyway so every service can use the same port. This makes life a lot easier.
For local starts (explicado debajo) via maven or for example the command line have a dedicated profile that uses randomized ports so you don't have conflicts (but be aware that there are or have been a few bugs surrounding random ports and service registration)

2) Para el caso de que tu entorno de trabajo (como tenemos en guru) que las componentes los ejecutamos en local sin docker
La solucion para estos casos es setear a la property server.port=${SERVER_PORT:0} esto te va a permitir
cuando corras la aplicacion dentro de un container con docker run pasarle -e es decir como variable de entorno el valor de server port
y para los casos que estes desarrollando en local sin docker le va a asignar un puerto random a la app asi nunca}
se van a pisar los puertos de distintas apps.

## Como probar comunicacion entre un contenedor A y otro contenedor B
Primero ejecutamos bash (recorda que bash hay que instalarlo si usas alpine como imagen base) sobre el container origen,
> docker exec -it rest-app /bin/bash  

Dentro de dicho contenedor hacemos un wget (alternativa a curl a diferencia que ya viene instalado en alpine)
Nota: supone que ambos contenedores estan sobre la network bridge por defecto, entonces docker le asigna automaticamente  las IP a los containers asi:
rest-app le asignó ip 172.17.0.2 y rest-app2 le asignó ip 172.17.0.3

> 1463e8f7853b:/app# wget 172.17.0.3:8081  
> Connecting to 172.17.0.3:8081 (172.17.0.3:8081)  
> saving to 'index.html'  
> 'index.html' saved  

Como ves la comunicacion via la network bridge por defecto hay que hacerla via IP's de los containers, por eso nace
las user defined networks, para estas ultimas networks docker habilita un servidor DNS para resolucion de nombre-container-IP
Entonces ese wget lo reemplazarias asi si usarias una network user defined:

> 1463e8f7853b:/app# wget rest-app2:8081  
> Connecting to rest-app2:8081 (172.17.0.3:8081)  
> saving to 'index.html'  
> 'index.html' saved  

## Docker compose
- Para buildear y generar las imagenes:
> docker-compose build
  
- Para que levantar los containers:
> docker-compose up

- Para buildear y generar las imagenes y levantar los containers en unico comando:
> docker-compose up --build
