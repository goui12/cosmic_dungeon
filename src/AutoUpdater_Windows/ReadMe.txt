✅ Windows: Run the Updater on Startup (BAT file)
1️⃣ Make sure these files are together
-----------------------------------
Step 1:
-----------------------------------
Put both files in the same folder (where the jar lives):

Update-CosmicDungeon.ps1
Update-CosmicDungeon.bat
cosmicdungeon-1.3.3.jar   (optional)

-----------------------------------
Step 2:
-----------------------------------

2️⃣ Open the Startup folder

On the Windows machine, press:
Win + R

Paste:
shell:startup
Press Enter

This opens the Startup folder for that user.
3️⃣ Add the updater to Startup

-----------------------------------
Step 3:
-----------------------------------

Right-click Update-CosmicDungeon.bat
Create shortcut
Drag the shortcut into the Startup folder

⚠️Do NOT move the original BAT file.


-----------------------------------
Step 4:
-----------------------------------

🧪 Test without rebooting

You can test instantly:
Double-click the shortcut in the Startup folder

No update → nothing visible
Update available → popup appears

🔍 If Windows blocks it once
On first run, Windows may warn about PowerShell scripts.

If so:
Right-click Update-CosmicDungeon.bat
Properties
Check Unblock
Apply → OK