import test from 'node:test';
import assert from 'node:assert/strict';
import { existsSync } from 'node:fs';
import { join } from 'node:path';
import { productImage, productLine } from '../lib/product-images.ts';
import { PRODUCT_PHOTOS } from '../lib/product-photos.ts';

test('miniatura pelo SKU, pelo nome (resumo do cliente) ou icone da linha', () => {
  assert.deepEqual(productImage('OL-IP17-256', 'qualquer'), { src: '/produtos/iphone-17.webp', photo: true });
  assert.deepEqual(productImage('ol-ip17-256', 'qualquer').photo, true);
  assert.deepEqual(productImage(undefined, 'iPhone 17 256 GB Azul'), { src: '/produtos/iphone-17.webp', photo: true });
  assert.deepEqual(productImage('NOVO-1', 'iPad 12 pol.'), { src: '/produtos/linhas/ipad.svg', photo: false });
  assert.equal(productLine('MacBook Neo'), 'mac');
  assert.equal(productLine('Apple Watch 41 mm'), 'watch');
  assert.equal(productLine('Apple TV 4K'), 'casa');
  assert.equal(productLine('Pulseira esportiva para Apple Watch 46 mm'), 'acessorio');
});

test('toda foto do manifesto existe em public/produtos e tem autor e licenca', () => {
  for (const photo of PRODUCT_PHOTOS) {
    assert.ok(existsSync(join(process.cwd(), 'public', 'produtos', photo.file)), photo.file);
    assert.ok(photo.author && photo.license && photo.page.startsWith('https://commons.wikimedia.org/'), photo.file);
  }
  for (const line of ['iphone', 'ipad', 'mac', 'airpods', 'watch', 'casa', 'acessorio']) {
    assert.ok(existsSync(join(process.cwd(), 'public', 'produtos', 'linhas', `${line}.svg`)), line);
  }
});
