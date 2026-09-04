import puppeteer from 'puppeteer-core'
import { mkdirSync } from 'node:fs'

const CHROME = '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome'
const URL = 'http://localhost:4173/'
const OUT = '/tmp/shots'
mkdirSync(OUT, { recursive: true })

const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

const browser = await puppeteer.launch({
  executablePath: CHROME,
  headless: 'new',
  args: ['--no-sandbox', '--force-color-profile=srgb', '--hide-scrollbars'],
})

async function newPage(mobile = true) {
  const page = await browser.newPage()
  await page.setViewport(
    mobile
      ? { width: 402, height: 874, deviceScaleFactor: 2, isMobile: true, hasTouch: true }
      : { width: 1440, height: 1000, deviceScaleFactor: 1 },
  )
  await page.goto(URL, { waitUntil: 'networkidle0' })
  await sleep(900)
  return page
}

// Click the first visible button/[role=button] whose text contains `text`.
async function clickText(page, text, nth = 0) {
  const ok = await page.evaluate(
    (t, n) => {
      const els = [...document.querySelectorAll('button,[role=button]')]
      const matches = els.filter((e) => (e.innerText || '').trim().includes(t))
      const el = matches[n]
      if (!el) return false
      el.click()
      return true
    },
    text,
    nth,
  )
  await sleep(650)
  return ok
}

async function shot(page, name) {
  await page.screenshot({ path: `${OUT}/${name}.png` })
  console.log('shot:', name)
}

// ---- Mobile screen tour ----
const p = await newPage(true)
await shot(p, '01-today')

await clickText(p, 'Calendar')
await shot(p, '02-calendar')
// open a day detail
await p.evaluate(() => {
  const cells = [...document.querySelectorAll('button')].filter((b) => /(^|\s)15(\s|$)/.test(b.innerText.trim()))
  if (cells[0]) cells[0].click()
})
await sleep(700)
await shot(p, '02b-calendar-day')

await clickText(p, 'Check-ins')
await shot(p, '03-checkins')

await clickText(p, 'Settings')
await shot(p, '04-settings')
const paywall = await clickText(p, 'Unlock Strakk Pro')
if (paywall) await shot(p, '05-paywall')
// close paywall
await p.evaluate(() => {
  const x = [...document.querySelectorAll('button[aria-label="Close"]')][0]
  if (x) x.click()
})
await sleep(600)

// Back to Today, open Add flow
await clickText(p, 'Today')
await sleep(300)
await clickText(p, 'Quick')
await shot(p, '06-add')

await clickText(p, 'Scan barcode')
await sleep(2000) // let the scan "find" the product
await shot(p, '07-scan')

// reopen add -> search
await p.evaluate(() => {
  const x = [...document.querySelectorAll('button[aria-label="Close"]')][0]
  if (x) x.click()
})
await sleep(500)
await clickText(p, 'Quick')
await clickText(p, 'Search food')
await shot(p, '08-search')
// open a food detail
await clickText(p, 'Greek yogurt')
await shot(p, '09-food-detail')
await p.evaluate(() => {
  const x = [...document.querySelectorAll('button[aria-label="Close"]')][0]
  if (x) x.click()
})
await sleep(500)

// Meal detail from Today
await p.evaluate(() => {
  const x = [...document.querySelectorAll('button[aria-label="Close"]')][0]
  if (x) x.click()
})
await sleep(400)
await clickText(p, 'Dinner')
await shot(p, '10-meal-detail')

// ---- Desktop framed hero ----
const d = await newPage(false)
await shot(d, '00-desktop-frame')

await browser.close()
console.log('done ->', OUT)
