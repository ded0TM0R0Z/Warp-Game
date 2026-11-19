# WARP GAME — DEVICE TARGETS AND GRAPHICS SCALING

**Status:** Main Control direction, 20 November 2026

## Product target

The primary target is modern Android phones in the **mid-range segment and above**. WARP GAME must not be designed around the weakest devices as its default performance ceiling.

The platform should use available CPU, GPU, memory, display refresh rate and storage bandwidth when the device supports them. Higher-end devices must be able to benefit from higher visual quality and more demanding games.

## Compatibility tiers

| Tier | Role | Expected behavior |
|---|---|---|
| Mid-range | Primary baseline | Full platform experience, normal 2D and 3D game profiles |
| Upper mid-range / flagship | Enhanced | Higher resolution assets, effects, draw distance and frame-rate targets when supported |
| Entry-level / older devices | Compatibility fallback | Reduced effects and scalable resolution; not the design target |

Exact minimum Android versions and hardware thresholds must be determined by device testing, not guessed from model names.

## Runtime and graphics policy

- Do not impose a global low-end graphics cap.
- Keep the platform runtime capable of using WebGL and hardware acceleration.
- Games may provide their own graphics presets through WarpGameSDK settings.
- The platform may expose global preferences such as automatic quality, balanced, high and maximum, but a game must remain in control of its own rendering pipeline.
- Automatic mode should consider available memory, WebGL capability, screen size and measured frame stability.
- Do not force a fixed 30 FPS cap. Use the display's supported refresh rate and a game's own frame pacing where possible.
- Battery saver or thermal throttling may reduce quality dynamically, but must not permanently downgrade saved user preferences.
- Asset quality and rendering resolution should be scalable independently.
- Avoid unnecessary background work while a game is running.

## Validation

Test at least one representative mid-range phone and one higher-performance phone before release. Add an entry-level device to compatibility testing after the primary experience is stable.

Measure:
- sustained frame rate and frame pacing;
- startup time and scene transitions;
- memory use and process stability;
- device temperature and thermal throttling;
- battery impact during a representative session;
- WebGL and touch behavior;
- install size and storage requirements.

A successful CI build does not count as device performance validation.

## Product rule

WARP GAME is a platform for mobile games, not a low-spec-only launcher. Compatibility fallbacks are required, but they must not define the quality ceiling or prevent developers from publishing more demanding 2D and 3D games.