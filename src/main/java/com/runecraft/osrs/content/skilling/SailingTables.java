package com.runecraft.osrs.content.skilling;

/**
 * Sailing reference tables generated from the OSRS Wiki (July 2026 wikitext; see
 * src/test/resources/osrs/sailing): sea charting tasks, seas and their completion XP, courier tasks and
 * core boat parts. Tab-separated rows, parsed by {@link SailingTraining}. Regenerate rather than edit.
 */
final class SailingTables {
    private SailingTables() {
    }

    /** Sea charting: id, level, type, hazard, sea, ocean, x, y, task. */
    static final String CHARTS = """
0	1	Generic	-	Bay of Sarim	Ardent Ocean	3030	3198	Look at the port task board in Port Sarim.
1	1	Generic	-	Bay of Sarim	Ardent Ocean	3029	3207	Interact with the salvaging station in Port Sarim.
2	1	Spyglass	-	Bay of Sarim	Bonus charts	3104	3181	Use your spyglass to get a good view of the Wizards' Tower from the north.
3	22	Current	-	Bay of Sarim	Bonus charts	3082	3201	Test the currents south of the small island in the Bay of Sarim.
4	1	Generic	-	Bay of Sarim	Ardent Ocean	3079	3231	Find a treacherous rock formation near Draynor Village.
5	1	Generic	-	Bay of Sarim	Ardent Ocean	3126	3202	Find the wreck of the Grandyozenaym north east of the Wizards' Tower.
6	1	Generic	-	Mudskipper Sound	Ardent Ocean	2944	3138	Find a strange stone tablet south east of Musa Point.
7	1	Generic	-	Kharidian Sea	Ardent Ocean	2987	3010	Find a crashed glider south of the Karamja Shipyard.
8	1	Generic	-	Mudskipper Sound	Ardent Ocean	3054	3051	Find the wreck of the Salty Grouper south west of Tutorial Island.
9	22	Current	-	Mudskipper Sound	Bonus charts	2890	3121	Test the currents at the mouth of the River Amja south of Musa Point.
10	1	Spyglass	-	Mudskipper Sound	Ardent Ocean	3008	3049	Use your spyglass to get a good view of the Karamja Shipyard from the east.
11	12	Crate	-	Kharidian Sea	Bonus charts	3013	2998	Find a Sealed crate near the Pandemonium and sample the contents.
12	38	Diving	-	Kharidian Sea	Bonus charts	2988	2949	With help from a mermaid guide, document the water depth south west of the Pandemonium. Answer to the riddle: Willow stock
13	22	Current	-	Kharidian Sea	Bonus charts	3145	2963	Test the currents south west of the Kharidian Bandit Camp.
14	1	Spyglass	-	Kharidian Sea	Ardent Ocean	3045	2995	Use your spyglass to get a good view of the Pandemonium from the cave entrance on the island.
15	1	Generic	-	Kharidian Sea	Ardent Ocean	2973	2994	Find the wreck of the Pandemonium II west of the Pandemonium.
16	1	Generic	-	Kharidian Sea	Ardent Ocean	3143	2979	Find the wreck of the Pandemonium III west of the Kharidian Bandit Camp.
17	1	Generic	-	Lumbridge Basin	Ardent Ocean	3133	3053	Find an unfortunate corpse on a small island south east of Tutorial Island.
18	1	Generic	-	Lumbridge Basin	Ardent Ocean	3182	3136	Find a treacherous rock formation south of Lumbridge Swamp.
19	1	Spyglass	-	Lumbridge Basin	Ardent Ocean	3145	3134	Use your spyglass to get a good view of the western mine in Lumbridge Swamp from the south.
20	22	Current	-	Lumbridge Basin	Bonus charts	3209	3134	Test the currents west of the Lum Lagoon.
21	1	Generic	-	Rimmington Strait	Ardent Ocean	2905	3281	Find a treacherous rock formation west of the Crafting Guild.
22	1	Generic	-	Rimmington Strait	Ardent Ocean	2873	3260	Find a warning about dragons east of Crandor.
23	1	Spyglass	-	Rimmington Strait	Ardent Ocean	2839	3210	Use your spyglass to get a good view of the Karamja Volcano from the north.
24	22	Current	-	Rimmington Strait	Bonus charts	2902	3317	Test the currents south west of the Makeover Mage's home.
25	1	Generic	-	Catherby Bay	Ardent Ocean	2897	3335	Find some odd aquatic plant life west of the Dark Wizards' Tower.
26	1	Generic	-	Catherby Bay	Ardent Ocean	2760	3364	Find evidence of megashrimp activity east of the Legends' Guild.
27	1	Spyglass	-	Catherby Bay	Ardent Ocean	2784	3401	Use your spyglass to get a good view of Keep le Faye from the east.
28	22	Current	-	Catherby Bay	Bonus charts	2835	3417	Test the currents south west of the Obelisk of Water.
29	1	Generic	-	Brimhaven Passage	Ardent Ocean	2741	3270	Find the wreck of the Shelled Snail east of Witchaven.
30	1	Generic	-	Brimhaven Passage	Ardent Ocean	2769	3267	Find a set of someone else's charts south of the Fishing Platform and copy them.
31	1	Spyglass	-	Brimhaven Passage	Ardent Ocean	2779	3265	Use your spyglass to get a good view of the Fishing Platform from the south.
32	22	Current	-	Brimhaven Passage	Ardent Ocean	2648	3244	Test the currents south east of Ardougne Zoo.
33	1	Generic	-	Strait of Khazard	Ardent Ocean	2702	3151	Find a rowboat off the west coast of Karamja near Port Khazard.
34	1	Generic	-	Strait of Khazard	Ardent Ocean	2720	3103	Find what remains of Colbansea Island in the middle of the Strait of Khazard.
35	1	Generic	-	Strait of Khazard	Ardent Ocean	2681	3134	Find a notice from General Khazard south east of Port Khazard.
36	1	Spyglass	-	Strait of Khazard	Ardent Ocean	2664	3219	Use your spyglass to get a good view of the Tower of Life from the east.
37	22	Current	-	Strait of Khazard	Ardent Ocean	2664	3172	Test the currents west of the trawler in Port Khazard.
38	12	Crate	-	Mudskipper Sound	Bonus charts	2994	3134	Find a Sealed crate near Mudskipper Point and sample the contents.
39	12	Crate	-	Lumbridge Basin	Bonus charts	3277	3135	Find a Sealed crate in the Lum Lagoon and sample the contents. Note: A raft is recommended but not required to reach this location.
40	12	Crate	-	Catherby Bay	Ardent Ocean	2869	3378	Find a Sealed crate near Entrana and sample the contents.
41	12	Crate	-	Strait of Khazard	Ardent Ocean	2674	3103	Find a Sealed crate near the Yanille Chain and sample the contents.
42	57	Weather	-	Rimmington Strait	Bonus charts	2912	3213	Help the meteorologist document the local weather on a small island west of Rimmington.
43	57	Weather	-	Strait of Khazard	Bonus charts	2656	3195	Help the meteorologist document the local weather on a small island north of Port Khazard.
44	38	Diving	-	Mudskipper Sound	Bonus charts	2979	3085	With help from a mermaid guide, document the water depth south of Mudskipper Point. Answer to the riddle: Pie dish, pot of flour, and cooking apple
45	38	Diving	-	Catherby Bay	Bonus charts	2873	3397	With help from a mermaid guide, document the water depth west of the entrance to Taverley Dungeon. Answer to the riddle: Iron med helm and bronze chainbody
46	38	Diving	-	Strait of Khazard	Bonus charts	2757	3138	With help from a mermaid guide, document the water depth south west of Brimhaven. Answer to the riddle: 5 cabbage seeds
47	38	Diving	-	Red Reef	Unquiet Ocean	2788	2548	With help from a mermaid guide, document the water depth north of Red Rock. Answer to the riddle: 10 watermelons
48	38	Diving	-	Barracuda Belt	Shrouded Ocean	2276	2598	With help from a mermaid guide, document the water depth north of the bank boat in the Barracuda Belt. Answer to the riddle:Vial, avantoe, snape grass, and caviar
49	38	Diving	-	Arrow Passage	Ardent Ocean	2917	2765	With help from a mermaid guide, document the water depth north of Crash Island. Answer to the riddle: Harralander potion (unf)
50	38	Diving	-	Turtle Belt	Unquiet Ocean	3014	2556	With help from a mermaid guide, document the water depth in the middle of the Turtle Belt. Answer to the riddle: Papaya fruit
51	38	Diving	-	Tortugan Sea	Unquiet Ocean	3282	2445	With help from a mermaid guide, document the water depth east of the Great Conch. Answer to the riddle: Ashes, or any demonic ashes
52	72	Diving	Tangled kelp	Rainbow Reef	Shrouded Ocean	2256	2312	With help from a mermaid guide, document the water depth in the middle of Rainbow Reef. Answer to the riddle: Bucket of sap and raw slimy eel
53	38	Diving	-	Anglerfish's Light	Unquiet Ocean	2766	2382	With help from a mermaid guide, document the water depth in the middle of Anglerfish's Light. Answer to the riddle: Barley
54	57	Weather	-	The Skullhorde	Bonus charts	2542	2541	Help the meteorologist document the local weather on the Isle of Bones.
55	57	Weather	-	Barracuda Belt	Shrouded Ocean	2082	2590	Help the meteorologist document the local weather on a small island south east of Wintumber Island.
56	57	Weather	-	Arrow Passage	Bonus charts	2903	2698	Help the meteorologist document the local weather on a small island south of Crash Island.
57	57	Weather	Stormy seas	The Storm Tempor	Bonus charts	2979	2905	Help the meteorologist document the local weather on a small island east of the Kharazi Jungle.
58	57	Weather	-	Pearl Bank	Bonus charts	3321	2327	Help the meteorologist document the local weather at the south east tip of Cape Conch.
59	57	Weather	-	Anglerfish's Light	Bonus charts	2864	2311	Help the meteorologist document the local weather on a small island south of Last Light.
60	12	Crate	-	The Simian Sea	Ardent Ocean	2677	2654	Find a Sealed crate near the Void Knights' Outpost and sample the contents.
61	12	Crate	-	Barracuda Belt	Shrouded Ocean	2282	2506	Find a Sealed crate near the bank boat in the Barracuda Belt and sample the contents.
62	12	Crate	-	Arrow Passage	Ardent Ocean	2816	2733	Find a Sealed crate off the eastern coast of Ape Atoll and sample the contents.
63	12	Crate	-	Kharazi Strait	Ardent Ocean	2888	2857	Find a Sealed crate near Rock Island Prison and sample the contents.
64	12	Crate	-	Turtle Belt	Unquiet Ocean	3046	2665	Find a Sealed crate near Dognose Island and sample the contents.
65	12	Crate	-	Sea of Shells	Unquiet Ocean	3098	2458	Find a Sealed crate off the western coast of the Great Conch and sample the contents.
66	72	Crate	Tangled kelp	Rainbow Reef	Shrouded Ocean	2205	2303	Find a Sealed crate near Sunbleak Island and sample the contents.
67	12	Crate	-	Anglerfish's Light	Unquiet Ocean	2840	2280	Find a Sealed crate near Last Light and sample the contents.
68	22	Current	-	The Simian Sea	Ardent Ocean	2691	2703	Test the currents south west of Cape Atoll.
69	22	Current	-	The Simian Sea	Ardent Ocean	2665	2560	Test the currents south of the pest-filled island near the Void Knights' Outpost.
70	22	Current	-	Kharazi Strait	Ardent Ocean	2767	2895	Test the currents off the south western coast of the Kharazi Jungle.
71	22	Current	-	Red Reef	Unquiet Ocean	2902	2548	Test the currents north east of Red Rock.
72	24	Current	Stormy seas	The Storm Tempor	Ardent Ocean	3001	2847	Test the currents west of Tempoross Cove.
73	22	Current	-	Sea of Shells	Unquiet Ocean	3204	2367	Test the currents near the Summer Shore docks on the Great Conch.
74	72	Current	Tangled kelp	Rainbow Reef	Shrouded Ocean	2335	2286	Test the currents north of Rainbow's End.
75	22	Current	-	The Lonely Sea	Unquiet Ocean	2624	2415	Test the currents west of Charred Island.
76	1	Spyglass	-	The Simian Sea	Ardent Ocean	2654	2682	Use your spyglass to get a good view of the Void Knights' Outpost from the north.
77	1	Spyglass	-	Arrow Passage	Ardent Ocean	2828	2785	Use your spyglass to get a good view of the temple on Ape Atoll from the east.
78	24	Spyglass	Stormy seas	Kharazi Strait	Ardent Ocean	2945	2878	Use your spyglass to get a good view of a cave in the Kharazi Jungle from the south.
79	40	Spyglass	-	Turtle Belt	Unquiet Ocean	3050	2656	Use your spyglass to get a good view of the surrounding sea from Dognose Island.
80	24	Spyglass	Stormy seas	The Storm Tempor	Ardent Ocean	3144	2854	Use your spyglass to get a good view of the Ruins of Unkah from north of the docks.
81	45	Spyglass	-	Sea of Shells	Unquiet Ocean	3187	2430	Use your spyglass to get a good view of the Summer Shore from the cliffs above it on the Great Conch. Note: Accessing the Great Conch requires partial completion of Troubled Tortugans.
82	1	Spyglass	-	Bay of Elidinis	Unquiet Ocean	3154	2548	Use your spyglass to get a good view of the Abalone Cliffs from the north east.
83	1	Spyglass	-	Anglerfish's Light	Unquiet Ocean	2895	2326	Use your spyglass to get a good view of Last Light from the east.
84	1	Generic	-	The Simian Sea	Ardent Ocean	2681	2714	Find a makeshift flag near Cape Atoll.
85	1	Generic	-	Oo'glog Channel	Ardent Ocean	2693	2793	Find the wreck of the bananaless boat west of Ape Atoll.
86	1	Generic	-	Red Reef	Unquiet Ocean	2804	2460	Find the corpse of a wayward shifter on a small island south of Red Rock.
87	1	Generic	-	The Skullhorde	Shrouded Ocean	2587	2593	Find the wreck of the Balanced Ballast south west of the Void Knights' Outpost.
88	1	Generic	-	Barracuda Belt	Shrouded Ocean	2149	2615	Find a spool of Barracuda grade rope east of Wintumber Island.
89	1	Generic	-	Barracuda Belt	Shrouded Ocean	2281	2447	Find the wreck of a Barracuda ship north east of Sunbleak Island.
90	1	Generic	-	Kharazi Strait	Ardent Ocean	2781	2815	Find a crashed glider north of Ape Atoll.
91	1	Generic	-	Arrow Passage	Ardent Ocean	2881	2750	Find some interesting aquatic life north west of Crash Island.
92	1	Generic	-	Kharazi Strait	Ardent Ocean	2798	2891	Find a map south west of the Kharazi Jungle and copy it.
93	1	Generic	-	Kharazi Strait	Ardent Ocean	2843	2837	Find some curious rapids west of Rock Island Prison.
94	1	Generic	-	Turtle Belt	Unquiet Ocean	3075	2601	Find a treasure chest south east of Dognose Island.
95	1	Generic	-	Barracuda Belt	Shrouded Ocean	2066	2608	Find an odd statue on Wintumber Island.
96	24	Generic	Stormy seas	The Storm Tempor	Ardent Ocean	3033	2796	Find the wreck of the Desert Trout south of Tempoross Cove.
97	24	Generic	Stormy seas	The Storm Tempor	Ardent Ocean	3065	2885	Find something trying to leech power from the Storm Tempor.
98	1	Generic	-	Tortugan Sea	Unquiet Ocean	3284	2514	Find a notable small shell north east of the Great Conch.
99	45	Generic	-	Tortugan Sea	Unquiet Ocean	3216	2466	Find a strange apple tree somewhere on the Great Conch. Note: Accessing the Great Conch requires partial completion of Troubled Tortugans.
100	72	Generic	Tangled kelp	Rainbow Reef	Shrouded Ocean	2336	2267	Find a lost friend on Rainbow's End.
101	72	Generic	Tangled kelp	Rainbow Reef	Shrouded Ocean	2334	2118	Find a rowboat to the far south of Rainbow's End.
102	1	Generic	-	The Lonely Sea	Unquiet Ocean	2634	2392	Find some dragon scales on Charred Island. It is not necessary to dock onto the island to chart this.
103	1	Generic	-	Anglerfish's Light	Unquiet Ocean	2719	2296	Find an odd light south west of Last Light.
104	57	Weather	-	Gu'tanoth Bay	Bonus charts	2638	3009	Help the meteorologist document the local weather north east of Rantz's cave.
105	57	Weather	Fetid waters	Breakbone Strait	Bonus charts	2335	2921	Help the meteorologist document the local weather on the eastern coast of the Isle of Souls.
106	57	Weather	-	Soul Bay	Shrouded Ocean	2140	2806	Help the meteorologist document the local weather on the south western coast of the Isle of Souls.
107	66	Weather	Crystal-flecked waters	Porth Neigwl	Western Ocean	2149	3068	Help the meteorologist document the local weather on a small island west of Zul-Andra.
108	1	Spyglass	-	Gu'tanoth Bay	Ardent Ocean	2597	3027	Use your spyglass to get a good view of the island in Gu'tanoth Bay from the east.
109	1	Spyglass	-	Oo'glog Channel	Ardent Ocean	2605	2867	Use your spyglass to get a good view of the throne in Corsair Cove from the east.
110	40	Spyglass	Fetid waters	Breakbone Strait	Shrouded Ocean	2429	2852	Use your spyglass to get a good view of the Myths' Guild from the west.
111	61	Spyglass	Fetid waters	Sea of Souls	Shrouded Ocean	2342	2768	Use your spyglass to get a good view of the surrounding sea from the Tear of the Soul.
112	40	Spyglass	Fetid waters	Backwater	Shrouded Ocean	2399	3027	Use your spyglass to get a good view of Chompy Marsh from the south.
113	1	Spyglass	-	Zul-Egil	Shrouded Ocean	2128	2983	Use your spyglass to get a good view of the ruined tower on the Isle of Souls from the south.
114	40	Spyglass	Fetid waters	Zul-Egil	Shrouded Ocean	2199	3036	Use your spyglass to get a good view of Zul-Andra from the south.
115	12	Crate	-	Gu'tanoth Bay	Ardent Ocean	2624	3069	Find a Sealed crate near Yanille and sample the contents.
116	12	Crate	-	Feldip Gulf	Ardent Ocean	2691	2940	Find a Sealed crate in the middle of the Feldip Gulf and sample the contents.
117	12	Crate	-	Oo'glog Channel	Ardent Ocean	2595	2787	Find a Sealed crate south of Corsair Cove and sample the contents.
118	12	Crate	-	Mythic Sea	Shrouded Ocean	2556	2664	Find a Sealed crate west of the Void Knights' Outpost and sample the contents.
119	40	Crate	Fetid waters	Breakbone Strait	Shrouded Ocean	2334	2781	Find a Sealed crate near the Tear of the Soul and sample the contents.
120	40	Crate	Fetid waters	Backwater	Shrouded Ocean	2318	2966	Find a Sealed crate off the north eastern coast of the Isle of Souls and sample the contents.
121	40	Crate	Fetid waters	Backwater	Shrouded Ocean	2476	2969	Find a Sealed crate off the western coast of the Feldip Hills and sample the contents.
122	12	Crate	-	Soul Bay	Shrouded Ocean	2086	2882	Find a Sealed crate off the western coast of the Isle of Souls and sample the contents.
123	12	Crate	-	Sea of Souls	Shrouded Ocean	2221	2765	Find a Sealed crate off the southern coast of the Isle of Souls and sample the contents.
124	40	Crate	Fetid waters	Zul-Egil	Shrouded Ocean	2279	3036	Find a Sealed crate south of the poison waste and sample the contents.
125	38	Diving	-	Gu'tanoth Bay	Ardent Ocean	2676	3035	With help from a mermaid guide, document the water depth south of the Yanille Chain. Answer to the riddle: Earth impling jar
126	38	Diving	-	Oo'glog Channel	Ardent Ocean	2529	2829	With help from a mermaid guide, document the water depth south west of Corsair Cove. Answer to the riddle: Coal Note: To be able to do this task at 38 Sailing, one must board another player's boat, provided that they have an innoculation station to stand the fetid waters.
127	40	Diving	Fetid waters	Mythic Sea	Shrouded Ocean	2446	2784	With help from a mermaid guide, document the water depth south of the Myths' Guild. Answer to the riddle: Cabbage, onion, and tomato
128	40	Diving	Fetid waters	Backwater	Shrouded Ocean	2469	3006	With help from a mermaid guide, document the water depth south of Jiggig. Answer to the riddle: Grimy kwuarm
129	38	Diving	-	Soul Bay	Shrouded Ocean	2012	2874	With help from a mermaid guide, document the water depth west of the Isle of Souls. Answer to the riddle: Dwellberries
130	22	Current	-	Feldip Gulf	Ardent Ocean	2645	2974	Test the currents south east of Rantz's cave.
131	40	Current	Fetid waters	Mythic Sea	Shrouded Ocean	2495	2708	Test the currents east of Anglers' Retreat.
132	40	Current	Fetid waters	Breakbone Strait	Shrouded Ocean	2340	2896	Test the currents off the eastern coast of the Isle of Souls.
133	22	Current	-	Soul Bay	Shrouded Ocean	2080	2859	Test the currents off the western coast of the Isle of Souls.
134	40	Current	Fetid waters	Backwater	Shrouded Ocean	2318	3057	Test the currents at the mouth of the River Dougne west of Chompy Marsh.
135	1	Generic	-	Gu'tanoth Bay	Ardent Ocean	2633	3039	Find the wreck of an ogre ship in Gu'tanoth Bay.
136	1	Generic	-	Feldip Gulf	Ardent Ocean	2695	2981	Find an odd geological phenomenon in the middle of the Feldip Gulf.
137	1	Generic	-	Oo'glog Channel	Ardent Ocean	2634	2882	Find the corpse of a sea monster north east of Corsair Cove.
138	51	Generic	Fetid waters	Mythic Sea	Shrouded Ocean	2476	2707	Find an abandoned camp on Anglers' Retreat.
139	1	Generic	-	Sea of Souls	Shrouded Ocean	2289	2712	Find the wreck of the Soul Searcher south west of the Tear of the Soul.
140	40	Generic	Fetid waters	Breakbone Strait	Shrouded Ocean	2460	2881	Find a chest of equipment north of the Myths' Guild.
141	40	Generic	Fetid waters	Backwater	Shrouded Ocean	2300	3043	Find some interesting coral south east of the Poison Waste.
142	1	Generic	-	Western Gate	Shrouded Ocean	1996	2947	Find some some interesting sea life in the middle of the Western Gate.
143	1	Generic	-	Soul Bay	Shrouded Ocean	2089	2797	Find some interesting sea life in the middle of Soul Bay.
144	40	Generic	Fetid waters	Zul-Egil	Shrouded Ocean	2165	3030	Find a toxic hazard in the middle of Zul-Egil.
145	66	Weather	Crystal-flecked waters	Tirannwn Bight	Western Ocean	2130	3395	Help the meteorologist document the local weather south west of Mynydd.
146	66	Weather	Crystal-flecked waters	Porth Neigwl	Western Ocean	2118	3159	Help the meteorologist document the local weather on a small island west of the Tyras Camp.
147	22	Current	-	Western Gate	Shrouded Ocean	1978	3106	Test the currents east of Minotaurs' Rest.
148	66	Current	Crystal-flecked waters	Porth Neigwl	Western Ocean	2071	3186	Test the currents west of Lledrith Island.
149	66	Current	Crystal-flecked waters	Tirannwn Bight	Western Ocean	2165	3260	Test the currents at the mouth of the Afon Ganol west of the Iorwerth Camp.
150	66	Spyglass	Crystal-flecked waters	Porth Neigwl	Western Ocean	2160	3155	Use your spyglass to get a good view of the Tyras Camp from the west.
151	66	Spyglass	Crystal-flecked waters	Tirannwn Bight	Western Ocean	2163	3318	Use your spyglass to get a good view of Prifddinas' western gates from the south west.
152	38	Diving	-	Western Gate	Shrouded Ocean	2041	3023	With help from a mermaid guide, document the water depth in the middle of the Western Gate. Answer to the riddle: Black flowers
153	38	Diving	-	Crystal Sea	Western Ocean	1979	3211	With help from a mermaid guide, document the water depth in the middle of the Crystal Sea. Answer to the riddle: Butterfly jar
154	66	Diving	Crystal-flecked waters	Porth Neigwl	Western Ocean	2113	3123	With help from a mermaid guide, document the water depth west of Port Tyras. Answer to the riddle: 2 calquat kegs, an ale yeast, one pair of oak roots, and 2 barley malt
155	38	Diving	-	Sapphire Sea	Shrouded Ocean	1762	2611	With help from a mermaid guide, document the water depth south of the Crown Jewel. Answer to the riddle: Vial, coconut milk, toadflax, and yew roots
156	66	Diving	Crystal-flecked waters	Tirannwn Bight	Western Ocean	2120	3345	With help from a mermaid guide, document the water depth in the middle of the Tirannwn Bight. Answer to the riddle: Soiled page
157	1	Generic	-	Western Gate	Shrouded Ocean	1947	2985	Find evidence of the Sharhai east of Vatrachos Island.
158	1	Generic	-	Crystal Sea	Western Ocean	1980	3232	Find an elven shipwreck in the middle of the Crystal Sea.
159	66	Generic	Crystal-flecked waters	Porth Neigwl	Western Ocean	2118	3158	Find some lost cargo south east of Lledrith Island.
160	66	Generic	Crystal-flecked waters	Tirannwn Bight	Western Ocean	2157	3274	Find a deadly trap west of the Iorwerth Camp.
161	1	Generic	-	Vagabonds Rest	Western Ocean	2026	3669	Find a half-built ship west of Buccaneers' Haven.
162	12	Crate	-	Western Gate	Shrouded Ocean	1993	2972	Find a Sealed crate in the middle of the Western Gate and sample the contents.
163	12	Crate	-	Crystal Sea	Western Ocean	2008	3261	Find a Sealed crate in the middle of the Crystal Sea and sample the contents.
164	66	Crate	Crystal-flecked waters	Porth Neigwl	Western Ocean	2161	3203	Find a Sealed crate off the western coast of Isafdar and sample the contents.
165	66	Crate	Crystal-flecked waters	Porth Gwenith	Western Ocean	2206	3484	Find a Sealed crate near Ynysdail and sample the contents.
166	66	Crate	Crystal-flecked waters	Tirannwn Bight	Western Ocean	2120	3418	Find a Sealed crate near Mynydd and sample the contents.
167	66	Weather	Crystal-flecked waters	Porth Gwenith	Western Ocean	2283	3520	Help the meteorologist document the local weather on a small island north west of Eagles' Peak.
168	57	Weather	-	Pilgrims' Passage	Bonus charts	1867	3458	Help the meteorologist document the local weather on a small island south east of the Tithe Farm. Note: A raft or skiff is recommended but not required to reach this location..
169	78	Weather	Icy seas	Winter's Edge	Northern Ocean	1817	3965	Help the meteorologist document the local weather on a small island north of the River of Souls.
170	66	Diving	Crystal-flecked waters	Porth Gwenith	Western Ocean	2283	3459	With help from a mermaid guide, document the water depth north of Gorlah. Answer to the riddle: Thatch spar dense Note: A skiff is necessary to reach this location.
171	66	Diving	Crystal-flecked waters	Piscatoris Sea	Western Ocean	2236	3593	With help from a mermaid guide, document the water depth south west of Kraken Cove. Answer to the riddle: Gold ore
172	38	Diving	-	Gulf of Kourend	Bonus charts	1851	3668	With help from a mermaid guide, document the water depth north of the Dibber. Answer to the riddle: 2 malicious ashes
173	66	Current	Crystal-flecked waters	Porth Gwenith	Western Ocean	2220	3429	Test the currents north of Gwenith.
174	22	Current	-	Pilgrims' Passage	Western Ocean	1871	3430	Test the currents west of Chinchompa Island.
175	22	Current	-	Gulf of Kourend	Western Ocean	1813	3664	Test the currents north west of the Dibber.
176	78	Current	Icy seas	Winter's Edge	Northern Ocean	1866	3911	Test the currents north east of the River of Souls.
177	1	Spyglass	-	Piscatoris Sea	Western Ocean	2303	3700	Use your spyglass to get a good view of the Piscatoris Fishing Colony from the north west.
178	1	Spyglass	-	Hosidian Sea	Western Ocean	1861	3528	Use your spyglass to get a good view of the Tithe Farm from the north east.
179	1	Spyglass	-	Gulf of Kourend	Western Ocean	1846	3769	Use your spyglass to get a good view of Port Piscarilius from north of the Foodhall.
180	78	Spyglass	Icy seas	Everwinter Sea	Northern Ocean	1793	3978	Use your spyglass to get a good view of the area north of the River of Souls from the east.
181	12	Crate	-	Vagabonds Rest	Western Ocean	2079	3655	Find a Sealed crate near Buccaneers' Haven and sample the contents.
182	12	Crate	-	Hosidian Sea	Western Ocean	1887	3556	Find a Sealed crate off the eastern coast of Hosidius and sample the contents.
183	12	Crate	-	Gulf of Kourend	Western Ocean	1934	3742	Find a Sealed crate in the middle of the Gulf of Kourend and sample the contents.
184	78	Crate	Icy seas	Winter's Edge	Northern Ocean	1898	4069	Find a Sealed crate near Brittle Isle and sample the contents.
185	66	Generic	Crystal-flecked waters	Porth Gwenith	Western Ocean	2210	3445	Find an unusual boat north of Gwenith.
186	1	Generic	-	Vagabonds Rest	Western Ocean	2133	3589	Find a crate of discarded goods north of Drumstick Isle.
187	1	Generic	-	Piscatoris Sea	Western Ocean	2300	3662	Find evidence of kraken activity south west of the Piscatoris Fishing Colony.
188	1	Generic	-	Hosidian Sea	Western Ocean	1855	3586	Find some discarded farming equipment east of Hosidius.
189	1	Generic	-	Gulf of Kourend	Western Ocean	1847	3796	Find something in desperate need of repair north east of Port Piscarilius.
190	78	Generic	Icy seas	Everwinter Sea	Northern Ocean	1762	4005	Find some odd eggs north east of the Fishing Hamlet.
191	1	Spyglass	-	Menaphite Sea	Ardent Ocean	3145	2734	Use your spyglass to get a good view of the pyramid in Menaphos from the west.
192	38	Diving	-	Menaphite Sea	Ardent Ocean	3044	2722	With help from a mermaid guide, document the water depth in the middle of the Menaphite Sea. Answer to the riddle: Sandwich lady bottom
193	1	Generic	-	Menaphite Sea	Ardent Ocean	3143	2770	Find some collected sand on an island west of Menaphos.
194	22	Current	-	Menaphite Sea	Ardent Ocean	3185	2654	Test the currents north of the island south of Menaphos.
195	12	Crate	-	Menaphite Sea	Ardent Ocean	3190	2706	Find a Sealed crate near Menaphos and sample the contents.
196	1	Generic	-	Fremensund	Northern Ocean	2587	3657	Find some suspicious eyes west of Rellekka.
197	1	Generic	-	Grandroot Bay	Northern Ocean	2479	3546	Find the roots of the Grand Tree in Grandroot Bay.
198	1	Generic	-	V's Belt	Northern Ocean	2528	3818	Find an informative sign on a small island south of Miscellania.
199	1	Generic	-	Fremennik Strait	Northern Ocean	2383	3769	Find a discarded book south of Jatizso.
200	78	Generic	Icy seas	Idestia Strait	Northern Ocean	2726	3855	Find a hunter free haven on a small island west of Trollweiss Mountain.
201	1	Generic	-	Lunar Bay	Northern Ocean	2225	3892	Find evidence of some strong magical spells east of Lunar Isle.
202	78	Generic	Icy seas	Winter's Edge	Northern Ocean	2083	3953	Find an odd dead plant on a small island north west of Lunar Isle.
203	78	Generic	Icy seas	Lunar Sea	Northern Ocean	2144	4067	Find some cleaning supplies north of Lunar Isle.
204	78	Generic	Icy seas	Kannski Tides	Northern Ocean	2426	3961	Find the wreck of the Fearless Fremennik south west of the Island of Stone.
205	78	Generic	Icy seas	Weissmere	Northern Ocean	2783	3882	Find some missing mountaineering equipment north of Trollweiss Mountain.
206	78	Generic	Icy seas	Stoneheart Sea	Northern Ocean	2587	3919	Find a shark corpse on a small island north west of Etceteria.
207	78	Generic	Icy seas	Shiverwake Expanse	Northern Ocean	2709	4124	Find evidence of spying north east of the Iceberg.
208	78	Generic	Icy seas	Weiss Melt	Northern Ocean	2902	3965	Find a failing boat north east of Weiss.
209	22	Current	-	Fremensund	Northern Ocean	2561	3626	Test the currents north of Rower's Arm.
210	22	Current	-	Grandroot Bay	Northern Ocean	2369	3507	Test the currents in the River Crannmor. Note: A raft is necessary to reach this location.
211	22	Current	-	V's Belt	Northern Ocean	2428	3831	Test the currents north east of Jatizso.
212	22	Current	-	Fremennik Strait	Northern Ocean	2305	3795	Test the currents west of Neitiznot.
213	22	Current	-	Idestia Strait	Northern Ocean	2695	3828	Test the currents west of the hunter area near Trollweiss Mountain.
214	22	Current	-	Lunar Bay	Northern Ocean	2159	3849	Test the currents south of the Astral Altar on Lunar Isle.
215	78	Current	Icy seas	Everwinter Sea	Northern Ocean	1792	4137	Test the currents near the shipwrecks in the Everwinter Sea.
216	78	Current	Icy seas	Kannski Tides	Northern Ocean	2316	3903	Test the currents north of the western cave on the Fremennik Isles.
217	78	Current	Icy seas	Weissmere	Northern Ocean	2833	3960	Test the currents north west of Weiss.
218	78	Current	Icy seas	Stoneheart Sea	Northern Ocean	2433	4006	Test the currents west of the Island of Stone.
219	78	Current	Icy seas	Weiss Melt	Northern Ocean	2880	3985	Test the currents north of Weiss.
220	57	Weather	-	Fremensund	Northern Ocean	2594	3644	Help the meteorologist document the local weather on a small island south west of Rellekka.
221	57	Weather	-	Grandroot Bay	Northern Ocean	2498	3546	Help the meteorologist document the local weather west of the Barbarian Outpost.
222	57	Weather	-	Fremennik Strait	Northern Ocean	2341	3790	Help the meteorologist document the local weather on the southern coast of Neitiznot.
223	78	Weather	Icy seas	Idestia Strait	Northern Ocean	2600	3910	Help the meteorologist document the local weather on a small island north of Etceteria.
224	78	Weather	Icy seas	Everwinter Sea	Northern Ocean	1958	4065	Help the meteorologist document the local weather on Brittle Isle.
225	78	Weather	Icy seas	Stoneheart Sea	Northern Ocean	2447	3995	Help the meteorologist document the local weather on a small island south west of the Island of Stone.
226	78	Weather	Icy seas	Shiverwake Expanse	Northern Ocean	2793	4020	Help the meteorologist document the local weather on the large rock formations in Weissmere.
227	78	Weather	Icy seas	Weiss Melt	Northern Ocean	2951	3962	Help the meteorologist document the local weather on a small island north of the Frozen Waste Plateau.
228	1	Spyglass	-	Fremensund	Northern Ocean	2636	3707	Use your spyglass to get a good view of the market in Rellekka from the north.
229	1	Spyglass	-	Grandroot Bay	Northern Ocean	2504	3612	Use your spyglass to get a good view of the lighthouse on Rower's Arm from the south.
230	1	Spyglass	-	V's Belt	Northern Ocean	2477	3862	Use your spyglass to get a good view of the castle on Miscellania from the west.
231	1	Spyglass	-	Fremennik Strait	Northern Ocean	2366	3890	Use your spyglass to get a good view of the towers on the Fremennik Isles from the south.
232	1	Spyglass	-	Idestia Strait	Northern Ocean	2636	3874	Use your spyglass to get a good view of the castle on Etceteria from the east.
233	1	Spyglass	-	Lunar Bay	Northern Ocean	2160	3901	Use your spyglass to get a good view of the pirate galleon docked at Lunar Isle from the east.
234	1	Spyglass	-	Winter's Edge	Northern Ocean	2051	3913	Use your spyglass to get a good view of the town on Lunar Isle from the west.
235	78	Spyglass	Icy seas	Lunar Sea	Northern Ocean	2237	4064	Use your spyglass to get a good view of Ungael from the west.
236	78	Spyglass	Icy seas	Kannski Tides	Northern Ocean	2351	3904	Use your spyglass to get a good view of the ice trolls of the Fremennik Isles from the north.
237	78	Spyglass	Icy seas	Weissmere	Northern Ocean	2781	3987	Use your spyglass to get a good view of the large rock formations in Weissmere from their centre.
238	78	Spyglass	Icy seas	Stoneheart Sea	Northern Ocean	2620	4059	Use your spyglass to get a good view of the KGP training area from the west.
239	78	Spyglass	Icy seas	Shiverwake Expanse	Northern Ocean	2879	4078	Use your spyglass to get a good view of Grimstone from the west.
240	78	Spyglass	Icy seas	Weiss Melt	Northern Ocean	2990	3971	Use your spyglass to get a good view of the agility course in the Wilderness from the north.
241	38	Diving	-	Fremensund	Northern Ocean	2518	3690	With help from a mermaid guide, document the water depth south of Waterbirth Island. Answer to the riddle: Kharyrll teleport (tablet)
242	38	Diving	-	Grandroot Bay	Northern Ocean	2513	3571	With help from a mermaid guide, document the water depth west of the Barbarian Outpost. Answer to the riddle: Raw cod
243	78	Diving	Icy seas	V's Belt	Northern Ocean	2506	3893	With help from a mermaid guide, document the water depth north west of Miscellania. Answer to the riddle: Bronze Limbs
244	78	Diving	Icy seas	Idestia Strait	Northern Ocean	2664	3954	With help from a mermaid guide, document the water depth south of the Iceberg. Answer to the riddle: Onion
245	38	Diving	-	Lunar Bay	Northern Ocean	2221	3833	With help from a mermaid guide, document the water depth north east of Pirates' Cove. Answer to the riddle: Torstol
246	78	Diving	Icy seas	Winter's Edge	Northern Ocean	1970	3966	With help from a mermaid guide, document the water depth in the middle of Winter's Edge. Answer to the riddle: Needle
247	78	Diving	Icy seas	Lunar Sea	Northern Ocean	2061	4118	With help from a mermaid guide, document the water depth in the north west of the Lunar Sea. Answer to the riddle: Clockwork
248	78	Diving	Icy seas	Everwinter Sea	Northern Ocean	1823	4018	With help from a mermaid guide, document the water depth north of the River of Souls. Answer to the riddle: Shield left half
249	78	Diving	Icy seas	Kannski Tides	Northern Ocean	2357	3977	With help from a mermaid guide, document the water depth north of the Fremennik Isles. Answer to the riddle: Vial of blood, cadantine, and wine of zamorak
250	78	Diving	Icy seas	Weissmere	Northern Ocean	2816	3975	With help from a mermaid guide, document the water depth north west of Weiss. Answer to the riddle: Dragon Bitter
251	78	Diving	Icy seas	Stoneheart Sea	Northern Ocean	2475	4133	With help from a mermaid guide, document the water depth north of the Island of Stone. Answer to the riddle: Rain bow
252	78	Diving	Icy seas	Shiverwake Expanse	Northern Ocean	2824	4092	With help from a mermaid guide, document the water depth in the east of the Shiverwake Expanse. Answer to the riddle: Royal crown
253	12	Crate	-	Fremensund	Northern Ocean	2689	3732	Find a Sealed crate north of Rellekka and sample the contents.
254	12	Crate	-	Grandroot Bay	Northern Ocean	2453	3597	Find a Sealed crate in the middle of Grandroot Bay and sample the contents.
255	12	Crate	-	V's Belt	Northern Ocean	2474	3798	Find a Sealed crate in the middle of V's Belt and sample the contents.
256	12	Crate	-	Fremennik Strait	Northern Ocean	2273	3745	Find a Sealed crate north west of the Piscatoris Fishing Colony and sample the contents.
257	12	Crate	-	Idestia Strait	Northern Ocean	2651	3811	Find a Sealed crate south east of Etceteria and sample the contents.
258	12	Crate	-	Lunar Bay	Northern Ocean	2142	3874	Find a Sealed crate near the Lunar Isle docks and sample the contents. Note: A raft is recommended but not required to reach this location.
259	78	Crate	Icy seas	Lunar Sea	Northern Ocean	2113	4142	Find a Sealed crate in the far north of the Lunar Sea and sample the contents.
260	78	Crate	Icy seas	Everwinter Sea	Northern Ocean	1757	4150	Find a Sealed crate in the far north of the Everwinter Sea and sample the contents.
261	12	Crate	-	Kannski Tides	Northern Ocean	2359	3878	Find a Sealed crate in the waters of the Fremennik Isles and sample the contents. Note: A raft is necessary to reach this location.
262	78	Crate	Icy seas	Weissmere	Northern Ocean	2775	3935	Find a Sealed crate in the middle of Weissmere and sample the contents.
263	78	Crate	Icy seas	Stoneheart Sea	Northern Ocean	2612	4146	Find a Sealed crate in the far north of the Stoneheart Sea and sample the contents.
264	78	Crate	Icy seas	Shiverwake Expanse	Northern Ocean	2785	4046	Find a Sealed crate near the large rock formations in Weissmere and sample the contents.
265	78	Crate	Icy seas	Weiss Melt	Northern Ocean	2912	4154	Find a Sealed crate north of Grimstone and sample the contents.
266	22	Current	-	Turtle Belt	Unquiet Ocean	2947	2584	Test the currents south west of the remote island in the Turtle Belt.
267	22	Current	-	Bay of Elidinis	Unquiet Ocean	3248	2703	Test the currents to the south west of the Elid Delta.
268	22	Current	-	Tortugan Sea	Unquiet Ocean	3279	2463	Test the currents to the east of the eastern pier on the Great Conch.
269	22	Current	-	Pearl Bank	Unquiet Ocean	3337	2179	Test the currents to the south west of the Little Pearl.
270	1	Spyglass	-	Tortugan Sea	Unquiet Ocean	3261	2506	Use your spyglass to get a good view of the coast of the Great Conch from the north east.
271	38	Diving	-	Sea of Shells	Unquiet Ocean	2963	2203	With help from a mermaid guide, document the water depth south of the Onyx Crest. Answer to the riddle: Nose peg
272	38	Diving	-	Bay of Elidinis	Unquiet Ocean	3338	2622	With help from a mermaid guide, document the water depth in the south east of the Bay of Elidinis. Answer to the riddle: Charcoal
273	38	Diving	-	Pearl Bank	Unquiet Ocean	3276	2130	With help from a mermaid guide, document the water depth in the south of the Pearl Bank. Answer to the riddle: 2 woad leaves and 2 onions
274	38	Diving	-	The Lonely Sea	Unquiet Ocean	2528	2149	With help from a mermaid guide, document the water depth in the far south of the Lonely Sea. Answer to the riddle: Swamp weed
275	47	Generic	-	Sea of Shells	Unquiet Ocean	2963	2264	Find the nest of a fearless bird on the Onyx Crest.
276	1	Generic	-	Bay of Elidinis	Unquiet Ocean	3243	2702	Find a damaged golem east of Menaphos.
277	1	Generic	-	Pearl Bank	Unquiet Ocean	3375	2135	Find a ship in a bottle south of the Little Pearl.
278	12	Crate	-	Red Reef	Unquiet Ocean	2778	2523	Find a Sealed crate near Red Rock and sample the contents.
279	12	Crate	-	Bay of Elidinis	Unquiet Ocean	3239	2598	Find a Sealed crate south of Menaphos and sample the contents.
280	12	Crate	-	Pearl Bank	Unquiet Ocean	3301	2146	Find a Sealed crate south west of the Little Pearl and sample the contents.
281	12	Crate	-	The Lonely Sea	Unquiet Ocean	2522	2248	Find a Sealed crate near some shipwrecks in the Lonely Sea and sample the contents.
282	57	Weather	-	Aureum Coast	Bonus charts	1779	2940	Help the meteorologist document the local weather south east of Stonecutter Outpost.
283	57	Weather	-	The Everdeep	Shrouded Ocean	1928	2791	Help the meteorologist document the local weather on the western coast of Deepfin Point.
284	72	Weather	Tangled kelp	Southern Expanse	Shrouded Ocean	1881	2445	Help the meteorologist document the local weather on a small islands east of the Isle of Serpents.
285	1	Spyglass	-	Fortis Bay	Shrouded Ocean	1823	3046	Use your spyglass to get a good view of the Fortis Colosseum from the south.
286	1	Spyglass	-	Aureum Coast	Shrouded Ocean	1737	2907	Use your spyglass to get a good view of the south eastern temple in the Avium Savannah from the south.
287	1	Spyglass	-	Wyrm's Waters	Shrouded Ocean	1634	2883	Use your spyglass to get a good view of the Colossal Wyrm Remains from the south.
288	1	Spyglass	-	The Everdeep	Shrouded Ocean	1943	2737	Use your spyglass to get a good view of Deepfin Point from the south.
289	1	Spyglass	-	Sapphire Sea	Shrouded Ocean	1796	2664	Use your spyglass to get a good view of the Crown Jewel from the east.
290	12	Crate	-	Fortis Bay	Shrouded Ocean	1826	2957	Find a Sealed crate east of Stonecutter Outpost and sample the contents.
291	12	Crate	-	Aureum Coast	Shrouded Ocean	1762	2854	Find a Sealed crate in the middle of the Aureum Coast and sample the contents.
292	12	Crate	-	Wyrm's Waters	Shrouded Ocean	1570	2808	Find a Sealed crate near the Shimmering Atoll and sample the contents.
293	12	Crate	-	The Skullhorde	Shrouded Ocean	2387	2477	Find a Sealed crate in the middle of the Skullhorde and sample the contents.
294	12	Crate	-	The Everdeep	Shrouded Ocean	1854	2771	Find a Sealed crate west of Deepfin Point and sample the contents.
295	12	Crate	-	Sapphire Sea	Shrouded Ocean	1675	2478	Find a Sealed crate where three waters meet in the Sapphire Sea and sample the contents.
296	72	Crate	Tangled kelp	Southern Expanse	Shrouded Ocean	1821	2439	Find a Sealed crate near the Isle of Serpents and sample the contents.
297	1	Generic	-	Fortis Bay	Shrouded Ocean	1896	2994	Find a nest on Vatrachos Island.
298	1	Generic	-	Aureum Coast	Shrouded Ocean	1845	2824	Find the wreck of the Counterweight north west of Deepfin Point.
299	1	Generic	-	Wyrm's Waters	Shrouded Ocean	1593	2755	Find an interesting book on the Shimmering Atoll.
300	1	Generic	-	The Everdeep	Shrouded Ocean	1976	2680	Find an odd shadow south of Deepfin Point.
301	1	Generic	-	Sapphire Sea	Shrouded Ocean	1762	2580	Find some odd sea life in the middle of the Sapphire Sea.
302	72	Generic	Tangled kelp	Southern Expanse	Shrouded Ocean	1828	2138	Find a useful sign to the far south of the Isle of Serpents.
303	22	Current	-	Fortis Bay	Shrouded Ocean	1779	2988	Test the currents north east of Stonecutter Outpost.
304	22	Current	-	Wyrm's Waters	Shrouded Ocean	1543	2785	Test the currents within the Shimmering Atoll.
305	22	Current	-	The Skullhorde	Shrouded Ocean	2488	2530	Test the currents west of the Isle of Bones.
306	22	Current	-	Sapphire Sea	Shrouded Ocean	1745	2662	Test the currents west of the Crown Jewel.
307	38	Diving	-	Fortis Bay	Shrouded Ocean	1898	3048	With help from a mermaid guide, document the water depth north of Vatrachos Island. Answer to the riddle: Stripy feather
308	38	Diving	-	Aureum Coast	Shrouded Ocean	1889	2882	With help from a mermaid guide, document the water depth south of Vatrachos Island. Answer to the riddle: 2 tomatoes, and one each of equa leaves, batta tin, cheese, dwellberries, onion, cabbage, and gianne dough
309	38	Diving	-	Wyrm's Waters	Shrouded Ocean	1711	2818	With help from a mermaid guide, document the water depth east of the Shimmering Atoll. Answer to the riddle: Lime
310	38	Diving	-	The Skullhorde	Shrouded Ocean	2460	2449	With help from a mermaid guide, document the water depth south west of the Isle of Bones. Answer to the riddle: Fedora
311	38	Diving	-	Sea of Souls	Shrouded Ocean	2118	2716	With help from a mermaid guide, document the water depth east of the shipyard in the Sea of Souls. Answer to the riddle: Common tench
312	38	Diving	-	The Everdeep	Shrouded Ocean	1942	2650	With help from a mermaid guide, document the water depth in the far south east of the Everdeep. Answer to the riddle: Any Plank
313	72	Diving	Tangled kelp	Southern Expanse	Shrouded Ocean	2057	2317	With help from a mermaid guide, document the water depth west of Sunbleak Island. Answer to the riddle: Ghrazi rapier
314	57	Weather	-	Crabclaw Bay	Bonus charts	1651	3461	Help the meteorologist document the local weather on a small island south of the Woodcutting Guild.
315	57	Weather	-	Litus Lucis	Bonus charts	1714	3193	Help the meteorologist document the local weather on a small island north of Civitas illa Fortis.
316	1	Spyglass	-	Great Sound	Western Ocean	1413	3394	Use your spyglass to get a good view of Auburnvale from the north.
317	1	Spyglass	-	Crabclaw Bay	Western Ocean	1600	3310	Use your spyglass to get a good view of Salvager Overlook from the west.
318	1	Spyglass	-	Crabclaw Bay	Western Ocean	1744	3405	Use your spyglass to get a good view of Crabclaw Island from the south west.
319	1	Spyglass	-	Crystal Sea	Western Ocean	1886	3266	Use your spyglass to get a good view of Port Roberts from the south.
320	1	Spyglass	-	Vagabonds Rest	Western Ocean	2101	3702	Use your spyglass to get a good view of Buccaneers' Haven from the north.
321	38	Diving	-	Crabclaw Bay	Bonus charts	1681	3380	With help from a mermaid guide, document the water depth in the south of Crabclaw Bay. Answer to the riddle: 3 air runes, and one each of silver ore, chisel, uncut jade, ring mould, and cosmic rune
322	38	Diving	-	Hosidian Sea	Bonus charts	1969	3597	With help from a mermaid guide, document the water depth in the east of the Hosidian Sea. Answer to the riddle: Potato and potato cactus
323	38	Diving	-	Pilgrims' Passage	Bonus charts	1880	3372	With help from a mermaid guide, document the water depth north of Port Roberts. Answer to the riddle: Sandstone (2kg), sandstone (1kg), and sandstone (10kg)
324	38	Diving	-	Litus Lucis	Western Ocean	1821	3166	With help from a mermaid guide, document the water depth north of the Fortis Colosseum. Answer to the riddle: Dark bow tie
325	38	Diving	-	Vagabonds Rest	Western Ocean	2018	3705	With help from a mermaid guide, document the water depth west of Buccaneers' Haven. Answer to the riddle: Double eye patch
326	38	Diving	-	Moonshadow	Western Ocean	2044	3792	With help from a mermaid guide, document the water depth in the south west of Lunar Isle. Answer to the riddle: Bucket helm (g)
327	22	Current	-	Great Sound	Bonus charts	1384	3465	Test the currents south of Lacerta Falls.
328	22	Current	-	Litus Lucis	Western Ocean	1721	3237	Test the currents east of the Twilight Temple.
329	22	Current	-	Crystal Sea	Western Ocean	1920	3296	Test the currents east of Port Roberts.
330	22	Current	-	Moonshadow	Western Ocean	2122	3796	Test the currents west of Pirates' Cove.
331	12	Crate	-	Great Sound	Bonus charts	1437	3430	Find a Sealed crate west of Land's End and sample the contents.
332	12	Crate	-	Pilgrims' Passage	Western Ocean	1926	3394	Find a Sealed crate in the middle of Pilgrims' Passage and sample the contents.
333	12	Crate	-	Litus Lucis	Western Ocean	1783	3252	Find a Sealed crate in the middle of Litus Lucis and sample the contents.
334	12	Crate	-	Piscatoris Sea	Western Ocean	2248	3744	Find a Sealed crate north west of the Piscatoris Fishing Colony and sample the contents.
335	12	Crate	-	Moonshadow	Western Ocean	2030	3829	Find a Sealed crate south west of Lunar Isle and sample the contents.
336	1	Generic	-	Great Sound	Western Ocean	1529	3405	Find an interesting sign south east of Land's End.
337	1	Generic	-	Great Sound	Western Ocean	1656	3304	Find an advert on a small island north east of Salvager Overlook.
338	1	Generic	-	Crabclaw Bay	Western Ocean	1789	3401	Find a sabotaged mooring point on Crabclaw Island.
339	1	Generic	-	Pilgrims' Passage	Western Ocean	1887	3424	Find evidence of an experiment on Chinchompa Island.
340	1	Generic	-	Litus Lucis	Western Ocean	1871	3177	Find a strange corpse north east of Civitas illa Fortis.
341	1	Generic	-	Crystal Sea	Western Ocean	1890	3271	Find the visitors book for the Port Roberts jail.
342	1	Generic	-	Moonshadow	Western Ocean	2170	3773	Find the wreck of the Lunar Longship south west of Pirates' Cove.
343	1	Spyglass	-	Sunset Bay	Sunset Ocean	1470	2980	Use your spyglass to get a good view of Sunset Coast from the west.
344	1	Spyglass	-	Misty Sea	Sunset Ocean	1387	2840	Use your spyglass to get a good view of Mistrock from the south.
345	57	Weather	-	Dusk's Maw	Bonus charts	1188	2822	Help the meteorologist document the local weather on a small island north of Laguna Aurorae.
346	22	Current	-	Sunset Bay	Sunset Ocean	1422	3046	Test the currents at the mouth of the River Varla. Note: A raft is recommended but not required to reach this location.
347	22	Current	-	Dusk's Maw	Sunset Ocean	1259	2876	Test the currents south west of the Darkmoon Ravine.
348	38	Diving	-	Sunset Bay	Sunset Ocean	1575	2942	With help from a mermaid guide, document the water depth west of the Colossal Wyrm Remains. Answer to the riddle: Ring mould
349	38	Diving	-	Misty Sea	Sunset Ocean	1322	2628	With help from a mermaid guide, document the water depth in the south west of the Misty Sea. Answer to the riddle: Bob's blue shirt and Bob's purple shirt
350	38	Diving	-	Dusk's Maw	Sunset Ocean	1314	2931	With help from a mermaid guide, document the water depth north of the Darkmoon Ravine. Answer to the riddle: Tarromin
351	1	Generic	-	Sunset Bay	Sunset Ocean	1448	2896	Find a wrecked shiplike thing south of Villa Lucens.
352	1	Generic	-	Misty Sea	Sunset Ocean	1439	2716	Find some discarded fishing equipment in the middle of the Misty Sea.
353	1	Generic	-	Dusk's Maw	Sunset Ocean	1232	3005	Find a collapsed cave on a small island south west of the Tlati Rainforest.
354	12	Crate	-	Sunset Bay	Sunset Ocean	1426	2865	Find a Sealed crate near Mistrock and sample the contents.
355	12	Crate	-	Misty Sea	Sunset Ocean	1561	2655	Find a Sealed crate near some shipwrecks in the Misty Sea and sample the contents.
356	12	Crate	-	Dusk's Maw	Sunset Ocean	1050	2886	Find a Sealed crate in the far west of Dusk's Maw and sample the contents.
357	1	Spyglass	-	Kharidian Sea	Ardent Ocean	3051	9388	Use your spyglass to admire Chartin' Charles McAtless in the cave on the Pandemonium.
            """;

    /** Seas: ocean, sea, level, tasks, completion XP. */
    static final String SEAS = """
Ardent Ocean	Kharidian Sea	1	5	190
Ardent Ocean	Bay of Sarim	1	5	190
Ardent Ocean	Lumbridge Basin	1	3	190
Ardent Ocean	Mudskipper Sound	1	3	190
Ardent Ocean	Rimmington Strait	1	3	190
Ardent Ocean	Catherby Bay	12	4	270
Ardent Ocean	Brimhaven Passage	22	4	510
Ardent Ocean	Strait of Khazard	22	6	510
Ardent Ocean	Feldip Gulf	22	3	510
Ardent Ocean	The Simian Sea	22	4	510
Ardent Ocean	Kharazi Strait	24	6	590
Ardent Ocean	The Storm Tempor	24	4	590
Ardent Ocean	Gu'tanoth Bay	38	4	1710
Ardent Ocean	Oo'glog Channel	38	5	1710
Ardent Ocean	Arrow Passage	38	5	1710
Ardent Ocean	Menaphite Sea	38	5	1710
Unquiet Ocean	Red Reef	38	5	1710
Unquiet Ocean	Anglerfish's Light	38	4	1710
Unquiet Ocean	Bay of Elidinis	38	5	1710
Unquiet Ocean	Pearl Bank	38	4	1710
Unquiet Ocean	The Lonely Sea	38	4	1710
Unquiet Ocean	Tortugan Sea	38	5	2880
Unquiet Ocean	Turtle Belt	40	5	1990
Unquiet Ocean	Sea of Shells	47	5	2880
Shrouded Ocean	Fortis Bay	38	5	1710
Shrouded Ocean	Aureum Coast	38	4	1710
Shrouded Ocean	Wyrm's Waters	38	5	1710
Shrouded Ocean	The Skullhorde	38	4	1710
Shrouded Ocean	Western Gate	38	5	1710
Shrouded Ocean	Sapphire Sea	38	5	1710
Shrouded Ocean	Breakbone Strait	40	4	1990
Shrouded Ocean	Backwater	40	6	1990
Shrouded Ocean	Zul-Egil	40	4	1990
Shrouded Ocean	Mythic Sea	51	4	1990
Shrouded Ocean	Soul Bay	57	5	6710
Shrouded Ocean	Barracuda Belt	57	6	6710
Shrouded Ocean	The Everdeep	57	5	6710
Shrouded Ocean	Sea of Souls	61	4	8750
Shrouded Ocean	Rainbow Reef	72	5	17230
Shrouded Ocean	Southern Expanse	72	4	17230
Sunset Ocean	Sunset Bay	38	5	1710
Sunset Ocean	Misty Sea	38	4	1710
Sunset Ocean	Dusk's Maw	38	4	1710
Western Ocean	Great Sound	1	3	190
Western Ocean	Crabclaw Bay	1	3	190
Western Ocean	Hosidian Sea	12	3	270
Western Ocean	Gulf of Kourend	22	4	510
Western Ocean	Pilgrims' Passage	22	3	1710
Western Ocean	Litus Lucis	38	4	1710
Western Ocean	Crystal Sea	38	6	1710
Western Ocean	Vagabonds Rest	38	5	1710
Western Ocean	Moonshadow	38	4	1710
Western Ocean	Porth Neigwl	66	7	12030
Western Ocean	Tirannwn Bight	66	6	12030
Western Ocean	Porth Gwenith	66	5	12030
Western Ocean	Piscatoris Sea	66	4	12030
Northern Ocean	Lunar Bay	38	5	1710
Northern Ocean	Fremensund	57	6	6710
Northern Ocean	Grandroot Bay	57	6	6710
Northern Ocean	Fremennik Strait	57	5	6710
Northern Ocean	V's Belt	78	5	23910
Northern Ocean	Idestia Strait	78	6	23910
Northern Ocean	Winter's Edge	78	6	23910
Northern Ocean	Lunar Sea	78	4	23910
Northern Ocean	Everwinter Sea	78	6	23910
Northern Ocean	Kannski Tides	78	5	23910
Northern Ocean	Weissmere	78	5	23910
Northern Ocean	Stoneheart Sea	78	6	23910
Northern Ocean	Shiverwake Expanse	78	5	23910
Northern Ocean	Weiss Melt	78	5	23910
            """;

    /** Courier tasks: id, level, xp, board port, cargo port, destination port, cargo, qty. */
    static final String COURIER = """
1	1	78	port_sarim	port_sarim	pandemonium	Crate of platebodies	1
2	1	155	port_sarim	pandemonium	port_sarim	Crate of spices	1
3	10	79	port_sarim	port_sarim	musa_point	Crate of logs	1
4	10	158	port_sarim	musa_point	port_sarim	Crate of logs	1
5	20	289	port_sarim	port_sarim	catherby	Crate of bait	1
6	20	578	port_sarim	catherby	port_sarim	Crate of honey	1
7	1	78	port_sarim	port_sarim	pandemonium	Crate of battleaxes	1
8	18	99	port_sarim	port_sarim	musa_point	Crate of fish	2
9	28	323	port_sarim	port_sarim	catherby	Crate of potatoes	1
10	28	300	port_sarim	port_sarim	ardougne	Crate of salamanders	1
11	28	601	port_sarim	ardougne	port_sarim	Crate of seeds	1
12	30	353	port_sarim	port_sarim	port_khazard	Crate of swamp paste	1
13	30	705	port_sarim	port_khazard	port_sarim	Crate of swords	1
14	25	262	port_sarim	port_sarim	brimhaven	Crate of vodka	1
15	25	554	port_sarim	brimhaven	port_sarim	Crate of nothing sinister	2
16	20	1011	port_sarim	port_sarim	port_piscarilius	Crate of books	2
17	48	655	port_sarim	port_sarim	ruins_of_unkah	Crate of runes	2
18	62	6521	port_sarim	port_sarim	rellekka	Crate of pies	4
19	50	2500	port_sarim	void_knights_outpost	port_sarim	Crate of pest remains	3
20	1	78	pandemonium	pandemonium	port_sarim	Crate of jewellery	1
21	1	155	pandemonium	port_sarim	pandemonium	Crate of steel bars	1
22	10	89	pandemonium	pandemonium	musa_point	Crate of rotten bananas	1
23	10	178	pandemonium	musa_point	pandemonium	Crate of rum	1
24	25	300	pandemonium	pandemonium	brimhaven	Crate of jewellery	2
25	25	601	pandemonium	brimhaven	pandemonium	Crate of pineapples	2
26	1	78	pandemonium	pandemonium	port_sarim	Crate of fish	1
27	18	99	pandemonium	pandemonium	musa_point	Crate of ship parts	1
28	32	304	pandemonium	pandemonium	brimhaven	Crate of cacti	1
29	20	296	pandemonium	pandemonium	catherby	Crate of grog	1
30	20	592	pandemonium	catherby	pandemonium	Crate of arrowtips	1
31	40	655	pandemonium	pandemonium	corsair_cove	Crate of scimitars	3
32	40	1309	pandemonium	corsair_cove	pandemonium	Crate of gold bars	3
33	30	377	pandemonium	pandemonium	port_khazard	Crate of kyatt teeth	1
34	30	755	pandemonium	port_khazard	pandemonium	Crate of swamp paste	1
35	48	308	pandemonium	pandemonium	ruins_of_unkah	Crate of cocktails	2
36	48	693	pandemonium	ruins_of_unkah	pandemonium	Crate of granite	3
37	42	681	pandemonium	pandemonium	cairn_isle	Crate of secret stuff	3
38	28	130	pandemonium	pandemonium	ardougne	Crate of sand	1
39	10	79	musa_point	musa_point	port_sarim	Crate of coconuts	1
40	10	158	musa_point	port_sarim	musa_point	Crate of bananas	1
41	10	89	musa_point	musa_point	pandemonium	Crate of bananas	1
42	10	178	musa_point	pandemonium	musa_point	Crate of eye patches	1
43	25	169	musa_point	musa_point	brimhaven	Crate of rum	1
44	25	339	musa_point	brimhaven	musa_point	Crate of mahogany logs	1
45	15	88	musa_point	musa_point	port_sarim	Crate of logs	1
46	18	110	musa_point	musa_point	pandemonium	Crate of rum	2
47	32	197	musa_point	musa_point	brimhaven	Crate of silver ore	2
48	20	193	musa_point	musa_point	catherby	Crate of coconuts	1
49	20	413	musa_point	catherby	musa_point	Crate of honey	2
50	36	185	musa_point	musa_point	entrana	Crate of bananas	2
51	36	371	musa_point	entrana	musa_point	Crate of beer glasses	2
52	28	223	musa_point	musa_point	ardougne	Crate of bananas	2
53	28	416	musa_point	ardougne	musa_point	Crate of silk	1
54	40	909	musa_point	musa_point	corsair_cove	Crate of peg legs	2
55	45	1001	musa_point	musa_point	the_summer_shore	Crate of bananas	2
56	66	4191	musa_point	musa_point	port_tyras	Crate of snakeskin	6
57	30	541	musa_point	port_khazard	musa_point	Crate of secret stuff	2
58	20	289	catherby	catherby	port_sarim	Crate of flax	1
59	20	578	catherby	port_sarim	catherby	Crate of bait	1
60	28	69	catherby	catherby	ardougne	Crate of fish	2
61	28	308	catherby	ardougne	catherby	Crate of arrowtips	2
62	30	197	catherby	catherby	port_khazard	Crate of bows	2
63	30	394	catherby	port_khazard	catherby	Crate of glassmaking supplies	2
64	26	323	catherby	catherby	port_sarim	Crate of beer	1
65	32	147	catherby	catherby	ardougne	Crate of poison	1
66	38	241	catherby	catherby	port_khazard	Crate of coal	3
67	36	130	catherby	catherby	entrana	Crate of barley	2
68	36	260	catherby	entrana	catherby	Crate of vials	2
69	20	296	catherby	catherby	pandemonium	Crate of wax	1
70	20	591	catherby	pandemonium	catherby	Crate of coconuts	1
71	25	116	catherby	catherby	brimhaven	Crate of fish	1
72	25	231	catherby	brimhaven	catherby	Crate of karambwans	1
73	20	206	catherby	catherby	musa_point	Crate of lobsters	2
74	20	413	catherby	musa_point	catherby	Crate of bananas	2
75	25	1078	catherby	catherby	port_piscarilius	Crate of honey	1
76	50	3147	catherby	void_knights_outpost	catherby	Crate of javelins	3
77	25	185	brimhaven	brimhaven	musa_point	Crate of cocktails	2
78	25	339	brimhaven	musa_point	brimhaven	Crate of fish	1
79	30	107	brimhaven	brimhaven	port_khazard	Crate of spears	1
80	30	246	brimhaven	port_khazard	brimhaven	Crate of iron bars	2
81	40	720	brimhaven	brimhaven	corsair_cove	Crate of swords	3
82	40	1246	brimhaven	corsair_cove	brimhaven	Crate of gems	2
83	32	180	brimhaven	brimhaven	musa_point	Crate of meat	1
84	38	157	brimhaven	brimhaven	port_khazard	Crate of fruit	3
85	46	808	brimhaven	brimhaven	corsair_cove	Crate of beer	3
86	25	285	brimhaven	brimhaven	pandemonium	Crate of meat	1
87	25	601	brimhaven	pandemonium	brimhaven	Crate of spices	2
88	25	131	brimhaven	brimhaven	catherby	Crate of pineapples	2
89	25	231	brimhaven	catherby	brimhaven	Crate of compost	1
90	52	1597	brimhaven	brimhaven	red_rock	Crate of karambwans	3
91	52		brimhaven	red_rock	brimhaven	Crate of machinery parts	4
92	25	277	brimhaven	brimhaven	port_sarim	Crate of amulets	2
93	28	69	brimhaven	brimhaven	ardougne	Crate of fish	1
94	38	945	brimhaven	brimhaven	civitas_illa_fortis	Crate of pineapples	3
95	45	3502	brimhaven	the_summer_shore	brimhaven	Crate of camphor logs	3
96	28	69	ardougne	ardougne	brimhaven	Crate of spices	1
97	28	139	ardougne	brimhaven	ardougne	Crate of gold ore	1
98	28	139	ardougne	ardougne	catherby	Crate of furs	1
99	28	277	ardougne	catherby	ardougne	Crate of arrowtips	1
100	66	3635	ardougne	ardougne	port_tyras	Crate of platebodies	6
101	66	7116	ardougne	port_tyras	ardougne	Crate of rabbit meat	5
102	36	120	ardougne	ardougne	brimhaven	Crate of beer	3
103	34	164	ardougne	ardougne	catherby	Crate of seeds	2
104	70	2323	ardougne	ardougne	port_tyras	Crate of swords	6
105	30	1051	ardougne	ardougne	port_piscarilius	Crate of wine	2
106	30	2068	ardougne	port_piscarilius	ardougne	Crate of furs	1
107	52	1736	ardougne	ardougne	red_rock	Crate of protective clothing	4
108	52		ardougne	red_rock	ardougne	Crate of red coral	4
109	30	99	ardougne	ardougne	port_khazard	Crate of bread	2
110	30	164	ardougne	port_khazard	ardougne	Crate of fish	1
111	45	1866	ardougne	ardougne	the_summer_shore	Crate of jewellery	2
112	38	1046	ardougne	ardougne	civitas_illa_fortis	Crate of historical artefacts	3
113	50	1204	ardougne	ardougne	void_knights_outpost	Crate of platebodies	3
114	28	631	ardougne	port_sarim	ardougne	Crate of runes	2
115	30	154	port_khazard	port_khazard	ardougne	Crate of fish	2
116	30	164	port_khazard	ardougne	port_khazard	Crate of platebodies	1
117	40	468	port_khazard	port_khazard	corsair_cove	Crate of daggers	3
118	40	872	port_khazard	corsair_cove	port_khazard	Crate of vodka	2
119	30	369	port_khazard	port_khazard	port_sarim	Crate of swords	2
120	30	738	port_khazard	port_sarim	port_khazard	Crate of swamp paste	2
121	36	331	port_khazard	port_khazard	ardougne	Crate of sand	3
122	45	577	port_khazard	port_khazard	corsair_cove	Crate of khali brews	3
123	38	417	port_khazard	port_khazard	port_sarim	Crate of fish	2
124	30	180	port_khazard	port_khazard	catherby	Crate of gold ore	1
125	30	361	port_khazard	catherby	port_khazard	Crate of secret stuff	1
126	38	945	port_khazard	port_khazard	civitas_illa_fortis	Crate of pineapples	2
127	38	1926	port_khazard	civitas_illa_fortis	port_khazard	Crate of huasca	3
128	48	982	port_khazard	port_khazard	ruins_of_unkah	Crate of ship parts	3
129	48	1962	port_khazard	ruins_of_unkah	port_khazard	Crate of angler outfit pieces	3
130	62	5527	port_khazard	port_khazard	rellekka	Crate of dragon bitter	4
131	30	977	port_khazard	port_khazard	port_piscarilius	Crate of platebodies	2
132	36	260	port_khazard	port_khazard	entrana	Crate of sand	3
133	30	246	port_khazard	brimhaven	port_khazard	Crate of herbs	2
134	40	468	corsair_cove	corsair_cove	port_khazard	Crate of scimitars	3
135	40	872	corsair_cove	port_khazard	corsair_cove	Crate of platebodies	2
136	50	509	corsair_cove	corsair_cove	void_knights_outpost	Crate of dragonhide	4
137	50	926	corsair_cove	void_knights_outpost	corsair_cove	Crate of arrowtips	3
138	66	2252	corsair_cove	corsair_cove	port_tyras	Crate of herbs	6
139	66	4349	corsair_cove	port_tyras	corsair_cove	Crate of bear meat	5
140	45	577	corsair_cove	corsair_cove	port_khazard	Crate of gunpowder	3
141	55	670	corsair_cove	corsair_cove	void_knights_outpost	Crate of wrath runes	3
142	70	7274	corsair_cove	corsair_cove	port_tyras	Crate of swords	6
143	40	1628	corsair_cove	corsair_cove	port_piscarilius	Crate of jewellery	2
144	40	2961	corsair_cove	port_piscarilius	corsair_cove	Crate of fish	2
145	42	358	corsair_cove	corsair_cove	cairn_isle	Crate of gold bars	2
146	42	788	corsair_cove	cairn_isle	corsair_cove	Crate of gems	3
147	40	720	corsair_cove	corsair_cove	brimhaven	Crate of jewellery	3
148	40	1440	corsair_cove	brimhaven	corsair_cove	Crate of rum	3
149	40	655	corsair_cove	corsair_cove	pandemonium	Crate of spices	3
150	40	1309	corsair_cove	pandemonium	corsair_cove	Crate of ship parts	3
151	40	1216	corsair_cove	corsair_cove	civitas_illa_fortis	Crate of books	2
152	46	1751	corsair_cove	corsair_cove	aldarin	Crate of daggers	3
153	48	693	ruins_of_unkah	ruins_of_unkah	port_sarim	Crate of cacti	3
154	48	1386	ruins_of_unkah	port_sarim	ruins_of_unkah	Crate of runes	3
155	48	789	ruins_of_unkah	ruins_of_unkah	the_summer_shore	Crate of sandstone	2
156	48	1655	ruins_of_unkah	the_summer_shore	ruins_of_unkah	Crate of coral	3
157	52	926	ruins_of_unkah	ruins_of_unkah	red_rock	Crate of granite	3
158	52	1851	ruins_of_unkah	red_rock	ruins_of_unkah	Crate of planks	3
159	54	880	ruins_of_unkah	ruins_of_unkah	port_sarim	Crate of kebabs	4
160	56	1509	ruins_of_unkah	ruins_of_unkah	the_summer_shore	Crate of cacti	4
161	60		ruins_of_unkah	ruins_of_unkah	red_rock	Crate of fish	4
162	48	347	ruins_of_unkah	ruins_of_unkah	pandemonium	Crate of cacti	3
163	48	616	ruins_of_unkah	pandemonium	ruins_of_unkah	Crate of cocktails	2
164	50	1088	ruins_of_unkah	ruins_of_unkah	void_knights_outpost	Crate of granite	4
165	50	2175	ruins_of_unkah	void_knights_outpost	ruins_of_unkah	Crate of javelins	4
166	48	1116	ruins_of_unkah	ruins_of_unkah	ardougne	Crate of silk	3
167	48	2154	ruins_of_unkah	ardougne	ruins_of_unkah	Crate of fishing supplies	2
168	50	1295	ruins_of_unkah	ruins_of_unkah	brimhaven	Crate of sandstone	4
169	48	1116	ruins_of_unkah	ruins_of_unkah	catherby	Crate of silk	2
170	67	3914	ruins_of_unkah	ruins_of_unkah	deepfin_point	Crate of granite	5
171	48	1885	ruins_of_unkah	port_khazard	ruins_of_unkah	Crate of ship parts	2
172	52	625	void_knights_outpost	void_knights_outpost	red_rock	Crate of runes	4
173	52	1250	void_knights_outpost	red_rock	void_knights_outpost	Crate of herbs	4
174	50	1180	void_knights_outpost	void_knights_outpost	the_summer_shore	Crate of pest remains	3
175	50	2453	void_knights_outpost	the_summer_shore	void_knights_outpost	Crate of logs	4
176	67	2647	void_knights_outpost	void_knights_outpost	deepfin_point	Crate of javelins	6
177	67	5140	void_knights_outpost	deepfin_point	void_knights_outpost	Crate of ores	5
178	60	1034	void_knights_outpost	void_knights_outpost	red_rock	Crate of fish	5
179	55	2360	void_knights_outpost	void_knights_outpost	the_summer_shore	Crate of javelins	3
180	70	2727	void_knights_outpost	void_knights_outpost	deepfin_point	Crate of pest remains	6
181	50	1295	void_knights_outpost	void_knights_outpost	port_sarim	Crate of runes	4
182	50	2500	void_knights_outpost	port_sarim	void_knights_outpost	Crate of potions	3
183	55	3218	void_knights_outpost	void_knights_outpost	civitas_illa_fortis	Crate of runes	4
184	55	6302	void_knights_outpost	civitas_illa_fortis	void_knights_outpost	Crate of maces	3
185	50	2176	void_knights_outpost	void_knights_outpost	port_roberts	Crate of fish	4
186	50	4258	void_knights_outpost	port_roberts	void_knights_outpost	Crate of seeds	3
187	52	1018	void_knights_outpost	void_knights_outpost	port_khazard	Crate of arrowtips	4
188	66	2529	void_knights_outpost	void_knights_outpost	port_tyras	Crate of arrowtips	5
189	50	1574	void_knights_outpost	void_knights_outpost	catherby	Crate of arrowtips	3
190	50	1019	void_knights_outpost	corsair_cove	void_knights_outpost	Crate of potions	4
191	48	789	the_summer_shore	the_summer_shore	ruins_of_unkah	Crate of calquat	2
192	48	1577	the_summer_shore	ruins_of_unkah	the_summer_shore	Crate of rope	2
193	50	1226	the_summer_shore	the_summer_shore	void_knights_outpost	Crate of coral	4
194	50	1710	the_summer_shore	void_knights_outpost	the_summer_shore	Crate of javelins	3
195	45	1001	the_summer_shore	the_summer_shore	pandemonium	Crate of coral	2
196	45	2001	the_summer_shore	pandemonium	the_summer_shore	Crate of gems	2
197	54	1041	the_summer_shore	the_summer_shore	ruins_of_unkah	Crate of fish	4
198	55	1710	the_summer_shore	the_summer_shore	void_knights_outpost	Crate of spears	3
199	52	1250	the_summer_shore	the_summer_shore	pandemonium	Crate of sea shells	3
200	45	1347	the_summer_shore	the_summer_shore	port_sarim	Crate of camphor logs	3
201	45	2616	the_summer_shore	port_sarim	the_summer_shore	Crate of shields	2
202	46	2712	the_summer_shore	the_summer_shore	aldarin	Crate of crab paste	3
203	46	5348	the_summer_shore	aldarin	the_summer_shore	Crate of potions	2
204	50	3078	the_summer_shore	the_summer_shore	port_roberts	Crate of coral	3
205	50	6155	the_summer_shore	port_roberts	the_summer_shore	Crate of meat	3
206	67	4230	the_summer_shore	the_summer_shore	deepfin_point	Crate of camphor logs	5
207	45	1462	the_summer_shore	the_summer_shore	port_khazard	Crate of crab paste	2
208	50	3285	the_summer_shore	the_summer_shore	civitas_illa_fortis	Crate of coral	4
209	45	2001	the_summer_shore	musa_point	the_summer_shore	Crate of pineapples	2
210	52	1319	red_rock	red_rock	port_khazard	Crate of red coral	3
211	52	2639	red_rock	port_khazard	red_rock	Crate of fish	3
212	67	3319	red_rock	red_rock	deepfin_point	Crate of planks	6
213	67	6479	red_rock	deepfin_point	red_rock	Crate of ores	5
214	52	1319	red_rock	red_rock	port_sarim	Crate of red coral	4
215	52	2639	red_rock	port_sarim	red_rock	Crate of battleaxes	4
216	58	1907	red_rock	red_rock	port_khazard	Crate of planks	3
217	70	3500	red_rock	red_rock	deepfin_point	Crate of logs	7
218	55	1840	red_rock	red_rock	port_sarim	Crate of fish	3
219	56	3513	red_rock	red_rock	port_roberts	Crate of red coral	4
220	56		red_rock	port_roberts	red_rock	Crate of spices	4
221	52	625	red_rock	red_rock	void_knights_outpost	Crate of ores	4
222	52	1157	red_rock	void_knights_outpost	red_rock	Crate of runes	3
223	60	4564	red_rock	red_rock	port_piscarilius	Crate of red coral	5
224	60	9129	red_rock	port_piscarilius	red_rock	Crate of fish	5
225	60		red_rock	red_rock	lands_end	Crate of fish	4
226	52	1736	red_rock	red_rock	ardougne	Crate of red coral	4
227	52	1805	red_rock	red_rock	catherby	Crate of jewellery	3
228	66	6716	red_rock	port_tyras	red_rock	Crate of furs	6
229	5	91	lands_end	lands_end	hosidius	Crate of rope	1
230	5	183	lands_end	hosidius	lands_end	Crate of vegetables	1
231	15	253	lands_end	lands_end	port_piscarilius	Crate of planks	1
232	15	506	lands_end	port_piscarilius	lands_end	Crate of fish	1
233	50	810	lands_end	lands_end	port_roberts	Crate of berries	4
234	50	1527	lands_end	port_roberts	lands_end	Crate of wine	3
235	5	91	lands_end	lands_end	hosidius	Crate of xerician fabrics	1
236	20	317	lands_end	lands_end	port_piscarilius	Crate of furs	1
237	55	1174	lands_end	lands_end	port_roberts	Crate of meat	4
238	66	2491	lands_end	lands_end	port_tyras	Crate of chinchompas	6
239	66	4823	lands_end	port_tyras	lands_end	Crate of bear meat	5
240	70	2526	lands_end	lands_end	prifddinas	Crate of xerician fabrics	7
241	70	4374	lands_end	prifddinas	lands_end	Crate of gems	7
242	75	3646	lands_end	lands_end	piscatoris	Crate of furs	8
243	75	2411	lands_end	piscatoris	lands_end	Crate of kebbit fur	8
244	67	1956	lands_end	lands_end	deepfin_point	Crate of logs	5
245	30	960	lands_end	lands_end	port_khazard	Crate of chainbodies	1
246	76	3827	lands_end	lands_end	lunar_isle	Crate of berries	8
247	40	2961	lands_end	corsair_cove	lands_end	Crate of swords	2
248	15	264	port_piscarilius	port_piscarilius	lands_end	Crate of fish	2
249	15	506	port_piscarilius	lands_end	port_piscarilius	Crate of planks	1
250	50	810	port_piscarilius	port_piscarilius	port_roberts	Crate of fish	3
251	50	1619	port_piscarilius	port_roberts	port_piscarilius	Crate of gin	3
252	75	2250	port_piscarilius	port_piscarilius	piscatoris	Crate of fish	8
253	75	4501	port_piscarilius	piscatoris	port_piscarilius	Crate of kebbit fur	8
254	20	330	port_piscarilius	port_piscarilius	lands_end	Crate of meat	2
255	55	1174	port_piscarilius	port_piscarilius	port_roberts	Crate of jewellery	3
256	78	2161	port_piscarilius	port_piscarilius	piscatoris	Crate of vodka	7
257	15	193	port_piscarilius	port_piscarilius	hosidius	Crate of fish	2
258	15	385	port_piscarilius	hosidius	port_piscarilius	Crate of seeds	2
259	76	2025	port_piscarilius	port_piscarilius	lunar_isle	Crate of fish	7
260	76	4231	port_piscarilius	lunar_isle	port_piscarilius	Crate of books	8
261	20	997	port_piscarilius	port_piscarilius	port_sarim	Crate of books	1
262	20	2022	port_piscarilius	port_sarim	port_piscarilius	Crate of books	2
263	66	2253	port_piscarilius	port_piscarilius	port_tyras	Crate of furs	5
264	20	990	port_piscarilius	port_piscarilius	musa_point	Crate of high importance	2
265	62	2316	port_piscarilius	port_piscarilius	rellekka	Crate of beer	5
266	70	4156	port_piscarilius	prifddinas	port_piscarilius	Crate of gems	6
267	46	1270	civitas_illa_fortis	civitas_illa_fortis	aldarin	Crate of platebodies	3
268	46	2463	civitas_illa_fortis	aldarin	civitas_illa_fortis	Crate of oranges	2
269	38	453	civitas_illa_fortis	civitas_illa_fortis	port_piscarilius	Crate of antelope meat	3
270	38	908	civitas_illa_fortis	port_piscarilius	civitas_illa_fortis	Crate of vegetables	3
271	50	486	civitas_illa_fortis	civitas_illa_fortis	port_roberts	Crate of gems	3
272	50	1064	civitas_illa_fortis	port_roberts	civitas_illa_fortis	Crate of tokens	4
273	52	1528	civitas_illa_fortis	civitas_illa_fortis	aldarin	Crate of furs	3
274	38	436	civitas_illa_fortis	civitas_illa_fortis	port_piscarilius	Crate of jewellery	2
275	55	772	civitas_illa_fortis	civitas_illa_fortis	port_roberts	Crate of silk	4
276	67	2175	civitas_illa_fortis	civitas_illa_fortis	deepfin_point	Crate of alpaca wool	6
277	67	4191	civitas_illa_fortis	deepfin_point	civitas_illa_fortis	Crate of ores	5
278	70	2078	civitas_illa_fortis	civitas_illa_fortis	prifddinas	Crate of clothes	7
279	70	4156	civitas_illa_fortis	prifddinas	civitas_illa_fortis	Crate of seeds	7
280	44	1147	civitas_illa_fortis	civitas_illa_fortis	sunset_coast	Crate of maces	3
281	44	2223	civitas_illa_fortis	sunset_coast	civitas_illa_fortis	Crate of fish	2
282	40	2026	civitas_illa_fortis	civitas_illa_fortis	port_sarim	Crate of sunbeam ale	2
283	40	2025	civitas_illa_fortis	civitas_illa_fortis	ardougne	Crate of spices	3
284	45	2655	civitas_illa_fortis	civitas_illa_fortis	the_summer_shore	Crate of leather	2
285	52		civitas_illa_fortis	red_rock	civitas_illa_fortis	Crate of red coral	4
286	46	212	aldarin	aldarin	sunset_coast	Crate of beer	2
287	46	424	aldarin	sunset_coast	aldarin	Crate of furs	2
288	46	1231	aldarin	aldarin	civitas_illa_fortis	Crate of fruit	2
289	46	2463	aldarin	civitas_illa_fortis	aldarin	Crate of spears	2
290	67	1898	aldarin	aldarin	deepfin_point	Crate of pickaxes	6
291	67	3795	aldarin	deepfin_point	aldarin	Crate of nickel ore	6
292	50	347	aldarin	aldarin	sunset_coast	Crate of wine	4
293	52	1574	aldarin	aldarin	civitas_illa_fortis	Crate of gems	4
294	70	2037	aldarin	aldarin	deepfin_point	Crate of machinery parts	7
295	50	1527	aldarin	aldarin	port_roberts	Crate of wine	3
296	50	3055	aldarin	port_roberts	aldarin	Crate of jewellery	3
297	70	4072	aldarin	aldarin	prifddinas	Crate of potions	7
298	70	7082	aldarin	prifddinas	aldarin	Crate of dyes	7
299	50	1018	aldarin	aldarin	void_knights_outpost	Crate of potions	4
300	50	4674	aldarin	void_knights_outpost	aldarin	Crate of pest remains	4
301	66	2686	aldarin	aldarin	port_tyras	Crate of shields	5
302	46	2406	aldarin	aldarin	brimhaven	Crate of wine	2
303	62	4707	aldarin	aldarin	rellekka	Crate of wine	4
304	75	10443	aldarin	piscatoris	aldarin	Crate of monkfish	9
305	50	856	port_roberts	port_roberts	port_piscarilius	Crate of cannonballs	4
306	50	2175	port_roberts	port_piscarilius	port_roberts	Crate of fish	3
307	50	486	port_roberts	port_roberts	civitas_illa_fortis	Crate of spices	3
308	50	1064	port_roberts	civitas_illa_fortis	port_roberts	Crate of antelope fur	4
309	50	810	port_roberts	port_roberts	lands_end	Crate of ores	4
310	50	1527	port_roberts	lands_end	port_roberts	Crate of seeds	3
311	55	1703	port_roberts	port_roberts	port_piscarilius	Crate of jewellery	3
312	55	705	port_roberts	port_roberts	civitas_illa_fortis	Crate of ship parts	3
313	55	1174	port_roberts	port_roberts	lands_end	Crate of secret things	4
314	67	1819	port_roberts	port_roberts	deepfin_point	Crate of planks	5
315	67	4033	port_roberts	deepfin_point	port_roberts	Crate of ores	5
316	76	3015	port_roberts	port_roberts	lunar_isle	Crate of furs	8
317	76	6032	port_roberts	lunar_isle	port_roberts	Crate of runes	8
318	60	4952	port_roberts	port_roberts	catherby	Crate of silver ore	5
319	60	9905	port_roberts	catherby	port_roberts	Crate of honey	5
320	60	5060	port_roberts	port_roberts	port_sarim	Crate of seeds	5
321	62	2636	port_roberts	port_roberts	rellekka	Crate of ship parts	4
322	60	3837	port_roberts	port_roberts	red_rock	Crate of herbs	4
323	55	8112	port_roberts	ardougne	port_roberts	Crate of silk	3
324	67	2094	deepfin_point	deepfin_point	port_roberts	Crate of nickel ore	6
325	67	4033	deepfin_point	port_roberts	deepfin_point	Crate of fabrics	5
326	67	1819	deepfin_point	deepfin_point	port_tyras	Crate of mithril ore	6
327	67	1954	deepfin_point	port_tyras	deepfin_point	Crate of logs	6
328	67	1819	deepfin_point	deepfin_point	aldarin	Crate of adamantite ore	5
329	67	3795	deepfin_point	aldarin	deepfin_point	Crate of fruit	6
330	70	2157	deepfin_point	deepfin_point	port_roberts	Crate of silver ore	6
331	70	1875	deepfin_point	deepfin_point	port_tyras	Crate of coal	6
332	70	1956	deepfin_point	deepfin_point	aldarin	Crate of lead ore	6
333	67	2094	deepfin_point	deepfin_point	civitas_illa_fortis	Crate of nickel ore	5
334	67	4349	deepfin_point	civitas_illa_fortis	deepfin_point	Crate of alpaca wool	6
335	67	3123	deepfin_point	deepfin_point	port_piscarilius	Crate of coal	5
336	67	2445	deepfin_point	port_piscarilius	deepfin_point	Crate of planks	5
337	75	3961	deepfin_point	deepfin_point	red_rock	Crate of coal	8
338	75	7921	deepfin_point	red_rock	deepfin_point	Crate of fabrics	8
339	72	4889	deepfin_point	deepfin_point	rellekka	Crate of lead ore	7
340	72	9605	deepfin_point	rellekka	deepfin_point	Crate of warhammers	6
341	70	2853	deepfin_point	deepfin_point	hosidius	Crate of lead ore	7
342	75	4997	deepfin_point	deepfin_point	the_summer_shore	Crate of nickel ore	7
343	70	1221	port_tyras	port_tyras	prifddinas	Crate of logs	7
344	70	2445	port_tyras	prifddinas	port_tyras	Crate of vegetables	7
345	66	1542	port_tyras	port_tyras	port_roberts	Crate of furs	5
346	66	3637	port_tyras	port_roberts	port_tyras	Crate of platebodies	5
347	67	2016	port_tyras	port_tyras	deepfin_point	Crate of planks	6
348	67	1441	port_tyras	deepfin_point	port_tyras	Crate of ores	5
349	72	1141	port_tyras	port_tyras	prifddinas	Crate of halberds	6
350	68	1620	port_tyras	port_tyras	port_roberts	Crate of bows	6
351	70	3637	port_tyras	port_tyras	deepfin_point	Crate of logs	7
352	66	3558	port_tyras	port_tyras	ardougne	Crate of halberds	5
353	66	3748	port_tyras	ardougne	port_tyras	Crate of swords	6
354	66	2411	port_tyras	port_tyras	lands_end	Crate of furs	5
355	66	4823	port_tyras	lands_end	port_tyras	Crate of seeds	5
356	66	2333	port_tyras	port_tyras	port_piscarilius	Crate of planks	6
357	66	4504	port_tyras	port_piscarilius	port_tyras	Crate of fish	5
358	66	3676	port_tyras	port_tyras	brimhaven	Crate of bear meat	6
359	76	3286	port_tyras	port_tyras	lunar_isle	Crate of logs	7
360	70		port_tyras	port_tyras	red_rock	Crate of ship parts	6
361	66	5534	port_tyras	aldarin	port_tyras	Crate of fruit	6
362	70	1141	prifddinas	prifddinas	port_tyras	Crate of fabrics	6
363	70	2279	prifddinas	port_tyras	prifddinas	Crate of logs	6
364	70	1671	prifddinas	prifddinas	port_roberts	Crate of logs	7
365	70	3175	prifddinas	port_roberts	prifddinas	Crate of fish	6
366	72	4482	prifddinas	prifddinas	ardougne	Crate of crystal seeds	7
367	72	8801	prifddinas	ardougne	prifddinas	Crate of silk	6
368	75	1440	prifddinas	prifddinas	port_tyras	Crate of ores	8
369	75	1846	prifddinas	prifddinas	port_roberts	Crate of staffs	7
370	78	5131	prifddinas	prifddinas	ardougne	Crate of jewellery	9
371	70	6247	prifddinas	prifddinas	deepfin_point	Crate of planks	7
372	70	4721	prifddinas	deepfin_point	prifddinas	Crate of ores	6
373	70	3545	prifddinas	prifddinas	aldarin	Crate of crystal seeds	7
374	70	7082	prifddinas	aldarin	prifddinas	Crate of potions	7
375	70	1996	prifddinas	prifddinas	civitas_illa_fortis	Crate of staffs	6
376	70	4156	prifddinas	civitas_illa_fortis	prifddinas	Crate of spices	7
377	76	2746	prifddinas	prifddinas	lunar_isle	Crate of crystal seeds	8
378	70	3134	prifddinas	prifddinas	rellekka	Crate of planks	7
379	72	3260	prifddinas	prifddinas	void_knights_outpost	Crate of runes	6
380	70	4319	prifddinas	port_piscarilius	prifddinas	Crate of books	7
381	65	870	rellekka	rellekka	etceteria	Crate of fish	6
382	65	1738	rellekka	etceteria	rellekka	Crate of teak logs	6
383	68	1265	rellekka	rellekka	neitiznot	Crate of coal	5
384	68	2529	rellekka	neitiznot	rellekka	Crate of rope	5
385	62	4778	rellekka	rellekka	sunset_coast	Crate of warhammers	5
386	62	9557	rellekka	sunset_coast	rellekka	Crate of fabrics	5
387	70	978	rellekka	rellekka	etceteria	Crate of warhammers	7
388	72	1386	rellekka	rellekka	neitiznot	Crate of sand	6
389	66	5376	rellekka	rellekka	sunset_coast	Crate of fish	6
390	62	2708	rellekka	rellekka	port_roberts	Crate of fish	5
391	62	5278	rellekka	port_roberts	rellekka	Crate of planks	4
392	75	1935	rellekka	rellekka	piscatoris	Crate of furs	8
393	75	3691	rellekka	piscatoris	rellekka	Crate of monkfish	7
394	62	2245	rellekka	rellekka	port_piscarilius	Crate of furs	4
395	62	4494	rellekka	port_piscarilius	rellekka	Crate of redwood logs	4
396	68	1107	rellekka	rellekka	jatizso	Crate of logs	5
397	68	2371	rellekka	jatizso	rellekka	Crate of adamantite ore	6
398	67	4583	rellekka	rellekka	deepfin_point	Crate of furs	5
399	66	6953	rellekka	port_tyras	rellekka	Crate of halberds	6
400	65	869	etceteria	etceteria	rellekka	Crate of fish	6
401	65	1581	etceteria	rellekka	etceteria	Crate of iron ore	5
402	68	988	etceteria	etceteria	jatizso	Crate of fish	5
403	68	2135	etceteria	jatizso	etceteria	Crate of adamantite ore	6
404	65	3004	etceteria	etceteria	port_roberts	Crate of teak logs	5
405	65	6005	etceteria	port_roberts	etceteria	Crate of fruit	5
406	70	977	etceteria	etceteria	rellekka	Crate of teak logs	7
407	72	1182	etceteria	etceteria	jatizso	Crate of flax	7
408	70	3175	etceteria	etceteria	port_roberts	Crate of flax	6
409	65	2569	etceteria	etceteria	port_piscarilius	Crate of mahogany logs	5
410	65	5294	etceteria	port_piscarilius	etceteria	Crate of planks	6
411	68	1423	etceteria	etceteria	neitiznot	Crate of mahogany logs	6
412	68	2686	etceteria	neitiznot	etceteria	Crate of yak hair	5
413	65	5294	etceteria	etceteria	sunset_coast	Crate of fabrics	5
414	65	10587	etceteria	sunset_coast	etceteria	Crate of swords	5
415	75	1935	etceteria	etceteria	piscatoris	Crate of flax	8
416	75	4051	etceteria	piscatoris	etceteria	Crate of monkfish	9
417	67	4662	etceteria	etceteria	deepfin_point	Crate of mahogany logs	6
418	68	6637	etceteria	hosidius	etceteria	Crate of onions	5
419	76	2025	lunar_isle	lunar_isle	port_piscarilius	Crate of fabrics	7
420	76	4231	lunar_isle	port_piscarilius	lunar_isle	Crate of fish	8
421	76	1575	lunar_isle	lunar_isle	piscatoris	Crate of fabrics	8
422	76	3150	lunar_isle	piscatoris	lunar_isle	Crate of kebbit fur	8
423	76	2746	lunar_isle	lunar_isle	prifddinas	Crate of herbs	8
424	76	5491	lunar_isle	prifddinas	lunar_isle	Crate of potions	8
425	80	2464	lunar_isle	lunar_isle	port_piscarilius	Crate of astral runes	9
426	80	1760	lunar_isle	lunar_isle	piscatoris	Crate of potions	8
427	80	3168	lunar_isle	lunar_isle	prifddinas	Crate of astral runes	9
428	76	4636	lunar_isle	lunar_isle	deepfin_point	Crate of suqah hides	8
429	76	9092	lunar_isle	deepfin_point	lunar_isle	Crate of coal	7
430	76	2926	lunar_isle	lunar_isle	port_roberts	Crate of fabrics	7
431	76	5851	lunar_isle	port_roberts	lunar_isle	Crate of fish	7
432	76	3781	lunar_isle	lunar_isle	civitas_illa_fortis	Crate of astral runes	8
433	76	9337	lunar_isle	civitas_illa_fortis	lunar_isle	Crate of antelope fur	7
434	76	6571	lunar_isle	lunar_isle	red_rock	Crate of astral runes	8
435	76	13143	lunar_isle	red_rock	lunar_isle	Crate of red coral	8
436	80	4374	lunar_isle	lunar_isle	lands_end	Crate of gems	9
437	80	12671	lunar_isle	aldarin	lunar_isle	Crate of potions	8
603	64	4921	rellekka	rellekka	void_knights_outpost	Crate of clothes	5
604	64	9842	rellekka	void_knights_outpost	rellekka	Crate of arrowtips	5
            """;

    /** Core boat parts: part, boat, sailing, construction, name, wiki materials, construction xp, stat. */
    static final String PARTS = """
hull	raft	1	1	Wooden raft	logs:10;rope:6;swamp_tar:10	19	1.5
hull	raft	20	8	Oak raft	oak_logs:10;rope:6;swamp_tar:10	27	1.5
hull	raft	31	23	Teak raft	teak_logs:10;rope:6;swamp_tar:10;lead_bar:5	41	2.0
hull	raft	48	41	Mahogany raft	mahogany_logs:10;rope:6;swamp_tar:10;lead_bar:5	46	2.0
hull	raft	67	59	Camphor raft	camphor_logs:10;rope:6;swamp_tar:10;lead_bar:5	70	2.5
hull	raft	81	75	Ironwood raft	ironwood_logs:10;rope:6;swamp_tar:10;cupronickel_bar:5	84	2.5
hull	raft	93	84	Rosewood raft	rosewood_logs:10;rope:6;swamp_tar:10;cupronickel_bar:5	93	3.0
hull	skiff	15	1	Wooden skiff	wooden_hull_parts:10;bronze_nails:300;swamp_tar:20	128	1.5
hull	skiff	20	8	Oak skiff	oak_hull_parts:10;iron_nails:300;swamp_tar:20	238	1.5
hull	skiff	31	23	Teak skiff	teak_hull_parts:10;steel_nails:300;swamp_tar:20;lead_bar:5	345	2.0
hull	skiff	48	41	Mahogany skiff	mahogany_hull_parts:10;mithril_nails:300;swamp_tar:20;lead_bar:5	588	2.0
hull	skiff	67	59	Camphor skiff	camphor_hull_parts:10;adamantite_nails:300;swamp_tar:20;lead_bar:5	881	2.5
hull	skiff	81	75	Ironwood skiff	ironwood_hull_parts:10;rune_nails:300;swamp_tar:20;cupronickel_bar:5	1771	2.5
hull	skiff	93	84	Rosewood skiff	rosewood_hull_parts:10;dragon_nails:300;swamp_tar:20;cupronickel_bar:5	2839	3.0
hull	sloop	50	1	Wooden sloop	large_wooden_hull_parts:16;bronze_nails:600;swamp_tar:25	199	1.5
hull	sloop	50	8	Oak sloop	large_oak_hull_parts:16;iron_nails:600;swamp_tar:25	373	1.5
hull	sloop	50	23	Teak sloop	large_teak_hull_parts:16;steel_nails:600;swamp_tar:25;lead_bar:5	538	2.0
hull	sloop	50	41	Mahogany sloop	large_mahogany_hull_parts:16;mithril_nails:600;swamp_tar:25;lead_bar:5	919	2.0
hull	sloop	67	59	Camphor sloop	large_camphor_hull_parts:16;adamantite_nails:600;swamp_tar:25;lead_bar:5	1380	2.5
hull	sloop	81	75	Ironwood sloop	large_ironwood_hull_parts:16;rune_nails:600;swamp_tar:25;cupronickel_bar:5	2779	2.5
hull	sloop	93	84	Rosewood sloop	large_rosewood_hull_parts:16;dragon_nails:600;swamp_tar:25;cupronickel_bar:5	4474	3.0
sails	raft	1	1	Wooden mast and linen sails	logs:5;bronze_nails:20;bolt_of_linen:5	20	10.5
sails	raft	24	11	Oak mast and linen sails	oak_logs:5;iron_nails:20;bolt_of_linen:5	28	19.5
sails	raft	36	26	Teak mast and canvas sails	teak_logs:5;steel_nails:20;bolt_of_canvas:5	41	30
sails	raft	52	45	Mahogany mast and canvas sails	mahogany_logs:5;mithril_nails:20;bolt_of_canvas:5	55	48
sails	raft	68	60	Camphor mast and canvas sails	camphor_logs:5;adamantite_nails:20;bolt_of_canvas:5	85	64
sails	raft	83	77	Ironwood mast and cotton sails	ironwood_logs:5;rune_nails:20;bolt_of_cotton:5	152	80
sails	raft	94	85	Rosewood mast and cotton sails	rosewood_logs:5;dragon_nails:20;bolt_of_cotton:5	175	125
sails	skiff	1	1	Wooden mast and linen sails	logs:10;bronze_nails:40;bolt_of_linen:5	24	10.5
sails	skiff	24	11	Oak mast and linen sails	oak_logs:10;iron_nails:40;bolt_of_linen:5	36	19.5
sails	skiff	36	26	Teak mast and canvas sails	teak_logs:10;steel_nails:40;bolt_of_canvas:5	49	30
sails	skiff	52	45	Mahogany mast and canvas sails	mahogany_logs:10;mithril_nails:40;bolt_of_canvas:5	70	48
sails	skiff	68	60	Camphor mast and canvas sails	camphor_logs:10;adamantite_nails:40;bolt_of_canvas:5	110	64
sails	skiff	83	77	Ironwood mast and cotton sails	ironwood_logs:10;rune_nails:40;bolt_of_cotton:5	197	80
sails	skiff	94	85	Rosewood mast and cotton sails	rosewood_logs:10;dragon_nails:40;bolt_of_cotton:5	231	125
sails	sloop	50	1	Wooden mast and linen sails	logs:15;bronze_nails:60;bolt_of_linen:10	31	10.5
sails	sloop	50	11	Oak mast and linen sails	oak_logs:15;iron_nails:60;bolt_of_linen:10	46	19.5
sails	sloop	50	26	Teak mast and canvas sails	teak_logs:15;steel_nails:60;bolt_of_canvas:10	65	30
sails	sloop	52	45	Mahogany mast and canvas sails	mahogany_logs:15;mithril_nails:60;bolt_of_canvas:10	90	48
sails	sloop	68	60	Camphor mast and canvas sails	camphor_logs:15;adamantite_nails:60;bolt_of_canvas:10	139	64
sails	sloop	83	77	Ironwood mast and cotton sails	ironwood_logs:15;rune_nails:60;bolt_of_cotton:10	250	80
sails	sloop	94	85	Rosewood mast and cotton sails	rosewood_logs:15;dragon_nails:60;bolt_of_cotton:10	291	125
helm	raft	1	1	Bronze helm	plank:2;bronze_bar:4	76	None
helm	raft	17	14	Iron helm	oak_plank:2;iron_bar:4	160	Gentle
helm	raft	38	30	Steel helm	teak_plank:2;steel_bar:4	260	Gentle
helm	raft	55	47	Mithril helm	mahogany_plank:2;mithril_bar:4	400	Strong
helm	raft	72	59	Adamant helm	camphor_plank:2;adamantite_bar:4	480	Strong
helm	raft	87	81	Rune helm	ironwood_plank:2;runite_bar:4	650	Powerful
helm	raft	96	86	Dragon helm	rosewood_plank:2;dragon_metal_sheet:4	780	Deadly
helm	skiff	1	1	Bronze helm	plank:3;bronze_bar:6	114	None
helm	skiff	17	14	Iron helm	oak_plank:3;iron_bar:6	240	Gentle
helm	skiff	38	30	Steel helm	teak_plank:3;steel_bar:6	390	Gentle
helm	skiff	55	47	Mithril helm	mahogany_plank:3;mithril_bar:6	600	Strong
helm	skiff	72	59	Adamant helm	camphor_plank:3;adamantite_bar:6	720	Strong
helm	skiff	87	81	Rune helm	ironwood_plank:3;runite_bar:6	975	Powerful
helm	skiff	96	86	Dragon helm	rosewood_plank:3;dragon_metal_sheet:6	1410	Deadly
helm	sloop	1	1	Bronze helm	plank:4;bronze_bar:8	152	None
helm	sloop	17	14	Iron helm	oak_plank:4;iron_bar:8	320	Gentle
helm	sloop	38	30	Steel helm	teak_plank:4;steel_bar:8	520	Gentle
helm	sloop	55	47	Mithril helm	mahogany_plank:4;mithril_bar:8	800	Strong
helm	sloop	72	59	Adamant helm	camphor_plank:4;adamantite_bar:8	960	Strong
helm	sloop	87	81	Rune helm	ironwood_plank:4;runite_bar:8	1300	Powerful
helm	sloop	96	86	Dragon helm	rosewood_plank:4;dragon_metal_sheet:8	1560	Deadly
keel	skiff	15	1	Bronze skiff keel	bronze_keel_parts:10	100	100
keel	skiff	22	17	Iron skiff keel	iron_keel_parts:10	200	200
keel	skiff	39	32	Steel skiff keel	steel_keel_parts:10;lead_bar:5	311	300
keel	skiff	54	50	Mithril skiff keel	mithril_keel_parts:10;lead_bar:5	511	400
keel	skiff	66	62	Adamant skiff keel	adamant_keel_parts:10;lead_bar:5	761	600
keel	skiff	85	78	Rune skiff keel	rune_keel_parts:10;cupronickel_bar:5	1511	800
keel	skiff	97	87	Dragon skiff keel	dragon_keel_parts:10;cupronickel_bar:5	2511	1000
keel	sloop	50	1	Bronze sloop keel	large_bronze_keel_parts:16	160	100
keel	sloop	50	17	Iron sloop keel	large_iron_keel_parts:16	320	200
keel	sloop	50	32	Steel sloop keel	large_steel_keel_parts:16;lead_bar:5	491	300
keel	sloop	54	50	Mithril sloop keel	large_mithril_keel_parts:16;lead_bar:5	811	400
keel	sloop	66	62	Adamant sloop keel	large_adamant_keel_parts:16;lead_bar:5	1211	600
keel	sloop	85	78	Rune sloop keel	large_rune_keel_parts:16;cupronickel_bar:5	2411	800
keel	sloop	97	87	Dragon sloop keel	large_dragon_keel_parts:16;cupronickel_bar:5	4011	1000
            """;
}
