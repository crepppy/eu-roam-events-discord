# EU Roam Events Discord Bot
[![Discord](https://img.shields.io/discord/769946284970606622?label=discord)](https://discord.gg/fqyr8Ez)
[![Website](https://img.shields.io/website?down_message=offline&up_message=online&url=https%3A%2F%2Feure.jack-chapman.com)](https://eure.jack-chapman.com)

<a href="https://discord.gg/fqyr8Ez"><img align="right" src="https://github.com/crepppy/eu-roam-events-discord/raw/main/frontend/src/assets/eure.png" width=25%></a>
EU Roam Events is a private 'roam simulator' server which hosts roam events every Sunday. Each team is given all the supplies they need and are left to roam on modified maps and engage in combat with one another.

## Features
* Linking discord and steam accounts
* Scheduled announcements when event signups have begun
* The creation of teams during the signup period including role assignment and substitutions
* Automatic whitelisting of teams on a rust server
* Creation of a spreadsheet after an event has ended with player and team statistics
* Web app to track the running event leaderboard in real time

## Roadmap
* Archive previous events
* Track player leaderboards during event
* Announce winner of each event and assign winner roles
* Continuous deployment
* Make the bot available for other servers

## Usage
There are currently no stable releases of the bot so in order to run the bot yourself you will need to compile the bot from the source.

The bot does not currently support discord servers other than EURE and so **running the bot at this time does not serve any purpose** unless the source code is modified

### Requirements
* A stable internet connection
* [JDK 11+](https://jdk.java.net/)
* [npm v10+](https://docs.npmjs.com/downloading-and-installing-node-js-and-npm)

If you have not done so already, the [Vue.js CLI](https://cli.vuejs.org/guide/installation.html) should be installed in order to build the frontend. You can do so by running:
```shell
npm install -g @vue/cli
```

### Configuration
If you have not already created a discord bot, one needs to be created in the [discord developer dashboard](https://discord.com/developers/applications). Before running the application, a `config.toml` needs to be created in the project directory in order to configure the discord bot and backend webserver. Your config should look similar to:
```toml
[discord]
token = "Nzcyrrq7qWsfAxGnNya4t2kN.DYimr9.nzhjs9vfrnbqbAhJwqv11MrM85s"

[database] # Currently only MySQL / MariaDB are explicitly supported
host = "127.0.0.1"
port = 3306
user = "username"
pass = "password"
database = "eure"

[server]
port = 8001 # The port the webserver is hosted at
root = "https://eure.jack-chapman.com" # OPTIONAL: The callback address for steam authentication
password = "1Bl*8U8Gv8F%fKhC" # The credentials used for priviliged endpoints

[rust]
ftp = "ftp://user:pass@127.0.0.1:21/rust" # OPTIONAL: FTP link directly to rust server directory
```

Furthermore, in order for the web app to track each team's stats, the plugin [RoamEventsTracking.cs](RoamEventsTracking.cs) should be added to your rust server. Due to the nature of the plugin, [**you must disable plugin sandboxing**](https://umod.org/guides/oxide/disabling-plugin-sandboxing) in order for it to run. This plugin also has a simple config that should look similar to:
```json
{
  "WebsocketAddress": "ws://127.0.0.1:8001/api/game",
  "WebsocketCredentials": "1Bl*8U8Gv8F%fKhC"
}
```
The credentials here should match the password specified in the `config.toml`

### Running
Once the application and rust plugin have been configured, running the bot is as easy running the following command:
```shell
## On Linux
./gradlew run

## On Windows
gradlew.bat run
```
This will start both the discord bot and the web server. To ensure the bot is working run the `/ping` command in discord or navigate to the webserver address in a browser.

### Starting an event
Starting and ending events is controlled by making requests the webserver. In the future this functionality will also be available through bot commands. Examples of each endpoint can be found in the snippet below. 

***Note:*** Any request made that can control the event requires [basic authorization](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Authorization) using any username and the password set in `config.toml`.

In most scenarios, you will want to schedule these requests in order to control the event. On Linux systems this can be done using a cron job:
```shell
SHELL=/bin/bash
MAILTO=""
PATH=/bin:/usr/bin:/usr/local/bin:/sbin:/usr/sbin:/usr/local/sbin

# Start signups on a Wednesday
00 18 * * wed curl -H "Authorization: Basic OjFCbCo4VThHdjhGJWZLaEM=" -H "Content-Type: application/json" -d '{"teamSize": 10, "maxTeams": 10}' http://127.0.0.1:8001/api/signups

# Stop signups and whitelist players 15 minutes before event on Sunday
45 18 * * sun curl -H "Authorization: Basic OjFCbCo4VThHdjhGJWZLaEM=" -X "POST" http://127.0.0.1:8001/api/event

# End the event 2 hours after it starts on a Sunday
00 21 * * sun curl -H "Authorization: Basic OjFCbCo4VThHdjhGJWZLaEM=" -X "DELETE" http://127.0.0.1:8001/api/event

# Delete all teams the monday following
00 18 * * mon curl -H "Authorization: Basic OjFCbCo4VThHdjhGJWZLaEM=" -X "DELETE" http://127.0.0.1:8001/api/teams
```