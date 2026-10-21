// Carrinho do PDV (WP-3.5): funcoes puras + persistencia em sessionStorage por usuario, junto com a
// Idempotency-Key da venda. Os dois so sao apagados depois de 201/200 do POST /sales/orders.
export interface CartLine {
  productId: number;
  sku: string;
  name: string;
  quantity: number;
  available: number;
  promotionId?: number;
}

/** Desconto manual digitado no carrinho (G-04): so percentual e motivo; o valor vem da API. */
export interface ManualDiscount { percent: string; reason: string }

export interface Cart {
  lines: CartLine[];
  idempotencyKey: string | null;
  manualDiscount?: ManualDiscount;
  /** Corpo (JSON) enviado com a chave; corpo diferente exige chave nova, senao a API responde 409. */
  pendingBody?: string;
}

export const MAX_LINES = 50;
export const MAX_QUANTITY = 999;

export const emptyCart = (): Cart => ({ lines: [], idempotencyKey: null });

function limit(line: CartLine): number {
  return Math.max(0, Math.min(line.available, MAX_QUANTITY));
}

export interface CatalogEntry {
  id: number;
  sku: string;
  name: string;
  available: number;
  activePromotion: { id: number } | null;
}

/** Adiciona uma unidade; respeita o disponivel e o limite de 50 itens (C-14). */
export function addProduct(cart: Cart, product: CatalogEntry): Cart {
  const existing = cart.lines.find(line => line.productId === product.id);
  if (existing) return setQuantity(cart, product.id, existing.quantity + 1);
  if (product.available < 1 || cart.lines.length >= MAX_LINES) return cart;
  const line: CartLine = { productId: product.id, sku: product.sku, name: product.name, quantity: 1, available: product.available };
  if (product.activePromotion) line.promotionId = product.activePromotion.id;
  return { ...cart, lines: [...cart.lines, line] };
}

export function setQuantity(cart: Cart, productId: number, quantity: number): Cart {
  return {
    ...cart,
    lines: cart.lines.flatMap(line => {
      if (line.productId !== productId) return [line];
      const next = Math.min(Math.max(Math.trunc(quantity), 0), limit(line));
      return next === 0 ? [] : [{ ...line, quantity: next }];
    }),
  };
}

export function removeProduct(cart: Cart, productId: number): Cart {
  return { ...cart, lines: cart.lines.filter(line => line.productId !== productId) };
}

/** Disponibilidade nova (ex.: depois de 409 STOCK_INSUFFICIENT) sem perder o carrinho. */
export function refreshAvailability(cart: Cart, available: ReadonlyMap<number, number>): Cart {
  return {
    ...cart,
    lines: cart.lines.map(line => {
      const now = available.get(line.productId);
      return now === undefined ? line : { ...line, available: now, quantity: Math.min(line.quantity, Math.max(now, 0)) };
    }).filter(line => line.quantity > 0),
  };
}

/** Mesma chave enquanto o corpo da venda nao muda; repetir o envio devolve o mesmo pedido (CT-WEB-104). */
export function withIdempotencyKey(cart: Cart, body: string, newKey: () => string): Cart {
  return cart.idempotencyKey && cart.pendingBody === body ? cart : { ...cart, idempotencyKey: newKey(), pendingBody: body };
}

export function setManualDiscount(cart: Cart, discount: ManualDiscount | null): Cart {
  const { manualDiscount: _previous, ...rest } = cart;
  return discount && (discount.percent.trim() || discount.reason.trim()) ? { ...rest, manualDiscount: discount } : rest;
}

/** Desconto pronto para enviar: 0,01 a 100 com ate 2 casas e motivo de 5 a 200 caracteres; senao, nada vai no corpo. */
export function manualDiscountBody(cart: Cart): { percent: number; reason: string } | undefined {
  const discount = cart.manualDiscount;
  if (!discount) return undefined;
  const text = discount.percent.trim().replace(',', '.');
  if (!/^\d{1,3}(\.\d{1,2})?$/.test(text)) return undefined;
  const percent = Number(text);
  const reason = discount.reason.trim();
  if (percent < 0.01 || percent > 100 || reason.length < 5 || reason.length > 200) return undefined;
  return { percent, reason };
}

export function saleItems(cart: Cart): { productId: number; quantity: number; promotionId?: number }[] {
  return cart.lines.map(line => (line.promotionId === undefined
    ? { productId: line.productId, quantity: line.quantity }
    : { productId: line.productId, quantity: line.quantity, promotionId: line.promotionId }));
}

/** Detalhes do 409 STOCK_INSUFFICIENT ("productId=12; available=3") -> disponivel por produto. */
export function availableFromDetails(details: readonly string[]): Map<number, number> {
  const result = new Map<number, number>();
  for (const detail of details) {
    const match = /productId=(\d+);\s*available=(\d+)/.exec(detail);
    if (match) result.set(Number(match[1]), Number(match[2]));
  }
  return result;
}

export interface KeyValueStorage {
  getItem(key: string): string | null;
  setItem(key: string, value: string): void;
  removeItem(key: string): void;
}

export const storageKey = (userId: number) => `sbi:pdv:${userId}`;

export function loadCart(storage: KeyValueStorage | undefined, userId: number): Cart {
  try {
    const raw = storage?.getItem(storageKey(userId));
    if (!raw) return emptyCart();
    const parsed = JSON.parse(raw) as Partial<Cart>;
    if (!Array.isArray(parsed.lines)) return emptyCart();
    const cart: Cart = { lines: parsed.lines, idempotencyKey: typeof parsed.idempotencyKey === 'string' ? parsed.idempotencyKey : null };
    if (typeof parsed.pendingBody === 'string') cart.pendingBody = parsed.pendingBody;
    // O desconto digitado volta com o carrinho; sem isto uma recarga apagava percentual e motivo sem aviso.
    const discount = parsed.manualDiscount;
    if (discount && typeof discount.percent === 'string' && typeof discount.reason === 'string') {
      cart.manualDiscount = { percent: discount.percent, reason: discount.reason };
    }
    return cart;
  } catch {
    return emptyCart();
  }
}

export function saveCart(storage: KeyValueStorage | undefined, userId: number, cart: Cart): void {
  try {
    if (cart.lines.length === 0 && !cart.idempotencyKey) storage?.removeItem(storageKey(userId));
    else storage?.setItem(storageKey(userId), JSON.stringify(cart));
  } catch {
    // Armazenamento cheio ou bloqueado: o carrinho continua na memoria da tela.
  }
}

export function clearCart(storage: KeyValueStorage | undefined, userId: number): void {
  try {
    storage?.removeItem(storageKey(userId));
  } catch {
    // Sem armazenamento disponivel nao ha o que limpar.
  }
}
