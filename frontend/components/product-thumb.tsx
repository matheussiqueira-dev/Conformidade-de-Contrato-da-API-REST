import { productImage } from '../lib/product-images.ts';

/** Miniatura decorativa ao lado do nome do produto (o nome ja esta no texto, entao alt vazio). */
export default function ProductThumb({ sku, name, size = 40 }: { sku?: string; name: string; size?: number }) {
  const image = productImage(sku, name);
  return (
    <img className={image.photo ? 'thumb' : 'thumb thumb-icon'} src={image.src} alt="" width={size} height={size}
         loading="lazy" decoding="async" />
  );
}
