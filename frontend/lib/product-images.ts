// Miniatura do produto pelo SKU que a API ja devolve (D-PUB-13): foto de licenca livre em public/produtos/ ou, sem
// foto (acessorio sem imagem livre, produto criado pela tela), o icone da linha deduzido do nome.
import { PRODUCT_PHOTOS } from './product-photos.ts';

export interface ProductImage { src: string; photo: boolean }

const BY_SKU = new Map(PRODUCT_PHOTOS.flatMap(photo => photo.skus.map(sku => [sku, photo.file] as const)));
// O resumo do cliente traz so o nome do produto, sem SKU.
const BY_NAME = new Map(PRODUCT_PHOTOS.flatMap(photo => photo.names.map(name => [name, photo.file] as const)));

const LINES: ReadonlyArray<readonly [RegExp, string]> = [
  [/^iphone/i, 'iphone'], [/^ipad/i, 'ipad'], [/^(macbook|imac|mac mini)/i, 'mac'], [/^airpods/i, 'airpods'],
  [/^apple watch/i, 'watch'], [/^(homepod|apple tv)/i, 'casa'],
];

export function productLine(name: string): string {
  return LINES.find(([pattern]) => pattern.test(name.trim()))?.[1] ?? 'acessorio';
}

export function productImage(sku: string | undefined, name: string): ProductImage {
  const file = (sku ? BY_SKU.get(sku.toUpperCase()) : undefined) ?? BY_NAME.get(name);
  return file ? { src: `/produtos/${file}`, photo: true } : { src: `/produtos/linhas/${productLine(name)}.svg`, photo: false };
}
