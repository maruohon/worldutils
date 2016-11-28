World Utils
==============
World Utils includes some useful tools for manipulating Minecraft worlds while in-game.

Implemented things thus far:
* Chunk Wand - Allows changing entire chunks in the world, or just importing biomes from a different world. The alternate worlds that are used as the source are stored inside the worldname/alternate_worlds/ directory. The mod also keeps track of all the changes to chunks and stores that "database" in the same directory.
* Command: /worldutils entities
* Command: /worldutils printspawn
* Command: /worldutils tileticks

For more information (especially about the commands) and the downloads (compiled builds), see http://minecraft.curseforge.com/projects/world-utils

Compiling
=========
* Clone the repository
* Open a command prompt/terminal to the repository directory
* run 'gradlew build'
* The built jar file will be in build/libs/