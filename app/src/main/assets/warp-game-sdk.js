(() => {
    "use strict";

    const nativeBridge = window.WarpGameNative;
    if (!nativeBridge) {
        console.error("WarpGameSDK native bridge is unavailable");
        return;
    }

    const parseJson = (value, fallback) => {
        try {
            return JSON.parse(value);
        } catch (_) {
            return fallback;
        }
    };

    const sdk = {
        version: "1.0.0",

        getGameInfo() {
            return parseJson(nativeBridge.getGameInfo(), {});
        },

        saveData(key, value) {
            const serialized = JSON.stringify(value);
            if (typeof serialized !== "string") return false;
            return nativeBridge.saveData(String(key), serialized);
        },

        loadData(key) {
            const value = nativeBridge.loadData(String(key));
            if (value === null || value === undefined) return null;
            return parseJson(value, value);
        },

        getSettings() {
            return parseJson(nativeBridge.getSettings(), {});
        },

        setSettings(settings) {
            return nativeBridge.setSettings(JSON.stringify(settings ?? {}));
        },

        vibrate(durationMs = 30) {
            return nativeBridge.vibrate(Number(durationMs));
        },

        setFullscreen(enabled) {
            nativeBridge.setFullscreen(Boolean(enabled));
        },

        exitGame() {
            nativeBridge.exitGame();
        }
    };

    Object.defineProperty(window, "WarpGameSDK", {
        value: Object.freeze(sdk),
        writable: false,
        configurable: false
    });
})();
