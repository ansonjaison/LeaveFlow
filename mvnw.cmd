@REM ----------------------------------------------------------------------------
@REM Maven Wrapper startup batch script
@REM ----------------------------------------------------------------------------
@IF "%__MVNW_ARG0_NAME__%"=="" (SET "MVN_CMD=mvn.cmd") ELSE (SET "MVN_CMD=%__MVNW_ARG0_NAME__%")
@SET WRAPPER_JAR="%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar"
@SET WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain

@FOR /F "usebackq tokens=1,2 delims==" %%A IN ("%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.properties") DO (
    @IF "%%A"=="distributionUrl" SET DISTRIBUTION_URL=%%B
)

@SET JAVA_HOME_SET=%JAVA_HOME%
@IF "%JAVA_HOME_SET%"=="" (
    @SET JAVA_HOME=C:\Users\Anson Mathew Jaison\AppData\Local\Programs\Eclipse Adoptium\jdk-17.0.10.7-hotspot
)

@SET JAVA_CMD="%JAVA_HOME%\bin\java.exe"

@IF NOT EXIST %WRAPPER_JAR% (
    @ECHO Downloading Maven Wrapper...
    @%JAVA_CMD% -classpath "" org.apache.maven.wrapper.MavenWrapperDownloader %DISTRIBUTION_URL% %WRAPPER_JAR% 2>NUL
    @IF NOT EXIST %WRAPPER_JAR% (
        @ECHO [ERROR] Could not download maven-wrapper.jar. Trying direct maven call.
        @SET MVN_CMD=mvn.cmd
        @GOTO :END
    )
)

@SET MAVEN_PROJECTBASEDIR=%~dp0
@%JAVA_CMD% -classpath %WRAPPER_JAR% %WRAPPER_LAUNCHER% %*

:END
