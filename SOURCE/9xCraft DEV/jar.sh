#!/bin/bash
cd "$(dirname "$0")"
"/usr/lib/jvm/java-8-openjdk/bin/jar" cfm 9xCraftLauncher.jar manifest.txt NineXCraftLauncher*.class LogWindow*.class LocalResourceServer*.class AppletRunner*.class DarkMode*.class SkinWindow*.class ZoomSupport*.class *.png *.jpg *.mid
echo "Packing process has finished."
read -p "Press Enter to close..."
