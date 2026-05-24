export const TEXT_ZOOM_MIN = 50
export const TEXT_ZOOM_MAX = 200
export const TEXT_ZOOM_STEP = 5
export const TEXT_ZOOM_DEFAULT = 100

export function normalizeTextZoomLevel(level: number) {
  if (!Number.isFinite(level)) return TEXT_ZOOM_DEFAULT

  const clamped = Math.min(TEXT_ZOOM_MAX, Math.max(TEXT_ZOOM_MIN, level))
  const snapped =
    Math.round((clamped - TEXT_ZOOM_MIN) / TEXT_ZOOM_STEP) * TEXT_ZOOM_STEP + TEXT_ZOOM_MIN

  return Math.min(TEXT_ZOOM_MAX, Math.max(TEXT_ZOOM_MIN, snapped))
}
