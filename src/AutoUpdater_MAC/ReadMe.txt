To make the "update-cosmic-dungeon.sh" executable, I need to run:
"chmod +x update-cosmic-dungeon.sh"
from its directory.


------------------------------------------

PLIST:
For on-start-up updating, I need to move com.cosmicdungeon.updater.plist to ~/Library/LaunchAgents/
⚠️NOTE:  Replace /FULL/PATH/TO/update-cosmic-dungeon.sh with the actual path.

⚠️ 1. If you edit the plist later
You must reload it:

launchctl unload ~/Library/LaunchAgents/com.cosmicdungeon.updater.plist
launchctl load   ~/Library/LaunchAgents/com.cosmicdungeon.updater.plist

⚠️ 2. First popup permission

The first time the script shows a popup, macOS may ask:

“Terminal wants to control Finder / display notifications”

Just click Allow once — it will not ask again.

---------------------------------------

🧪 Test without rebooting

You can trigger it manually:

launchctl kickstart -k gui/$(id -u)/com.cosmicdungeon.updater