set JAVA_HOME=C:\Program Files\Java\jdk-11.0.3
call gradlew bootJar
"%JAVA_HOME%\bin\java.exe" -jar build\libs\token-server-0.0.1-SNAPSHOT.jar