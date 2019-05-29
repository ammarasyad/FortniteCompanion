# FortniteCompanion
A mini Fortnite client for Android.\
Download: [releases page](https://github.com/Amrsatrio/FortniteCompanion/releases)\
For Android 5.0 Lollipop (API 21) and up.

## Features
* View BR profile, account creation date, and other hidden account info
* View challenges
* Replace daily challenges
* View events and its leaderboard
* View stats of yourself or other player’s, with playlist breakdown
* View BR cosmetics/locker, with filtering, set/unset favorite, and marking as seen
* View BR Item Shop and purchase items, full display for unencrypted items
* Change leaderboard privacy setting

## Permissions used
* Contacts: to autocomplete the email field in the log in screen
* Network: required for this app to work

## FAQ
* **Q: Is this app totally safe? Will my account get banned for using this?**\
  A: Yes, this is safe. This app communicates only with Epic’s servers and nothing else. Not even a third party API or analytics service. When you’re logging in, your password is only sent to Epic. Tested for a month using my account and my account haven’t got banned. I’ve made this app with that thing in consideration (should only do things that the Fortnite client does).
* **Q: Does this app has scams written all over it?**\
  A: Absolutely not. If you have a bit of extra time to prove it, go on read the source code of the [ItemShopActivity](https://github.com/Amrsatrio/FortniteCompanion/blob/master/app/src/main/java/com/tb24/fn/activity/ItemShopActivity.java) and [PurchaseCatalogEntry](https://github.com/Amrsatrio/FortniteCompanion/blob/master/app/src/main/java/com/tb24/fn/model/command/PurchaseCatalogEntry.java). No third party URLs or information collecting chunk of codes there.
* **Q: Why the app is so huge for just like this? 50 megs for just like this, wow!**\
  A: Because in order for the Challenges, Locker, and Item Shop to be user friendly, the assets of the items are required. Currently, I don’t have a server to host those assets, so for now I decided to store them in the app. In the future, I will make the assets an optional download to reduce the app size although the items’ icons will not appear.
* **Q: The locker page is sophisticated. But why I still can’t change my skin using this app?**\
  A: I still haven’t figured out the behind the scenes of that thing, so it isn’t possible yet. If you’re a good reverse engineer you can help me find out how POST .../EquipBattleRoyaleCustomization works and make a pull request here.
* **Q: Will there be an iOS version of this?
  A: Not anytime soon. I’m a total noob on iOS app development. Plus, I don’t think theres many players with iPhone 6 or earlier which can’t run FN Mobile which means this isn’t in my priority list.

## To-do
* View all owned banners and apply them
* Apply cosmetic item
* Set Support a Creator code
* Set party assist
* Add creative map code
* View friends, their activity, and whispering them via Epic Chat
* [STW info in user friendly view](https://www.stormshield.one/save-the-world) (to see if there’s a 25-30 V-Bucks mission without opening big screen Fortnite)
* __Manually__ collect STW research points and daily rewards (Manually because if it’s automatic it breaks the purpose of the word "Daily" and I have no purpose to cheat the game using this app)

## Credits
* [@cryotus](https://www.instagram.com/cryotus/): for the app name and icon
* [u/easkate](https://www.reddit.com/user/easkate): for the [concept and idea of this app](https://www.reddit.com/r/FortNiteBR/comments/b5wlwg/fortnite_retail_row_app_a_ui_concept_for_a/)
* [Kysune](https://github.com/SzymonLisowiec), [Roberto Graham](https://github.com/RobertoGraham), and [Vrekt](https://github.com/Vrekt): for the network endpoints and its usage, from their libraries
* [Umodel](https://www.gildor.org/en/projects/umodel) by Gildor: for extracting/exporting the .uassets from the .pak's
* [John Wick Parse](https://github.com/SirWaddles/JohnWickParse) by [Waddlesworth](https://github.com/SirWaddles): for parsing the item data from the .uasset's
