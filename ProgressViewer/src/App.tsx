import { useEffect, useMemo, useState } from 'react'
import {
  Chart as ChartJS,
  LineElement,
  PointElement,
  LinearScale,
  Tooltip,
  Legend,
  Title
} from 'chart.js'
import { Line } from 'react-chartjs-2'
import GitHubButton from 'react-github-btn'

ChartJS.register(LineElement, PointElement, LinearScale, Tooltip, Legend, Title)

type Reading = [number, number]
type ReadingsJson = {
  anga205?: Reading[]
  munish42?: Reading[]
}

function useReadings() {
  const [data, setData] = useState<ReadingsJson>({})
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let active = true
    fetch('/readings.json')
      .then(r => {
        if (!r.ok) throw new Error(`HTTP ${r.status}`)
        return r.json()
      })
      .then((j: ReadingsJson) => {
        if (!active) return
        setData(j)
        setLoading(false)
      })
      .catch(e => {
        if (!active) return
        setError(e.message)
        setLoading(false)
      })
    return () => {
      active = false
    }
  }, [])

  return { data, loading, error }
}

function toXY(points: Reading[] | undefined) {
  if (!points) return []
  return points.map(([solved, ts]) => ({ x: ts, y: solved }))
}

function formatTs(ts: number) {
  try {
    return new Date(ts * 1000).toLocaleString()
  } catch {
    return String(ts)
  }
}

function formatDateOnly(ts: number) {
  try {
    return new Date(ts * 1000).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric'
    })
  } catch {
    return String(ts)
  }
}

export default function App() {
  const { data, loading, error } = useReadings()
  const [range, setRange] = useState<'7d' | '30d' | '90d' | '1y' | 'all' | 'custom'>('all')
  const [customFrom, setCustomFrom] = useState<string>('') // datetime-local value
  const [customTo, setCustomTo] = useState<string>('') // datetime-local value

  const nowSecs = Math.floor(Date.now() / 1000)
  const rangeToSeconds: Record<'7d' | '30d' | '90d' | '1y' | 'all', number | 'all'> = {
    '7d': 7 * 24 * 60 * 60,
    '30d': 30 * 24 * 60 * 60,
    '90d': 90 * 24 * 60 * 60,
    '1y': 365 * 24 * 60 * 60,
    'all': 'all'
  }

  function parseLocalDateTimeToSeconds(v: string): number | null {
    if (!v) return null
    const ms = new Date(v).getTime()
    if (Number.isNaN(ms)) return null
    return Math.floor(ms / 1000)
  }

  const [minTs, maxTs] = useMemo((): [number, number] => {
    if (range === 'custom') {
      const from = parseLocalDateTimeToSeconds(customFrom)
      const to = parseLocalDateTimeToSeconds(customTo)
      // If both provided, normalize order; if one missing, treat as unbounded on that side
      let lo = -Infinity
      let hi = Infinity
      if (from != null) lo = from
      if (to != null) hi = to
      if (from != null && to != null && from > to) {
        // swap to keep it sane
        lo = to
        hi = from
      }
      return [lo, hi]
    }
    const span = rangeToSeconds[range as Exclude<typeof range, 'custom'>]
    if (span === 'all') return [-Infinity, Infinity]
    return [nowSecs - span, Infinity]
  }, [range, customFrom, customTo, nowSecs])

  const chartData = useMemo(() => {
  const anga = toXY(data.anga205).filter(p => p.x >= minTs && p.x <= maxTs)
  const munis = toXY(data.munish42).filter(p => p.x >= minTs && p.x <= maxTs)
    const datasets: any[] = [
      {
        label: 'Angad',
        data: anga,
        borderColor: 'rgb(59, 130, 246)',
        backgroundColor: 'rgba(59, 130, 246, 0.2)',
        tension: 0.2,
        pointRadius: 2
      },
      {
        label: 'Munis',
        data: munis,
        borderColor: 'rgb(16, 185, 129)',
        backgroundColor: 'rgba(16, 185, 129, 0.2)',
        tension: 0.2,
        pointRadius: 2
      }
    ]

    // Add dotted line if there is a >1 day gap between latest points
    const lastAnga = anga.length ? anga[anga.length - 1] : null
    const lastMunis = munis.length ? munis[munis.length - 1] : null
    if (lastAnga && lastMunis) {
      const gap = Math.abs(lastAnga.x - lastMunis.x)
      const oneDay = 24 * 60 * 60
      if (gap > oneDay) {
        const aheadIsAnga = lastAnga.x > lastMunis.x
        const aheadTime = aheadIsAnga ? lastAnga.x : lastMunis.x
        const lagPoint = aheadIsAnga ? lastMunis : lastAnga
        const dotted = [
          { x: lagPoint.x, y: lagPoint.y },
          { x: aheadTime, y: lagPoint.y }
        ]
        const lagColor = aheadIsAnga ? 'rgb(16, 185, 129)' : 'rgb(59, 130, 246)'
        datasets.push({
          label: 'Gap',
          data: dotted,
          borderColor: lagColor,
          backgroundColor: 'transparent',
          borderDash: [6, 6],
          borderWidth: 1.5,
          pointRadius: 0,
          tension: 0,
          order: 3,
        })
      }
    }

    return { datasets }
  }, [data, minTs, maxTs])

  const options = useMemo(() => {
    return {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        title: {
          display: true,
          text: 'LeetCode Solves Over Time',
          color: '#e5e7eb'
        },
        tooltip: {
          backgroundColor: 'rgba(17, 24, 39, 0.95)',
          callbacks: {
            title: (ctx: any) => {
              if (ctx.length && ctx[0].parsed?.x) return formatTs(ctx[0].parsed.x)
              return ''
            }
          }
        },
        legend: {
          labels: {
            color: '#e5e7eb',
            filter: (legendItem: any) => legendItem.text !== 'Gap'
          }
        }
      },
      scales: {
        x: {
          type: 'linear' as const,
          title: { display: true, text: 'Time', color: '#e5e7eb' },
          ticks: {
            callback: (val: any) => formatDateOnly(Number(val)),
            color: '#9ca3af'
          },
          grid: {
            color: 'rgba(255,255,255,0.08)'
          }
        },
        y: {
          title: { display: true, text: 'Total Solved', color: '#e5e7eb' },
          beginAtZero: false,
          ticks: { color: '#9ca3af' },
          grid: {
            color: 'rgba(255,255,255,0.08)'
          }
        }
      }
    }
  }, [])

  if (loading) return <div className="min-h-screen bg-neutral-900 text-neutral-100 p-4">Loading…</div>
  if (error) return <div className="min-h-screen bg-neutral-900 text-neutral-100 p-4">Error: {error}</div>

  return (
    <div className="min-h-screen bg-neutral-900 text-neutral-100 p-4">
  <div className="max-w-5xl mx-auto">
        <header className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <h1 className="text-xl font-semibold">Angad vs Munis</h1>
          <div className="flex flex-col sm:flex-row items-start sm:items-center gap-2 sm:gap-3">
            <div className="flex items-center gap-2">
              <label htmlFor="range" className="text-sm text-neutral-300">Time range</label>
              <select
                id="range"
                value={range}
                onChange={e => setRange(e.target.value as typeof range)}
                className="bg-neutral-800 text-neutral-100 border border-neutral-700 rounded px-2 py-1 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                <option value="7d">Last 7 days</option>
                <option value="30d">Last 30 days</option>
                <option value="90d">Last 90 days</option>
                <option value="1y">Last year</option>
                <option value="all">All time</option>
                <option value="custom">Custom…</option>
              </select>
            </div>
            {range === 'custom' && (
              <div className="flex flex-wrap items-center gap-2">
                <label className="text-sm text-neutral-300" htmlFor="from">From</label>
                <input
                  id="from"
                  type="datetime-local"
                  value={customFrom}
                  onChange={e => setCustomFrom(e.target.value)}
                  className="bg-neutral-800 text-neutral-100 border border-neutral-700 rounded px-2 py-1 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
                <label className="text-sm text-neutral-300" htmlFor="to">To</label>
                <input
                  id="to"
                  type="datetime-local"
                  value={customTo}
                  onChange={e => setCustomTo(e.target.value)}
                  className="bg-neutral-800 text-neutral-100 border border-neutral-700 rounded px-2 py-1 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
            )}
          </div>
        </header>

        <div className="w-full lg:max-w-[66vw] mx-auto h-80 md:h-[420px]">
          <Line data={chartData} options={options} />
        </div>
      </div>
      <div className="fixed inset-x-0 bottom-0 z-10 pointer-events-none">
        <div className="flex items-end justify-between w-full pb-[0.3vh]">
          <div />
          <a className='pointer-events-auto pr-[1vh]'>
            <GitHubButton
              href="https://github.com/Anga205/anga_vs_munis_tracker"
              data-color-scheme="no-preference: dark; light: dark; dark: dark;"
              data-size="large"
              aria-label="Star Anga205/anga_vs_munis_tracker on GitHub"
            >
              Source Code
            </GitHubButton>
          </a>
        </div>
      </div>
    </div>
  )
}
