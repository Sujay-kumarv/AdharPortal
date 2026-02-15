@echo off
echo Starting New Adhar Development Environment...

echo Starting Docker Containers...
docker-compose up -d
if %errorlevel% neq 0 (
    echo Docker Start Failed! Please ensure Docker Desktop is running.
    pause
    exit /b
)

echo Docker Containers Started.

start "New Adhar Backend" cmd /c "cd backend && d:\Adhar_Project\tools\apache-maven-3.9.6\bin\mvn.cmd spring-boot:run || pause"
start "New Adhar Frontend" cmd /c "cd frontend && npm run dev || pause"

echo Backend and Frontend have been launched in separate windows.
