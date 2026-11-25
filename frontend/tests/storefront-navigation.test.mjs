import {test,before} from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs/promises'
import path from 'node:path'
import {pathToFileURL} from 'node:url'
import {build} from 'esbuild'
let navigation
before(async()=>{
 const file=path.resolve('node_modules/.cache/storefront-navigation.mjs');await fs.mkdir(path.dirname(file),{recursive:true})
 await build({entryPoints:['src/storefront/utils/catalogNavigation.ts'],outfile:file,bundle:true,platform:'node',format:'esm'})
 navigation=await import(pathToFileURL(file))
})
test('consulta con espacios y acentos encuentra palabras aunque estén separadas',()=>{
 assert.equal(navigation.matchesSearch('Samsung Galaxy A56 5G',' Samsung A56 '),true)
 assert.equal(navigation.matchesSearch('CÁMARA de Córdoba','camara cordoba'),true)
 assert.equal(navigation.matchesSearch('Samsung Galaxy A56','Samsung A55'),false)
})
test('enlaces preservan lista y asesor sin aceptar teléfono de la URL',()=>{
 const q=navigation.campaignParams('?lista=p1,p2&asesorId=a1&asesor=Ana&storefrontHost=tienda.test.invalid&phone=123&q=tv')
 assert.equal(q.get('lista'),'p1,p2');assert.equal(q.get('asesorId'),'a1');assert.equal(q.get('storefrontHost'),'tienda.test.invalid');assert.equal(q.has('phone'),false);assert.equal(q.has('q'),false)
})
test('vuelta conserva búsqueda, filtros y posición de la navegación interna',()=>{
 const snapshot={path:'/catalogo?q=Samsung+A56&categoria=Telefonia&ver=48',visible:48,scroll:1200,product:'p1'}
 assert.deepEqual(navigation.catalogReturn({state:{catalogReturn:snapshot},search:''}),snapshot)
})
test('enlace directo vuelve a la misma lista compartida y rechaza un retorno externo',()=>{
 const back=navigation.catalogReturn({state:{catalogReturn:{path:'//evil.invalid',visible:24,scroll:0}},search:'?lista=p1,p2&asesorId=a1'})
 assert.equal(back.path,'/catalogo?lista=p1%2Cp2&asesorId=a1')
})
