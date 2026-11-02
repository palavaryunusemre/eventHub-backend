@echo off
setlocal

echo ================================================
echo  EventHub - Kullanici Seed Scripti
echo ================================================
echo.

docker ps --format "{{.Names}}" | findstr /i "eventhub-postgres" >nul
if errorlevel 1 (
    echo [HATA] "eventhub-postgres" container'i calismiyor.
    echo Once asagidaki komutu calistirip Postgres'i ayaga kaldirin:
    echo     cd docker-stack ^&^& docker compose up -d
    exit /b 1
)

echo Kullanicilar ekleniyor...
docker exec -i eventhub-postgres psql -U postgres -d eventhub < "%~dp0seed-users.sql"

if errorlevel 1 (
    echo.
    echo [HATA] Kullanicilar eklenirken bir sorun olustu.
    exit /b 1
)

echo.
echo ================================================
echo  Islem tamamlandi. Giris bilgileri:
echo    admin@eventhub.local      / Admin123!  (ADMIN)
echo    organizer@eventhub.local  / Admin123!  (USER)
echo    user@eventhub.local       / Admin123!  (USER)
echo ================================================
pause
