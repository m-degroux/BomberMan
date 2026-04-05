@echo off
setlocal enabledelayedexpansion

:: Gestion des arguments (Variables par défaut si vides)
:: %1 est le premier argument (HOST), %2 le deuxième (PORT)
set HOST=%~1
if "%HOST%"=="" set HOST=localhost

set PORT=%~2
if "%PORT%"=="" set PORT=7777

echo ==========================================
echo Lancement du client SAE401 (JavaFX)
echo Serveur cible: %HOST%:%PORT%
echo ==========================================

:: Compilation des dépendances communes
:: Utilisation de 'call' pour éviter que le script ne s'arrête prématurément
call mvn install -pl common -am -DskipTests

:: Lancement de JavaFX avec les propriétés système pour le host et le port
call mvn -pl clientSide javafx:run -Dsae.server.host="%HOST%" -Dsae.server.port="%PORT%"

pause