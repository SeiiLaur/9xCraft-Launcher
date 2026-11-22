#!/bin/bash
cd "$(dirname "$0")"
"/usr/lib/jvm/java-8-openjdk/bin/javac" -source 1.5 -target 1.5 NineXCraftLauncher.java LogWindow.java AppletRunner.java LocalResourceServer.java
echo "Compilation finished."
read -p "Press Enter to close..."
