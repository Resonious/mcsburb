Minecraft SBURB
===============
An attempt to recreate the process of SBURB (from [Homestuck](http://mspaintadventures.com/)) in Minecraft.
I will not describe the system in detail - instead I will refer those of you who aren't familiar
[here](http://mspaintadventures.wikia.com/wiki/Sburb).

The current game "flow"
-----------------------
1. Log into server as a new player (not necessarily as a new player, but makes somewhat more sense to do it that way).

2. Execute `/homestuck` command. This will generate a random house for you at a random location, and then warp you and set your spawn point there. If you'd prefer to build your own house, execute `/homestuck nohouse`. It is possible to serialize custom houses to add to the "pool", but not documented at the moment. (Ask me if you're actually curious). 

3. You will be given a SBURB disc. Locate your OpenComputers computer (see Dependencies) and insert the disc!

4. Use OpenComputers computer to become a client or server to another player. Remember, if all players already have a client and server, no more can join!

5. If you are a client, go kill stuff to collect Build Grist!

6. If you are a server, use your clients build grist to expand/modify their house.

7. Once the client collects 100 build grist, have the server place a Cruxtruder somewhere.

8. As of right now, the Cruxtruder will send the client, and their house directly to The Medium.

9. Within The Medium, there are portals above the house that lead to various places (including other players' mediums). There are also return nodes scattered throughout in case you get lost (and lucky).

That's about it for now. Not much actual fun stuff, but definitely a fairly solid platform.

Gotchas/Bugs
------------
* If a player logs out while their house or medium is being generated, they _should_ be okay (warped when they log back on). If a player _dies and does not respawn_ while their house or medium is generating, I have no idea what happens. So try not to do anything completely insane.

* When you activate the Cruxtruder, there is a delay before actually warping to The Medium (slow-ass server is generating it). Currently, MCSBURB saves the house _as soon as you click the Cruxtruder_. This means anything you do to your house during the inbetween time will not carry over. So anything you put into a chest will be gone, and anything you grab from a chest will be duped (enjoy).

What isn't implemented (but perhaps could be)
---------------------------------------------
* The whole Paraphernalia Registry. As of right now, there is a (slightly janky) Cruxtruder that you can pick up, and that will warp you directly to The Medium on use.

* Much option for house extension. Right now it's just a block, and a stair block.

* The Battlefield, Derse, and Prospit (totally planned, will get back to that later).

* Alchemiter (part of paraphernalia registry, but noteworthy because it would be way cool).

* GristTorrent (lol).

* Sylladex. This was originally one of the first things I started on, but quickly discovered that messing with the main inventory is a PAIN.

* Relavent mobs. Currently the only entities registered in this mod are portals and return nodes.

* Interesting stuff in The Medium. At the moment this mod depends on MystCraft for generating The Medium (more on that later) and does not add anything particularly interesting.

* Anything class/aspect related.

* Medium names (land of ?? and ??).

Dependencies
------------
So, there are a couple of things that I decided were already being done well, so why reinvent the wheel?

1. OpenComputers - Computers are a large part of SBURB, and I figured why invent my own crappy computer block and interface when there are mods out there that are 100% dedicated to _just_ providing computers. And OpenComputers happens to be very extensible and 3rd party friendly.

2. MystCraft - This is I guess more of a temporary solution. In the end, I would like to have more control over how The Medium is generated, but integrating MystCraft took way less time than both learning to do my own world generation AND implementing enough variety in the generation to make all Medium worlds unique.
Now because these are not mine, I will not provide them with this mod. I will instead advise you to seek them out yourself.

3. Reflections - What? [This](https://code.google.com/p/reflections/) java library. Why? Well, when the project was new, I got super sick of `someTypeOfRegistry.register(classOf[EveryGoddamnThing], blah, blah)` and thought to myself: it would be great if I could just loop through every subclass of some abstract and call the registry function just once. So yeah.

I should just use the gradle.build to download dependencies, or make a script, but ... maybe later.

In case you plan to peek at the source code
-------------------------------------------
First thing is that some things are kind of weirdly organized. I did some experimentation on code organization early on and never actually cleaned up after myself. There's also some weird stuff that's a result of me playing around with Scala features that seemed novel to me at the time.

Basically this is just a weekend/freetime project, so my focus is more on getting the thing working than being meticulous. That said, I do try to keep the code itself efficient and readable, even if it's disorganized.

And lastly, you may notice the dinkyman namespace. That is for a friend of mine who is still somewhat of a beginner Java programmer.
