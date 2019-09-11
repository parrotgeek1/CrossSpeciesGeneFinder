#!/bin/sh
javac --release 7 -Xlint:-options src/*.java
rm -f CSGF.jar
cd src
jar cvfe ../CSGF.jar CrossSpeciesGeneFinder `find . -type f -name '*.class' -or -name '*.wav'`
cd ..
find . -name '*.class' -type f -delete
find . -name '*\~' -type f -delete