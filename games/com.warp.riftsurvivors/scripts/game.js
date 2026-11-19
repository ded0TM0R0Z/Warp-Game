(function () {
    "use strict";

    const W = 960;
    const H = 540;
    const sdk = window.WarpGameSDK || null;
    const STORAGE_KEY = "rift-survivors-save";
    const COLORS = {
        bg: 0x080b14, grid: 0x182139, cyan: 0x59e0d0,
        violet: 0x9b7cff, red: 0xff637d, gold: 0xffd36a,
        white: 0xf3f5ff, muted: 0x9ba8c7, green: 0x73e6a5
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
            this.xpOrbs = null;
            this.player = null;
            this.joystick = { active: false, pointerId: null, x: 0, y: 0, dx: 0, dy: 0 };
            this.stats = null;
            this.wave = 1;
            this.waveClock = 0;
            this.spawnClock = 0;
            this.fireClock = 0;
            this.dashCooldown = 0;
            this.invulnerable = 0;
            this.level = 1;
            this.xp = 0;
            this.xpNext = 8;
            this.kills = 0;
            this.score = 0;
            this.best = Number(readSave().best || 0);
            this.upgrades = { damage: 1, fireRate: 1, speed: 1, maxHp: 0, magnet: 0, projectile: 1 };
            this.hud = {};
            this.overlay = [];
            this.lastAim = { x: 1, y: 0 };
            this.dashButton = null;
        }

        create() {
            this.physics.world.setBounds(0, 0, W, H);
            this.add.rectangle(W / 2, H / 2, W, H, COLORS.bg);
            const grid = this.add.graphics();
            grid.lineStyle(1, COLORS.grid, 0.45);
            for (let x = 0; x <= W; x += 48) grid.lineBetween(x, 0, x, H);
            for (let y = 0; y <= H; y += 48) grid.lineBetween(0, y, W, y);
            this.add.circle(W / 2, H / 2, 180, 0x12172b, 0.26);
            this.enemies = this.physics.add.group({ runChildUpdate: true });
            this.bullets = this.physics.add.group();
            this.xpOrbs = this.physics.add.group();
            this.createHud();
            this.createPlayer();
            this.physics.add.overlap(this.bullets, this.enemies, this.hitEnemy, null, this);
            this.physics.add.overlap(this.player, this.enemies, this.hitPlayer, null, this);
            this.physics.add.overlap(this.player, this.xpOrbs, this.collectXp, null, this);
            this.input.on("pointerdown", this.pointerDown, this);
            this.input.on("pointermove", this.pointerMove, this);
            this.input.on("pointerup", this.pointerUp, this);
            this.input.on("pointerupoutside", this.pointerUp, this);
            this.showMenu();
            this.scale.on("resize", () => this.layoutHud());
        }

        createPlayer() {
            this.player = this.add.container(W / 2, H / 2);
            const glow = this.add.circle(0, 0, 26, COLORS.violet, 0.12);
            const body = this.add.circle(0, 0, 15, COLORS.violet);
            body.setStrokeStyle(3, 0xe3dcff, 0.95);
            const core = this.add.circle(0, 0, 5, COLORS.white);
            this.player.add([glow, body, core]);
            this.physics.add.existing(this.player);
            this.player.body.setCircle(15, -15, -15);
            this.player.body.setCollideWorldBounds(true);
            this.player.body.setDamping(true);
            this.player.body.setDrag(900);
            this.player.body.setMaxVelocity(620);
        }

        createHud() {
            this.hud.top = this.add.container(0, 0).setDepth(20);
            this.hud.wave = this.add.text(22, 18, "ВОЛНА 1", this.textStyle(17, COLORS.white, true));
            this.hud.level = this.add.text(22, 43, "УР. 1", this.textStyle(12, COLORS.cyan, true));
            this.hud.score = this.add.text(W - 22, 18, "0", this.textStyle(18, COLORS.gold, true)).setOrigin(1, 0);
            this.hud.hpBack = this.add.rectangle(W / 2, 25, 220, 12, 0x252b40).setOrigin(0.5);
            this.hud.hp = this.add.rectangle(W / 2 - 108, 25, 216, 6, COLORS.green).setOrigin(0, 0.5);
            this.hud.xpBack = this.add.rectangle(W / 2, 47, 220, 5, 0x252b40);
            this.hud.xp = this.add.rectangle(W / 2 - 108, 47, 1, 3, COLORS.cyan).setOrigin(0, 0.5);
            this.hud.top.add([this.hud.wave, this.hud.level, this.hud.score, this.hud.hpBack, this.hud.hp, this.hud.xpBack, this.hud.xp]);
            this.hud.dash = this.add.container(W - 76, H - 76).setDepth(30);
            const dashCircle = this.add.circle(0, 0, 34, 0x27304b, 0.9).setStrokeStyle(2, COLORS.cyan, 0.9);
            const dashText = this.add.text(0, 0, "РЫВОК", this.textStyle(11, COLORS.white, true)).setOrigin(0.5);
            this.dashButton = this.add.circle(0, 0, 38, 0xffffff, 0.001).setInteractive();
            this.dashButton.on("pointerdown", () => this.dash());
            this.hud.dash.add([dashCircle, dashText, this.dashButton]);
            this.hud.joy = this.add.container(92, H - 86).setDepth(25);
            this.hud.joyBase = this.add.circle(0, 0, 48, 0x27304b, 0.42).setStrokeStyle(2, 0x7784a8, 0.45);
            this.hud.joyKnob = this.add.circle(0, 0, 20, COLORS.cyan, 0.55).setStrokeStyle(2, 0xc9fff8, 0.8);
            this.hud.joy.add([this.hud.joyBase, this.hud.joyKnob]);
            this.hud.bottom = this.add.text(W / 2, H - 17, "АТАКА АВТОМАТИЧЕСКАЯ", this.textStyle(10, 0x71809f, true)).setOrigin(0.5, 1);
            this.layoutHud();
            this.hud.top.setVisible(false);
            this.hud.dash.setVisible(false);
            this.hud.joy.setVisible(false);
            this.hud.bottom.setVisible(false);
        }

        layoutHud() {
            if (!this.hud.top) return;
            this.hud.score.setPosition(W - 22, 18);
            this.hud.hpBack.setPosition(W / 2, 25);
            this.hud.hp.setPosition(W / 2 - 108, 25);
            this.hud.xpBack.setPosition(W / 2, 47);
            this.hud.xp.setPosition(W / 2 - 108, 47);
        }

        textStyle(size, color, bold) {
            return { fontFamily: "system-ui, sans-serif", fontSize: size + "px", color: "#" + color.toString(16).padStart(6, "0"), fontStyle: bold ? "bold" : "normal" };
        }

        clearOverlay() {
            this.overlay.forEach(item => item.destroy());
            this.overlay = [];
        }
        addOverlay(item) { item.setDepth(100); this.overlay.push(item); return item; }

        panel(title, subtitle) {
            this.clearOverlay();
            const shade = this.add.rectangle(W / 2, H / 2, W, H, 0x050711, 0.84);
            this.addOverlay(shade);
            const box = this.add.rectangle(W / 2, H / 2, 520, 330, 0x11182b, 0.98).setStrokeStyle(2, 0x394568, 0.9);
            this.addOverlay(box);
            this.addOverlay(this.add.text(W / 2, H / 2 - 120, title, this.textStyle(30, COLORS.white, true)).setOrigin(0.5));
            if (subtitle) this.addOverlay(this.add.text(W / 2, H / 2 - 78, subtitle, this.textStyle(14, COLORS.muted, false)).setOrigin(0.5));
        }

        makeButton(x, y, label, callback, width) {
            const w = width || 230;
            const bg = this.add.rectangle(x, y, w, 48, 0x6551b8).setStrokeStyle(1, 0xb5a4ff, 0.8).setInteractive({ useHandCursor: true });
            const text = this.add.text(x, y, label, this.textStyle(16, COLORS.white, true)).setOrigin(0.5);
            bg.on("pointerdown", callback);
            this.addOverlay(bg); this.addOverlay(text);
        }

        showMenu() {
            this.mode = "menu";
            this.physics.pause();
            this.panel("RIFT SURVIVORS", "АРЕНА НЕ ПРОЩАЕТ ОШИБОК");
            this.addOverlay(this.add.text(W / 2, H / 2 - 25, "Выдержи волны существ из разлома.\nСобирай энергию и усиливай оружие.", this.textStyle(15, COLORS.muted, false)).setOrigin(0.5).setAlign("center"));
            this.addOverlay(this.add.text(W / 2, H / 2 + 35, "РЕКОРД: " + this.best, this.textStyle(14, COLORS.gold, true)).setOrigin(0.5));
            this.makeButton(W / 2, H / 2 + 100, "НАЧАТЬ ЗАБЕГ", () => this.startRun());
            this.addOverlay(this.add.text(W / 2, H - 24, "Левый стик — движение   •   Рывок — уклонение", this.textStyle(11, 0x8390ae, false)).setOrigin(0.5));
        }

        startRun() {
            this.clearOverlay();
            this.mode = "playing";
            this.physics.resume();
            this.enemies.clear(true, true);
            this.bullets.clear(true, true);
            this.xpOrbs.clear(true, true);
            this.player.setPosition(W / 2, H / 2);
            this.player.body.setVelocity(0, 0);
            this.wave = 1; this.waveClock = 0; this.spawnClock = 0;
            this.fireClock = 0; this.dashCooldown = 0; this.invulnerable = 0;
            this.level = 1; this.xp = 0; this.xpNext = 8; this.kills = 0; this.score = 0;
            this.upgrades = { damage: 1, fireRate: 1, speed: 1, maxHp: 0, magnet: 0, projectile: 1 };
            this.stats = { hp: 100, maxHp: 100, damage: 18, fireDelay: 520, moveSpeed: 190, bulletSpeed: 430, magnet: 90 };
            this.joystick.dx = 0; this.joystick.dy = 0; this.joystick.active = false;
            this.hud.top.setVisible(true); this.hud.dash.setVisible(true);
            this.hud.joy.setVisible(true); this.hud.bottom.setVisible(true);
            this.refreshHud();
            this.spawnEnemy();
            this.spawnEnemy();
        }

        pointerDown(pointer) {
            if (this.mode !== "playing" || this.joystick.active) return;
            if (pointer.x < W * 0.43 && pointer.y > H * 0.48) {
                this.joystick.active = true;
                this.joystick.pointerId = pointer.id;
                this.joystick.x = 92; this.joystick.y = H - 86;
                this.updateJoystick(pointer);
            }
        }
        pointerMove(pointer) {
            if (this.mode === "playing" && this.joystick.active && pointer.id === this.joystick.pointerId) this.updateJoystick(pointer);
        }
        pointerUp(pointer) {
            if (this.joystick.active && pointer.id === this.joystick.pointerId) {
                this.joystick.active = false; this.joystick.pointerId = null;
                this.joystick.dx = 0; this.joystick.dy = 0;
                this.hud.joyKnob.setPosition(0, 0);
            }
        }
        updateJoystick(pointer) {
            const dx = pointer.x - this.joystick.x;
            const dy = pointer.y - this.joystick.y;
            const length = Math.max(1, Math.hypot(dx, dy));
            const scale = Math.min(1, 42 / length);
            this.joystick.dx = dx / length * scale;
            this.joystick.dy = dy / length * scale;
            this.hud.joyKnob.setPosition(this.joystick.dx * 34, this.joystick.dy * 34);
        }

        dash() {
            if (this.mode !== "playing" || this.dashCooldown > 0) return;
            let dx = this.joystick.dx, dy = this.joystick.dy;
            if (Math.hypot(dx, dy) < 0.15) {
                const target = this.nearestEnemy();
                if (target) {
                    dx = this.player.x - target.x; dy = this.player.y - target.y;
                    const len = Math.max(1, Math.hypot(dx, dy)); dx /= len; dy /= len;
                } else { dx = 1; dy = 0; }
            }
            this.player.body.setVelocity(dx * 590, dy * 590);
            this.invulnerable = 420;
            this.dashCooldown = 3200;
            this.cameras.main.shake(100, 0.003);
            if (sdk) sdk.vibrate(35);
        }

        spawnEnemy() {
            if (this.mode !== "playing" || this.enemies.countActive(true) >= 75) return;
            const side = Phaser.Math.Between(0, 3);
            const x = side === 0 ? -20 : side === 1 ? W + 20 : Phaser.Math.Between(20, W - 20);
            const y = side === 2 ? -20 : side === 3 ? H + 20 : Phaser.Math.Between(70, H - 30);
            const heavy = this.wave >= 3 && Math.random() < Math.min(0.32, 0.08 + this.wave * 0.012);
            const fast = !heavy && this.wave >= 2 && Math.random() < 0.24;
            const radius = heavy ? 22 : fast ? 11 : 15;
            const color = heavy ? COLORS.gold : fast ? COLORS.red : 0x4d8cff;
            const enemy = this.add.container(x, y);
            const aura = this.add.circle(0, 0, radius + 8, color, 0.13);
            const shape = this.add.circle(0, 0, radius, color).setStrokeStyle(2, 0xffffff, 0.45);
            const eye = this.add.circle(radius * 0.28, -radius * 0.12, Math.max(2, radius * 0.18), 0xffffff);
            enemy.add([aura, shape, eye]);
            this.physics.add.existing(enemy);
            enemy.body.setCircle(radius, -radius, -radius);
            enemy.body.setCollideWorldBounds(false);
            enemy.hp = heavy ? 95 + this.wave * 8 : fast ? 20 + this.wave * 2 : 36 + this.wave * 4;
            enemy.maxHp = enemy.hp;
            enemy.speed = heavy ? 48 + this.wave * 1.5 : fast ? 105 + this.wave * 2 : 68 + this.wave * 2;
            enemy.damage = heavy ? 22 : 12;
            enemy.xpValue = heavy ? 4 : fast ? 2 : 1;
            enemy.radius = radius;
            enemy.lastHit = 0;
            this.enemies.add(enemy);
        }

        nearestEnemy() {
            let best = null, bestDistance = Infinity;
            this.enemies.children.iterate(enemy => {
                if (!enemy || !enemy.active) return;
                const d = Phaser.Math.Distance.Between(this.player.x, this.player.y, enemy.x, enemy.y);
                if (d < bestDistance) { bestDistance = d; best = enemy; }
            });
            return best;
        }

        fire() {
            const target = this.nearestEnemy();
            if (!target) return;
            const angle = Phaser.Math.Angle.Between(this.player.x, this.player.y, target.x, target.y);
            this.lastAim.x = Math.cos(angle); this.lastAim.y = Math.sin(angle);
            const count = this.upgrades.projectile;
            for (let i = 0; i < count; i++) {
                const spread = (i - (count - 1) / 2) * 0.13;
                const a = angle + spread;
                const bullet = this.add.circle(this.player.x + Math.cos(a) * 20, this.player.y + Math.sin(a) * 20, 5, COLORS.cyan);
                bullet.setStrokeStyle(1, 0xffffff, 0.8);
                this.physics.add.existing(bullet);
                bullet.body.setCircle(5);
                bullet.body.setVelocity(Math.cos(a) * this.stats.bulletSpeed, Math.sin(a) * this.stats.bulletSpeed);
                bullet.body.setAllowGravity(false);
                bullet.damage = this.stats.damage;
                bullet.life = 1050;
                this.bullets.add(bullet);
            }
        }

        hitEnemy(bullet, enemy) {
            if (!bullet.active || !enemy.active) return;
            bullet.destroy();
            enemy.hp -= bullet.damage;
            enemy.setAlpha(0.62);
            this.time.delayedCall(65, () => { if (enemy.active) enemy.setAlpha(1); });
            if (enemy.hp <= 0) this.killEnemy(enemy);
        }

        killEnemy(enemy) {
            if (!enemy.active) return;
            const x = enemy.x, y = enemy.y, value = enemy.xpValue;
            enemy.destroy();
            this.kills++;
            this.score += 10 * this.wave;
            this.hud.score.setText(this.score.toLocaleString("ru-RU"));
            const orb = this.add.circle(x, y, 6 + value, COLORS.green).setStrokeStyle(2, 0xe5fff0, 0.7);
            this.physics.add.existing(orb);
            orb.body.setCircle(7 + value);
            orb.value = value;
            this.xpOrbs.add(orb);
            this.tweens.add({ targets: orb, scale: 1.35, duration: 400, yoyo: true, repeat: -1 });
            this.cameras.main.shake(45, 0.0015);
        }

        hitPlayer(player, enemy) {
            if (this.mode !== "playing" || this.invulnerable > 0 || !enemy.active) return;
            this.stats.hp -= enemy.damage;
            this.invulnerable = 650;
            this.cameras.main.flash(100, 255, 70, 100, false);
            const angle = Phaser.Math.Angle.Between(enemy.x, enemy.y, player.x, player.y);
            player.body.setVelocity(Math.cos(angle) * 240, Math.sin(angle) * 240);
            if (this.stats.hp <= 0) this.endRun();
            this.refreshHud();
        }

        collectXp(player, orb) {
            if (!orb.active) return;
            this.xp += orb.value;
            orb.destroy();
            while (this.xp >= this.xpNext) {
                this.xp -= this.xpNext;
                this.level++;
                this.xpNext = Math.floor(this.xpNext * 1.32 + 3);
                this.openUpgrade();
                break;
            }
            this.refreshHud();
        }

        upgradeOptions() {
            const all = [
                { id: "damage", title: "СИЛЬНЫЙ ВЫСТРЕЛ", desc: "+35% урон снарядов", apply: () => { this.stats.damage *= 1.35; } },
                { id: "fireRate", title: "БЫСТРЫЙ СПУСК", desc: "Атака на 18% быстрее", apply: () => { this.stats.fireDelay *= 0.82; } },
                { id: "speed", title: "ЛЁГКИЕ НОГИ", desc: "+12% скорость движения", apply: () => { this.stats.moveSpeed *= 1.12; } },
                { id: "maxHp", title: "ЖИВОЙ ЩИТ", desc: "+25 к максимуму здоровья", apply: () => { this.stats.maxHp += 25; this.stats.hp = Math.min(this.stats.maxHp, this.stats.hp + 35); } },
                { id: "magnet", title: "ЭНЕРГОМАГНИТ", desc: "Дальше притягивает опыт", apply: () => { this.stats.magnet += 55; } },
                { id: "projectile", title: "ДВОЙНОЙ ИМПУЛЬС", desc: "+1 снаряд за выстрел", apply: () => { this.upgrades.projectile = Math.min(5, this.upgrades.projectile + 1); } }
            ];
            return Phaser.Utils.Array.Shuffle(all).slice(0, 3);
        }

        openUpgrade() {
            if (this.mode !== "playing") return;
            this.mode = "upgrade";
            this.physics.pause();
            this.panel("НОВЫЙ УРОВЕНЬ", "ВЫБЕРИ ОДНО УЛУЧШЕНИЕ");
            this.addOverlay(this.add.text(W / 2, H / 2 - 35, "УРОВЕНЬ " + this.level, this.textStyle(13, COLORS.cyan, true)).setOrigin(0.5));
            const options = this.upgradeOptions();
            options.forEach((option, index) => {
                const y = H / 2 + 20 + index * 67;
                const bg = this.add.rectangle(W / 2, y, 390, 56, 0x202b46).setStrokeStyle(1, 0x56658d, 0.8).setInteractive();
                const title = this.add.text(W / 2 - 165, y - 9, option.title, this.textStyle(13, COLORS.white, true));
                const desc = this.add.text(W / 2 - 165, y + 12, option.desc, this.textStyle(11, COLORS.muted, false));
                bg.on("pointerdown", () => {
                    option.apply();
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
            this.hud.level.setText("УР. " + this.level);
            this.hud.hp.width = Math.max(0, 216 * this.stats.hp / this.stats.maxHp);
            this.hud.hp.fillColor = this.stats.hp / this.stats.maxHp < 0.3 ? COLORS.red : COLORS.green;
            this.hud.xp.width = Math.max(1, 216 * this.xp / this.xpNext);
        }

        endRun() {
            this.mode = "gameover";
            this.physics.pause();
            this.joystick.active = false;
            this.best = Math.max(this.best, this.score);
            writeSave({ best: this.best, lastScore: this.score, bestWave: Math.max(Number(readSave().bestWave || 0), this.wave) });
            this.hud.top.setVisible(false); this.hud.dash.setVisible(false);
            this.hud.joy.setVisible(false); this.hud.bottom.setVisible(false);
            this.panel("ЗАБЕГ ОКОНЧЕН", "ТВОЯ ЭНЕРГИЯ ИСЧЕРПАНА");
            this.addOverlay(this.add.text(W / 2, H / 2 - 25, "СЧЁТ  " + this.score.toLocaleString("ru-RU") + "\nВОЛНА  " + this.wave + "     УРОВЕНЬ  " + this.level, this.textStyle(18, COLORS.white, true)).setOrigin(0.5).setAlign("center"));
            this.addOverlay(this.add.text(W / 2, H / 2 + 45, "ЛУЧШИЙ РЕЗУЛЬТАТ  " + this.best, this.textStyle(13, COLORS.gold, true)).setOrigin(0.5));
            this.makeButton(W / 2, H / 2 + 105, "ЕЩЁ ОДИН ЗАБЕГ", () => this.startRun());
            if (sdk) sdk.vibrate(120);
        }

        update(_, delta) {
            if (this.mode !== "playing" || !this.stats) return;
            const dt = Math.min(delta, 40);
            this.waveClock += dt; this.spawnClock += dt; this.fireClock += dt;
            this.dashCooldown = Math.max(0, this.dashCooldown - dt);
            this.invulnerable = Math.max(0, this.invulnerable - dt);
            this.player.setAlpha(this.invulnerable > 0 && Math.floor(this.time.now / 70) % 2 ? 0.45 : 1);

            const dx = this.joystick.dx, dy = this.joystick.dy;
            this.player.body.setAcceleration(dx * this.stats.moveSpeed * 7, dy * this.stats.moveSpeed * 7);
            if (Math.hypot(dx, dy) < 0.05) this.player.body.setAcceleration(0, 0);

            if (this.spawnClock > Math.max(330, 1150 - this.wave * 45)) {
                this.spawnClock = 0;
                const count = Math.min(3, 1 + Math.floor(this.wave / 5));
                for (let i = 0; i < count; i++) this.spawnEnemy();
            }
            if (this.fireClock >= this.stats.fireDelay) {
                this.fireClock = 0;
                this.fire();
            }
            if (this.waveClock >= 30000) {
                this.waveClock = 0; this.wave++;
                this.refreshHud();
                this.addWaveNotice();
            }

            this.enemies.children.iterate(enemy => {
                if (!enemy || !enemy.active) return;
                const angle = Phaser.Math.Angle.Between(enemy.x, enemy.y, this.player.x, this.player.y);
                enemy.body.setVelocity(Math.cos(angle) * enemy.speed, Math.sin(angle) * enemy.speed);
            });
            this.xpOrbs.children.iterate(orb => {
                if (!orb || !orb.active) return;
                const d = Phaser.Math.Distance.Between(orb.x, orb.y, this.player.x, this.player.y);
                if (d < this.stats.magnet) {
                    const angle = Phaser.Math.Angle.Between(orb.x, orb.y, this.player.x, this.player.y);
                    orb.body.setVelocity(Math.cos(angle) * 230, Math.sin(angle) * 230);
                } else orb.body.setVelocity(0, 0);
            });
            this.bullets.children.iterate(bullet => {
                if (!bullet || !bullet.active) return;
                bullet.life -= dt;
                if (bullet.life <= 0 || bullet.x < -20 || bullet.x > W + 20 || bullet.y < -20 || bullet.y > H + 20) bullet.destroy();
            });
            if (this.dashCooldown <= 0) this.hud.dash.getAt(0).setFillStyle(0x27304b, 0.95);
            else this.hud.dash.first.setFillStyle(0x20253a, 0.65);
        }

        addWaveNotice() {
            const label = this.add.text(W / 2, H / 2 - 110, "ВОЛНА " + this.wave, this.textStyle(26, COLORS.gold, true)).setOrigin(0.5).setDepth(50);
            this.tweens.add({ targets: label, alpha: 0, y: label.y - 35, duration: 1200, onComplete: () => label.destroy() });
            if (sdk) sdk.vibrate(22);
        }
    }

    const game = new Phaser.Game({
        type: Phaser.AUTO,
        parent: "game",
        width: W,
        height: H,
        backgroundColor: "#080b14",
        scale: { mode: Phaser.Scale.FIT, autoCenter: Phaser.Scale.CENTER_BOTH },
        physics: { default: "arcade", arcade: { debug: false } },
        input: { activePointers: 3 },
        render: { antialias: true, pixelArt: false, powerPreference: "low-power" },
        fps: { target: 60, forceSetTimeOut: false },
        scene: [Arena]
    });

    window.addEventListener("error", event => console.error("Rift Survivors:", event.message));
    window.addEventListener("unhandledrejection", event => console.error("Rift Survivors promise:", event.reason));
    window.addEventListener("contextmenu", event => event.preventDefault());
})();
