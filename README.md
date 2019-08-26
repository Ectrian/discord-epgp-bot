# discord-epgp-bot

A discord bot for managing your World of Warcraft guild's EPGP.

## Why use a Discord bot?

* Everything is tracked in Discord - there's no need for your raiders to sign up for anything or install an addon. You can even track EPGP for people not in your in-game guild.
* Complete transparency - everyone can freely access the EPGP transaction logs. Addon-based solutions generally do not have logs, or have severe synchronization issues with log data that allow a corrupt officer to cheat the system.
* Data integrity - addon-based solutions often have problems with officers overwriting eachothers changes.
* Alt-friendly - each player's characters are always linked together and their EPGP is shared. Addon-based solutions often have problems keeping these linkages synchronized between officers.
* Huge time savings for the entire officer team. Users can now ask the bot questions instead of you!
* Prevent mistakes - bot validates commands to help you avoid common mistakes such as granting a player double EP for the same raid.
* Keep player and officer notes free - addon-based solutions occupy these so you can't use them.

## Screenshots

![Screenshot](https://raw.githubusercontent.com/Ectrian/discord-epgp-bot/master/screenshots/screenshot1.png "Screenshot")
![Screenshot](https://raw.githubusercontent.com/Ectrian/discord-epgp-bot/master/screenshots/screenshot2.png "Screenshot")
![Screenshot](https://raw.githubusercontent.com/Ectrian/discord-epgp-bot/master/screenshots/screenshot3.png "Screenshot")
![Screenshot](https://raw.githubusercontent.com/Ectrian/discord-epgp-bot/master/screenshots/screenshot4.png "Screenshot")
![Screenshot](https://raw.githubusercontent.com/Ectrian/discord-epgp-bot/master/screenshots/screenshot5.png "Screenshot")
![Screenshot](https://raw.githubusercontent.com/Ectrian/discord-epgp-bot/master/screenshots/screenshot6.png "Screenshot")
![Screenshot](https://raw.githubusercontent.com/Ectrian/discord-epgp-bot/master/screenshots/screenshot7.png "Screenshot")
![Screenshot](https://raw.githubusercontent.com/Ectrian/discord-epgp-bot/master/screenshots/screenshot8.png "Screenshot")

## Set-Up

* Install MySQL 8+ and create a database.
  * Import `src/main/resources/schema.sql`, `src/main/resources/loot-classic.sql`.

* Create a Discord Application.
  * Go [here](https://discordapp.com/developers/applications).
  * Create a "New Application".
  * On the "Bot" tab, click "Add Bot".
  * Ensure "Public Bot" is disabled.
  * Set an avatar for your bot user (optional).
  * Copy and save the token. You will need it later.

* Add bot to your Discord server.
  * In your browser, click the "OAuth2" tab.
  * In the URL generator, select:
    * Scopes: bot
    * Permissions: send messages
  * Copy the generated URL and paste it into your browser.
  * Select the server you wish to add your bot to and click "Authorize". You must be a server administrator to perform this step.

* Create a config file (JSON). 
  * Refer to `com.epgpbot.config.Config` for schema.
  * *Note*: Server ID can be obtained from Discord via Server Settings > Widget > Server ID.
  * An example is provided below:

```json
{
  "game_guild_name": "Your Guild Name",
  "game_realm_name": "Your Realm Name",
  "game_armory_type": "NONE",
  "game_expansion": "VANILLA",
  "loot_method": "EPGP",
  "discord_bot_token": "XXXXXXXXXXXXXXXXXX",
  "discord_server_id": "XXXXXXXXXXXXXXXXXX",
  "discord_channels": ["epgp"],
  "discord_ranks_to_permissions": {
    "guild master": [
      "VIEW_EPGP",
      "MODIFY_EPGP",
      "MODIFY_PERMISSIONS",
      "IMPORT_EPGP",
      "ARMORY_SYNC",
      "AUDIT_EPGP",
      "DECAY_EPGP"
    ],
    "leadership": ["VIEW_EPGP", "MODIFY_EPGP"],
    "officer": ["VIEW_EPGP", "MODIFY_EPGP"],
    "core raider": ["VIEW_EPGP"],
    "raider": ["VIEW_EPGP"],
    "trial": ["VIEW_EPGP"],
    "social": ["VIEW_EPGP"]
  },
  "epgp_decay_rate": 10,
  "epgp_base_gp": 600,
  "epgp_decay_base_gp": 600,
  "database_host": "localhost",
  "database_port": 3306,
  "database_user": "httpd",
  "database_password": "httpd",
  "database_name": "epgpdb",
  "database_use_tls": false
}
```

* Run your bot.
  * `java com.epgpbot.transport.discord.DiscordBotLauncher config.json`

* Install the in-game helper addon (optional, recommended).
  * Copy `src/main/resources/RaidMemberList` into your WoW addons folder.
  * The RaidMemberList addon provides `/raidmembers`, which gives a copiable list of all characters in your raid group for bot commands.

## Back-Up and Restore

All state is stored in the SQL database. To back-up your EPGP data, simply export the SQL database. To restore your data, simply import the backup.

## Usage

The bot only responds to commands via direct message and in designated channels on your server (see `discord_server_id`, `discord_channels` in config).

Permissions are based entirely on each user's role(s) in your server. You specify a mapping of role names to permissions (see `discord_ranks_to_permissions` in config). Our recommendation is to provide the guild master with all permissions, officers with `MODIFY_EPGP, VIEW_EPGP`, and all other members with `VIEW_EPGP`.

To see a list of commands, simply send `!help`.

A few useful commands (for everyone):

* `!character.whois CharacterName` - Tells you which player a character belongs to.
* `!character.alts CharacterName` - Lists alts of a given character.
* `!item.link "Item Name"` - Links an item tooltip.
* `!item.search "Search Query"` - Searches for items.
* `!item.stats "Item Name"` - Displays statistics about a given item.
* `!roll min max` - equivalent to in-game's `/roll`.
* `!epgp [CharacterName]` - Displays a character's EP, GP, and Priority.
* `!epgp.standings` - Displays the EPGP standings, sorted by priority.
* `!epgp.totals` - Displays the EPGP standings, sorted by total EP earned.
* `!epgp.docked` - Displays the most docked players, sorted by number of penalties.
* `!epgp.log <all>` - Displays the EPGP transaction log for all players.
* `!epgp.log CharacterName` - Displays the EPGP transaction log for a specific player.
* `!epgp.officerlog CharacterName` - Displays the EPGP transaction log for all actions performed by a given officer.
* `!epgp.compare Character1 Character2` - Compares EPGP between listed characters.

## Usage (Officers)

You should create a player record for each distinct member of your guild:

`!player.add PlayerName`

You can then link the player record to the member's Discord account (optional, recommended):

`!player.link @DiscordName`

You can then link the member's characters to their player record:

`!player.character.add PlayerName CharacterName`

To see all of a player's characters:

`!player.character.list PlayerName`

If you are importing data from another system (such as an in-game addon or web service), you can use the following command to set a player's initial EPGP:

`!epgp.import CharacterName EP GP`

Otherwise, you may award EP/GP through:

* To dock (remove) EP for poor performance:
 * `!epgp.dock EP "Reason" Character1 Character2 ...`

* To award (add) EP for reasons other than raiding:
 * `!epgp.award.incentive EP "Reason" Character1 Character2 ...`

* To award (add) EP for raiding:
 * `!epgp.award.raid EP "Raid Name" Character1 Character2 ...`
  * *Note:* if a player shows up late or leaves early, you can just grant them less EP.

* To award (add) EP for showing up to raid but not getting invited:
 * `!epgp.award.standby EP "Raid Name" Character1 Character2 ...`

* To award (add) GP for receiving loot:
 * `!epgp.award.loot GP "Item Name" CharacterName`
 * Note: Item names are validated against the database. If the database does not contain the item name you specified, you can append `--add-loot` to the command to add the item to the database. Check for typos before adding additional items to the database to keep statistics accurate. You can use `!item.alias` to create short names for commonly looted items.
 * Generally, you would use `!epgp.compare` to determine who wins the item before using `!epgp.award.loot`. Our guild typically has players interested in an item `/roll` in-game, and then we type their names into `!epgp.compare` to determine the winner (the values rolled being irrelevant except in cases where priority is tied).

Additional Useful Commands:

* `!transport.whois @DiscordName` - Displays the player name associated with a given Discord account.
* `!charater.update CharacterName` - Allows you to manually set character info such as class, race, level, guild, specialization. Required for WoW Classic, since no Armory API is provided.
* `!epgp.decay` - Performs EPGP decay on all players. A single officer should run this command once per decay period (generally, once a week).

## FAQ

* **What versions of the game are supported?**

The bot is currently targeted for WoW Classic, but can used with retail or private servers running any version of the game. 

Use `game_expansion` in your config to let the bot know what version of the game you are targeting.

* **What does armory integration provide?**

Armory integration allows the bot to automatically fetch the guild roster and race, class, level, guild, and specialization information for each character from the armory.

WoW Classic **does not** have an armory, so `game_armory_type` should be set to `NONE` until support is added.

* **What values should I use for EP and GP?**

First, make sure you understand [how EPGP works](http://www.epgpweb.com/help/system).

The bot simply tracks EP and GP. It's up to your individual guild to set a policy for the actual values used (values are provided to the bot each time you run a command that modifies EP or GP). `epgp_decay_rate` and `epgp_base_gp` are specified in the configuration file.

My guild has had success with the following:

  * 600 base GP
  * 10% decay/week
  * +1000 EP/hr for raiding
  * -200 GP/item received
  * -500 EP/penalty (officer discretion)

While these numbers work well for us, they are not necessarily the best choice for your guild.

* **How can I undo an EPGP modification? **

Just repeat the command, but use a negative value.