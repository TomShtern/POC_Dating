@echo off
REM POC Dating - Start All Microservices (Windows)
REM This script starts all backend services in separate command windows

setlocal EnableDelayedExpansion

echo ==================================================
echo   POC Dating - Starting All Microservices
echo ==================================================
echo.

REM Get script directory
set "SCRIPT_DIR=%~dp0"
set "SCRIPT_DIR=%SCRIPT_DIR:~0,-1%"

echo Checking prerequisites...

REM Check Java
where java >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Java is not installed. Please install Java 21 or higher.
    pause
    exit /b 1
)
echo [OK] Java found

REM Check Maven
where mvn >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Maven is not installed. Please install Maven 3.8 or higher.
    pause
    exit /b 1
)
echo [OK] Maven found

REM Check PostgreSQL
where psql >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [WARNING] PostgreSQL client (psql) not found.
    echo           Make sure PostgreSQL is installed and databases are set up.
) else (
    echo [OK] PostgreSQL client found
)

echo.
echo ==================================================
echo   Database Setup Check
echo ==================================================
echo.
echo Make sure you have run the database setup script:
echo   psql -U postgres -f "%SCRIPT_DIR%\setup-databases.sql"
echo.
set /p REPLY="Have you set up the databases? (y/n) "
if /i not "%REPLY%"=="y" (
    echo Please run the database setup script first, then try again.
    pause
    exit /b 1
)

echo.
echo ==================================================
echo   Starting Services
echo ==================================================
echo.

REM Start services in new windows
echo Starting user-service on port 8081...
start "user-service" cmd /k "cd /d "%SCRIPT_DIR%\user-service" && mvn spring-boot:run"
timeout /t 3 /nobreak >nul

echo Starting match-service on port 8082...
start "match-service" cmd /k "cd /d "%SCRIPT_DIR%\match-service" && mvn spring-boot:run"
timeout /t 3 /nobreak >nul

echo Starting chat-service on port 8083...
start "chat-service" cmd /k "cd /d "%SCRIPT_DIR%\chat-service" && mvn spring-boot:run"
timeout /t 3 /nobreak >nul

echo Starting recommendation-service on port 8084...
start "recommendation-service" cmd /k "cd /d "%SCRIPT_DIR%\recommendation-service" && mvn spring-boot:run"
timeout /t 3 /nobreak >nul

echo Starting vaadin-ui-service on port 8090...
start "vaadin-ui-service" cmd /k "cd /d "%SCRIPT_DIR%\vaadin-ui-service" && mvn spring-boot:run"

echo.
echo ==================================================
echo   All Services Started!
echo ==================================================
echo.
echo Services are starting up. This may take a minute...
echo.
echo Service URLs:
echo   User Service:            http://localhost:8081/api
echo   Match Service:           http://localhost:8082/api
echo   Chat Service:            http://localhost:8083/api/chat
echo   Recommendation Service:  http://localhost:8084/api
echo   Vaadin UI:               http://localhost:8090
echo.
echo Health Check URLs:
echo   User Service:            http://localhost:8081/actuator/health
echo   Match Service:           http://localhost:8082/actuator/health
echo   Chat Service:            http://localhost:8083/actuator/health
echo   Recommendation Service:  http://localhost:8084/actuator/health
echo   Vaadin UI:               http://localhost:8090/actuator/health
echo.
echo To stop services: Close the command windows or use Ctrl+C
echo.
echo Happy coding! 🚀
echo.
pause
