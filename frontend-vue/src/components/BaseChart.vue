<script setup>
// Desenha UM gráfico com Chart.js (barra, linha ou rosca). Acompanha o modo escuro.
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { Chart, registerables } from 'chart.js'

Chart.register(...registerables)

const props = defineProps({
  tipo: { type: String, default: 'bar' },       // 'bar' | 'line' | 'doughnut'
  rotulos: { type: Array, default: () => [] },
  valores: { type: Array, default: () => [] },
  titulo: { type: String, default: '' },
  horizontal: Boolean,
  moeda: Boolean                                 // formata o eixo e o tooltip como R$
})

const canvas = ref(null)
let grafico = null
let observador = null
const CORES = ['#2563eb', '#0ea5e9', '#14b8a6', '#f59e0b', '#8b5cf6', '#64748b']
const brl = (n) => Number(n).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL', maximumFractionDigits: 0 })

function desenhar() {
  if (!canvas.value) return
  grafico?.destroy()
  const escuro = document.documentElement.classList.contains('dark')
  const texto = escuro ? '#cbd5e1' : '#475569'
  const grade = escuro ? 'rgba(255,255,255,0.08)' : 'rgba(15,23,42,0.08)'
  const rosca = props.tipo === 'doughnut'
  const formato = props.moeda ? (v) => brl(v) : (v) => Number(v).toLocaleString('pt-BR')

  grafico = new Chart(canvas.value, {
    type: props.tipo,
    data: {
      labels: props.rotulos,
      datasets: [{
        label: props.titulo,
        data: props.valores,
        backgroundColor: rosca ? CORES : '#2563eb',
        borderColor: rosca ? (escuro ? '#131a2b' : '#ffffff') : '#2563eb',
        borderRadius: props.tipo === 'bar' ? 6 : 0,
        tension: 0.3
      }]
    },
    options: {
      locale: 'pt-BR',
      indexAxis: props.horizontal ? 'y' : 'x',
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { display: rosca, labels: { color: texto } },
        tooltip: { callbacks: { label: (c) => ` ${c.dataset.label ? c.dataset.label + ': ' : ''}${formato(c.parsed.y ?? c.parsed.x ?? c.parsed)}` } }
      },
      scales: rosca ? {} : {
        x: { ticks: { color: texto, callback: props.horizontal ? (v) => formato(v) : undefined }, grid: { color: grade } },
        y: { beginAtZero: true, ticks: { color: texto, callback: props.horizontal ? undefined : (v) => formato(v) }, grid: { color: grade } }
      }
    }
  })
}

onMounted(() => {
  desenhar()
  // redesenha quando o modo escuro é ligado/desligado (a classe "dark" do <html> muda)
  observador = new MutationObserver(desenhar)
  observador.observe(document.documentElement, { attributes: true, attributeFilter: ['class'] })
})
watch(() => [props.rotulos, props.valores], desenhar)
onBeforeUnmount(() => { observador?.disconnect(); grafico?.destroy() })
</script>

<template>
  <div class="h-64"><canvas ref="canvas"></canvas></div>
</template>
