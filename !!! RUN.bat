@echo off
title CSGF Launcher
echo Do not close this window
start "" /MIN %~dp0\Insomnia.exe 
java CrossSpeciesGeneFinder
taskkill /IM Insomnia.exe > NUL