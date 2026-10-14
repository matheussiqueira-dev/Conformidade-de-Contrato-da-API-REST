import test from 'node:test';
import assert from 'node:assert/strict';
import { emptyCart, manualDiscountBody, setManualDiscount } from '../lib/cart.ts';

test('desconto manual so vai no corpo com percentual de 0,01 a 100 (2 casas) e motivo de 5 a 200 caracteres', () => {
  const cart = (percent: string, reason: string) => setManualDiscount(emptyCart(), { percent, reason });
  assert.deepEqual(manualDiscountBody(cart('5', 'Cliente fiel')), { percent: 5, reason: 'Cliente fiel' });
  assert.deepEqual(manualDiscountBody(cart('3,33', ' Cliente fiel ')), { percent: 3.33, reason: 'Cliente fiel' });
  for (const [percent, reason] of [['0', 'Cliente fiel'], ['5.555', 'Cliente fiel'], ['101', 'Cliente fiel'], ['abc', 'Cliente fiel'], ['5', 'oi']]) {
    assert.equal(manualDiscountBody(cart(percent ?? '', reason ?? '')), undefined, `${percent} / ${reason}`);
  }
  assert.equal(manualDiscountBody(emptyCart()), undefined);
  assert.equal(setManualDiscount(cart('5', 'Cliente fiel'), { percent: '', reason: ' ' }).manualDiscount, undefined);
});
