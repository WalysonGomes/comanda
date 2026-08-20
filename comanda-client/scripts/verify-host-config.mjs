import { readdir, readFile } from 'node:fs/promises'
import { resolve } from 'node:path'

const rootDomains = (process.env.VITE_ROOT_DOMAINS || process.env.APP_DOMAIN || '')
  .split(',')
  .map((value) => value.trim().toLowerCase())
  .filter(Boolean)

if (rootDomains.length === 0) {
  throw new Error('VITE_ROOT_DOMAINS or APP_DOMAIN is required for a production build')
}
const tenantDomain = (process.env.VITE_TENANT_DOMAIN || process.env.APP_DOMAIN || rootDomains[0]).trim().toLowerCase()
if (!tenantDomain) throw new Error('VITE_TENANT_DOMAIN or APP_DOMAIN is required for a production build')

const assetsDirectory = resolve('../comanda-api/target/generated-resources/static/assets')
const javascriptAssets = (await readdir(assetsDirectory)).filter((name) => name.endsWith('.js'))
const bundles = await Promise.all(javascriptAssets.map((name) => readFile(resolve(assetsDirectory, name), 'utf8')))

for (const domain of rootDomains) {
  if (!bundles.some((bundle) => bundle.includes(domain))) {
    throw new Error(`Built frontend does not contain configured root domain: ${domain}`)
  }
}
if (!bundles.some((bundle) => bundle.includes(tenantDomain))) {
  throw new Error(`Built frontend does not contain configured tenant domain: ${tenantDomain}`)
}

console.log(`Verified frontend root domains: ${rootDomains.join(', ')}`)
