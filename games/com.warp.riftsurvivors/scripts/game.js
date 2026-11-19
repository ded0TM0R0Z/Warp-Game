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
            this.dashCooldown = 0;
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

        create() {
            this.physics.world.setBounds(-100000, -100000, 200000, 200000);
            this.createTextures();
            this.obstacles = this.physics.add.staticGroup();
            this.createWorld();
            this.enemies = this.physics.add.group({ runChildUpdate: true });
            this.bullets = this.physics.add.group();
            this.orbs = this.physics.add.group();
            this.createPlayer();
            this.createHud();
            this.physics.add.overlap(this.bullets, this.enemies, this.hitEnemy, null, this);
            this.physics.add.overlap(this.player, this.enemies, this.hitPlayer, null, this);
            this.physics.add.overlap(this.player, this.orbs, this.collectXp, null, this);
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
            make("ground", 32, 32, g => {
                g.fillStyle(0x426b3e); g.fillRect(0, 0, 32, 32);
                g.fillStyle(0x4e7946); g.fillRect(2, 3, 5, 3); g.fillRect(18, 8, 4, 3);
                g.fillStyle(0x365d38); g.fillRect(10, 17, 5, 2); g.fillRect(25, 25, 4, 3);
                g.fillStyle(0x6b8a4b); g.fillRect(24, 2, 2, 2); g.fillRect(5, 25, 2, 2);
            });
            make("hero", 24, 28, g => {
                g.fillStyle(0x302b28); g.fillRect(6, 2, 12, 5);
                g.fillStyle(0xc6c6bb); g.fillRect(5, 6, 14, 12); g.fillRect(7, 4, 10, 3);
                g.fillStyle(0x596b78); g.fillRect(7, 9, 10, 8);
                g.fillStyle(0xd9b98a); g.fillRect(9, 7, 6, 5);
                g.fillStyle(0xe3c36d); g.fillRect(10, 13, 4, 3);
                g.fillStyle(0xddd7c6); g.fillRect(18, 10, 3, 13);
                g.fillStyle(0x8e9699); g.fillRect(19, 5, 2, 7);
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
                g.fillStyle(0xa99f89); g.fillRect(7, 15, 10, 3);
                g.fillStyle(0xd8d0b9); g.fillRect(6, 22, 4, 5); g.fillRect(14, 22, 4, 5);
                g.fillStyle(0x6d6251); g.fillRect(3, 12, 3, 9); g.fillRect(18, 12, 3, 9);
            });
            make("brute", 32, 34, g => {
                g.fillStyle(0x302923); g.fillRect(6, 5, 20, 25);
                g.fillStyle(0x88634a); g.fillRect(4, 8, 24, 18);
                g.fillStyle(0xb08a63); g.fillRect(8, 4, 16, 8);
                g.fillStyle(0x382b25); g.fillRect(9, 10, 4, 4); g.fillRect(19, 10, 4, 4);
                g.fillStyle(0xb9a27b); g.fillRect(9, 18, 14, 5);
                g.fillStyle(0x59402f); g.fillRect(2, 22, 7, 9); g.fillRect(23, 22, 7, 9);
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
                g.fillStyle(0x55534d); g.fillRect(3, 7, 38, 40);
                g.fillStyle(0x858176); g.fillRect(7, 4, 30, 8);
                g.fillStyle(0x393a38); g.fillRect(14, 20, 16, 27);
                g.fillStyle(0xa29b88); g.fillRect(7, 15, 8, 5); g.fillRect(29, 15, 8, 5);
                g.fillStyle(0x68665d); g.fillRect(3, 34, 38, 5);
            });
            make("torch", 12, 22, g => {
                g.fillStyle(0x62452e); g.fillRect(5, 7, 3, 15);
                g.fillStyle(0xb45c2d); g.fillRect(3, 4, 7, 7);
                g.fillStyle(0xf3c45d); g.fillRect(4, 1, 5, 6);
                g.fillStyle(0xffed9b); g.fillRect(5, 2, 2, 3);
            });
            make("dirt", 48, 48, g => {
                g.fillStyle(0x66513a); g.fillRect(0,0,48,48);
                g.fillStyle(0x806447); g.fillRect(4,7,12,4); g.fillRect(25,5,8,3); g.fillRect(18,27,15,5);
                g.fillStyle(0x4e402f); g.fillRect(9,20,7,4); g.fillRect(36,34,8,5); g.fillRect(2,39,10,3);
                g.fillStyle(0x9a7950); g.fillRect(20,12,4,3); g.fillRect(39,16,3,3);
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
            this.ground = this.add.tileSprite(W / 2, H / 2, 2400, 1600, "ground").setDepth(-20);
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
            obj.refreshBody();
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

            this.hud.dash = this.add.container(W - 92, H - 88).setDepth(50).setScrollFactor(0);
            const dashBase = this.add.circle(0, 0, 43, 0x493b2a, 0.94).setStrokeStyle(3, C.gold, 0.95);
            const dashLabel = this.add.text(0, -2, "РЫВОК", this.style(12, C.parchment, true)).setOrigin(0.5);
            this.dashButton = this.add.circle(0, 0, 46, 0xffffff, 0.001).setInteractive();
            this.dashButton.on("pointerdown", pointer => { pointer.event.stopPropagation(); this.dash(); });
            this.hud.dash.add([dashBase, dashLabel, this.dashButton]);

            this.hud.help = this.add.text(W / 2, H - 12, "ДВИЖЕНИЕ — ЛЕВЫЙ СТИК     •     АТАКА — АВТОМАТИЧЕСКИ ПО БЛИЖАЙШЕМУ ВРАГУ", this.style(10, 0xf0dfb3, true)).setOrigin(0.5, 1).setDepth(40).setScrollFactor(0);
            this.hud.top.setVisible(false);
            this.hud.joy.setVisible(false);
            this.hud.dash.setVisible(false);
            this.hud.help.setVisible(false);
            this.layoutJoystick();
        }

        layoutJoystick() {
            if (!this.hud.joy) return;
            this.hud.joy.setPosition(this.joy.baseX, this.joy.baseY);
        }

        style(size, color, bold) {
            return {
                fontFamily: "Georgia, 'Times New Roman', serif",
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
            this.addOverlay(this.add.rectangle(W / 2, H / 2, W, H, 0x10150f, 0.78));
            this.addOverlay(this.add.rectangle(W / 2, H / 2, 540, 360, 0x29251e, 0.98).setStrokeStyle(4, 0xc3a46a, 1));
            this.addOverlay(this.add.rectangle(W / 2, H / 2, 526, 346, 0x403628, 0.24).setStrokeStyle(1, 0x796442, 0.9));
            this.addOverlay(this.add.text(W / 2, H / 2 - 135, title, this.style(30, C.parchment, true)).setOrigin(0.5));
            if (subtitle) this.addOverlay(this.add.text(W / 2, H / 2 - 92, subtitle, this.style(14, C.gold, true)).setOrigin(0.5));
        }

        makeButton(x, y, label, callback, width) {
            const w = width || 250;
            const bg = this.add.rectangle(x, y, w, 48, 0x665032).setStrokeStyle(2, 0xd0b174, 1).setInteractive({ useHandCursor: true });
            const text = this.add.text(x, y, label, this.style(16, C.parchment, true)).setOrigin(0.5);
            bg.on("pointerdown", callback);
            this.addOverlay(bg); this.addOverlay(text);
        }

        showMenu() {
            this.mode = "menu";
            this.physics.pause();
            this.panel("RIFT SURVIVORS", "ПРОКЛЯТЫЕ РУИНЫ");
            this.addOverlay(this.add.text(W / 2, H / 2 - 30,
                "Ты — последний рыцарь у древнего разлома.\nОтбивайся от тварей, собирай эссенцию и усиливай клинок.",
                this.style(15, C.parchment, false)).setOrigin(0.5).setAlign("center").setLineSpacing(8));
            this.addOverlay(this.add.text(W / 2, H / 2 + 35, "РЕКОРД: " + this.best, this.style(14, C.gold, true)).setOrigin(0.5));
            this.makeButton(W / 2, H / 2 + 105, "НАЧАТЬ ЗАБЕГ", () => this.startRun());
            this.addOverlay(this.add.text(W / 2, H - 24,
                "Левый стик — движение   •   Рыцарь сам атакует ближайшего врага",
                this.style(11, C.muted, false)).setOrigin(0.5));
        }

        startRun() {
            this.clearOverlay();
            this.mode = "playing";
            this.physics.resume();
            this.enemies.clear(true, true);
            if(this.windOrb){this.windOrb.destroy();this.windOrb=null;}
            this.bullets.clear(true, true);
            this.orbs.clear(true, true);
            this.player.setPosition(W / 2, H / 2);
            this.player.body.setVelocity(0, 0);
            this.player.body.setAcceleration(0, 0);
            this.wave = 1; this.waveClock = 0; this.spawnClock = 0;
            this.fireClock = 250; this.dashCooldown = 0; this.invulnerable = 0;
            this.level = 1; this.xp = 0; this.xpNext = 7; this.kills = 0; this.score = 0;
            this.projectiles = 1;
            this.stats = { hp:100,maxHp:100,damage:20,fireDelay:900,moveSpeed:145,bulletSpeed:590,magnet:115,earthUnlocked:false,earthDamage:42,earthDelay:8500,windUnlocked:false,windDamage:18,windDelay:650,windRadius:76 };
            this.elapsedMs=0; this.lastBossMinute=0; this.earthClock=0; this.windAngle=0; this.windOrb=null; this.selectedUpgrades=[];
            this.worldChunks.forEach(items => items.forEach(obj => obj.destroy()));
            this.worldChunks.clear();
            this.player.body.reset(W / 2, H / 2);
            this.cameras.main.stopFollow(); this.cameras.main.startFollow(this.player,true,0.085,0.085);
            this.player.body.setMaxVelocity(180); this.playerWalkTween.pause(); this.playerSprite.setPosition(0,0).setAngle(0);
            this.joy.active = false; this.joy.pointerId = null; this.joy.dx = 0; this.joy.dy = 0;
            this.joy.baseX = 100; this.joy.baseY = H - 120;
            this.hud.joy.setPosition(this.joy.baseX, this.joy.baseY);
            this.hud.joyKnob.setPosition(0, 0);
            this.hud.top.setVisible(true); this.hud.joy.setVisible(true);
            this.hud.dash.setVisible(true); this.hud.help.setVisible(true);
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

        dash() {
            if (this.mode !== "playing" || this.dashCooldown > 0) return;
            let dx = this.joy.dx, dy = this.joy.dy;
            if (Math.hypot(dx, dy) < 0.15) {
                const enemy = this.nearestEnemy();
                if (enemy) {
                    dx = this.player.x - enemy.x; dy = this.player.y - enemy.y;
                    const length = Math.max(1, Math.hypot(dx, dy));
                    dx /= length; dy /= length;
                } else { dx = this.lastMoveX; dy = this.lastMoveY; }
            }
            this.player.body.setVelocity(dx * 590, dy * 590);
            this.invulnerable = 430;
            this.dashCooldown = 2600;
            this.cameras.main.shake(100, 0.0025);
            if (sdk) sdk.vibrate(35);
        }

        spawnEnemy(type) {
            if (this.mode !== "playing" || this.enemies.countActive(true) >= 65) return;
            const side = Phaser.Math.Between(0, 3);
            const view=this.cameras.main.worldView;
            const x=side===0?view.x-35:side===1?view.right+35:Phaser.Math.Between(view.x+25,view.right-25);
            const y=side===2?view.y-35:side===3?view.bottom+35:Phaser.Math.Between(view.y+75,view.bottom-30);
            const kind = type || (this.wave >= 4 && Math.random() < 0.12 ? "brute" : this.wave >= 2 && Math.random() < 0.34 ? "skeleton" : "slime");
            const key = kind === "brute" ? "brute" : kind === "skeleton" ? "skeleton" : "slime";
            const scale = kind === "brute" ? 1.45 : kind === "skeleton" ? 1.15 : 1.2;
            const enemy = this.add.container(x, y).setDepth(12);
            enemy.sprite = this.add.image(0, 0, key).setScale(scale);
            enemy.add(enemy.sprite);
            this.physics.add.existing(enemy);
            const radius = kind === "brute" ? 17 : kind === "skeleton" ? 12 : 11;
            enemy.body.setCircle(radius, -radius, -radius);
            enemy.body.setCollideWorldBounds(false);
            enemy.animTween=this.tweens.add({targets:enemy.sprite,y:2,duration:kind==="brute"?260:190,yoyo:true,repeat:-1});
            enemy.isBoss=false; enemy.bossName="";
            enemy.kind = kind;
            enemy.hp = kind === "brute" ? 125 + this.wave * 10 : kind === "skeleton" ? 58 + this.wave * 5 : 38 + this.wave * 4;
            enemy.maxHp = enemy.hp;
            enemy.speed = kind === "brute" ? 47 + this.wave : kind === "skeleton" ? 78 + this.wave * 1.5 : 65 + this.wave * 1.5;
            enemy.damage = kind === "brute" ? 24 : 13;
            enemy.xpValue = kind === "brute" ? 5 : kind === "skeleton" ? 2 : 1;
            enemy.radius = radius;
            enemy.hitFlash = 0;
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
                const bolt = this.add.image(this.player.x + Math.cos(a) * 17, this.player.y + Math.sin(a) * 17, "bolt").setDepth(18).setRotation(a).setScale(1.15);
                this.physics.add.existing(bolt);
                bolt.body.setAllowGravity(false);
                bolt.body.setVelocity(Math.cos(a) * this.stats.bulletSpeed, Math.sin(a) * this.stats.bulletSpeed);
                bolt.damage = this.stats.damage;
                bolt.life = 1300;
                this.bullets.add(bolt);
            }
            this.showTarget(target);
        }

        showTarget(target) {
            if (!this.targetMark || !this.targetMark.active) {
                this.targetMark = this.add.graphics().setDepth(30);
            }
            this.targetMark.clear();
            this.targetMark.lineStyle(2, 0xffe18a, 0.9);
            this.targetMark.strokeCircle(target.x, target.y, 20);
            this.targetMark.lineBetween(target.x - 25, target.y, target.x - 16, target.y);
            this.targetMark.lineBetween(target.x + 16, target.y, target.x + 25, target.y);
        }

        hitEnemy(bolt, enemy) {
            if (!bolt.active || !enemy.active) return;
            if(bolt.trail)bolt.trail.destroy();
            bolt.destroy();
            enemy.hp -= bolt.damage;
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
            if(enemy.isBoss){if(this.bossBar){this.bossBar.destroy();this.bossBar=null;}if(this.bossLabel){this.bossLabel.destroy();this.bossLabel=null;}this.score+=250*this.wave;this.stats.hp=Math.min(this.stats.maxHp,this.stats.hp+35);this.cameras.main.flash(220,255,205,110);}
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
                { title: "МАГНИТ ЭССЕНЦИИ", desc: "Сильнее притягивает опыт", apply: () => { this.stats.magnet += 55; } }
            ];
            if(!this.stats.earthUnlocked) all.push({title:"ЗЕМЛЯНОЙ РАЗЛОМ",desc:"Открывает круговую волну земли вокруг героя",apply:()=>{this.stats.earthUnlocked=true;this.stats.earthClock=0;}});
            else {
                all.push({title:"СИЛА ЗЕМЛИ",desc:"+35% урон волны и +12% радиус",apply:()=>{this.stats.earthDamage*=1.35;this.stats.earthRadius=(this.stats.earthRadius||145)*1.12;}});
                all.push({title:"ДРЕВНИЙ ИМПУЛЬС",desc:"Волна земли перезаряжается на 18% быстрее",apply:()=>{this.stats.earthDelay=Math.max(3200,this.stats.earthDelay*0.82);}});
            }
            if(!this.stats.windUnlocked) all.push({title:"ДУХ ВЕТРА",desc:"Открывает вращающийся вокруг героя шар ветра",apply:()=>{this.stats.windUnlocked=true;this.stats.windOrbCount=1;this.stats.windDamage=18;this.stats.windDelay=650;this.stats.windRadius=76;}});
            else {
                all.push({title:"ШТОРМОВОЙ ШАР",desc:"+35% урон ветра и +12% скорость вращения",apply:()=>{this.stats.windDamage*=1.35;this.stats.windDelay=Math.max(260,this.stats.windDelay*0.88);}});
                all.push({title:"ВТОРОЙ ПОТОК",desc:"Добавляет второй шар ветра на другой стороне орбиты",apply:()=>{this.stats.windOrbCount=Math.min(3,(this.stats.windOrbCount||1)+1);}});
                all.push({title:"ВОРОНКА ВЕТРА",desc:"+18% радиус вращения и +20% урон",apply:()=>{this.stats.windRadius*=1.18;this.stats.windDamage*=1.2;}});
            }
            return Phaser.Utils.Array.Shuffle(all).slice(0,3);
        }

        openUpgrade() {
            if (this.mode !== "playing") return;
            this.mode = "upgrade";
            this.physics.pause();
            this.panel("НОВЫЙ УРОВЕНЬ", "ВЫБЕРИ ОДНО УЛУЧШЕНИЕ");
            this.addOverlay(this.add.text(W / 2, H / 2 - 40, "УРОВЕНЬ " + this.level, this.style(13, C.gold, true)).setOrigin(0.5));
            this.upgradeOptions().forEach((option, index) => {
                const y = H / 2 + 20 + index * 67;
                const bg = this.add.rectangle(W / 2, y, 410, 56, 0x493a29).setStrokeStyle(2, 0xb99a62, 0.95).setInteractive();
                const title = this.add.text(W / 2 - 180, y - 9, option.title, this.style(13, C.parchment, true));
                const desc = this.add.text(W / 2 - 180, y + 12, option.desc, this.style(11, C.muted, false));
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

        refreshHud() {
            if (!this.stats) return;
            this.hud.wave.setText("ВОЛНА " + this.wave);
            this.hud.timer.setText(this.formatTime(this.elapsedMs));
            this.hud.level.setText("УР. " + this.level);
            this.hud.hp.width = Math.max(0, 146 * this.stats.hp / this.stats.maxHp);
            this.hud.xp.width=Math.max(1,146*this.xp/this.xpNext);
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
            this.hud.dash.setVisible(false);
            this.hud.help.setVisible(false);
        }

        update(_, delta) {
            if (this.mode !== "playing" || !this.stats) return;
            const dt = Math.min(delta, 40);
            this.elapsedMs+=dt; this.hud.timer.setText(this.formatTime(this.elapsedMs)); this.waveClock+=dt; this.earthClock+=dt;
            this.updateWorldChunks(this.player.x,this.player.y);
            this.spawnClock += dt;
            this.fireClock += dt;
            this.dashCooldown = Math.max(0, this.dashCooldown - dt);
            this.invulnerable = Math.max(0, this.invulnerable - dt);
            this.player.setAlpha(this.invulnerable > 0 && Math.floor(this.time.now / 75) % 2 ? 0.48 : 1);

            if(this.bossBar&&this.bossBar.active&&this.bossBar.boss&&this.bossBar.boss.active)this.bossBar.fill.width=Math.max(1,276*this.bossBar.boss.hp/this.bossBar.boss.maxHp);
            if (this.target && this.target.active) this.showTarget(this.target);
            else if (this.targetMark) { this.target = null; this.targetMark.clear(); }

            const moving = Math.hypot(this.joy.dx, this.joy.dy) > 0.08;
            if (moving) {
                this.playerWalkTween.resume(); this.playerSprite.setAngle(Phaser.Math.Clamp(this.joy.dx*0.08,-0.08,0.08));
                this.lastMoveX = this.joy.dx;
                this.lastMoveY = this.joy.dy;
                this.player.body.setVelocity(this.joy.dx * this.stats.moveSpeed, this.joy.dy * this.stats.moveSpeed);
            } else {
                this.playerWalkTween.pause(); this.playerSprite.y=0; this.playerSprite.angle=0;
                this.player.body.setVelocity(this.player.body.velocity.x*0.82,this.player.body.velocity.y*0.82);
            }

            if (this.spawnClock > Math.max(390, 1500 - this.wave * 48)) {
                this.spawnClock = 0;
                const count = Math.min(4, 1 + Math.floor(this.wave / 4));
                for (let i = 0; i < count; i++) this.spawnEnemy();
            }
            if(this.elapsedMs>=180000 && Math.floor(this.elapsedMs/180000)>this.lastBossMinute){this.lastBossMinute=Math.floor(this.elapsedMs/180000);this.spawnBoss(this.lastBossMinute);}
            if(this.stats.earthUnlocked && this.earthClock>=this.stats.earthDelay){this.earthClock=0;this.castEarthWave();}
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
                if(!enemy.charging)enemy.body.setVelocity(Math.cos(angle)*enemy.speed,Math.sin(angle)*enemy.speed);
                if(enemy.sprite)enemy.sprite.setFlipX(this.player.x<enemy.x);
                if(enemy.isBoss){enemy.attackClock-=dt;if(enemy.attackClock<=0)this.bossAttack(enemy);}
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
                const v=this.cameras.main.worldView;
                if(bolt.life<=0||bolt.x<v.x-80||bolt.x>v.right+80||bolt.y<v.y-80||bolt.y>v.bottom+80)bolt.destroy();
            });
            if (this.dashButton && this.hud.dash) {
                const base = this.hud.dash.getAt(0);
                base.setFillStyle(this.dashCooldown <= 0 ? 0x493b2a : 0x29251f, 0.94);
            }
        }


        formatTime(ms){const n=Math.floor(ms/1000);return String(Math.floor(n/60)).padStart(2,"0")+":"+String(n%60).padStart(2,"0");}
        updateWorldChunks(x,y){
            const size=520,cx=Math.floor(x/size),cy=Math.floor(y/size);
            for(let gx=cx-2;gx<=cx+2;gx++)for(let gy=cy-2;gy<=cy+2;gy++){
                const key=gx+":"+gy;if(this.worldChunks.has(key))continue;
                const objs=[];let seed=Math.abs((gx*73856093)^(gy*19349663))>>>0;
                const rand=()=>{seed=(seed*1664525+1013904223)>>>0;return seed/4294967296;};
                for(let i=0,n=7+Math.floor(rand()*8);i<n;i++){
                    const px=gx*size+25+rand()*(size-50),py=gy*size+25+rand()*(size-50);
                    if(Phaser.Math.Distance.Between(px,py,x,y)<155)continue;
                    const roll=rand();
                    if(roll<0.13){
                        const obj=this.add.image(px,py,"dirt").setDepth(-5).setScale(0.65+rand()*0.8).setAlpha(0.92).setRotation(rand()*Math.PI);objs.push(obj);
                    }else if(roll<0.23){
                        const obj=this.add.image(px,py,"rubble").setDepth(3+py/100000).setScale(0.7+rand()*0.8).setRotation(rand()*0.8);objs.push(obj);
                    }else if(roll<0.29){
                        const obj=this.add.image(px,py,"grassTuft").setDepth(3+py/100000).setScale(0.8+rand()*0.7);objs.push(obj);
                    }else{
                        const obj=this.addObstacle(px,py,roll<0.39?"ruin":"tree",0.8+rand()*0.45);objs.push(obj);
                    }
                }
                if(rand()<0.48){
                    const px=gx*size+80+rand()*(size-160),py=gy*size+80+rand()*(size-160);
                    const obj=this.add.image(px,py,"pillar").setDepth(3+py/100000).setAngle(rand()*0.5-0.25).setScale(0.8+rand()*0.4);objs.push(obj);
                }
                this.worldChunks.set(key,objs);
            }
            for(const [key,objs] of this.worldChunks.entries()){const [gx,gy]=key.split(":").map(Number);if(Math.abs(gx-cx)>3||Math.abs(gy-cy)>3){objs.forEach(o=>o.destroy());this.worldChunks.delete(key);}}
            if(this.ground){this.ground.setPosition(x,y);this.ground.setTilePosition(x,y);}
        }
        damageSkillEnemy(enemy,damage,knockbackX,knockbackY) {
            if(!enemy||!enemy.active)return;
            enemy.hp-=damage;
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
            const count=this.stats.windOrbCount||1,orbit=this.stats.windRadius||76;
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
                    if(Phaser.Math.Distance.Between(this.player.x+x,this.player.y+y,enemy.x,enemy.y)<25){
                        if(!enemy.windHitAt||this.time.now-enemy.windHitAt>=this.stats.windDelay){
                            enemy.windHitAt=this.time.now;
                            this.damageSkillEnemy(enemy,this.stats.windDamage,Math.cos(a)*55,Math.sin(a)*55);
                        }
                    }
                });
            });
        }

        spawnBoss(stage){
            const types=[{name:"МАЛЫЙ СТРАЖ РАЗЛОМА",kind:"brute",hp:290,speed:43,damage:19,scale:1.55,tint:0x9c7052},
                {name:"КОСТЯНОЙ ПАЛАЧ",kind:"skeleton",hp:520,speed:68,damage:25,scale:2.0,tint:0xc8c0a9},
                {name:"ДРЕВНИЙ КОЛОСС",kind:"brute",hp:820,speed:48,damage:34,scale:2.55,tint:0x806a9b}];
            const b=types[(stage-1)%types.length],factor=1+(stage-1)*.42,v=this.cameras.main.worldView,side=Phaser.Math.Between(0,3);
            const x=side===0?v.x-50:side===1?v.right+50:Phaser.Math.Between(v.x+50,v.right-50);
            const y=side===2?v.y-50:side===3?v.bottom+50:Phaser.Math.Between(v.y+80,v.bottom-50);
            const boss=this.add.container(x,y).setDepth(24);boss.sprite=this.add.image(0,0,b.kind==="skeleton"?"skeleton":"brute").setScale(b.scale).setTint(b.tint);boss.add(boss.sprite);
            this.physics.add.existing(boss);boss.body.setCircle(25,-25,-25);boss.body.setAllowGravity(false);
            boss.kind=b.kind;boss.isBoss=true;boss.bossName=b.name;boss.bossStage=stage;boss.attackClock=4300;boss.hp=Math.round(b.hp*factor);boss.maxHp=boss.hp;boss.speed=b.speed+stage*2.4;boss.damage=Math.round(b.damage*factor);boss.xpValue=16+stage*3;boss.radius=25;
            boss.healthBack=this.add.rectangle(0,-43,64,6,0x241a18).setOrigin(.5).setVisible(false);boss.healthBar=this.add.rectangle(-32,-43,64,4,0xd94b3e).setOrigin(0,.5).setVisible(false);boss.add([boss.healthBack,boss.healthBar]);
            boss.animTween=this.tweens.add({targets:boss.sprite,y:4,duration:310,yoyo:true,repeat:-1});this.enemies.add(boss);
            this.bossLabel=this.add.text(W/2,78,b.name,this.style(18,0xffd38a,true)).setOrigin(.5).setScrollFactor(0).setDepth(70);
            this.bossBar=this.add.container(W/2,105).setScrollFactor(0).setDepth(70);const back=this.add.rectangle(0,0,280,10,0x2b1b18).setStrokeStyle(1,0xd0a45e),fill=this.add.rectangle(-138,0,276,6,0xb83d36).setOrigin(0,.5);
            this.bossBar.add([back,fill]);this.bossBar.fill=fill;this.bossBar.boss=boss;
            const notice=this.add.text(W/2,H/2-125,"БОСС: "+b.name,this.style(24,0xffc66e,true)).setOrigin(.5).setScrollFactor(0).setDepth(75);
            this.tweens.add({targets:notice,alpha:0,y:notice.y-45,duration:1800,onComplete:()=>notice.destroy()});this.cameras.main.shake(350,.006);if(sdk)sdk.vibrate(100);
        }

        bossAttack(boss){
            if(!boss.active)return;const stage=boss.bossStage%3;
            if(stage===1){
                boss.attackClock=5200;
                const ring=this.add.circle(boss.x,boss.y,24,0xd94b3e,.18).setStrokeStyle(3,0xff8b61,.95).setDepth(26);
                this.tweens.add({targets:ring,radius:155,alpha:0,duration:650,onComplete:()=>{
                    if(this.mode==="playing"&&Phaser.Math.Distance.Between(this.player.x,this.player.y,boss.x,boss.y)<155&&this.invulnerable<=0){
                        this.stats.hp-=Math.round(boss.damage*1.25);this.invulnerable=700;this.cameras.main.shake(180,.006);
                        if(this.stats.hp<=0)this.endRun();this.refreshHud();
                    }ring.destroy();
                }});
            }else if(stage===2){
                boss.attackClock=6100;const a=Phaser.Math.Angle.Between(boss.x,boss.y,this.player.x,this.player.y);
                boss.charging=true;boss.body.setVelocity(Math.cos(a)*330,Math.sin(a)*330);boss.sprite.setTint(0xffb05a);
                this.time.delayedCall(780,()=>{if(boss.active){boss.charging=false;boss.body.setVelocity(0,0);boss.sprite.setTint(0xc8c0a9);}});
            }else{
                boss.attackClock=6900;this.spawnEnemy("skeleton");this.spawnEnemy("slime");
                boss.hp=Math.min(boss.maxHp,boss.hp+Math.round(boss.maxHp*.07));
                if(this.bossBar&&this.bossBar.active)this.bossBar.fill.width=Math.max(1,276*boss.hp/boss.maxHp);
                const pulse=this.add.circle(boss.x,boss.y,20,0x9c70c9,.4).setStrokeStyle(3,0xd9b8ff).setDepth(25);
                this.tweens.add({targets:pulse,radius:100,alpha:0,duration:450,onComplete:()=>pulse.destroy()});
            }
        }

        pauseRun(){
            if(this.mode!=="playing")return;this.mode="paused";this.physics.pause();this.playerWalkTween.pause();
            this.joy.active=false;this.joy.dx=0;this.joy.dy=0;this.hud.joyKnob.setPosition(0,0);
            this.panel("ПАУЗА","ЗАБЕГ ПРИОСТАНОВЛЕН");
            this.addOverlay(this.add.text(W/2,H/2-53,"ВРЕМЯ  "+this.formatTime(this.elapsedMs)+"     УРОВЕНЬ  "+this.level+
                "\nСЛЕДУЮЩИЙ БОСС  "+this.formatTime(Math.max(0,(Math.floor(this.elapsedMs/180000)+1)*180000-this.elapsedMs)),this.style(15,C.parchment,true)).setOrigin(.5).setAlign("center").setLineSpacing(7));
            this.addOverlay(this.add.text(W/2,H/2+12,"ВЫБРАННЫЕ УСИЛЕНИЯ\n"+(this.selectedUpgrades.length?this.selectedUpgrades.join("\n"):"Пока нет выбранных усилений"),this.style(12,C.gold,true)).setOrigin(.5).setAlign("center").setLineSpacing(5));
            this.makeButton(W/2,H/2+105,"ПРОДОЛЖИТЬ",()=>{this.clearOverlay();this.mode="playing";this.physics.resume();},210);
            this.makeButton(W/2,H/2+160,"В ГЛАВНОЕ МЕНЮ",()=>{this.clearOverlay();this.hideHud();this.showMenu();},210);
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