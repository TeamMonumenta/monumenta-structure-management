# Monumenta Structure Management
This plugin provides the backbone of Monumenta's respawning Point-of-Interest
system using FastAsyncWorldEdit (FAWE)

It was originally developed for [Monumenta](https://www.playmonumenta.com/), a
free community developed Complete-The-Monument MMORPG Minecraft server.

There are several core features provided here:
- The ability to save (`/savestructure`) and load (`/loadstructure`) large portions of the world very similarly to vanilla Structure Blocks (structure void functions the same way).
- The ability to designate certain areas as respawning, periodically loading the associated structure at that location
- Integration with ScriptedQuests so that user-defined actions will be run when a certain number of spawners have been broken
- Integration with Dynmap to display respawning structures on the map
- An optimized `/forceload addlazy` command which forceloads an area lazily without lagging the server

## Download
You can download the latest version of this plugin from [GitHub Packages](https://github.com/TeamMonumenta/monumenta-structure-management/packages).

## Maven dependency
```xml
    <repositories>
        <repository>
            <id>monumenta-structure-management</id>
            <url>https://raw.githubusercontent.com/TeamMonumenta/monumenta-structure-management/master/mvn-repo/</url>
        </repository>
    </repositories>

    <dependencies>
        <dependency>
            <groupId>com.playmonumenta</groupId>
            <artifactId>structures</artifactId>
            <version>9.3</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>
```
