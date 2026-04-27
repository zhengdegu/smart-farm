export function isValidLatitude(lat) {
  return typeof lat === 'number' && !isNaN(lat) && isFinite(lat) && lat >= -90 && lat <= 90
}

export function isValidLongitude(lng) {
  return typeof lng === 'number' && !isNaN(lng) && isFinite(lng) && lng >= -180 && lng <= 180
}

export function isValidGeoJSON(json) {
  if (!json || typeof json !== 'object') return false
  return json.type === 'Polygon' && Array.isArray(json.coordinates) && json.coordinates.length > 0
}

export function parseGeoJSON(str) {
  if (!str || typeof str !== 'string') return null
  try {
    const obj = JSON.parse(str)
    return isValidGeoJSON(obj) ? obj : null
  } catch { return null }
}

export function tempToColor(temp, min = 18, max = 35) {
  const t = Math.max(0, Math.min(1, (temp - min) / (max - min)))
  // Blue (240°) → Green (120°) → Red (0°)
  const hue = (1 - t) * 240
  const r = t > 0.5 ? Math.round(255 * (t - 0.5) * 2) : 0
  const g = t < 0.5 ? Math.round(255 * t * 2) : Math.round(255 * (1 - t) * 2)
  const b = t < 0.5 ? Math.round(255 * (1 - t * 2)) : 0
  return { r: Math.min(255, Math.max(0, r)), g: Math.min(255, Math.max(0, g)), b: Math.min(255, Math.max(0, b)), hue }
}
