import puppeteer from 'puppeteer-core'
const CHROME = '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome'
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))
const browser = await puppeteer.launch({ executablePath: CHROME, headless: 'new', args: ['--no-sandbox', '--hide-scrollbars'] })
const page = await browser.newPage()
await page.setViewport({ width: 402, height: 874, deviceScaleFactor: 2, isMobile: true, hasTouch: true })
await page.goto('http://localhost:4173/', { waitUntil: 'networkidle0' })
await sleep(900)
async function clickText(t, n = 0) {
  const ok = await page.evaluate((tt, nn) => {
    const els = [...document.querySelectorAll('button,[role=button]')].filter((e) => (e.innerText || '').includes(tt))
    if (!els[nn]) return false
    els[nn].click(); return true
  }, t, n)
  await sleep(700); return ok
}
await clickText('Quick')
await clickText('Search food')
await clickText('Chicken breast')
await page.screenshot({ path: '/tmp/shots/09-food-detail.png' })
console.log('food detail captured')
await browser.close()
