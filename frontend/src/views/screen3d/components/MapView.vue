<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { isValidLatitude, isValidLongitude, parseGeoJSON } from '../utils/coordinates'

const props = defineProps({
  greenhouses: { type: Array, default: () => [] }
})

const mapRef = ref(null)
let map = null
let markers = []
let polygons = []

function initMap() {
  if (!mapRef.value) return
  map = L.map(mapRef.value, {
    center: [36.885, 118.737],
    zoom: 16,
    zoomControl: false,
  })
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '© OpenStreetMap'
  }).addTo(map)
  renderGreenhouses()
}

function renderGreenhouses() {
  // Clear existing
  markers.forEach(m => map.removeLayer(m))
  polygons.forEach(p => map.removeLayer(p))
  markers = []
  polygons = []

  const bounds = []
  for (const gh of props.greenhouses) {
    if (isValidLatitude(gh.latitude) && isValidLongitude(gh.longitude)) {
      const marker = L.marker([gh.latitude, gh.longitude])
        .bindPopup(`<b>${gh.name}</b><br/>编号: ${gh.greenhouseNo}<br/>状态: ${gh.status === 'ACTIVE' ? '使用中' : '空闲'}`)
        .addTo(map)
      markers.push(marker)
      bounds.push([gh.latitude, gh.longitude])
    }
    const geojson = parseGeoJSON(gh.boundaryGeojson)
    if (geojson) {
      const poly = L.geoJSON(geojson, {
        style: { color: gh.status === 'ACTIVE' ? '#4CAF50' : '#F57C00', weight: 2, fillOpacity: 0.2 }
      }).addTo(map)
      polygons.push(poly)
    }
  }
  if (bounds.length > 0) {
    map.fitBounds(bounds, { padding: [50, 50] })
  }
}

watch(() => props.greenhouses, () => { if (map) renderGreenhouses() }, { deep: true })

onMounted(() => { setTimeout(initMap, 100) })
onUnmounted(() => { if (map) { map.remove(); map = null } })
</script>

<template>
  <div ref="mapRef" class="map-container"></div>
</template>

<style scoped>
.map-container {
  width: 100%;
  height: 100%;
  border-radius: 6px;
  overflow: hidden;
}
</style>
