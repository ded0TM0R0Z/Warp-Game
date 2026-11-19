(() => {
  "use strict";

  const sdk = window.WarpGameSDK || null;
  const savedScore = sdk ? Number(sdk.loadData("best-score") || 0) : 0;

  class CrystalRaid extends Phaser.Scene {
    constructor() {
      super("CrystalRaid");
      this.score = 0;
      this.best = Number.isFinite(savedScore) ? savedScore : 0;
      this.player = null;
      this.crystal = null;
      this.scoreText = null;
      this.bestText = null;
      this.target = null;
    }

    create() {
      const { width, height } = this.scale;
      this.physics.world.setBounds(0, 0, width, height);

      this.add.rectangle(width / 2, height / 2, width, height, 0x0b0e18);
      const grid = this.add.graphics();
      grid.lineStyle(1, 0x222944, 0.55);
      for (let x = 0; x < width; x += 48) {
        grid.lineBetween(x, 0, x, height);
      }
      for (let y = 0; y < height; y += 48) {
        grid.lineBetween(0, y, width, y);
      }

      this.add.text(24, 18, "КРИСТАЛЬНЫЙ РЕЙД", {
        fontFamily: "system-ui, sans-serif", fontSize: "19px",
        fontStyle: "bold", color: "#f5f6fc"
      });
      this.add.text(24, 44, "Коснись экрана, чтобы двигаться и собирать кристаллы", {
        fontFamily: "system-ui, sans-serif", fontSize: "12px", color: "#9ba7c5"
      });

      this.scoreText = this.add.text(width - 24, 18, "СЧЁТ 0", {
        fontFamily: "system-ui, sans-serif", fontSize: "17px",
        fontStyle: "bold", color: "#69dca4"
      }).setOrigin(1, 0);
      this.bestText = this.add.text(width - 24, 43, "РЕКОРД " + this.best, {
        fontFamily: "system-ui, sans-serif", fontSize: "11px", color: "#a9b2c9"
      }).setOrigin(1, 0);

      this.player = this.add.circle(width * 0.5, height * 0.58, 17, 0x7e67ff);
      this.physics.add.existing(this.player);
      this.player.body.setCircle(17);
      this.player.body.setCollideWorldBounds(true);

      this.spawnCrystal();
      this.physics.add.overlap(this.player, this.crystal, this.collectCrystal, null, this);

      this.input.on("pointerdown", pointer => {
        this.movePlayer(pointer);
      });
      this.input.on("pointermove", pointer => {
        if (pointer.isDown) this.movePlayer(pointer);
      });

      this.add.text(width / 2, height - 22, "Управление касанием", {
        fontFamily: "system-ui, sans-serif", fontSize: "11px", color: "#727e9b"
      }).setOrigin(0.5, 1);

      this.scale.on("resize", size => {
        this.physics.world.setBounds(0, 0, size.width, size.height);
      });
    }

    spawnCrystal() {
      const width = this.scale.width;
      const height = this.scale.height;
      const x = Phaser.Math.Between(60, Math.max(61, width - 60));
      const y = Phaser.Math.Between(100, Math.max(101, height - 70));
      this.crystal = this.add.circle(x, y, 12, 0x69dca4);
      this.physics.add.existing(this.crystal, true);
      this.tweens.add({
        targets: this.crystal, scale: 1.35, duration: 650,
        yoyo: true, repeat: -1, ease: "Sine.inOut"
      });
      if (this.player) {
        this.physics.add.overlap(this.player, this.crystal, this.collectCrystal, null, this);
      }
    }

    movePlayer(pointer) {
      if (!this.player || !this.player.body) return;
      this.target = { x: pointer.worldX, y: pointer.worldY };
      this.physics.moveTo(this.player, this.target.x, this.target.y, 260);
    }

    collectCrystal() {
      if (!this.crystal || !this.crystal.active) return;
      this.crystal.destroy();
      this.score += 1;
      this.best = Math.max(this.best, this.score);
      this.scoreText.setText("СЧЁТ " + this.score);
      this.bestText.setText("РЕКОРД " + this.best);
      if (sdk) {
        sdk.saveData("best-score", this.best);
        sdk.saveData("last-score", this.score);
        sdk.vibrate(18);
      }
      this.cameras.main.flash(100, 105, 220, 164, false);
      this.spawnCrystal();
    }

    update() {
      if (!this.player || !this.player.body || !this.target) return;
      const distance = Phaser.Math.Distance.Between(
        this.player.x, this.player.y, this.target.x, this.target.y
      );
      if (distance < 12) this.player.body.setVelocity(0, 0);
    }
  }

  const config = {
    type: Phaser.AUTO,
    parent: "game",
    width: 960,
    height: 540,
    backgroundColor: "#0b0e18",
    scale: {
      mode: Phaser.Scale.FIT,
      autoCenter: Phaser.Scale.CENTER_BOTH
    },
    physics: {
      default: "arcade",
      arcade: { debug: false }
    },
    input: { activePointers: 2 },
    render: { antialias: true, pixelArt: false, powerPreference: "low-power" },
    scene: [CrystalRaid]
  };

  window.addEventListener("error", event => {
    console.error("Phaser demo error:", event.message);
  });

  new Phaser.Game(config);
})();