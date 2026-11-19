(function () {
    "use strict";

    const W = 960, H = 540;
    const sdk = window.WarpGameSDK || null;
    const STORAGE_KEY = "rift-survivors-save";
    const C = {
        ink: 0x1b1714, parchment: 0xe8d6a8, gold: 0xd7a84f,
        grass: 0x477344, grass2: 0x365e3d, stone: 0x77756e,
        red: 0xb94d43, blue: 0x527ca5, green: 0x8cc56b,
        white: 0xf5edda, muted: 0xb9ad91, hp: 0x9c3935
    };

    function readSave() {
        try {
            const data = sdk ? sdk.loadData(STORAGE_KEY) : JSON.parse(localStorage.getItem(STORAGE_KEY) || "null");
            return data && typeof data === "object" ? data : {};
        } catch (_) { return {}; }
    }
    function writeSave(data) {
        try {
            if (sdk) sdk.saveData(STORAGE_KEY, data);
            else localStorage.setItem(STORAGE_KEY, JSON.stringify(data));
        } catch (_) {}
    }

    class Arena extends Phaser.Scene {
        constructor() {
            super("Arena");
            this.mode = "menu";
            this.menuBackdrop=null;
            this.enemies = null;
            this.bullets = null;
            this.orbs = null;
            this.player = null;
            this.playerSprite = null;
            this.joy = { active: false, pointerId: null, baseX: 100, baseY: 420, dx: 0, dy: 0, radius: 66 };
            this.stats = null;
            this.wave = 1;
            this.waveClock = 0;
            this.spawnClock = 0;
            this.fireClock = 0;
            this.invulnerable = 0;
            this.level = 1;
            this.xp = 0;
            this.xpNext = 7;
            this.kills = 0;
            this.score = 0;
            this.best = Number(readSave().best || 0);
            this.projectiles = 1;
            this.hud = {};
            this.overlay = [];
            this.target = null;
            this.elapsedMs = 0; this.lastBossMinute = 0; this.selectedUpgrades = [];
            this.worldChunks = new Map(); this.ground = null; this.bossBar = null; this.bossLabel = null;
            this.earthClock = 0; this.windAngle = 0; this.windOrb = null; this.pauseButton = null;
            this.aimLine = null;
            this.lastMoveX = 1;
            this.lastMoveY = 0;
        }

        preload() {
            this.load.image("gameIcon", "assets/rift-survivors-icon.webp");
            this.load.image("menuBackdrop", "assets/rift-menu-bg.webp");
        }

        create() {
            this.physics.world.setBounds(-100000, -100000, 200000, 200000);
            this.createTextures();
            this.obstacles = this.physics.add.staticGroup();
            this.createWorld();
            this.enemies = this.physics.add.group({ runChildUpdate: true });
            this.bullets = this.physics.add.group();
            this.orbs = this.physics.add.group();
            this.loot = this.physics.add.group();
            this.createPlayer();
            this.createHud();
            this.physics.add.overlap(this.bullets, this.enemies, this.hitEnemy, null, this);
            this.physics.add.overlap(this.player, this.enemies, this.hitPlayer, null, this);
            this.physics.add.collider(this.player, this.obstacles);
            this.physics.add.overlap(this.player, this.orbs, this.collectXp, null, this);
            this.physics.add.overlap(this.player, this.loot, this.collectLoot, null, this);
            this.input.on("pointerdown", this.pointerDown, this);
            this.input.on("pointermove", this.pointerMove, this);
            this.input.on("pointerup", this.pointerUp, this);
            this.input.on("pointerupoutside", this.pointerUp, this);
            this.showMenu();
        }

        createTextures() {
            const make = (key, width, height, draw) => {
                const g = this.make.graphics({ x: 0, y: 0, add: false });
                draw(g);
                g.generateTexture(key, width, height);
                g.destroy();
            };
            make("ground", 48, 48, g => {
                g.fillStyle(0x426b3e); g.fillRect(0, 0, 48, 48);
                g.fillStyle(0x4e7946); g.fillRect(2, 3, 5, 3); g.fillRect(18, 8, 4, 3); g.fillRect(34, 29, 5, 3);
                g.fillStyle(0x365d38); g.fillRect(10, 17, 5, 2); g.fillRect(25, 25, 4, 3); g.fillRect(4,38,7,3);
                g.fillStyle(0x6b8a4b); g.fillRect(24, 2, 2, 2); g.fillRect(5, 25, 2, 2); g.fillRect(41,8,3,2);
                g.fillStyle(0x557b43); g.fillRect(29,39,4,2); g.fillRect(13,31,3,2);
            });
            make("groundAsh",48,48,g=>{g.fillStyle(0x514b47);g.fillRect(0,0,48,48);g.fillStyle(0x625a52);g.fillRect(3,6,8,3);g.fillRect(25,13,6,4);g.fillRect(13,33,10,3);g.fillStyle(0x39383a);g.fillRect(17,17,5,3);g.fillRect(37,37,6,4);g.fillStyle(0x85786a);g.fillRect(7,26,3,2);g.fillRect(33,5,4,2);});
            make("groundRift",48,48,g=>{g.fillStyle(0x263c3d);g.fillRect(0,0,48,48);g.fillStyle(0x315b58);g.fillRect(3,4,7,3);g.fillRect(28,10,8,4);g.fillRect(13,31,9,4);g.fillStyle(0x1b292f);g.fillRect(17,17,7,4);g.fillRect(36,34,6,5);g.fillStyle(0x5b8c7b);g.fillRect(6,39,3,3);g.fillRect(39,6,4,3);});
            make("crystal",30,42,g=>{g.fillStyle(0x192b3b);g.fillRect(8,13,14,27);g.fillStyle(0x4e8ca8);g.fillRect(10,8,10,29);g.fillRect(4,20,6,15);g.fillStyle(0x9be8e4);g.fillRect(13,4,4,24);g.fillRect(7,20,3,8);g.fillStyle(0xd6fff1);g.fillRect(14,8,2,8);g.fillStyle(0x273d4a);g.fillRect(5,37,20,4);});
            make("potion",18,22,g=>{g.fillStyle(0x292b31);g.fillRect(6,2,6,4);g.fillRect(4,6,10,13);g.fillRect(6,18,6,3);g.fillStyle(0x9b3543);g.fillRect(5,9,8,8);g.fillStyle(0xf08a79);g.fillRect(6,9,3,5);g.fillStyle(0xe9d5a0);g.fillRect(7,1,4,2);});
            make("rune",20,20,g=>{g.fillStyle(0x372650);g.fillRect(5,1,10,18);g.fillRect(1,5,18,10);g.fillStyle(0x9e67d7);g.fillRect(7,3,6,14);g.fillRect(3,7,14,6);g.fillStyle(0xe6c6ff);g.fillRect(8,5,4,10);g.fillStyle(0xffffff);g.fillRect(9,7,2,5);});
            make("hero", 24, 28, g => {
                g.fillStyle(0x302b28); g.fillRect(6, 2, 12, 5);
                g.fillStyle(0xc6c6bb); g.fillRect(5, 6, 14, 12); g.fillRect(7, 4, 10, 3);
                g.fillStyle(0x596b78); g.fillRect(7, 9, 10, 8);
                g.fillStyle(0xd9b98a); g.fillRect(9, 7, 6, 5);
                g.fillStyle(0xe3c36d); g.fillRect(10, 13, 4, 3);
                g.fillStyle(0xddd7c6); g.fillRect(18, 10, 3, 13);
                g.fillStyle(0x8e9699); g.fillRect(19, 5, 2, 7);
                g.fillStyle(0xf4f0df);g.fillRect(6,6,3,5);g.fillRect(15,6,3,5);g.fillStyle(0x343b45);g.fillRect(8,10,8,2);
                g.fillStyle(0x9b7b48);g.fillRect(5,17,14,2);g.fillRect(9,19,6,3);g.fillStyle(0x5e7484);g.fillRect(6,20,12,3);
                g.fillStyle(0xe8c56f);g.fillRect(10,16,4,2);g.fillStyle(0x6c7479);g.fillRect(3,11,3,7);g.fillRect(18,11,3,7);
            });
            make("heroLeg", 10, 15, g => {
                g.fillStyle(0x443a32); g.fillRect(2, 0, 6, 10);
                g.fillStyle(0x8a5838); g.fillRect(1, 9, 8, 5);
                g.fillStyle(0xc59a60); g.fillRect(2, 9, 5, 1);
            });
            make("slime", 22, 20, g => {
                g.fillStyle(0x253b2c); g.fillRect(4, 16, 14, 3);
                g.fillStyle(0x76ad55); g.fillRect(3, 9, 16, 8); g.fillRect(6, 5, 10, 5);
                g.fillStyle(0xa7d878); g.fillRect(6, 6, 5, 3);
                g.fillStyle(0x18251d); g.fillRect(7, 11, 2, 3); g.fillRect(14, 11, 2, 3);
            });
            make("skeleton", 24, 28, g => {
                g.fillStyle(0x34302b); g.fillRect(6, 2, 12, 4);
                g.fillStyle(0xd8d0b9); g.fillRect(7, 5, 10, 9); g.fillRect(5, 14, 14, 8);
                g.fillStyle(0x26231f); g.fillRect(8, 8, 3, 3); g.fillRect(14, 8, 3, 3);
                g.fillStyle(0xf1e8d1);g.fillRect(10,6,2,1);g.fillRect(15,6,2,1);g.fillStyle(0x756d5e);g.fillRect(8,14,8,2);
                g.fillStyle(0xe4d9c1);g.fillRect(8,18,2,3);g.fillRect(14,18,2,3);g.fillRect(10,20,4,2);
                g.fillStyle(0xa99f89); g.fillRect(7, 15, 10, 3);
                g.fillStyle(0xd8d0b9); g.fillRect(6, 22, 4, 5); g.fillRect(14, 22, 4, 5);
                g.fillStyle(0x6d6251); g.fillRect(3, 12, 3, 9); g.fillRect(18, 12, 3, 9);
            });
            make("cultist", 30, 34, g => {
                g.fillStyle(0x211b2d);g.fillRect(7,5,16,25);g.fillRect(3,15,24,13);
                g.fillStyle(0x513d70);g.fillRect(8,7,14,20);g.fillRect(5,16,20,10);
                g.fillStyle(0x9c73d2);g.fillRect(9,8,12,5);g.fillRect(7,22,16,4);
                g.fillStyle(0xd5c3f0);g.fillRect(10,10,3,3);g.fillRect(17,10,3,3);
                g.fillStyle(0x21172c);g.fillRect(10,11,2,2);g.fillRect(18,11,2,2);
                g.fillStyle(0x8e68b7);g.fillRect(23,9,3,22);g.fillStyle(0xd6baff);g.fillRect(22,6,5,5);
            });
            make("brute", 32, 34, g => {
                g.fillStyle(0x302923); g.fillRect(6, 5, 20, 25);
                g.fillStyle(0x88634a); g.fillRect(4, 8, 24, 18);
                g.fillStyle(0xb08a63); g.fillRect(8, 4, 16, 8);
                g.fillStyle(0x382b25); g.fillRect(9, 10, 4, 4); g.fillRect(19, 10, 4, 4);
                g.fillStyle(0xb9a27b); g.fillRect(9, 18, 14, 5);
                g.fillStyle(0x59402f); g.fillRect(2, 22, 7, 9); g.fillRect(23, 22, 7, 9);
                g.fillStyle(0xd0a77a);g.fillRect(7,5,4,3);g.fillRect(21,5,4,3);g.fillStyle(0x6a4938);g.fillRect(8,14,16,3);
                g.fillStyle(0xd7b88b);g.fillRect(10,18,4,3);g.fillRect(18,18,4,3);g.fillStyle(0x30241f);g.fillRect(13,24,6,4);
            });
            make("bolt", 12, 8, g => {
                g.fillStyle(0x4a3828); g.fillRect(1, 3, 9, 2);
                g.fillStyle(0xf0d18a); g.fillRect(7, 1, 4, 6);
                g.fillStyle(0xffffff); g.fillRect(9, 2, 2, 2);
            });
            make("windorb", 20, 20, g => { g.fillStyle(0x315e70,0.65); g.fillCircle(10,10,9); g.fillStyle(0x71d7df,0.9); g.fillCircle(10,10,6); g.fillStyle(0xd9ffff,1); g.fillCircle(8,7,3); g.lineStyle(2,0xa6f4ee,0.9); g.strokeCircle(10,10,8); });
            make("orb", 12, 12, g => {
                g.fillStyle(0x426a37); g.fillRect(3, 1, 6, 10);
                g.fillStyle(0x9ddc72); g.fillRect(2, 2, 8, 8);
                g.fillStyle(0xeaffb0); g.fillRect(4, 3, 3, 3);
            });
            make("tree", 38, 48, g => {
                g.fillStyle(0x49352a); g.fillRect(15, 24, 9, 23);
                g.fillStyle(0x24452d); g.fillRect(6, 13, 26, 23);
                g.fillStyle(0x376d3d); g.fillRect(3, 9, 30, 19); g.fillRect(9, 3, 19, 15);
                g.fillStyle(0x5b8747); g.fillRect(9, 7, 8, 6); g.fillRect(22, 15, 7, 5);
                g.fillStyle(0x1d3a29); g.fillRect(5, 26, 8, 7); g.fillRect(26, 24, 7, 7);
            });
            make("ruin", 44, 50, g => {
                g.fillStyle(0x55534d); g.fillRect(3, 15, 10, 32); g.fillRect(13, 21, 9, 26); g.fillRect(29, 12, 12, 35);
                g.fillStyle(0x858176); g.fillRect(2, 12, 12, 6); g.fillRect(12, 18, 11, 5); g.fillRect(28, 9, 14, 6);
                g.fillStyle(0x393a38); g.fillRect(31, 23, 7, 24); g.fillRect(17, 29, 8, 18);
                g.fillStyle(0xa29b88); g.fillRect(5, 22, 5, 4); g.fillRect(31, 17, 7, 4);
                g.fillStyle(0x68665d); g.fillRect(1, 43, 42, 5);
                g.fillStyle(0x343532); g.fillRect(17, 34, 8, 13);
            });
            make("torch", 12, 22, g => {
                g.fillStyle(0x62452e); g.fillRect(5, 7, 3, 15);
                g.fillStyle(0xb45c2d); g.fillRect(3, 4, 7, 7);
                g.fillStyle(0xf3c45d); g.fillRect(4, 1, 5, 6);
                g.fillStyle(0xffed9b); g.fillRect(5, 2, 2, 3);
            });
            make("dirt", 112, 80, g => {
                g.fillStyle(0x66513a); g.fillRect(16,0,80,8); g.fillRect(8,8,96,16); g.fillRect(0,24,112,40); g.fillRect(12,64,88,8); g.fillRect(24,72,64,8);
                g.fillStyle(0x806447); g.fillRect(20,8,32,8); g.fillRect(8,24,24,8); g.fillRect(64,24,32,8); g.fillRect(32,48,40,8); g.fillRect(80,56,20,8);
                g.fillStyle(0x4e402f); g.fillRect(24,32,24,8); g.fillRect(72,40,24,8); g.fillRect(16,56,16,8); g.fillRect(48,64,24,8);
                g.fillStyle(0x9a7950); g.fillRect(24,16,8,4); g.fillRect(56,32,8,4); g.fillRect(88,48,8,4); g.fillRect(16,48,4,4);
                g.fillStyle(0x3f392d); g.fillRect(40,8,8,4); g.fillRect(80,16,8,4); g.fillRect(56,56,8,4);
            });
            make("wraith", 28, 32, g => {
                g.fillStyle(0x18333b); g.fillRect(8,4,12,19); g.fillRect(4,12,20,12); g.fillRect(6,22,5,8); g.fillRect(17,22,5,8);
                g.fillStyle(0x397f83); g.fillRect(7,7,14,15); g.fillRect(3,13,22,7); g.fillRect(9,22,10,6);
                g.fillStyle(0x9bf5df); g.fillRect(9,10,4,4); g.fillRect(17,10,4,4); g.fillStyle(0x14272d); g.fillRect(10,11,2,2); g.fillRect(18,11,2,2);
                g.fillStyle(0x65b5a7);g.fillRect(8,5,12,2);g.fillRect(7,20,14,2);g.fillStyle(0x14292f);g.fillRect(12,24,4,5);g.fillStyle(0xb6fff0);g.fillRect(4,14,2,5);g.fillRect(22,14,2,5);
            });
            make("beetle", 32, 28, g => {
                g.fillStyle(0x33251d); g.fillRect(7,8,18,15); g.fillRect(2,11,7,4); g.fillRect(23,11,7,4); g.fillRect(6,21,5,6); g.fillRect(21,21,5,6);
                g.fillStyle(0x9b4932); g.fillRect(8,5,16,17); g.fillRect(12,2,8,7); g.fillStyle(0xd77a43); g.fillRect(11,7,5,10); g.fillRect(18,7,4,10);
                g.fillStyle(0xffd28a); g.fillRect(13,4,2,3); g.fillRect(18,4,2,3); g.fillStyle(0x271b18); g.fillRect(12,10,3,3); g.fillRect(19,10,3,3);
                g.fillStyle(0xf0a46a);g.fillRect(9,7,3,2);g.fillRect(20,7,3,2);g.fillStyle(0x5b3028);g.fillRect(14,12,4,8);g.fillStyle(0xffc78a);g.fillRect(15,13,2,6);
            });
            make("bossRift", 56, 60, g => {
                g.fillStyle(0x172c2b); g.fillRect(10,12,36,40); g.fillRect(4,22,48,22); g.fillRect(15,4,26,12);
                g.fillStyle(0x397b68); g.fillRect(12,14,32,34); g.fillRect(7,24,42,15); g.fillRect(18,7,20,10);
                g.fillStyle(0x9cf4c5); g.fillRect(16,19,7,7); g.fillRect(34,19,7,7); g.fillStyle(0x18241e); g.fillRect(18,21,3,3); g.fillRect(36,21,3,3);
                g.fillStyle(0xd8f7b0); g.fillRect(23,33,10,4); g.fillStyle(0x71533b); g.fillRect(6,44,44,8);
                g.fillStyle(0x244b42);g.fillRect(8,13,5,5);g.fillRect(43,13,5,5);g.fillStyle(0x70b89a);g.fillRect(13,11,5,3);g.fillRect(38,11,5,3);
                g.fillStyle(0x182e2c);g.fillRect(24,25,8,3);g.fillRect(18,42,20,3);g.fillStyle(0xc4f8c8);g.fillRect(26,31,4,2);g.fillRect(25,47,6,2);
            });
            make("bossBone", 56, 60, g => {
                g.fillStyle(0x272321); g.fillRect(12,6,32,46); g.fillRect(5,18,46,27); g.fillStyle(0xcfc4a7); g.fillRect(15,8,26,15); g.fillRect(9,22,38,22); g.fillRect(14,43,9,9); g.fillRect(33,43,9,9);
                g.fillStyle(0x6d2d2a); g.fillRect(18,14,6,6); g.fillRect(33,14,6,6); g.fillStyle(0x171515); g.fillRect(19,15,4,4); g.fillRect(34,15,4,4);
                g.fillStyle(0x8c3e36); g.fillRect(24,29,8,5); g.fillStyle(0xe7dcc0); g.fillRect(5,24,7,17); g.fillRect(44,24,7,17);
                g.fillStyle(0xf4e8ca);g.fillRect(16,25,5,3);g.fillRect(35,25,5,3);g.fillRect(18,35,20,3);g.fillRect(21,40,14,3);
                g.fillStyle(0x5b3c36);g.fillRect(12,32,4,7);g.fillRect(40,32,4,7);g.fillStyle(0xa77c69);g.fillRect(25,11,6,2);g.fillRect(26,17,4,2);
            });
            make("bossColossus", 64, 68, g => {
                g.fillStyle(0x292332); g.fillRect(10,13,44,47); g.fillRect(2,25,60,29); g.fillRect(16,5,32,14);
                g.fillStyle(0x66527e); g.fillRect(12,15,40,40); g.fillRect(5,28,54,20); g.fillRect(19,8,26,12);
                g.fillStyle(0xc29be8); g.fillRect(18,23,9,8); g.fillRect(37,23,9,8); g.fillStyle(0x251c32); g.fillRect(20,25,5,4); g.fillRect(39,25,5,4);
                g.fillStyle(0xe4c9ff); g.fillRect(27,38,10,5); g.fillStyle(0x9884a7); g.fillRect(4,49,56,8);
                g.fillStyle(0x43364f);g.fillRect(14,19,8,6);g.fillRect(42,19,8,6);g.fillStyle(0x9b82b2);g.fillRect(9,28,7,5);g.fillRect(48,28,7,5);
                g.fillStyle(0x34283f);g.fillRect(25,14,14,3);g.fillRect(24,49,16,4);g.fillStyle(0xdcbaff);g.fillRect(29,29,6,6);g.fillRect(30,40,4,3);
                g.fillStyle(0xb6a1c8);g.fillRect(16,54,8,4);g.fillRect(40,54,8,4);
            });
            make("rubble", 28, 22, g => {
                g.fillStyle(0x514f49); g.fillRect(2,10,9,8); g.fillRect(12,5,10,12); g.fillRect(21,12,6,7);
                g.fillStyle(0x898477); g.fillRect(4,8,5,3); g.fillRect(14,3,6,4); g.fillRect(22,10,4,3);
                g.fillStyle(0x353632); g.fillRect(7,17,17,3);
            });
            make("pillar", 22, 40, g => {
                g.fillStyle(0x777267); g.fillRect(4,8,14,29); g.fillRect(1,5,20,5);
                g.fillStyle(0xa29b88); g.fillRect(6,10,4,24); g.fillRect(3,35,16,4);
                g.fillStyle(0x45443f); g.fillRect(12,14,4,17);
            });
            make("grassTuft", 20, 16, g => {
                g.fillStyle(0x294b30); g.fillTriangle(2,15,7,2,9,15); g.fillTriangle(8,15,13,0,15,15);
                g.fillStyle(0x8aa65a); g.fillTriangle(5,15,8,6,10,15); g.fillTriangle(12,15,15,5,17,15);
            });
        }

        createWorld() {
            this.ground = null;
            const deco = this.add.graphics().setDepth(-10);
            for (let i = 0; i < 210; i++) {
                const x = Phaser.Math.Between(8, W - 8), y = Phaser.Math.Between(8, H - 8);
                if (x > 100 && x < W - 100 && y > 80 && y < H - 75) continue;
                deco.fillStyle(Math.random() > 0.5 ? 0x2d5435 : 0x6d8245, 0.8);
                deco.fillRect(x, y, 3, 3);
            }
            const placements = [
                [36, 48, "tree"], [104, 35, "tree"], [200, 22, "ruin"],
                [760, 25, "ruin"], [865, 45, "tree"], [925, 115, "tree"],
                [24, 425, "tree"], [150, 490, "tree"], [795, 490, "tree"],
                [920, 420, "tree"], [310, 25, "tree"], [650, 20, "tree"]
            ];
            placements.forEach(p => this.addObstacle(p[0], p[1], p[2], 1));
            this.add.image(72, 220, "torch").setDepth(5);
            this.add.image(888, 260, "torch").setDepth(5);
            this.add.image(245, 405, "rubble").setDepth(4).setAlpha(0.9);
            this.add.image(710, 130, "rubble").setDepth(4).setAngle(18);
            this.add.image(610, 455, "pillar").setDepth(4).setAngle(-8);
            this.add.image(345, 115, "grassTuft").setDepth(3).setAlpha(0.9);
            const vignette = this.add.graphics().setDepth(10);
            vignette.fillStyle(0x182017, 0.08);
            vignette.fillRect(0, 0, W, 8); vignette.fillRect(0, H - 8, W, 8);
        }

        addObstacle(x, y, key, scale) {
            const obj = this.obstacles.create(x, y, key);
            obj.setScale(scale || 1).setDepth(2 + y / 100000);
            obj.refreshBody();
            const width = key === "ruin" ? 32 : 23;
            const height = key === "ruin" ? 35 : 31;
            obj.body.setSize(width, height);
            obj.body.setOffset((obj.width - width) / 2, obj.height - height);
            return obj;
        }

        createPlayer() {
            this.player = this.add.container(W / 2, H / 2).setDepth(15);
            this.playerLegLeft = this.add.image(-4, 10, "heroLeg").setOrigin(0.5, 0.15);
            this.playerLegRight = this.add.image(4, 10, "heroLeg").setOrigin(0.5, 0.15);
            this.playerSprite = this.add.image(0, -2, "hero").setOrigin(0.5);
            this.player.add([this.playerLegLeft, this.playerLegRight, this.playerSprite]);
            this.physics.add.existing(this.player);
            this.player.body.setSize(16, 20).setOffset(-8, -10);
            this.player.body.setCollideWorldBounds(true);
            this.player.body.setDamping(true);
            this.player.body.setDrag(1150);
            this.player.body.setMaxVelocity(180);
            this.cameras.main.setBounds(-100000,-100000,200000,200000);
            this.cameras.main.startFollow(this.player,true,0.085,0.085);
            this.cameras.main.setDeadzone(170,100);
            this.playerWalkTween=this.tweens.add({targets:this.playerSprite,y:0,duration:150,yoyo:true,repeat:-1,paused:true});
        }

        createHud() {
            this.hud.top = this.add.container(0, 0).setDepth(40).setScrollFactor(0);
            const plate = this.add.rectangle(W / 2, 28, 360, 48, 0x29251f, 0.94).setStrokeStyle(2, 0xb79a5e, 0.95);
            this.hud.wave = this.add.text(28, 16, "ВОЛНА 1", this.style(16, C.parchment, true));
            this.hud.level = this.add.text(28, 39, "УР. 1", this.style(11, C.gold, true));
            this.hud.score = this.add.text(W - 25, 17, "0", this.style(17, C.gold, true)).setOrigin(1, 0);
            this.hud.hpBack = this.add.rectangle(W / 2, 19, 150, 9, 0x4b302a);
            this.hud.hp = this.add.rectangle(W / 2 - 73, 19, 146, 5, 0x9c3935).setOrigin(0, 0.5);
            this.hud.xpBack = this.add.rectangle(W / 2, 37, 150, 5, 0x51452f);
            this.hud.xp = this.add.rectangle(W / 2 - 73, 37, 1, 3, 0xd7b65f).setOrigin(0, 0.5);
            this.hud.timer=this.add.text(W/2,48,"00:00",this.style(10,C.gold,true)).setOrigin(0.5,0);
            this.hud.pauseBg=this.add.circle(W-28,28,20,0x493b2a,0.96).setStrokeStyle(2,C.gold,1).setInteractive();
            this.hud.pauseText=this.add.text(W-28,28,"Ⅱ",this.style(17,C.parchment,true)).setOrigin(0.5);
            this.hud.pauseBg.on("pointerdown",()=>this.pauseRun()); this.pauseButton=this.hud.pauseBg;
            this.hud.top.add([plate,this.hud.wave,this.hud.level,this.hud.score,this.hud.hpBack,this.hud.hp,this.hud.xpBack,this.hud.xp,this.hud.timer,this.hud.pauseBg,this.hud.pauseText]);

            this.hud.joy = this.add.container(100, 420).setDepth(50).setScrollFactor(0);
            this.hud.joyBase = this.add.circle(0, 0, 66, 0x242d20, 0.55).setStrokeStyle(3, 0xcab77f, 0.78);
            this.hud.joyInner = this.add.circle(0, 0, 43, 0x536344, 0.28).setStrokeStyle(1, 0xe3d4a9, 0.35);
            this.hud.joyKnob = this.add.circle(0, 0, 25, 0xc3ad78, 0.8).setStrokeStyle(3, 0xf1e2bb, 0.95);
            this.hud.joy.add([this.hud.joyBase, this.hud.joyInner, this.hud.joyKnob]);

             this.hud.help = this.add.text(W / 2, H - 12, "ДВИЖЕНИЕ — ЛЕВЫЙ СТИК     •     АТАКА — АВТОМАТИЧЕСКИ ПО БЛИЖАЙШЕМУ ВРАГУ", this.style(10, 0xf0dfb3, true)).setOrigin(0.5, 1).setDepth(40).setScrollFactor(0);
            this.hud.skills=this.add.container(W-105,H-70).setDepth(42).setScrollFactor(0);
            this.hud.skillCards={};
            [["wind","ВЕТЕР",0x65cbd0],["earth","ЗЕМЛЯ",0xc59a5d],["lightning","МОЛНИЯ",0x76dfff]].forEach((spec,i)=>{
                const x=-154+i*77,card=this.add.container(x,0);const bg=this.add.rectangle(0,0,70,40,0x171d25,0.9).setStrokeStyle(2,spec[2],0.8);
                const text=this.add.text(0,0,spec[1],this.style(9,spec[2],true)).setOrigin(0.5);card.add([bg,text]);card.setVisible(false);
                this.hud.skills.add(card);this.hud.skillCards[spec[0]]=card;
            });
            this.hud.skills.setVisible(false);
            this.hud.top.setVisible(false);
            this.hud.joy.setVisible(false);
             this.hud.help.setVisible(false);
            this.layoutJoystick();
        }

        layoutJoystick() {
            if (!this.hud.joy) return;
            this.hud.joy.setPosition(this.joy.baseX, this.joy.baseY);
        }

        style(size, color, bold) {
            return {
                fontFamily: "Trebuchet MS, Arial, sans-serif",
                fontSize: size + "px",
                color: "#" + color.toString(16).padStart(6, "0"),
                fontStyle: bold ? "bold" : "normal",
                stroke: "#211b16",
                strokeThickness: 2
            };
        }

        clearOverlay() {
            this.overlay.forEach(item => item.destroy());
            this.overlay = [];
        }
        addOverlay(item) { item.setDepth(100).setScrollFactor(0); this.overlay.push(item); return item; }

        panel(title, subtitle) {
            this.clearOverlay();
            this.addOverlay(this.add.rectangle(W / 2, H / 2, W, H, 0x10150f, 0.58));
            this.addOverlay(this.add.rectangle(W / 2+7, H / 2+9, 700, 470, 0x080b10, 0.72));
            this.addOverlay(this.add.rectangle(W / 2, H / 2, 700, 470, 0x171827, 0.84).setStrokeStyle(4, 0x9d8bc8, 1));
            this.addOverlay(this.add.rectangle(W / 2, H / 2, 684, 454, 0x39364c, 0.16).setStrokeStyle(1, 0x7569a6, 0.72));
            this.addOverlay(this.add.rectangle(W / 2, H / 2 - 206, 180, 4, 0xb89556, 0.9));
            this.addOverlay(this.add.rectangle(W / 2, H / 2 + 206, 180, 4, 0xb89556, 0.9));
            [[W/2-337,H/2-222],[W/2+337,H/2-222],[W/2-337,H/2+222],[W/2+337,H/2+222]].forEach(([x,y])=>this.addOverlay(this.add.rectangle(x,y,10,10,0xd8b66f,0.95).setAngle(45)));
            this.addOverlay(this.add.text(W / 2, H / 2 - 194, title, this.style(30, C.parchment, true)).setOrigin(0.5));
            if (subtitle) this.addOverlay(this.add.text(W / 2, H / 2 - 153, subtitle, this.style(14, C.gold, true)).setOrigin(0.5));
        }

        makeButton(x, y, label, callback, width) {
            const w = width || 250;
            const bg = this.add.rectangle(x, y, w, 48, 0x665032).setStrokeStyle(2, 0xd0b174, 1).setInteractive({ useHandCursor: true });
            const text = this.add.text(x, y, label, this.style(16, C.parchment, true)).setOrigin(0.5);
            bg.on("pointerover",()=>{bg.setFillStyle(0x80633d);bg.setStrokeStyle(2,0xf1d28e,1);text.setScale(1.04);});
            bg.on("pointerout",()=>{bg.setFillStyle(0x665032);bg.setStrokeStyle(2,0xd0b174,1);text.setScale(1);});
            bg.on("pointerdown", callback);
            this.addOverlay(bg); this.addOverlay(text);
        }

        showMenu() {
            this.mode = "menu";
            this.physics.pause();
            this.panel("RIFT SURVIVORS", "ПРОКЛЯТЫЕ РУИНЫ");
            if(!this.menuBackdrop||!this.menuBackdrop.active)this.menuBackdrop=this.add.image(W/2,H/2,"menuBackdrop").setDisplaySize(W,H).setScrollFactor(0).setDepth(80);
            this.menuBackdrop.setVisible(true).setAlpha(0.92);
            const halo=this.add.circle(W/2,H/2-84,64,0x8f48d4,0.08).setStrokeStyle(3,0x9c70d0,0.75);
            this.addOverlay(halo);
            this.tweens.add({targets:halo,scale:1.13,alpha:0.55,duration:1800,yoyo:true,repeat:-1,ease:"Sine.InOut"});
            this.addOverlay(this.add.image(W/2,H/2-84,"gameIcon").setDisplaySize(116,116).setDepth(101));
            this.addOverlay(this.add.text(W / 2, H / 2 - 15,
                "СОБИРАЙ РЕЛИКВИИ • ОСВАИВАЙ МАГИЮ • УБИВАЙ БОССОВ\nКаждый забег — новая сборка и новые угрозы.",
                this.style(13, C.parchment, false)).setOrigin(0.5).setAlign("center").setLineSpacing(7));
            const statsY = H / 2 + 30;
            [["РЕКОРД", this.best.toLocaleString("ru-RU")], ["БОССЫ", "КАЖДЫЕ 3 МИН."], ["АТАКА", "АВТОМАТИЧЕСКАЯ"]].forEach((item, i) => {
                const x = W / 2 - 205 + i * 205;
                this.addOverlay(this.add.rectangle(x, statsY, 184, 52, 0x393326, 0.98).setStrokeStyle(1, 0x806c48, 0.9));
                this.addOverlay(this.add.text(x, statsY - 10, item[0], this.style(10, C.muted, true)).setOrigin(0.5));
                this.addOverlay(this.add.text(x, statsY + 10, String(item[1]), this.style(13, C.gold, true)).setOrigin(0.5));
            });
            this.makeButton(W / 2, H / 2 + 121, "НАЧАТЬ ЗАБЕГ", () => this.startRun(), 290);
            this.makeButton(W / 2, H / 2 + 181, "КАК ИГРАТЬ", () => this.showControls(), 220);
            this.addOverlay(this.add.text(W / 2, H - 22,
                "ЛЕВЫЙ СТИК — ДВИЖЕНИЕ     •     РЫЦАРЬ САМ ВЫБИРАЕТ ЦЕЛЬ",
                this.style(10, C.muted, false)).setOrigin(0.5));
        }

        showControls() {
            if(this.menuBackdrop)this.menuBackdrop.setVisible(false);
            this.panel("УПРАВЛЕНИЕ", "ПРОСТЫЕ ПРАВИЛА ВЫЖИВАНИЯ");
            const rows = [
                ["ПЕРЕДВИЖЕНИЕ", "Удерживай левый виртуальный стик"],
                ["СТРЕЛЬБА", "Рыцарь автоматически стреляет в ближайшего врага"],
                ["УСИЛЕНИЯ", "Открывай ветер, землю, цепную молнию и их улучшения"],
                ["БОССЫ", "Новый босс появляется каждые три минуты"],
                ["ПАУЗА", "Кнопка II в верхней части экрана"]
            ];
            rows.forEach((row, i) => {
                const y = H / 2 - 82 + i * 48;
                this.addOverlay(this.add.text(W / 2 - 275, y, row[0], this.style(12, C.gold, true)));
                this.addOverlay(this.add.text(W / 2 - 80, y, row[1], this.style(12, C.parchment, false)));
            });
            this.makeButton(W / 2, H / 2 + 175, "НАЗАД", () => this.showMenu(), 220);
        }

        startRun() {
            if(this.menuBackdrop)this.menuBackdrop.setVisible(false);
            this.clearOverlay();
            this.mode = "playing";
            this.physics.resume();
            this.enemies.clear(true, true);
            if(this.windOrb){this.windOrb.destroy();this.windOrb=null;}
            this.bullets.clear(true, true);
            this.orbs.clear(true, true);
            this.loot.clear(true,true);
            this.player.setPosition(W / 2, H / 2);
            this.player.body.setVelocity(0, 0);
            this.player.body.setAcceleration(0, 0);
            this.wave = 1; this.waveClock = 0; this.spawnClock = 0;
            this.fireClock = 250; this.invulnerable = 0;
            this.level = 1; this.xp = 0; this.xpNext = 7; this.kills = 0; this.score = 0;
            this.projectiles = 1;
            this.stats = { hp:100,maxHp:100,damage:20,fireDelay:900,moveSpeed:145,bulletSpeed:590,magnet:115,critChance:0.08,critMultiplier:2,earthUnlocked:false,earthDamage:48,earthDelay:8000,windUnlocked:false,windDamage:30,windDelay:430,windRadius:88,windOrbCount:0 };
            this.elapsedMs=0; this.lastBossMinute=0; this.earthClock=0; this.windAngle=0; this.windOrb=null; this.selectedUpgrades=[];
            this.lightningClock=0;this.lightningUnlocked=false;this.lightningDamage=58;this.lightningDelay=6200;this.lightningTargets=2;this.runePower=1;
            this.worldChunks.forEach(items => items.forEach(obj => obj.destroy()));
            this.worldChunks.clear();
            this.player.body.reset(W / 2, H / 2);
            this.cameras.main.stopFollow(); this.cameras.main.startFollow(this.player,true,0.085,0.085);
            this.player.body.setMaxVelocity(180); this.playerWalkTween.pause(); this.playerSprite.setPosition(0,-2).setAngle(0);
            this.playerLegLeft.setPosition(-4,10).setAngle(0);
            this.playerLegRight.setPosition(4,10).setAngle(0);
            this.joy.active = false; this.joy.pointerId = null; this.joy.dx = 0; this.joy.dy = 0;
            this.joy.baseX = 100; this.joy.baseY = H - 120;
            this.hud.joy.setPosition(this.joy.baseX, this.joy.baseY);
            this.hud.joyKnob.setPosition(0, 0);
            this.hud.top.setVisible(true); this.hud.joy.setVisible(true);
            this.hud.skills.setVisible(true);
            this.hud.help.setVisible(true);
            this.refreshHud();
            this.spawnEnemy("slime"); this.spawnEnemy("slime"); this.spawnEnemy("skeleton");
        }

        pointerDown(pointer) {
            if (this.mode !== "playing" || this.joy.active) return;
            if (pointer.x < W * 0.56 && pointer.y > 58) {
                this.joy.active = true;
                this.joy.pointerId = pointer.id;
                this.joy.baseX = Phaser.Math.Clamp(pointer.x, 76, 250);
                this.joy.baseY = Phaser.Math.Clamp(pointer.y, 95, H - 85);
                this.hud.joy.setPosition(this.joy.baseX, this.joy.baseY);
                this.updateJoystick(pointer);
            }
        }
        pointerMove(pointer) {
            if (this.mode === "playing" && this.joy.active && pointer.id === this.joy.pointerId) this.updateJoystick(pointer);
        }
        pointerUp(pointer) {
            if (this.joy.active && pointer.id === this.joy.pointerId) {
                this.joy.active = false; this.joy.pointerId = null;
                this.joy.dx = 0; this.joy.dy = 0;
                this.hud.joyKnob.setPosition(0, 0);
                this.joy.baseX = 100; this.joy.baseY = H - 120;
                this.hud.joy.setPosition(this.joy.baseX, this.joy.baseY);
            }
        }
        updateJoystick(pointer) {
            const dx = pointer.x - this.joy.baseX;
            const dy = pointer.y - this.joy.baseY;
            const length = Math.max(1, Math.hypot(dx, dy));
            const amount = Math.min(1, length / this.joy.radius);
            this.joy.dx = dx / length * amount;
            this.joy.dy = dy / length * amount;
            this.hud.joyKnob.setPosition(this.joy.dx * 39, this.joy.dy * 39);
        }

         spawnEnemy(type) {
            if (this.mode !== "playing" || this.enemies.countActive(true) >= 65) return;
            const side = Phaser.Math.Between(0, 3);
            const view=this.cameras.main.worldView;
            const x=side===0?view.x-35:side===1?view.right+35:Phaser.Math.Between(view.x+25,view.right-25);
            const y=side===2?view.y-35:side===3?view.bottom+35:Phaser.Math.Between(view.y+75,view.bottom-30);
            const roll = Math.random();
            const kind = type || (this.wave >= 8 && roll < 0.13 ? "cultist" : this.wave >= 7 && roll < 0.13 ? "beetle" : this.wave >= 5 && roll < 0.29 ? "wraith" : this.wave >= 4 && roll < 0.43 ? "brute" : this.wave >= 2 && roll < 0.70 ? "skeleton" : "slime");
            const key = kind === "brute" ? "brute" : kind === "skeleton" ? "skeleton" : kind === "wraith" ? "wraith" : kind === "beetle" ? "beetle" : kind === "cultist" ? "cultist" : "slime";
            const scale = kind === "brute" ? 1.45 : kind === "skeleton" ? 1.15 : kind === "wraith" ? 1.28 : kind === "beetle" ? 1.32 : kind === "cultist" ? 1.24 : 1.2;
            const enemy = this.add.container(x, y).setDepth(12);
            enemy.sprite = this.add.image(0, 0, key).setScale(scale);
            enemy.add(enemy.sprite);
            this.physics.add.existing(enemy);
            const radius = kind === "brute" ? 17 : kind === "beetle" ? 15 : kind === "wraith" ? 12 : kind === "cultist" ? 12 : kind === "skeleton" ? 12 : 11;
            enemy.body.setCircle(radius, -radius, -radius);
            enemy.body.setCollideWorldBounds(false);
            enemy.animTween=this.tweens.add({targets:enemy.sprite,y:2,duration:kind==="brute"?260:190,yoyo:true,repeat:-1});
            enemy.isBoss=false; enemy.bossName="";
            enemy.kind = kind;
            enemy.hp = kind === "brute" ? 150 + this.wave * 12 : kind === "beetle" ? 190 + this.wave * 13 : kind === "cultist" ? 92 + this.wave * 8 : kind === "wraith" ? 72 + this.wave * 7 : kind === "skeleton" ? 64 + this.wave * 6 : 42 + this.wave * 4;
            enemy.maxHp = enemy.hp;
            enemy.speed = kind === "brute" ? 48 + this.wave : kind === "beetle" ? 38 + this.wave * 0.8 : kind === "cultist" ? 54 + this.wave : kind === "wraith" ? 105 + this.wave * 1.8 : kind === "skeleton" ? 82 + this.wave * 1.5 : 65 + this.wave * 1.5;
            enemy.damage = kind === "brute" ? 25 : kind === "beetle" ? 30 : kind === "cultist" ? 18 : kind === "wraith" ? 16 : 14;
            enemy.xpValue = kind === "brute" ? 5 : kind === "beetle" ? 7 : kind === "cultist" ? 4 : kind === "wraith" ? 3 : kind === "skeleton" ? 2 : 1;
            enemy.radius = radius;
            enemy.hitFlash = 0;
            enemy.skillClock=Phaser.Math.Between(2600,4600);enemy.skillState="idle";enemy.skillDuration=0;
            enemy.healthBack = this.add.rectangle(0, -22, 28, 4, 0x33251f).setOrigin(0.5).setVisible(false);
            enemy.healthBar = this.add.rectangle(-14, -22, 28, 2, 0xb84c3e).setOrigin(0, 0.5).setVisible(false);
            enemy.add([enemy.healthBack, enemy.healthBar]);
            this.enemies.add(enemy);
        }

        nearestEnemy() {
            let best = null, bestDistance = Infinity;
            this.enemies.children.iterate(enemy => {
                if (!enemy || !enemy.active) return;
                const d = Phaser.Math.Distance.Between(this.player.x, this.player.y, enemy.x, enemy.y);
                if (d < bestDistance) { bestDistance = d; best = enemy; }
            });
            return bestDistance < 620 ? best : null;
        }

        fire() {
            const target = this.nearestEnemy();
            this.target = target;
            if (!target) return;
            const angle = Phaser.Math.Angle.Between(this.player.x, this.player.y, target.x, target.y);
            this.playerSprite.setFlipX(target.x < this.player.x);
            this.playerSprite.setScale(1.08,0.94);
            this.tweens.add({targets:this.playerSprite,scaleX:1,scaleY:1,duration:130,ease:"Back.Out"});
            const flash=this.add.circle(this.player.x+Math.cos(angle)*18,this.player.y+Math.sin(angle)*18,5,0xffe3a0,0.9).setDepth(19);
            this.tweens.add({targets:flash,alpha:0,scale:2.2,duration:130,onComplete:()=>flash.destroy()});
            for (let i = 0; i < this.projectiles; i++) {
                const spread = (i - (this.projectiles - 1) / 2) * 0.11;
                const a = angle + spread;
                const bolt = this.add.image(this.player.x + Math.cos(a) * 17, this.player.y + Math.sin(a) * 17, "bolt").setDepth(18).setRotation(a).setScale(1.5);
                this.bullets.add(bolt);
                if (!bolt.body) this.physics.add.existing(bolt);
                bolt.body.enable = true;
                bolt.body.moves = true;
                bolt.body.setAllowGravity(false);
                bolt.body.setVelocity(Math.cos(a) * this.stats.bulletSpeed, Math.sin(a) * this.stats.bulletSpeed);
                bolt.body.setMaxSpeed(this.stats.bulletSpeed);
                bolt.isCrit=Math.random()<this.stats.critChance;
                bolt.damage = this.stats.damage*(bolt.isCrit?this.stats.critMultiplier:1);
                if(bolt.isCrit)bolt.setTint(0xffd36a).setScale(1.9);
                bolt.life = 1600;
                bolt.setData("velocityX", Math.cos(a) * this.stats.bulletSpeed);
                bolt.setData("velocityY", Math.sin(a) * this.stats.bulletSpeed);
            }
        }

        hitEnemy(bolt, enemy) {
            if (!bolt.active || !enemy.active) return;
            if(bolt.trail)bolt.trail.destroy();
            const isCrit=!!bolt.isCrit;
            bolt.destroy();
            enemy.hp -= bolt.damage * (enemy.kind === "beetle" ? 0.78 : 1);
            if(isCrit){const label=this.add.text(enemy.x,enemy.y-28,"КРИТ!",this.style(13,0xffdf79,true)).setOrigin(.5).setDepth(32);this.tweens.add({targets:label,y:label.y-22,alpha:0,duration:480,onComplete:()=>label.destroy()});}
            enemy.healthBack.setVisible(true);
            enemy.healthBar.setVisible(true);
            enemy.healthBar.width = Math.max(1, 28 * enemy.hp / enemy.maxHp);
            enemy.sprite.setTint(0xffffff);
            this.time.delayedCall(75, () => { if (enemy.active) enemy.sprite.clearTint(); });
            if (enemy.hp <= 0) this.killEnemy(enemy);
        }

        killEnemy(enemy) {
            if (!enemy.active) return;
            const x=enemy.x,y=enemy.y,value=enemy.xpValue;
            if(enemy.isBoss){if(this.bossBar){this.bossBar.destroy();this.bossBar=null;}if(this.bossLabel){this.bossLabel.destroy();this.bossLabel=null;}this.score+=250*this.wave;this.stats.hp=Math.min(this.stats.maxHp,this.stats.hp+35);this.cameras.main.flash(220,255,205,110);this.dropLoot(x,y,"rune");this.dropLoot(x+22,y,"potion");}
            else if(enemy.kind==="brute"||enemy.kind==="beetle"||Math.random()<0.035)this.dropLoot(x,y,"potion");
            const burst=this.add.circle(x,y,enemy.isBoss?18:8,enemy.isBoss?0xc58cff:enemy.kind==="wraith"?0x83fff0:0xffd58a,0.72).setDepth(30);
            this.tweens.add({targets:burst,scale:enemy.isBoss?5:2.8,alpha:0,duration:enemy.isBoss?520:260,onComplete:()=>burst.destroy()});
            for(let i=0;i<(enemy.isBoss?12:5);i++){const spark=this.add.rectangle(x,y,enemy.isBoss?5:3,enemy.isBoss?5:3,enemy.isBoss?0xdab6ff:0xffdf9b).setDepth(30);const a=(Math.PI*2*i/(enemy.isBoss?12:5))+Math.random()*0.3;this.tweens.add({targets:spark,x:x+Math.cos(a)*(enemy.isBoss?65:24),y:y+Math.sin(a)*(enemy.isBoss?65:24),alpha:0,duration:enemy.isBoss?500:280,onComplete:()=>spark.destroy()});}
            enemy.destroy();
            this.kills++;
            this.score += 10 * this.wave;
            this.hud.score.setText(this.score.toLocaleString("ru-RU"));
            const orb = this.add.image(x, y, "orb").setDepth(14).setScale(1.25);
            this.physics.add.existing(orb);
            orb.body.setCircle(7);
            orb.body.setAllowGravity(false);
            orb.value = value;
            this.orbs.add(orb);
            this.tweens.add({ targets: orb, y: y - 5, duration: 350, yoyo: true, repeat: -1 });
            this.cameras.main.shake(45, 0.0012);
        }

        dropLoot(x,y,type){
            const item=this.add.image(x,y,type==="rune"?"rune":"potion").setDepth(16).setScale(1.35);
            this.physics.add.existing(item);item.body.setCircle(8);item.body.setAllowGravity(false);item.body.setImmovable(true);item.lootType=type;
            this.loot.add(item);this.tweens.add({targets:item,y:y-7,alpha:0.82,duration:420,yoyo:true,repeat:-1,ease:"Sine.InOut"});
        }

        collectLoot(player,item){
            if(!item.active||this.mode!=="playing")return;
            if(item.lootType==="potion"){const before=this.stats.hp;this.stats.hp=Math.min(this.stats.maxHp,this.stats.hp+28);this.showCombatNotice("ВОССТАНОВЛЕНИЕ +"+(this.stats.hp-before),0x8ee6a0);}
            else{this.runePower*=1.12;this.stats.damage*=1.12;this.stats.windDamage*=1.12;this.stats.earthDamage*=1.12;this.lightningDamage*=1.12;this.showCombatNotice("РУНА СИЛЫ +12% УРОНА",0xd9a6ff);}
            item.destroy();this.refreshHud();if(sdk)sdk.vibrate(18);
        }

        showCombatNotice(message,color){
            const t=this.add.text(W/2,H/2-78,message,this.style(15,color||C.gold,true)).setOrigin(.5).setScrollFactor(0).setDepth(80);
            this.tweens.add({targets:t,y:t.y-28,alpha:0,duration:1050,onComplete:()=>t.destroy()});
        }

        hitPlayer(player, enemy) {
            if (this.mode !== "playing" || this.invulnerable > 0 || !enemy.active) return;
            this.stats.hp -= enemy.damage;
            this.invulnerable = 720;
            this.cameras.main.flash(100, 180, 55, 45, false);
            const angle = Phaser.Math.Angle.Between(enemy.x, enemy.y, player.x, player.y);
            player.body.setVelocity(Math.cos(angle) * 260, Math.sin(angle) * 260);
            if (this.stats.hp <= 0) this.endRun();
            this.refreshHud();
        }

        collectXp(player, orb) {
            if (!orb.active || this.mode !== "playing") return;
            this.xp += orb.value;
            orb.destroy();
            if (this.xp >= this.xpNext) {
                this.xp -= this.xpNext;
                this.level++;
                this.xpNext = Math.floor(this.xpNext * 1.3 + 3);
                this.openUpgrade();
            }
            this.refreshHud();
        }

        upgradeOptions() {
            const all = [
                { title: "ОСТРЫЕ СТРЕЛЫ", desc: "+30% урон обычных стрел", apply: () => { this.stats.damage *= 1.30; } },
                { title: "БЫСТРАЯ ТЕТИВА", desc: "Стрелы выпускаются на 16% быстрее", apply: () => { this.stats.fireDelay = Math.max(260,this.stats.fireDelay*0.84); } },
                { title: "ЗАЛП ИЗ ЛУКА", desc: "+1 стрела за атаку (максимум 5)", apply: () => { this.projectiles = Math.min(5,this.projectiles+1); } },
                { title: "ЛЁГКИЕ САПОГИ", desc: "+12% скорость движения", apply: () => { this.stats.moveSpeed *= 1.12; } },
                { title: "КРЕПКИЕ ДОСПЕХИ", desc: "+25 к максимуму здоровья и лечение", apply: () => { this.stats.maxHp += 25; this.stats.hp = Math.min(this.stats.maxHp,this.stats.hp+35); } },
                { title: "МАГНИТ ЭССЕНЦИИ", desc: "Сильнее притягивает опыт", apply: () => { this.stats.magnet += 55; } },
                { title: "ТОЧНЫЙ ВЫСТРЕЛ", desc: "+7% шанс критического попадания", apply: () => { this.stats.critChance=Math.min(0.5,this.stats.critChance+0.07); } },
                { title: "КАЗНЯЩАЯ СТРЕЛА", desc: "+0.35x множитель критического урона", apply: () => { this.stats.critMultiplier+=0.35; } }
            ];
            if(!this.stats.earthUnlocked) all.push({title:"ЗЕМЛЯНОЙ РАЗЛОМ",desc:"Открывает круговую волну земли вокруг героя",apply:()=>{this.stats.earthUnlocked=true;this.stats.earthClock=0;}});
            else {
                all.push({title:"СИЛА ЗЕМЛИ",desc:"+35% урон волны и +12% радиус",apply:()=>{this.stats.earthDamage*=1.35;this.stats.earthRadius=(this.stats.earthRadius||145)*1.12;}});
                all.push({title:"ДРЕВНИЙ ИМПУЛЬС",desc:"Волна земли перезаряжается на 18% быстрее",apply:()=>{this.stats.earthDelay=Math.max(3200,this.stats.earthDelay*0.82);}});
            }
            if(!this.stats.windUnlocked) all.push({title:"ДУХ ВЕТРА",desc:"Призывает сильный шар ветра с периодическим уроном",apply:()=>{this.stats.windUnlocked=true;this.stats.windOrbCount=1;this.stats.windDamage=(38+this.level*2.5)*this.runePower;this.stats.windDelay=420;this.stats.windRadius=88;this.stats.windEvolved=false;}});
            else {
                all.push({title:"ШТОРМОВОЙ ШАР",desc:"+55% урон ветра и быстрее повторные попадания",apply:()=>{this.stats.windDamage*=1.55;this.stats.windDelay=Math.max(220,this.stats.windDelay*0.84);}});
                if((this.stats.windOrbCount||1)<4) all.push({title:"ВТОРОЙ ПОТОК",desc:"Добавляет ещё один шар ветра вокруг героя",apply:()=>{this.stats.windOrbCount=Math.min(4,(this.stats.windOrbCount||1)+1);this.stats.windDamage*=1.12;}});
                all.push({title:"ВОРОНКА ВЕТРА",desc:"+20% радиус вращения и +25% урон",apply:()=>{this.stats.windRadius*=1.2;this.stats.windDamage*=1.25;}});
                if((this.stats.windOrbCount||1)>=3&&!this.stats.windEvolved)all.push({title:"ЭВОЛЮЦИЯ: ГРОЗОВАЯ КОРОНА",desc:"Четыре сферы ветра создают ударные всплески при попадании",apply:()=>{this.stats.windEvolved=true;this.stats.windOrbCount=4;this.stats.windDamage*=1.6;this.stats.windDelay=Math.max(190,this.stats.windDelay*.82);this.stats.windRadius*=1.18;}});
            }
            if(!this.lightningUnlocked)all.push({title:"ЦЕПНАЯ МОЛНИЯ",desc:"Молния поражает двух ближайших врагов и перескакивает между ними",apply:()=>{this.lightningUnlocked=true;this.lightningClock=0;this.lightningDamage=(58+this.level*2)*this.runePower;this.lightningDelay=6200;this.lightningTargets=2;}});
            else{all.push({title:"ГРОЗОВОЙ РАЗРЯД",desc:"+45% урон молнии и -12% перезарядка",apply:()=>{this.lightningDamage*=1.45;this.lightningDelay=Math.max(2600,this.lightningDelay*0.88);}});all.push({title:"ЦЕПНАЯ РЕАКЦИЯ",desc:"Молния поражает ещё одну цель",apply:()=>{this.lightningTargets=Math.min(5,this.lightningTargets+1);this.lightningDamage*=1.12;}});}
            return Phaser.Utils.Array.Shuffle(all).slice(0,3);
        }

        openUpgrade() {
            if (this.mode !== "playing") return;
            this.mode = "upgrade";
            this.physics.pause();
            this.panel("НОВЫЙ УРОВЕНЬ", "ВЫБЕРИ ОДНО УЛУЧШЕНИЕ");
            this.addOverlay(this.add.text(W / 2, H / 2 - 105, "УРОВЕНЬ " + this.level, this.style(13, C.gold, true)).setOrigin(0.5));
            this.upgradeOptions().forEach((option, index) => {
                const y = H / 2 - 48 + index * 72;
                const bg = this.add.rectangle(W / 2, y, 520, 58, 0x493a29).setStrokeStyle(2, 0xb99a62, 0.95).setInteractive();
                const title = this.add.text(W / 2 - 235, y - 9, option.title, this.style(13, C.parchment, true));
                const desc = this.add.text(W / 2 - 235, y + 12, option.desc, this.style(11, C.muted, false));
                bg.on("pointerdown", () => {
                    option.apply(); this.selectedUpgrades.push(option.title);
                    this.mode = "playing";
                    this.clearOverlay();
                    this.physics.resume();
                    this.refreshHud();
                });
                this.addOverlay(bg); this.addOverlay(title); this.addOverlay(desc);
            });
        }

        castChainLightning(){
            const targets=[];this.enemies.children.iterate(e=>{if(e&&e.active)targets.push(e);});
            const reachable=targets.filter(e=>Phaser.Math.Distance.Between(this.player.x,this.player.y,e.x,e.y)<590);
            reachable.sort((a,b)=>Phaser.Math.Distance.Between(this.player.x,this.player.y,a.x,a.y)-Phaser.Math.Distance.Between(this.player.x,this.player.y,b.x,b.y));
            targets.length=0;targets.push(...reachable);
            if(!targets.length)return;
            const graphics=this.add.graphics().setDepth(29);let sx=this.player.x,sy=this.player.y;
            const count=Math.min(this.lightningTargets,targets.length);
            for(let i=0;i<count;i++){
                const e=targets[i],dx=e.x-sx,dy=e.y-sy;graphics.lineStyle(5,0x6fe9ff,0.95);graphics.beginPath();graphics.moveTo(sx,sy);
                for(let p=1;p<5;p++){const t=p/5;graphics.lineTo(sx+dx*t+Phaser.Math.Between(-13,13),sy+dy*t+Phaser.Math.Between(-13,13));}graphics.lineTo(e.x,e.y);graphics.strokePath();
                this.damageSkillEnemy(e,this.lightningDamage*(1-i*0.12),0,0);sx=e.x;sy=e.y;
                const flash=this.add.circle(e.x,e.y,13,0x9df7ff,0.9).setDepth(30);this.tweens.add({targets:flash,scale:2.6,alpha:0,duration:230,onComplete:()=>flash.destroy()});
            }
            this.tweens.add({targets:graphics,alpha:0,duration:260,onComplete:()=>graphics.destroy()});this.cameras.main.shake(90,0.0018);
        }

        updateEnemySkill(enemy,dt,angle){
            enemy.skillClock-=dt;
            if(enemy.skillState==="dash"){
                enemy.skillDuration-=dt;if(enemy.skillDuration<=0){enemy.skillState="idle";enemy.charging=false;enemy.body.setVelocity(0,0);enemy.skillClock=Phaser.Math.Between(4200,6000);enemy.sprite.clearTint();}
                return;
            }
            if(enemy.skillState==="windup"){
                enemy.skillDuration-=dt;if(enemy.skillDuration<=0){
                    enemy.skillState="dash";enemy.charging=true;enemy.skillDuration=enemy.kind==="beetle"?720:520;
                    const a=Phaser.Math.Angle.Between(enemy.x,enemy.y,this.player.x,this.player.y),speed=enemy.kind==="beetle"?300:350;
                    enemy.body.setVelocity(Math.cos(a)*speed,Math.sin(a)*speed);enemy.sprite.setTint(enemy.kind==="beetle"?0xffa05a:0x8effef);
                }return;
            }
            if(enemy.kind==="cultist"&&enemy.skillClock<=0){
                enemy.skillClock=3600;const tx=this.player.x,ty=this.player.y;
                const bolt=this.add.circle(enemy.x,enemy.y,8,0x9d6cff,0.95).setStrokeStyle(2,0xe5caff).setDepth(28);
                this.tweens.add({targets:bolt,x:tx,y:ty,scale:0.55,duration:850,ease:"Cubic.In",onComplete:()=>{
                    if(enemy.active&&this.mode==="playing"&&Phaser.Math.Distance.Between(this.player.x,this.player.y,tx,ty)<42&&this.invulnerable<=0){this.stats.hp-=enemy.damage;this.invulnerable=520;this.cameras.main.flash(90,135,75,190);if(this.stats.hp<=0)this.endRun();this.refreshHud();}
                    bolt.destroy();
                }});
            }
            if(enemy.skillClock<=0&&(enemy.kind==="wraith"||enemy.kind==="beetle")){
                enemy.skillState="windup";enemy.skillDuration=520;enemy.charging=true;enemy.body.setVelocity(0,0);enemy.sprite.setTint(0xffe18a);
                const mark=this.add.circle(enemy.x,enemy.y,enemy.kind==="beetle"?23:17,0xffffff,0.12).setStrokeStyle(2,enemy.kind==="beetle"?0xff8d52:0x8dfff2).setDepth(18);
                this.tweens.add({targets:mark,alpha:0,scale:1.7,duration:520,onComplete:()=>mark.destroy()});
            }
            if(enemy.kind==="brute"&&enemy.skillClock<=0&&Phaser.Math.Distance.Between(enemy.x,enemy.y,this.player.x,this.player.y)<115){
                enemy.skillClock=6200;const x=enemy.x,y=enemy.y;const ring=this.add.circle(x,y,20,0xd58b4b,0.12).setStrokeStyle(3,0xffb56c).setDepth(19);
                this.tweens.add({targets:ring,radius:92,alpha:0,duration:600,onComplete:()=>{if(this.mode==="playing"&&Phaser.Math.Distance.Between(this.player.x,this.player.y,x,y)<92&&this.invulnerable<=0){this.stats.hp-=Math.max(8,Math.round(enemy.damage*.75));this.invulnerable=500;this.cameras.main.shake(110,.003);if(this.stats.hp<=0)this.endRun();this.refreshHud();}ring.destroy();}});
            }
        }

        bossTelegraph(boss,x,y,radius,damage,color,delay){
            const warning=this.add.circle(x,y,Math.max(12,radius*.18),color,0.16).setStrokeStyle(4,color,0.95).setDepth(27);
            this.tweens.add({targets:warning,radius:radius,alpha:0.45,duration:delay||850,ease:"Sine.In"});
            this.time.delayedCall(delay||850,()=>{
                if(this.mode==="playing"&&boss.active&&Phaser.Math.Distance.Between(this.player.x,this.player.y,x,y)<radius&&this.invulnerable<=0){this.stats.hp-=damage;this.invulnerable=650;this.cameras.main.shake(170,.0045);if(this.stats.hp<=0)this.endRun();this.refreshHud();}
                if(warning.active)warning.destroy();
            });
        }

        refreshHud() {
            if (!this.stats) return;
            this.hud.wave.setText("ВОЛНА " + this.wave);
            this.hud.timer.setText(this.formatTime(this.elapsedMs));
            this.hud.level.setText("УР. " + this.level);
            this.hud.hp.width = Math.max(0, 146 * this.stats.hp / this.stats.maxHp);
            this.hud.xp.width=Math.max(1,146*this.xp/this.xpNext);
            if(this.hud.skillCards){this.hud.skillCards.wind.setVisible(!!this.stats.windUnlocked);this.hud.skillCards.earth.setVisible(!!this.stats.earthUnlocked);this.hud.skillCards.lightning.setVisible(!!this.lightningUnlocked);if(this.stats.windUnlocked){const t=this.hud.skillCards.wind.list[1];if(t)t.setText(this.stats.windEvolved?"КОРОНА":"ВЕТЕР");}}
            if(this.bossBar&&this.bossBar.active&&this.bossBar.boss&&this.bossBar.boss.active)this.bossBar.fill.width=Math.max(1,276*this.bossBar.boss.hp/this.bossBar.boss.maxHp);
            else if(this.bossBar){this.bossBar.destroy();this.bossBar=null;if(this.bossLabel){this.bossLabel.destroy();this.bossLabel=null;}}
        }

        endRun() {
            this.mode = "gameover"; this.playerWalkTween.pause();
            this.physics.pause();
            this.joy.active = false;
            this.best = Math.max(this.best, this.score);
            writeSave({ best: this.best, lastScore: this.score, bestWave: Math.max(Number(readSave().bestWave || 0), this.wave) });
            this.hideHud();
            this.panel("ЗАБЕГ ОКОНЧЕН", "РУИНЫ СНОВА ПОГЛОТИЛИ РЫЦАРЯ");
            this.addOverlay(this.add.text(W / 2, H / 2 - 22,
                "СЧЁТ  " + this.score.toLocaleString("ru-RU") + "\nВОЛНА  " + this.wave + "     УРОВЕНЬ  " + this.level,
                this.style(18, C.parchment, true)).setOrigin(0.5).setAlign("center").setLineSpacing(8));
            this.addOverlay(this.add.text(W / 2, H / 2 + 48, "ЛУЧШИЙ РЕЗУЛЬТАТ  " + this.best, this.style(13, C.gold, true)).setOrigin(0.5));
            this.makeButton(W / 2, H / 2 + 108, "ЕЩЁ ОДИН ЗАБЕГ", () => this.startRun());
            if (sdk) sdk.vibrate(120);
        }

        hideHud() {
            this.hud.top.setVisible(false);
            this.hud.joy.setVisible(false);
            if(this.hud.skills)this.hud.skills.setVisible(false);
            this.hud.help.setVisible(false);
        }

        update(_, delta) {
            if (this.mode !== "playing" || !this.stats) return;
            const dt = Math.min(delta, 40);
            this.elapsedMs+=dt; this.hud.timer.setText(this.formatTime(this.elapsedMs)); this.waveClock+=dt; this.earthClock+=dt;
            this.updateWorldChunks(this.player.x,this.player.y);
            this.spawnClock += dt;
            this.fireClock += dt;
             this.invulnerable = Math.max(0, this.invulnerable - dt);
            this.player.setAlpha(this.invulnerable > 0 && Math.floor(this.time.now / 75) % 2 ? 0.48 : 1);

            if(this.bossBar&&this.bossBar.active&&this.bossBar.boss&&this.bossBar.boss.active)this.bossBar.fill.width=Math.max(1,276*this.bossBar.boss.hp/this.bossBar.boss.maxHp);
            if (this.target && !this.target.active) this.target = null;

            const moving = Math.hypot(this.joy.dx, this.joy.dy) > 0.08;
            if (moving) {
                this.playerWalkTween.resume();
                this.playerSprite.setAngle(Phaser.Math.Clamp(this.joy.dx * 0.045, -0.045, 0.045));
                this.playerSprite.setFlipX(this.joy.dx < -0.08);
                const step = Math.sin(this.time.now * 0.018);
                this.playerLegLeft.setAngle(step * 0.42);
                this.playerLegRight.setAngle(-step * 0.42);
                this.playerLegLeft.y = 10 + Math.max(0, step) * 1.4;
                this.playerLegRight.y = 10 + Math.max(0, -step) * 1.4;
                this.lastMoveX = this.joy.dx;
                this.lastMoveY = this.joy.dy;
                this.player.body.setVelocity(this.joy.dx * this.stats.moveSpeed, this.joy.dy * this.stats.moveSpeed);
            } else {
                this.playerWalkTween.pause();
                this.playerSprite.y = -2;
                this.playerSprite.angle = 0;
                this.playerLegLeft.setAngle(0).setY(10);
                this.playerLegRight.setAngle(0).setY(10);
                this.player.body.setVelocity(this.player.body.velocity.x * 0.82, this.player.body.velocity.y * 0.82);
            }

            if (this.spawnClock > Math.max(390, 1500 - this.wave * 48)) {
                this.spawnClock = 0;
                const count = Math.min(4, 1 + Math.floor(this.wave / 5));
                for (let i = 0; i < count; i++) this.spawnEnemy();
            }
            if(this.elapsedMs>=180000 && Math.floor(this.elapsedMs/180000)>this.lastBossMinute){this.lastBossMinute=Math.floor(this.elapsedMs/180000);this.spawnBoss(this.lastBossMinute);}
            if(this.stats.earthUnlocked && this.earthClock>=this.stats.earthDelay){this.earthClock=0;this.castEarthWave();}
            if(this.lightningUnlocked){this.lightningClock+=dt;if(this.lightningClock>=this.lightningDelay){this.lightningClock=0;this.castChainLightning();}}
            this.updateWindOrbs(dt);
            if (this.fireClock >= this.stats.fireDelay) {
                this.fireClock = 0;
                this.fire();
            }
            if (this.waveClock >= 28000) {
                this.waveClock = 0;
                this.wave++;
                this.refreshHud();
                this.addWaveNotice();
            }

            this.enemies.children.iterate(enemy => {
                if (!enemy || !enemy.active) return;
                const angle=Phaser.Math.Angle.Between(enemy.x,enemy.y,this.player.x,this.player.y);
                if(!enemy.isBoss)this.updateEnemySkill(enemy,dt,angle);
                if(enemy.kind==="cultist"&&!enemy.charging&&enemy.skillState==="idle"){
                    const distance=Phaser.Math.Distance.Between(enemy.x,enemy.y,this.player.x,this.player.y);
                    if(distance<190)enemy.body.setVelocity(-Math.cos(angle)*enemy.speed,-Math.sin(angle)*enemy.speed);
                    else if(distance>310)enemy.body.setVelocity(Math.cos(angle)*enemy.speed,Math.sin(angle)*enemy.speed);
                    else enemy.body.setVelocity(0,0);
                }else if(!enemy.charging&&enemy.skillState!=="windup")enemy.body.setVelocity(Math.cos(angle)*enemy.speed,Math.sin(angle)*enemy.speed);
                if(enemy.sprite)enemy.sprite.setFlipX(this.player.x<enemy.x);
                if(enemy.isBoss){
                    if(!enemy.enraged&&enemy.hp<=enemy.maxHp*.45){enemy.enraged=true;enemy.speed*=1.28;enemy.attackClock=Math.min(enemy.attackClock,1200);enemy.sprite.setTint(0xff8b83);this.showCombatNotice(enemy.bossName+" ВПАДАЕТ В ЯРОСТЬ",0xff8b83);this.cameras.main.shake(260,.004);}
                    enemy.attackClock-=dt;if(enemy.attackClock<=0)this.bossAttack(enemy);
                }
            });
            this.orbs.children.iterate(orb => {
                if (!orb || !orb.active) return;
                const d = Phaser.Math.Distance.Between(orb.x, orb.y, this.player.x, this.player.y);
                if (d < this.stats.magnet) {
                    const angle = Phaser.Math.Angle.Between(orb.x, orb.y, this.player.x, this.player.y);
                    orb.body.setVelocity(Math.cos(angle) * 250, Math.sin(angle) * 250);
                } else orb.body.setVelocity(0, 0);
            });
            this.bullets.children.iterate(bolt => {
                if (!bolt || !bolt.active) return;
                bolt.life -= dt;
                if (bolt.body && bolt.body.enable) {
                    bolt.body.moves = true;
                    if (bolt.body.speed < 1) bolt.body.setVelocity(bolt.getData("velocityX"), bolt.getData("velocityY"));
                }
                const v=this.cameras.main.worldView;
                if(bolt.life<=0||bolt.x<v.x-100||bolt.x>v.right+100||bolt.y<v.y-100||bolt.y>v.bottom+100)bolt.destroy();
            });
         }


        formatTime(ms){const n=Math.floor(ms/1000);return String(Math.floor(n/60)).padStart(2,"0")+":"+String(n%60).padStart(2,"0");}
        updateWorldChunks(x,y){
            const size=520,cx=Math.floor(x/size),cy=Math.floor(y/size);
            for(let gx=cx-2;gx<=cx+2;gx++)for(let gy=cy-2;gy<=cy+2;gy++){
                const key=gx+":"+gy;if(this.worldChunks.has(key))continue;
                const objs=[];
                const biome=Math.abs((gx*31+gy*17)%3);const groundKey=biome===1?"groundAsh":biome===2?"groundRift":"ground";
                const ground=this.add.tileSprite((gx+0.5)*size,(gy+0.5)*size,size,size,groundKey).setDepth(-20).setTilePosition(gx*size,gy*size);
                objs.push(ground);
                let seed=Math.abs((gx*73856093)^(gy*19349663))>>>0;
                const rand=()=>{seed=(seed*1664525+1013904223)>>>0;return seed/4294967296;};
                for(let i=0,n=3+Math.floor(rand()*3);i<n;i++){
                    const px=gx*size+25+rand()*(size-50),py=gy*size+25+rand()*(size-50);
                    if(Phaser.Math.Distance.Between(px,py,x,y)<155)continue;
                    const roll=rand();
                    if(roll<0.11){
                        const obj=this.add.image(px,py,"dirt").setDepth(-5).setScale(1.7+rand()*0.8).setAlpha(0.98).setRotation(0);objs.push(obj);
                    }else if(roll<0.20){
                        const obj=this.add.image(px,py,"rubble").setDepth(3+py/100000).setScale(0.7+rand()*0.8).setRotation(rand()*0.8);objs.push(obj);
                    }else if(roll<0.26){
                        const obj=this.add.image(px,py,"grassTuft").setDepth(3+py/100000).setScale(0.8+rand()*0.7);objs.push(obj);
                    }else if(roll<0.34&&biome===2){
                        const obj=this.add.image(px,py,"crystal").setDepth(3+py/100000).setScale(0.9+rand()*0.6);objs.push(obj);
                    }else{
                        const obj=this.addObstacle(px,py,roll<0.39?"ruin":"tree",0.8+rand()*0.45);objs.push(obj);
                    }
                }
                if(rand()<0.32){
                    const px=gx*size+80+rand()*(size-160),py=gy*size+80+rand()*(size-160);
                    const obj=this.add.image(px,py,"pillar").setDepth(3+py/100000).setAngle(rand()*0.5-0.25).setScale(0.8+rand()*0.4);objs.push(obj);
                }
                this.worldChunks.set(key,objs);
            }
            for(const [key,objs] of this.worldChunks.entries()){const [gx,gy]=key.split(":").map(Number);if(Math.abs(gx-cx)>2||Math.abs(gy-cy)>2){objs.forEach(o=>o.destroy());this.worldChunks.delete(key);}}
        }
        damageSkillEnemy(enemy,damage,knockbackX,knockbackY) {
            if(!enemy||!enemy.active)return;
            enemy.hp-=damage * (enemy.kind === "beetle" ? 0.82 : 1);
            enemy.healthBack.setVisible(true);enemy.healthBar.setVisible(true);
            enemy.healthBar.width=Math.max(1,28*enemy.hp/enemy.maxHp);
            enemy.sprite.setTint(0xb9f4ef);
            this.time.delayedCall(100,()=>{if(enemy.active)enemy.sprite.clearTint();});
            if(knockbackX||knockbackY)enemy.body.setVelocity(knockbackX,knockbackY);
            if(enemy.hp<=0)this.killEnemy(enemy);
        }

        castEarthWave(){
            const radius=this.stats.earthRadius||145;
            const ring=this.add.circle(this.player.x,this.player.y,24,0xb18a55,0.12).setStrokeStyle(5,0xd7bd82,0.95).setDepth(21);
            const inner=this.add.circle(this.player.x,this.player.y,12,0xe7d3a1,0.28).setDepth(21);
            const cracks=this.add.graphics().setDepth(20);
            cracks.lineStyle(3,0xc7a66c,0.9);
            for(let i=0;i<12;i++){
                const a=i*Math.PI/6, r1=18, r2=Phaser.Math.Between(55,105);
                const ex=this.player.x+Math.cos(a)*r2,ey=this.player.y+Math.sin(a)*r2;
                cracks.lineBetween(this.player.x+Math.cos(a)*r1,this.player.y+Math.sin(a)*r1,ex,ey);
                cracks.lineBetween(ex,ey,ex+Math.cos(a+0.45)*14,ey+Math.sin(a+0.45)*14);
            }
            this.tweens.add({targets:ring,scale:radius/24,alpha:0,duration:520,ease:"Cubic.Out",onComplete:()=>ring.destroy()});
            this.tweens.add({targets:inner,scale:5,alpha:0,duration:350,onComplete:()=>inner.destroy()});
            this.tweens.add({targets:cracks,alpha:0,duration:520,onComplete:()=>cracks.destroy()});
            this.cameras.main.shake(110,0.0028);
            this.enemies.getChildren().slice().forEach(enemy=>{
                if(!enemy.active)return;
                const dx=enemy.x-this.player.x,dy=enemy.y-this.player.y,d=Math.hypot(dx,dy);
                if(d<=radius){const force=180,den=Math.max(1,d);this.damageSkillEnemy(enemy,this.stats.earthDamage,dx/den*force,dy/den*force);}
            });
        }

        updateWindOrbs(dt){
            if(!this.stats||!this.stats.windUnlocked){
                if(this.windOrb){this.windOrb.destroy();this.windOrb=null;}
                return;
            }
            const count=this.stats.windOrbCount||1,orbit=this.stats.windRadius||88;
            this.windAngle+=dt*0.00165;
            if(!this.windOrb||!this.windOrb.active){
                this.windOrb=this.add.container(this.player.x,this.player.y).setDepth(23);
                this.windOrb.items=[];
                for(let i=0;i<count;i++){
                    const orb=this.add.image(0,0,"windorb").setScale(1.15);
                    this.windOrb.add(orb);this.windOrb.items.push(orb);
                    this.tweens.add({targets:orb,scale:1.38,duration:420,yoyo:true,repeat:-1,ease:"Sine.InOut"});
                }
            }
            while(this.windOrb.items.length<count){
                const orb=this.add.image(0,0,"windorb").setScale(1.15);
                this.windOrb.add(orb);this.windOrb.items.push(orb);
                this.tweens.add({targets:orb,scale:1.38,duration:420,yoyo:true,repeat:-1,ease:"Sine.InOut"});
            }
            while(this.windOrb.items.length>count)this.windOrb.items.pop().destroy();
            this.windOrb.setPosition(this.player.x,this.player.y);
            this.windOrb.items.forEach((orb,i)=>{
                const a=this.windAngle+(Math.PI*2*i/count);
                const x=Math.cos(a)*orbit,y=Math.sin(a)*orbit;
                orb.setPosition(x,y).setRotation(a+Math.PI/2);
                this.enemies.getChildren().forEach(enemy=>{
                    if(!enemy.active)return;
                    if(Phaser.Math.Distance.Between(this.player.x+x,this.player.y+y,enemy.x,enemy.y)<34){
                        if(!enemy.windHitAt||this.time.now-enemy.windHitAt>=this.stats.windDelay){
                            enemy.windHitAt=this.time.now;
                            this.damageSkillEnemy(enemy,this.stats.windDamage,Math.cos(a)*55,Math.sin(a)*55);
                            if(this.stats.windEvolved&&(!enemy.windBurstAt||this.time.now-enemy.windBurstAt>1500)){
                                enemy.windBurstAt=this.time.now;const burst=this.add.circle(enemy.x,enemy.y,12,0x7de8f2,0.16).setStrokeStyle(3,0xbaffff,0.9).setDepth(26);
                                this.tweens.add({targets:burst,radius:70,alpha:0,duration:360,onComplete:()=>burst.destroy()});
                                this.enemies.getChildren().slice().forEach(other=>{if(other&&other.active&&other!==enemy&&Phaser.Math.Distance.Between(enemy.x,enemy.y,other.x,other.y)<70)this.damageSkillEnemy(other,this.stats.windDamage*.48,0,0);});
                            }
                        }
                    }
                });
            });
        }

        spawnBoss(stage){
            const types=[{name:"МАЛЫЙ СТРАЖ РАЗЛОМА",kind:"bossRift",hp:380,speed:43,damage:21,scale:1.75,tint:0xffffff},
                {name:"КОСТЯНОЙ ПАЛАЧ",kind:"bossBone",hp:650,speed:68,damage:27,scale:1.9,tint:0xffffff},
                {name:"ДРЕВНИЙ КОЛОСС",kind:"bossColossus",hp:1050,speed:48,damage:36,scale:1.8,tint:0xffffff}];
            const b=types[(stage-1)%types.length],factor=1+(stage-1)*.42,v=this.cameras.main.worldView,side=Phaser.Math.Between(0,3);
            const x=side===0?v.x-50:side===1?v.right+50:Phaser.Math.Between(v.x+50,v.right-50);
            const y=side===2?v.y-50:side===3?v.bottom+50:Phaser.Math.Between(v.y+80,v.bottom-50);
            const boss=this.add.container(x,y).setDepth(24);boss.sprite=this.add.image(0,0,b.kind).setScale(b.scale).setTint(b.tint);boss.add(boss.sprite);
            this.physics.add.existing(boss);boss.body.setCircle(25,-25,-25);boss.body.setAllowGravity(false);
            boss.kind=b.kind;boss.isBoss=true;boss.bossName=b.name;boss.bossStage=stage;boss.attackClock=4300;boss.enraged=false;boss.hp=Math.round(b.hp*factor);boss.maxHp=boss.hp;boss.speed=b.speed+stage*2.4;boss.damage=Math.round(b.damage*factor);boss.xpValue=16+stage*3;boss.radius=25;
            boss.healthBack=this.add.rectangle(0,-43,64,6,0x241a18).setOrigin(.5).setVisible(false);boss.healthBar=this.add.rectangle(-32,-43,64,4,0xd94b3e).setOrigin(0,.5).setVisible(false);boss.add([boss.healthBack,boss.healthBar]);
            boss.animTween=this.tweens.add({targets:boss.sprite,y:4,duration:310,yoyo:true,repeat:-1});this.enemies.add(boss);
            this.bossLabel=this.add.text(W/2,78,b.name,this.style(18,0xffd38a,true)).setOrigin(.5).setScrollFactor(0).setDepth(70);
            this.bossBar=this.add.container(W/2,105).setScrollFactor(0).setDepth(70);const back=this.add.rectangle(0,0,280,10,0x2b1b18).setStrokeStyle(1,0xd0a45e),fill=this.add.rectangle(-138,0,276,6,0xb83d36).setOrigin(0,.5);
            this.bossBar.add([back,fill]);this.bossBar.fill=fill;this.bossBar.boss=boss;
            const notice=this.add.text(W/2,H/2-125,"БОСС: "+b.name,this.style(24,0xffc66e,true)).setOrigin(.5).setScrollFactor(0).setDepth(75);
            this.tweens.add({targets:notice,alpha:0,y:notice.y-45,duration:1800,onComplete:()=>notice.destroy()});this.cameras.main.shake(350,.006);if(sdk)sdk.vibrate(100);
        }

        bossAttack(boss){
            if(!boss.active)return;
            if(boss.kind==="bossRift"){
                boss.attackClock=5000*(boss.enraged?0.72:1);const tx=this.player.x,ty=this.player.y;this.showCombatNotice("СТРАЖ: УДАР РАЗЛОМА",0x9bf4c5);this.bossTelegraph(boss,tx,ty,108,Math.round(boss.damage*1.35),0x72f0c1,900);
                boss.charging=true;const a=Phaser.Math.Angle.Between(boss.x,boss.y,tx,ty);boss.body.setVelocity(Math.cos(a)*230,Math.sin(a)*230);this.time.delayedCall(480,()=>{if(boss.active){boss.charging=false;boss.body.setVelocity(0,0);}});
            }else if(boss.kind==="bossBone"){
                boss.attackClock=5700*(boss.enraged?0.72:1);this.showCombatNotice("ПАЛАЧ: КОСТЯНЫЕ ШИПЫ",0xffb5a0);const a=Phaser.Math.Angle.Between(boss.x,boss.y,this.player.x,this.player.y),px=this.player.x,py=this.player.y;
                for(let i=-1;i<=1;i++){const x=px+Math.cos(a+Math.PI/2)*i*58,y=py+Math.sin(a+Math.PI/2)*i*58;this.bossTelegraph(boss,x,y,43,Math.round(boss.damage*.85),0xe8d6b0,760+i*90);}
                boss.charging=true;boss.body.setVelocity(Math.cos(a)*285,Math.sin(a)*285);this.time.delayedCall(620,()=>{if(boss.active){boss.charging=false;boss.body.setVelocity(0,0);}});
            }else{
                boss.attackClock=6800*(boss.enraged?0.72:1);this.showCombatNotice("КОЛОСС: ЗЕМЛЕТРЯСЕНИЕ",0xd3a8ff);const x=boss.x,y=boss.y;
                const ring=this.add.circle(x,y,28,0x9b75c9,.13).setStrokeStyle(5,0xd4a7ff,.95).setDepth(27);
                this.tweens.add({targets:ring,radius:190,alpha:0,duration:1000,ease:"Cubic.Out",onComplete:()=>ring.destroy()});
                this.time.delayedCall(850,()=>{if(this.mode==="playing"&&boss.active){const d=Phaser.Math.Distance.Between(this.player.x,this.player.y,x,y);if(d<190&&d>62&&this.invulnerable<=0){this.stats.hp-=Math.round(boss.damage*1.25);this.invulnerable=700;if(this.stats.hp<=0)this.endRun();this.refreshHud();}this.spawnEnemy("skeleton");this.spawnEnemy("brute");boss.hp=Math.min(boss.maxHp,boss.hp+Math.round(boss.maxHp*.035));}});
            }
        }

        pauseRun(){
            if(this.mode!=="playing")return;this.mode="paused";this.physics.pause();this.playerWalkTween.pause();
            this.joy.active=false;this.joy.dx=0;this.joy.dy=0;this.hud.joyKnob.setPosition(0,0);
            this.panel("ПАУЗА","ЗАБЕГ ПРИОСТАНОВЛЕН");
            this.addOverlay(this.add.text(W/2,H/2-111,"ВРЕМЯ  "+this.formatTime(this.elapsedMs)+"     УРОВЕНЬ  "+this.level+
                "     СЛЕДУЮЩИЙ БОСС  "+this.formatTime(Math.max(0,(Math.floor(this.elapsedMs/180000)+1)*180000-this.elapsedMs)),this.style(12,C.parchment,true)).setOrigin(.5).setAlign("center"));
            this.addOverlay(this.add.text(W/2,H/2-75,"УСИЛЕНИЯ, ВЫБРАННЫЕ ПРИ ПОВЫШЕНИИ УРОВНЯ",this.style(12,C.gold,true)).setOrigin(.5));
            const recent = this.selectedUpgrades.slice(-6);
            if (!recent.length) {
                this.addOverlay(this.add.text(W/2,H/2+10,"Пока не выбрано ни одного усиления",this.style(13,C.muted,false)).setOrigin(.5));
            } else {
                recent.forEach((name,i)=>{
                    const col=i%2,row=Math.floor(i/2),x=W/2-155+col*310,y=H/2-32+row*48;
                    this.addOverlay(this.add.rectangle(x,y,286,38,0x3b3428,0.98).setStrokeStyle(1,0x8b744c,0.9));
                    this.addOverlay(this.add.text(x,y,(this.selectedUpgrades.length-recent.length+i+1)+".  "+name,this.style(11,C.parchment,true)).setOrigin(.5));
                });
            }
            this.makeButton(W/2,H/2+153,"ПРОДОЛЖИТЬ",()=>{this.clearOverlay();this.mode="playing";this.physics.resume();},250);
            this.makeButton(W/2,H/2+207,"В ГЛАВНОЕ МЕНЮ",()=>{this.clearOverlay();this.hideHud();this.showMenu();},250);
        }

        addWaveNotice() {
            const label = this.add.text(W / 2, H / 2 - 105, "ВОЛНА " + this.wave, this.style(27, C.gold, true)).setOrigin(0.5).setScrollFactor(0).setDepth(60);
            this.tweens.add({ targets: label, alpha: 0, y: label.y - 35, duration: 1200, onComplete: () => label.destroy() });
            if (sdk) sdk.vibrate(22);
        }
    }

    const game = new Phaser.Game({
        type: Phaser.AUTO,
        parent: "game",
        width: W,
        height: H,
        backgroundColor: "#426b3e",
        scale: { mode: Phaser.Scale.FIT, autoCenter: Phaser.Scale.CENTER_BOTH },
        physics: { default: "arcade", arcade: { debug: false } },
        input: { activePointers: 3 },
        render: { antialias: false, pixelArt: true, roundPixels: true, powerPreference: "low-power" },
        fps: { target: 60, forceSetTimeOut: false },
        scene: [Arena]
    });

    window.addEventListener("error", event => console.error("Rift Survivors:", event.message));
    window.addEventListener("unhandledrejection", event => console.error("Rift Survivors promise:", event.reason));
    window.addEventListener("contextmenu", event => event.preventDefault());
})();