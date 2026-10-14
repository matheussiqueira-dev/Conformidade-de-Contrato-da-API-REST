import test from 'node:test';
import assert from 'node:assert/strict';
import {
  addProduct, availableFromDetails, emptyCart, loadCart, MAX_LINES, refreshAvailability, saleItems, saveCart, setQuantity,
  storageKey, withIdempotencyKey, type KeyValueStorage,
} from '../lib/cart.ts';

const mouse = { id: 7, sku: 'MOU-001', name: 'Mouse', available: 3, activePromotion: { id: 9 } };
const cable = { id: 8, sku: 'CAB-001', name: 'Cabo', available: 1, activePromotion: null };

function memory(): KeyValueStorage & { data: Map<string, string> } {
  const data = new Map<string, string>();
  return { data, getItem: k => data.get(k) ?? null, setItem: (k, v) => { data.set(k, v); }, removeItem: k => { data.delete(k); } };
}

test('adiciona, soma e limita ao disponivel; promocao vigente vai pre-preenchida (C-08)', () => {
  let cart = addProduct(emptyCart(), mouse);
  cart = addProduct(cart, mouse);
  cart = setQuantity(cart, 7, 99);
  assert.equal(cart.lines[0]?.quantity, 3);
  assert.deepEqual(saleItems(cart), [{ productId: 7, quantity: 3, promotionId: 9 }]);
  cart = addProduct(cart, cable);
  cart = addProduct(cart, cable);
  assert.equal(cart.lines[1]?.quantity, 1);
  assert.deepEqual(saleItems(setQuantity(cart, 7, 0)), [{ productId: 8, quantity: 1 }]);
});

test('produto sem disponivel nao entra e o carrinho para em 50 itens (C-14)', () => {
  assert.equal(addProduct(emptyCart(), { ...mouse, available: 0 }).lines.length, 0);
  let cart = emptyCart();
  for (let id = 1; id <= MAX_LINES + 5; id++) cart = addProduct(cart, { ...cable, id, available: 5 });
  assert.equal(cart.lines.length, MAX_LINES);
});

test('mesma chave enquanto o corpo nao muda; corpo novo gera chave nova (CT-WEB-104)', () => {
  let n = 0;
  const key = () => `k${++n}`;
  const first = withIdempotencyKey(addProduct(emptyCart(), mouse), '{"a":1}', key);
  assert.equal(withIdempotencyKey(first, '{"a":1}', key).idempotencyKey, 'k1');
  assert.equal(withIdempotencyKey(first, '{"a":2}', key).idempotencyKey, 'k2');
});

test('409 STOCK_INSUFFICIENT: detalhe vira disponivel e o carrinho e preservado com a quantidade ajustada', () => {
  const available = availableFromDetails(['productId=7; available=2', 'texto qualquer']);
  assert.deepEqual([...available], [[7, 2]]);
  const cart = refreshAvailability(setQuantity(addProduct(emptyCart(), mouse), 7, 3), available);
  assert.equal(cart.lines[0]?.quantity, 2);
  assert.equal(cart.lines[0]?.available, 2);
  assert.equal(refreshAvailability(cart, new Map([[7, 0]])).lines.length, 0);
});

test('carrinho e chave ficam no sessionStorage por usuario e sobrevivem a JSON invalido', () => {
  const storage = memory();
  const cart = withIdempotencyKey(addProduct(emptyCart(), mouse), '{}', () => 'abc');
  saveCart(storage, 3, cart);
  assert.deepEqual(loadCart(storage, 3), cart);
  assert.deepEqual(loadCart(storage, 4), emptyCart());
  storage.setItem(storageKey(3), '{quebrado');
  assert.deepEqual(loadCart(storage, 3), emptyCart());
  saveCart(storage, 3, emptyCart());
  assert.equal(storage.data.has(storageKey(3)), false);
  const failing: KeyValueStorage = { getItem: () => { throw new Error('bloqueado'); }, setItem: () => { throw new Error('cheio'); }, removeItem: () => {} };
  assert.deepEqual(loadCart(failing, 1), emptyCart());
  assert.doesNotThrow(() => saveCart(failing, 1, cart));
});
