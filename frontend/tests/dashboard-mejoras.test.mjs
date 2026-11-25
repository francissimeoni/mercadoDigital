import {test,before} from 'node:test'
import assert from 'node:assert/strict'
import {build} from 'esbuild'
import fs from 'node:fs/promises'
import path from 'node:path'
import {pathToFileURL} from 'node:url'
let phone
before(async()=>{
 const target=path.resolve('node_modules/.cache/dashboard-mejoras/phone.mjs');await fs.mkdir(path.dirname(target),{recursive:true})
 await build({entryPoints:['src/storefront/dashboard/contactPhone.ts'],outfile:target,bundle:true,platform:'node',format:'esm',packages:'external'})
 phone=await import(pathToFileURL(target).href)
})
for(const [raw,country,mobile,expected] of [
 ['3482670015','AR',true,'+5493482670015'],['0351 15 5123456','AR',true,'+5493515123456'],
 ['+54 11 2345 6789','AR',true,'+541123456789'],['612345678','ES',false,'+34612345678'],
 ['2025550123','US',false,'+12025550123'],['0054 9 3482 670015','AR',true,'+5493482670015'],
 ['+34 612 345 678','AR',true,'+34612345678'],
]) test('Número explícito o nacional con país: '+raw,()=>assert.equal(phone.parseCommercialPhone(raw,country,mobile)?.number,expected))
for(const raw of ['1234567','tel:3482670015','3482670015 ext 1','++5493482670015',''])test('Rechaza incompleto o ambiguo: '+raw,()=>assert.equal(phone.parseCommercialPhone(raw,'AR',true),null))
test('País inferido solamente del teléfono internacional para editar; no se usa como zona geográfica',()=>{assert.equal(phone.phoneCountry('+34612345678'),'ES');assert.equal(phone.phoneCountry(null),'AR')})
