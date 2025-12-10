# Render Helper Extractions (AnyKeyboardViewBase)

- ProximityCalculator: computes proximity thresholds.
- SwipeConfiguration: holds swipe velocity/distance thresholds.
- HintLayoutCalculator: hint icon/text placement.
- KeyPreviewManagerFacade: preview show/dismiss delegation.
- KeyIconResolver: icon builders/cache + lookup.
- DirtyRegionDecider: decide single-key vs full redraw.
- KeyLabelAdjuster: shift/function label adjustments.
- LabelPaintConfigurator: key/label paint sizing and typeface setup.
- ActionIconStateSetter: applies IME action states to enter icons.
- SpecialKeyLabelProvider: fallback labels for special keys.
- KeyboardNameRenderer: substitutes keyboard name on space and prepares paint/metrics.
- KeyHintRenderer: draws hint text/icons with placement from HintLayoutCalculator.
- KeyLabelRenderer: draws main key labels (text sizing, shadow, StaticLayout handling).
- KeyIconDrawer: centers/draws icons or falls back to guessed labels when missing.
- KeyTextColorResolver: resolves per-key text color including modifier-active states.

Status: all integrated; behavior unchanged. Use this list when peeling more render logic.
