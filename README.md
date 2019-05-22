# FortniteCompanion
A mini Fortnite client for Android.

## Permissions used
* Contacts: to autocomplete the email field in the log in screen
* Network: required for this app to work

## Features
* View BR profile
* View events and its leaderboard
* View stats of yourself or other player's
* View BR cosmetics, with filtering and set/unset favorite
* View Item Shop, full display for unencrypted items
* Make Battle Royale Item Shop purchases
* Change leaderboard privacy setting

## To-do
* Add creative map code
* Apply cosmetic item
* Manage daily challenges
* [STW info in user friendly view](https://www.stormshield.one/save-the-world) (to see if there's a 25 V-Bucks mission without opening big screen Fortnite)
* __Manually__ collect STW research points and daily rewards (Manually because if it's automatic it breaks the purpose of the word "Daily" and I have no purpose to cheat the game using this app)
* Most importantly, finding out a way to mitm HTTP POST requests on the actual Fortnite client

## FAQ
* **Q: Why the app is so huge for just like this? 50 megs for just like this, wow!**
  A: Because in order for the Challenges, Locker, and Item Shop to be user friendly, the assets of the items are required. Currently, I don't have a server to host those assets, so for now I decided to store them in the app. In the future, I will make the assets an optional download to reduce the app size although the items' icons will not appear.
* **Q: The locker page is sophisticated. But why I still can't change my skin using this app?**
  A: I still haven't figured out the behind the scenes of that thing, so it isn't possible yet. If you're a good reverse engineer you can help me find out how POST .../EquipBattleRoyaleCustomization works and make a pull request here.

## Credits
* [Umodel](https://www.gildor.org/en/projects/umodel) for extracting/exporting the .uassets from the PAKs
* [John Wick Parse](https://github.com/SirWaddles/JohnWickParse) for parsing the item data from the .uassets
