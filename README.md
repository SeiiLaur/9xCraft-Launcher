______________________________

９ｘＣｒａｆｔ Ｌａｕｎｃｈｅｒ — Windows 98 Minecraft Launcher

_______________________________


Welcome to the official GitHub page for the 9xCraft Minecraft Launcher, Thanks for stopping by, I appreciate it.

This launcher is basically a graphical extension to simple command line strings used to run older Minecraft versions directly from the .jar files, very similar to Betacraft, which I heavily recommend you check out over at https://betacraft.uk/.

By they way, we now also have an official page at http://9xcraft.org/, go check it out!

The launcher itself is designed to run with Java 5 SE and above. It supports Windows 98 - 11, Linux, MacOS and PowerPC (seperate release). Ontop of that, the Minecraft instances you play fetch their resources entirely locally, which completely bypasses the issue where versions like Classic or Indev would face several problems when not connected to the internet, sometimes not even being able to load worlds without throwing a java.lang.NullPointerException.

To run a game of minecraft, you will need to provide your own .jar files. You can add said jars to the "Versions" folder which is located in the launcher's core directory. Alternatively, you can select "File" --> "Open .jar" in the top menu to point to the path of a Minecraft .jar client on your computer to open it directly without the need to move it into the Versions folder. 
The versions list can arrange versions by: "Pre_Classic" -> "4k" -> "Classic" -> "Indev" -> "Infdev" -> "Alpha" -> "Beta" -> "Release", if your .jar files start with these strings in their file name, you can use this hierarchy to keep all your versions easily accessible and organized.

You can find client jars on https://minecraft.wiki.

<img width="800" height="600" alt="image" src="https://github.com/user-attachments/assets/4f3c55ff-a5e4-462b-b93b-0967dd8eddb3" />


▓░░░░░░░░░░▓░░*Usage*░░▓░░░░░░░░░░░▓


In case you're interested in creating a desktop shortcut to the launcher, there's a "launcher.ico" and a "launcher_alt.ico" file inside the natives folder for you to use. Note that linux (and maybe other platforms) don't support this file extension for shortcuts, so you might have to convert them to a .png file first.

If you're wondering how to exit older versions without a "Quit" button in the menu when launching in fullscreen mode, simply press Alt+F4, sometimes it may take a while for the window to close, but it should eventually do just that.

A few more notes: Older Minecraft instances ARE NOT ABLE TO SAVE WORLDS when using Java 5 SE. This is fixed by using Java 6 SE or above. 

Another thing, every single version of the 9xCraft Launcher and its successor, 2kCraft Launcher, have been recompiled on February 18th to utilize an LWJGL-fork that runs on Microsoft™ Windows© 95 and above, it's called MiniJGL, you can check it out at https://github.com/Vulpovile/MiniJGL. This fixes absolute incompatibility with DOS-based Windows operating systems where previously it would get stuck on a white screen and throw a path error in the log console.


▓░░░░░░░░░░▓░░*About*░░▓░░░░░░░░░░░▓

I once again own a discord server, which you can join at: https://discord.gg/5YdPQm6hfm;
Huge thanks to Q0AL for helping me test everything and make a PowerPC build work! (Although I've cut ties with them long ago, therefore the current PPC release may be somewhat hit/miss)
Due to my strong personal convictions, I wish to stress that this launcher in no way endorses a belief in the occult.

▓▒▓▒▓▒▓▒▓▒▓-Exivaliant; 2026-▓▒▓▒▓▒▓▒▓▒▓
-This project is for archival purposes only and is not affiliated with Mojang Studios or Microsoft.
