<script setup>
import { ref, onMounted, onUnmounted, defineProps, defineExpose } from 'vue'

const props = defineProps({
  envData: { type: Array, default: () => [] }
})

const canvasRef = ref(null)
const wrapperRef = ref(null)

let THREE = null
let scene = null
let camera = null
let renderer = null
let animId = null
let angle = 0.3
let particles = []
let heatMat = null
let allGeometries = []
let allMaterials = []

async function initScene() {
  THREE = await import('three')
  const canvas = canvasRef.value
  const wrapper = wrapperRef.value
  if (!canvas || !wrapper) return

  const w = wrapper.clientWidth
  const h = wrapper.clientHeight
  canvas.width = w
  canvas.height = h

  // Scene
  scene = new THREE.Scene()
  scene.background = new THREE.Color(0x0a1628)
  scene.fog = new THREE.FogExp2(0x0a1628, 0.012)

  // Camera
  camera = new THREE.PerspectiveCamera(40, w / h, 0.1, 300)
  camera.position.set(35, 28, 45)
  camera.lookAt(0, 0, 0)

  // Renderer
  renderer = new THREE.WebGLRenderer({ canvas, antialias: true, alpha: true })
  renderer.setSize(w, h)
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
  renderer.shadowMap.enabled = true

  // === Lights ===
  const ambient = new THREE.AmbientLight(0x304060, 3)
  scene.add(ambient)

  const dir = new THREE.DirectionalLight(0xffffff, 2)
  dir.position.set(30, 40, 20)
  dir.castShadow = true
  dir.shadow.mapSize.width = 1024
  dir.shadow.mapSize.height = 1024
  scene.add(dir)

  const pointGreen = new THREE.PointLight(0x4caf50, 2, 60)
  pointGreen.position.set(0, 15, 0)
  scene.add(pointGreen)

  // === Ground + Grid ===
  const groundGeo = new THREE.PlaneGeometry(100, 80)
  const groundMat = new THREE.MeshStandardMaterial({ color: 0x0d1f0d, roughness: 0.95 })
  allGeometries.push(groundGeo)
  allMaterials.push(groundMat)
  const ground = new THREE.Mesh(groundGeo, groundMat)
  ground.rotation.x = -Math.PI / 2
  ground.receiveShadow = true
  scene.add(ground)

  const grid = new THREE.GridHelper(100, 40, 0x1a3a2a, 0x0f2a1a)
  grid.position.y = 0.01
  scene.add(grid)

  // === Greenhouse Structure ===
  const ghGroup = new THREE.Group()

  // Steel frame material
  const frameMat = new THREE.MeshStandardMaterial({ color: 0x607d8b, metalness: 0.8, roughness: 0.3 })
  allMaterials.push(frameMat)

  // Vertical pillars (10 pillars)
  const pillarGeo = new THREE.CylinderGeometry(0.12, 0.12, 8, 8)
  allGeometries.push(pillarGeo)
  const pillarPositions = [
    [-15, -12], [-15, 12], [-8, -12], [-8, 12], [0, -12],
    [0, 12], [8, -12], [8, 12], [15, -12], [15, 12]
  ]
  pillarPositions.forEach(([x, z]) => {
    const p = new THREE.Mesh(pillarGeo, frameMat)
    p.position.set(x, 4, z)
    p.castShadow = true
    ghGroup.add(p)
  })

  // Arched roof beams (7 beams)
  const roofCurve = new THREE.CatmullRomCurve3([
    new THREE.Vector3(-12, 8, 0),
    new THREE.Vector3(-6, 10.5, 0),
    new THREE.Vector3(0, 11, 0),
    new THREE.Vector3(6, 10.5, 0),
    new THREE.Vector3(12, 8, 0)
  ])
  const roofTubeGeo = new THREE.TubeGeometry(roofCurve, 20, 0.1, 6, false)
  allGeometries.push(roofTubeGeo)
  for (let z = -12; z <= 12; z += 4) {
    const beam = new THREE.Mesh(roofTubeGeo, frameMat)
    beam.position.z = z
    ghGroup.add(beam)
  }

  // Glass panels
  const glassMat = new THREE.MeshPhysicalMaterial({
    color: 0x88ccff, transparent: true, opacity: 0.15,
    roughness: 0.1, metalness: 0, transmission: 0.6,
    side: THREE.DoubleSide
  })
  allMaterials.push(glassMat)

  // Front/back walls
  const wallGeo = new THREE.PlaneGeometry(30, 8)
  allGeometries.push(wallGeo)
  const wallFront = new THREE.Mesh(wallGeo, glassMat)
  wallFront.position.set(0, 4, 12)
  ghGroup.add(wallFront)
  const wallBack = new THREE.Mesh(wallGeo, glassMat)
  wallBack.position.set(0, 4, -12)
  ghGroup.add(wallBack)

  // Side walls
  const sideGeo = new THREE.PlaneGeometry(24, 8)
  allGeometries.push(sideGeo)
  const wallLeft = new THREE.Mesh(sideGeo, glassMat)
  wallLeft.rotation.y = Math.PI / 2
  wallLeft.position.set(-15, 4, 0)
  ghGroup.add(wallLeft)
  const wallRight = new THREE.Mesh(sideGeo, glassMat)
  wallRight.rotation.y = Math.PI / 2
  wallRight.position.set(15, 4, 0)
  ghGroup.add(wallRight)

  // Roof glass
  const roofGlassGeo = new THREE.PlaneGeometry(30, 24)
  allGeometries.push(roofGlassGeo)
  const roofGlass = new THREE.Mesh(roofGlassGeo, glassMat)
  roofGlass.rotation.x = -Math.PI / 2.2
  roofGlass.position.set(0, 10, 0)
  ghGroup.add(roofGlass)

  scene.add(ghGroup)

  // === Plant rows ===
  for (let row = -10; row <= 10; row += 3) {
    for (let col = -10; col <= 10; col += 2.5) {
      const ph = 1.5 + Math.random() * 2
      const plantGeo = new THREE.BoxGeometry(1.2, ph, 1.2)
      const plantMat = new THREE.MeshStandardMaterial({
        color: new THREE.Color().setHSL(0.3 + Math.random() * 0.1, 0.7, 0.25 + Math.random() * 0.15)
      })
      allGeometries.push(plantGeo)
      allMaterials.push(plantMat)
      const plant = new THREE.Mesh(plantGeo, plantMat)
      plant.position.set(col, ph / 2, row)
      plant.castShadow = true
      scene.add(plant)
    }
  }

  // === Irrigation pipes ===
  const pipeMat = new THREE.MeshStandardMaterial({ color: 0x1565c0, metalness: 0.5, roughness: 0.4 })
  allMaterials.push(pipeMat)
  for (let z = -10; z <= 10; z += 5) {
    const pipeGeo = new THREE.CylinderGeometry(0.08, 0.08, 28, 6)
    allGeometries.push(pipeGeo)
    const pipe = new THREE.Mesh(pipeGeo, pipeMat)
    pipe.rotation.z = Math.PI / 2
    pipe.position.set(0, 0.3, z)
    scene.add(pipe)
  }

  // === Heatmap overlay ===
  const heatGeo = new THREE.PlaneGeometry(10, 10)
  heatMat = new THREE.MeshBasicMaterial({
    color: 0xff3333, transparent: true, opacity: 0.2, side: THREE.DoubleSide
  })
  allGeometries.push(heatGeo)
  allMaterials.push(heatMat)
  const heatPlane = new THREE.Mesh(heatGeo, heatMat)
  heatPlane.rotation.x = -Math.PI / 2
  heatPlane.position.set(5, 0.1, -3)
  scene.add(heatPlane)

  // Blue heatmap zone
  const blueHeatGeo = new THREE.PlaneGeometry(12, 12)
  const blueHeatMat = new THREE.MeshBasicMaterial({
    color: 0x2196f3, transparent: true, opacity: 0.12, side: THREE.DoubleSide
  })
  allGeometries.push(blueHeatGeo)
  allMaterials.push(blueHeatMat)
  const blueHeat = new THREE.Mesh(blueHeatGeo, blueHeatMat)
  blueHeat.rotation.x = -Math.PI / 2
  blueHeat.position.set(-5, 0.1, 3)
  scene.add(blueHeat)

  // === Water particles ===
  const pGeo = new THREE.SphereGeometry(0.1, 4, 4)
  const pMat = new THREE.MeshBasicMaterial({ color: 0x4fc3f7, transparent: true, opacity: 0.6 })
  allGeometries.push(pGeo)
  allMaterials.push(pMat)
  particles = []
  for (let i = 0; i < 60; i++) {
    const p = new THREE.Mesh(pGeo, pMat.clone())
    allMaterials.push(p.material)
    p.userData = { speed: 0.02 + Math.random() * 0.05, t: Math.random(), pipe: Math.floor(Math.random() * 5) }
    scene.add(p)
    particles.push(p)
  }

  // Start animation
  animate()

  // Handle resize
  window.addEventListener('resize', handleResize)
}

function animate() {
  animId = requestAnimationFrame(animate)
  angle += 0.001

  // Auto-rotate camera
  camera.position.x = 42 * Math.cos(angle)
  camera.position.z = 42 * Math.sin(angle)
  camera.position.y = 25 + Math.sin(angle * 2) * 3
  camera.lookAt(0, 3, 0)

  // Water flow particles
  particles.forEach(p => {
    p.userData.t += p.userData.speed
    if (p.userData.t > 1) p.userData.t = 0
    const z = -10 + p.userData.pipe * 5
    p.position.set(-14 + p.userData.t * 28, 0.5, z)
    p.material.opacity = 0.3 + Math.sin(p.userData.t * Math.PI) * 0.4
  })

  // Pulse heatmap
  if (heatMat) {
    heatMat.opacity = 0.15 + Math.sin(Date.now() * 0.003) * 0.08
  }

  renderer.render(scene, camera)
}

function handleResize() {
  if (!wrapperRef.value || !camera || !renderer) return
  const w = wrapperRef.value.clientWidth
  const h = wrapperRef.value.clientHeight
  camera.aspect = w / h
  camera.updateProjectionMatrix()
  renderer.setSize(w, h)
}

// === Exposed methods ===
function resetCamera() {
  angle = 0.3
}

function topView() {
  if (!camera) return
  angle = 0
  camera.position.set(0, 50, 0.1)
  camera.lookAt(0, 0, 0)
}

function zoomIn() {
  if (!camera) return
  camera.fov = Math.max(20, camera.fov - 5)
  camera.updateProjectionMatrix()
}

function zoomOut() {
  if (!camera) return
  camera.fov = Math.min(80, camera.fov + 5)
  camera.updateProjectionMatrix()
}

defineExpose({ resetCamera, topView, zoomIn, zoomOut })

onMounted(() => {
  initScene()
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (animId) cancelAnimationFrame(animId)
  // Dispose all geometries and materials
  allGeometries.forEach(g => g.dispose())
  allMaterials.forEach(m => m.dispose())
  if (renderer) {
    renderer.dispose()
    renderer.forceContextLoss()
  }
  scene = null
  camera = null
  renderer = null
})
</script>

<template>
  <div ref="wrapperRef" class="three-scene-wrapper">
    <canvas ref="canvasRef"></canvas>
  </div>
</template>

<style scoped>
.three-scene-wrapper {
  width: 100%;
  height: 100%;
  position: relative;
  overflow: hidden;
}
.three-scene-wrapper canvas {
  width: 100% !important;
  height: 100% !important;
  display: block;
}
</style>
