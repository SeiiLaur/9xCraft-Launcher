#!/bin/bash
cd "$(dirname "$0")"
rm -f 9xCraftLauncher.jar
rm -f NineXCraftLauncher*.class
rm -f LogWindow*.class
rm -f AppletRunner*.class
rm -f LocalResourceServer*.class
rm -f DarkMode*.class
rm -f SkinWindow*.class
rm -f ZoomSupport*.class
echo "Finished deleting files."
read -p "Press Enter to close..."
