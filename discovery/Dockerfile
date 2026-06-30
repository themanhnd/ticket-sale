#Dùng runtime Java 21.
FROM eclipse-temurin:21-jre
#Tạo/thay đổi thư mục làm việc trong container.
WORKDIR /app
#Copy jar build từ Maven vào image.
COPY target/discovery-1.0-SNAPSHOT.jar app.jar
#Chạy app khi container start.
ENTRYPOINT ["java", "-jar", "/app/app.jar"]