(() => {
    "use strict";

    const canvas = document.getElementById("game");
    const context = canvas.getContext("2d");
    const scriptStatus = document.getElementById("script-status");
    const webglStatus = document.getElementById("webgl-status");
    const sdkStatus = document.getElementById("sdk-status");
    const tapCount = document.getElementById("tap-count");
    const sdk = window.WarpGameSDK || null;

    if (!context) {
        scriptStatus.textContent = "Canvas недоступен";
        return;
    }

    scriptStatus.textContent = "Работает";

    const probeCanvas = document.createElement("canvas");
    const webgl = probeCanvas.getContext("webgl2") || probeCanvas.getContext("webgl");
    webglStatus.textContent = webgl
        ? (probeCanvas.getContext("webgl2") ? "Доступен (2)" : "Доступен (1)")
        : "Недоступен";

    sdkStatus.textContent = sdk && sdk.getGameInfo().gameId
        ? "Работает"
        : "Не подключён";
    const savedTaps = sdk ? Number(sdk.loadData("tap-count") || 0) : 0;

    const state = {
        x: 0.5,
        y: 0.5,
        taps: Number.isFinite(savedTaps) ? savedTaps : 0,
        pulse: 0
    };

    tapCount.textContent = "Касаний: " + state.taps;

    function resize() {
        const rect = canvas.getBoundingClientRect();
        const ratio = Math.min(window.devicePixelRatio || 1, 2);
        canvas.width = Math.max(1, Math.floor(rect.width * ratio));
        canvas.height = Math.max(1, Math.floor(rect.height * ratio));
        context.setTransform(ratio, 0, 0, ratio, 0, 0);
        draw();
    }

    function draw() {
        const width = canvas.clientWidth;
        const height = canvas.clientHeight;
        context.clearRect(0, 0, width, height);

        context.strokeStyle = "rgba(160, 150, 255, 0.10)";
        context.lineWidth = 1;
        for (let x = 0; x < width; x += 32) {
            context.beginPath();
            context.moveTo(x, 0);
            context.lineTo(x, height);
            context.stroke();
        }
        for (let y = 0; y < height; y += 32) {
            context.beginPath();
            context.moveTo(0, y);
            context.lineTo(width, y);
            context.stroke();
        }

        const targetX = width * 0.5;
        const targetY = height * 0.5;
        context.beginPath();
        context.arc(targetX, targetY, 31, 0, Math.PI * 2);
        context.strokeStyle = "rgba(154, 140, 255, 0.5)";
        context.stroke();

        const x = width * state.x;
        const y = height * state.y;
        const glow = context.createRadialGradient(x, y, 2, x, y, 28);
        glow.addColorStop(0, "rgba(105, 220, 164, 0.95)");
        glow.addColorStop(1, "rgba(105, 220, 164, 0)");
        context.fillStyle = glow;
        context.beginPath();
        context.arc(x, y, 28, 0, Math.PI * 2);
        context.fill();

        context.fillStyle = "#69dca4";
        context.beginPath();
        context.arc(x, y, 9, 0, Math.PI * 2);
        context.fill();

        context.fillStyle = "#f5f6fc";
        context.font = "12px system-ui, sans-serif";
        context.textAlign = "center";
        context.fillText("Нажми в любом месте поля", width / 2, height - 18);
    }

    canvas.addEventListener("pointerdown", (event) => {
        const rect = canvas.getBoundingClientRect();
        state.x = Math.max(0, Math.min(1, (event.clientX - rect.left) / rect.width));
        state.y = Math.max(0, Math.min(1, (event.clientY - rect.top) / rect.height));
        state.taps += 1;
        tapCount.textContent = "Касаний: " + state.taps;
        if (sdk) {
            sdk.saveData("tap-count", state.taps);
            sdk.vibrate(20);
        }
        draw();
    });

    window.addEventListener("resize", resize, { passive: true });
    resize();
})();
