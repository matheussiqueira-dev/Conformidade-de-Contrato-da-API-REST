'use client';
// PDV (WP-3.5): 1 Itens -> 2 Cliente e pagamento -> 3 Confirmacao. Pensado para teclado: "/" foca a busca,
// setas percorrem os resultados, Enter adiciona; SKU exato + Enter (leitor de codigo de barras) adiciona direto.
import { useEffect, useMemo, useRef, useState, type KeyboardEvent } from 'react';
import Link from 'next/link';
import { keepPreviousData, useMutation, useQuery } from '@tanstack/react-query';
import type { z } from 'zod';
import {
  CancelOrderResponse, ConfirmSalePaymentResponse, CreateSaleOrderBody, CreateSaleOrderResponse, GetCustomerResponse, ListCustomersResponse,
  ListSalesCatalogResponse, QuoteSaleResponse,
} from '../../../lib/api/generated/sbi.zod.ts';
import { ApiError, api } from '../../../lib/api/http.ts';
import ProductThumb from '../../../components/product-thumb.tsx';
import {
  addProduct, availableFromDetails, clearCart, emptyCart, loadCart, manualDiscountBody, refreshAvailability, removeProduct, saleItems, saveCart,
  setManualDiscount, setQuantity, withIdempotencyKey, type Cart,
} from '../../../lib/cart.ts';
import { label, money, ORDER_STATUS, statusPill, PAYMENT_TYPE } from '../../../lib/format.ts';
import { ErrorNotice, errorMessage } from '../../../components/feedback.tsx';
import CustomerForm, { Field } from '../../../components/customer-form.tsx';
import { Icon } from '../../../components/brand/icons.tsx';

type CatalogItem = z.infer<typeof ListSalesCatalogResponse>['content'][number];
type CustomerSummary = z.infer<typeof ListCustomersResponse>['content'][number];
type SalesOrder = z.infer<typeof CreateSaleOrderResponse>;
type PaymentType = 'PIX' | 'CARD' | 'BOLETO';

const STEPS = ['Itens', 'Cliente e pagamento', 'Confirmação'] as const;

function useDebounced<T>(value: T, delay: number): T {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delay);
    return () => clearTimeout(timer);
  }, [value, delay]);
  return debounced;
}

function storage(): Storage | undefined {
  try {
    return window.sessionStorage;
  } catch {
    return undefined;
  }
}

function inDays(days: number): string {
  const date = new Date(Date.now() + days * 86_400_000);
  return new Intl.DateTimeFormat('en-CA', { timeZone: 'America/Fortaleza' }).format(date);
}

export default function SalePoint({ userId, userName, initialCustomerId = null }: { userId: number; userName: string; initialCustomerId?: number | null }) {
  const [cart, setCart] = useState<Cart>(emptyCart);
  const [loaded, setLoaded] = useState(false);
  const [step, setStep] = useState(0);
  const [order, setOrder] = useState<SalesOrder | null>(null);
  const [notice, setNotice] = useState('');

  useEffect(() => {
    setCart(loadCart(storage(), userId));
    setLoaded(true);
  }, [userId]);
  useEffect(() => {
    if (loaded) saveCart(storage(), userId, cart);
  }, [cart, loaded, userId]);

  function restart() {
    clearCart(storage(), userId);
    setCart(emptyCart());
    setOrder(null);
    setNotice('');
    setStep(0);
  }

  return (
    <div className="stack">
      <header className="page-head">
        <div className="stack-sm">
          <h1>Nova venda</h1>
          <p className="muted">Responsável: {userName} · definido pela sua conta</p>
          <ol className="stepper" aria-label="Etapas da venda">
            {STEPS.map((name, index) => (
              <li key={name} className={index < step ? 'done' : undefined} aria-current={index === step ? 'step' : undefined}>
                {index < step ? <Icon name="check" size={16} label="Concluída" /> : `${index + 1}.`} {name}
              </li>
            ))}
          </ol>
        </div>
        {step > 0 && step < 2 && <button type="button" className="btn" onClick={() => setStep(step - 1)}>Voltar aos itens</button>}
      </header>
      {notice && <p className="notice notice-warn" role="status">{notice}</p>}
      {step === 0 && <ItemsStep cart={cart} setCart={setCart} onNext={() => { setNotice(''); setStep(1); }} />}
      {step === 1 && <CheckoutStep cart={cart} setCart={setCart} userId={userId} initialCustomerId={initialCustomerId}
                                   onStockProblem={message => { setNotice(message); setStep(0); }}
                                   onCreated={created => { clearCart(storage(), userId); setCart(emptyCart()); setOrder(created); setStep(2); }} />}
      {step === 2 && order && <ConfirmStep order={order} onOrder={setOrder} onRestart={restart} />}
    </div>
  );
}

// ---------------- 1. Itens ----------------

function ItemsStep({ cart, setCart, onNext }: { cart: Cart; setCart: (cart: Cart) => void; onNext: () => void }) {
  const [term, setTerm] = useState('');
  const [selected, setSelected] = useState(0);
  const search = useRef<HTMLInputElement>(null);
  const q = useDebounced(term.trim(), 250);

  useEffect(() => {
    search.current?.focus();
    function onKey(event: globalThis.KeyboardEvent) {
      const target = event.target as HTMLElement | null;
      const typing = target && (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA' || target.isContentEditable);
      if (event.key === '/' && !typing) {
        event.preventDefault();
        search.current?.focus();
      }
    }
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, []);

  const catalog = useQuery({
    queryKey: ['catalog', q],
    queryFn: ({ signal }) => api.request(`/sales/catalog?${new URLSearchParams({ q, size: '8' })}`, { signal, schema: ListSalesCatalogResponse }),
    placeholderData: keepPreviousData,
  });
  const results = catalog.data?.content ?? [];

  const lines = cart.lines;
  const items = useMemo(() => saleItems(cart), [cart]);
  const manual = useMemo(() => manualDiscountBody(cart), [cart]);
  const debouncedItems = useDebounced(items, 300);
  const debouncedManual = useDebounced(manual, 300);
  const quote = useQuery({
    queryKey: ['quote', debouncedItems, debouncedManual],
    queryFn: ({ signal }) => api.request('/sales/quote', { method: 'POST', signal, schema: QuoteSaleResponse,
      body: { items: debouncedItems, ...(debouncedManual ? { manualDiscount: debouncedManual } : {}) } }),
    enabled: debouncedItems.length > 0,
    placeholderData: keepPreviousData,
    retry: false,
  });
  const discountProblem = quote.error instanceof ApiError && (quote.error.code === 'DISCOUNT_ABOVE_LIMIT' || quote.error.code === 'BELOW_COST');
  const limit = quote.error instanceof ApiError && quote.error.code === 'DISCOUNT_ABOVE_LIMIT'
    ? quote.error.details.find(d => d.startsWith('limitPercent='))?.split('=')[1]
    : quote.data?.manualDiscountLimitPercent?.toString();

  function add(product: CatalogItem) {
    setCart(addProduct(cart, product));
    setTerm('');
    setSelected(0);
    search.current?.focus();
  }

  function onSearchKey(event: KeyboardEvent<HTMLInputElement>) {
    if (event.key === 'ArrowDown') { event.preventDefault(); setSelected(i => Math.min(i + 1, Math.max(results.length - 1, 0))); }
    if (event.key === 'ArrowUp') { event.preventDefault(); setSelected(i => Math.max(i - 1, 0)); }
    if (event.key === 'Enter') {
      event.preventDefault();
      const exact = results.find(item => item.sku.toUpperCase() === term.trim().toUpperCase());
      const chosen = exact ?? results[selected];
      if (chosen) add(chosen);
    }
  }

  const quoted = new Map((quote.data?.items ?? []).map(item => [item.productId, item]));
  const totalUnits = lines.reduce((sum, line) => sum + line.quantity, 0);

  return (
    <div className="pdv">
      <section className="card stack" aria-labelledby="busca-titulo">
        <h2 id="busca-titulo">Buscar produto</h2>
        <div className="field">
          <label htmlFor="busca">Nome ou SKU <span className="muted">(atalho: /)</span></label>
          <input id="busca" ref={search} className="input" type="search" autoComplete="off" value={term}
                 role="combobox" aria-expanded={results.length > 0} aria-controls="resultados" aria-autocomplete="list"
                 aria-activedescendant={results[selected] ? `resultado-${results[selected]?.id}` : undefined}
                 onChange={event => { setTerm(event.target.value); setSelected(0); }} onKeyDown={onSearchKey} />
        </div>
        <ErrorNotice error={catalog.error} />
        {catalog.isLoading ? <p role="status" className="muted">Buscando produtos…</p>
          : results.length === 0 ? <p className="empty">Nenhum produto disponível para esta busca.</p>
          : <ul id="resultados" className="results" role="listbox" aria-label="Produtos encontrados">
              {results.map((item, index) => (
                <li key={item.id} role="presentation">
                  <button type="button" role="option" id={`resultado-${item.id}`} aria-selected={index === selected}
                          onClick={() => add(item)} disabled={item.available < 1}>
                    <span className="product-cell"><ProductThumb sku={item.sku} name={item.name} />
                      <span><strong>{item.name}</strong><br /><span className="mono muted">{item.sku}</span>
                        {item.activePromotion && <> · <span className="pill pill-ok">{item.activePromotion.name}</span></>}</span></span>
                    <span className="num right">{money(item.price)}<br /><span className="muted">{item.available} disp.</span></span>
                  </button>
                </li>
              ))}
            </ul>}
      </section>

      <section className="card stack" aria-labelledby="carrinho-titulo">
        <h2 id="carrinho-titulo">Carrinho <span className="muted num">({totalUnits} {totalUnits === 1 ? 'item' : 'itens'})</span></h2>
        {lines.length === 0 ? <p className="empty">Adicione produtos para começar a venda.</p> : <div>
          {lines.map(line => {
            const price = quoted.get(line.productId);
            const short = price && price.available < line.quantity;
            return (
              <div className="cart-line" key={line.productId}>
                <div className="product-cell">
                  <ProductThumb sku={line.sku} name={line.name} />
                  <div>
                    <strong>{line.name}</strong><br /><span className="mono muted">{line.sku}</span>
                    {short && <p className="pill pill-warn">Restam {price.available}</p>}
                  </div>
                </div>
                <div className="stack-sm end">
                  <span className="num">{price ? money(price.subtotal) : '—'}</span>
                  <div className="qty" role="group" aria-label={`Quantidade de ${line.name}`}>
                    <button type="button" className="btn" aria-label={`Diminuir ${line.name}`}
                            onClick={() => setCart(setQuantity(cart, line.productId, line.quantity - 1))}>−</button>
                    <output aria-live="polite" className="num">{line.quantity}</output>
                    <button type="button" className="btn" aria-label={`Aumentar ${line.name}`} disabled={line.quantity >= line.available}
                            onClick={() => setCart(setQuantity(cart, line.productId, line.quantity + 1))}>+</button>
                    <button type="button" className="btn btn-ghost" aria-label={`Remover ${line.name}`}
                            onClick={() => setCart(removeProduct(cart, line.productId))}>Remover</button>
                  </div>
                </div>
              </div>
            );
          })}
        </div>}
        {lines.length > 0 && <fieldset className="stack-sm plain-fieldset">
          <legend className="label">Desconto manual (opcional)</legend>
          <div className="form-grid">
            <Field id="desconto-percentual" label="Percentual (%)" hint={limit ? `Seu limite: ${limit.replace('.', ',')}%` : undefined}>
              <input id="desconto-percentual" className="input" inputMode="decimal" maxLength={6} value={cart.manualDiscount?.percent ?? ''}
                     onChange={event => setCart(setManualDiscount(cart, { percent: event.target.value, reason: cart.manualDiscount?.reason ?? '' }))} />
            </Field>
            <Field id="desconto-motivo" label="Motivo" hint="De 5 a 200 caracteres">
              <input id="desconto-motivo" className="input" maxLength={200} value={cart.manualDiscount?.reason ?? ''}
                     onChange={event => setCart(setManualDiscount(cart, { percent: cart.manualDiscount?.percent ?? '', reason: event.target.value }))} />
            </Field>
          </div>
        </fieldset>}
        {discountProblem
          ? <p className="notice notice-error" role="alert">{quote.error instanceof ApiError && quote.error.code === 'DISCOUNT_ABOVE_LIMIT'
              ? `Desconto acima do seu limite${limit ? ` de ${limit.replace('.', ',')}%` : ''}. Venda dentro do limite e peça ao gerente para aprovar o restante no pedido.`
              : errorMessage(quote.error)}</p>
          : <ErrorNotice error={quote.error} />}
        {lines.length > 0 && <dl className="totals" aria-live="polite">
          <div><dt>Subtotal</dt><dd className="num">{money(quote.data?.subtotal)}</dd></div>
          <div><dt>Descontos</dt><dd className="num">{money(quote.data?.discountTotal)}</dd></div>
          {quote.data?.manualDiscount && <div><dt>Desconto manual</dt><dd className="num">{money(quote.data.manualDiscount)}</dd></div>}
          <div className="total"><dt>Total</dt><dd className="num">{money(quote.data?.total)}</dd></div>
        </dl>}
        <button type="button" className="btn btn-primary btn-block" disabled={lines.length === 0 || discountProblem} onClick={onNext}>Continuar para cliente e pagamento</button>
      </section>
    </div>
  );
}

// ---------------- 2. Cliente e pagamento ----------------

interface AddressForm { street: string; number: string; complement: string; neighborhood: string; city: string; state: string; zipCode: string }
const EMPTY_ADDRESS: AddressForm = { street: '', number: '', complement: '', neighborhood: '', city: '', state: 'CE', zipCode: '' };

function CheckoutStep({ cart, setCart, userId, initialCustomerId, onCreated, onStockProblem }: {
  cart: Cart; setCart: (cart: Cart) => void; userId: number; initialCustomerId: number | null;
  onCreated: (order: SalesOrder) => void; onStockProblem: (message: string) => void;
}) {
  const [term, setTerm] = useState('');
  const [customer, setCustomer] = useState<CustomerSummary | { id: number; name: string; document: string } | null>(null);
  const [presetUsed, setPresetUsed] = useState(false);
  const [drawer, setDrawer] = useState(false);
  const [payment, setPayment] = useState<PaymentType>('PIX');
  const [cardLast4, setCardLast4] = useState('');
  const [installments, setInstallments] = useState('1');
  const [authorizationCode, setAuthorizationCode] = useState('');
  const [dueDate, setDueDate] = useState(inDays(3));
  const [delivery, setDelivery] = useState(false);
  const [address, setAddress] = useState<AddressForm>(EMPTY_ADDRESS);
  const [problems, setProblems] = useState<string[]>([]);
  const dialog = useRef<HTMLDialogElement>(null);
  const q = useDebounced(term.trim(), 250);

  useEffect(() => {
    if (drawer) dialog.current?.showModal(); else dialog.current?.close();
  }, [drawer]);

  // Cliente vindo do perfil (/vender?cliente=ID): carregado uma vez; "Trocar cliente" continua livre.
  const preset = useQuery({
    queryKey: ['customer', initialCustomerId],
    queryFn: ({ signal }) => api.request(`/customers/${initialCustomerId}`, { signal, schema: GetCustomerResponse }),
    enabled: initialCustomerId !== null && !presetUsed,
  });
  useEffect(() => {
    if (presetUsed || !preset.data) return;
    setCustomer({ id: preset.data.id, name: preset.data.name, document: preset.data.document });
    setPresetUsed(true);
  }, [preset.data, presetUsed]);

  const customers = useQuery({
    queryKey: ['customers', q],
    queryFn: ({ signal }) => api.request(`/customers?${new URLSearchParams({ q, size: '6' })}`, { signal, schema: ListCustomersResponse }),
    enabled: q.length >= 2,
  });

  function body(): unknown {
    const paymentBody = payment === 'CARD'
      ? { type: 'CARD', cardLast4: cardLast4.trim(), installments: Number(installments), ...(authorizationCode.trim() ? { authorizationCode: authorizationCode.trim() } : {}) }
      : payment === 'BOLETO' ? { type: 'BOLETO', dueDate } : { type: 'PIX' };
    const shipping = Object.fromEntries(Object.entries(address).map(([key, value]) => [key, value.trim()]).filter(([key, value]) => key !== 'complement' || value));
    return {
      customerId: customer?.id ?? 0,
      items: saleItems(cart),
      ...(manualDiscountBody(cart) ? { manualDiscount: manualDiscountBody(cart) } : {}),
      payment: paymentBody,
      fulfillment: delivery ? 'DELIVERY' : 'PICKUP',
      ...(delivery ? { shippingAddress: { ...shipping, zipCode: address.zipCode.replace(/\D/g, '') } } : {}),
    };
  }

  const submit = useMutation({
    mutationFn: async () => {
      const payload = body();
      const json = JSON.stringify(payload);
      const keyed = withIdempotencyKey(cart, json, () => crypto.randomUUID());
      setCart(keyed);
      saveCart(storage(), userId, keyed);
      return api.request('/sales/orders', { method: 'POST', body: payload, idempotencyKey: keyed.idempotencyKey ?? undefined, schema: CreateSaleOrderResponse });
    },
    onSuccess: onCreated,
    onError: error => {
      if (error instanceof ApiError && error.code === 'STOCK_INSUFFICIENT') {
        const available = availableFromDetails(error.details);
        const messages = cart.lines.filter(line => available.has(line.productId))
          .map(line => `Estoque insuficiente: restam ${available.get(line.productId)} unidades de ${line.name}.`);
        setCart(refreshAvailability(cart, available));
        onStockProblem(messages.join(' ') || 'Estoque insuficiente para um dos itens. Revise o carrinho.');
      }
    },
  });

  function finish() {
    const check = CreateSaleOrderBody.safeParse(body());
    const local: string[] = [];
    if (!customer) local.push('Escolha o cliente da venda.');
    if (payment === 'CARD' && !/^\d{4}$/.test(cardLast4.trim())) local.push('Informe os 4 últimos dígitos do cartão.');
    if (delivery && !check.success) local.push('Preencha o endereço de entrega (rua, número, bairro, cidade, UF e CEP com 8 dígitos).');
    if (!check.success && local.length === 0) local.push('Revise os dados da venda.');
    setProblems(local);
    if (local.length === 0) submit.mutate();
  }

  const setAddressField = (key: keyof AddressForm) => (value: string) => setAddress(current => ({ ...current, [key]: value }));

  return (
    <div className="pdv">
      <section className="card stack" aria-labelledby="cliente-titulo">
        <h2 id="cliente-titulo">Cliente</h2>
        {customer ? <div className="stack-sm">
          <p><strong>{customer.name}</strong> · <span className="mono">{customer.document}</span></p>
          <div><button type="button" className="btn" onClick={() => setCustomer(null)}>Trocar cliente</button></div>
        </div> : <>
          <div className="field">
            <label htmlFor="busca-cliente">CPF, CNPJ ou nome</label>
            <input id="busca-cliente" className="input" type="search" autoComplete="off" value={term} onChange={event => setTerm(event.target.value)} />
          </div>
          <ErrorNotice error={customers.error} />
          {q.length >= 2 && customers.data && (customers.data.content.length === 0
            ? <p className="empty">Nenhum cliente encontrado.</p>
            : <ul className="results" aria-label="Clientes encontrados">
                {customers.data.content.map(found => (
                  <li key={found.id}><button type="button" onClick={() => setCustomer(found)}>
                    <span><strong>{found.name}</strong><br /><span className="mono muted">{found.document}</span></span>
                    <span className="muted">Escolher</span>
                  </button></li>
                ))}
              </ul>)}
          <div><button type="button" className="btn" onClick={() => setDrawer(true)}>Cadastrar cliente</button></div>
        </>}
        <dialog ref={dialog} className="drawer" aria-labelledby="novo-cliente" onClose={() => setDrawer(false)}>
          <div className="stack">
            <h2 id="novo-cliente">Cadastrar cliente</h2>
            {drawer && <CustomerForm idPrefix="pdv-cliente" onCancel={() => setDrawer(false)}
                                     onCreated={created => { setCustomer(created); setDrawer(false); }} />}
          </div>
        </dialog>
      </section>

      <section className="card stack" aria-labelledby="pagamento-titulo">
        <h2 id="pagamento-titulo">Pagamento e entrega</h2>
        <fieldset className="segmented">
          <legend className="sr-only">Forma de pagamento</legend>
          {(['PIX', 'CARD', 'BOLETO'] as const).map(type => (
            <label key={type}><input type="radio" name="pagamento" value={type} checked={payment === type} onChange={() => setPayment(type)} />
              <span>{label(PAYMENT_TYPE, type)}</span></label>
          ))}
        </fieldset>
        {payment === 'CARD' && <div className="form-grid">
          <Field id="final-cartao" label="4 últimos dígitos">
            <input id="final-cartao" className="input" inputMode="numeric" maxLength={4} autoComplete="off" value={cardLast4}
                   onChange={event => setCardLast4(event.target.value.replace(/\D/g, ''))} />
          </Field>
          <Field id="parcelas" label="Parcelas">
            <select id="parcelas" className="select" value={installments} onChange={event => setInstallments(event.target.value)}>
              {Array.from({ length: 12 }, (_, i) => String(i + 1)).map(n => <option key={n} value={n}>{n}x</option>)}
            </select>
          </Field>
          <Field id="autorizacao" label="Código de autorização (opcional)">
            <input id="autorizacao" className="input" maxLength={20} autoComplete="off" value={authorizationCode}
                   onChange={event => setAuthorizationCode(event.target.value.replace(/[^A-Za-z0-9]/g, ''))} />
          </Field>
        </div>}
        {payment === 'BOLETO' && <Field id="vencimento" label="Vencimento">
          <input id="vencimento" className="input" type="date" min={inDays(0)} value={dueDate} onChange={event => setDueDate(event.target.value)} />
        </Field>}
        <fieldset className="segmented">
          <legend className="sr-only">Retirada ou entrega</legend>
          <label><input type="radio" name="entrega" checked={!delivery} onChange={() => setDelivery(false)} /><span>Retirada na loja</span></label>
          <label><input type="radio" name="entrega" checked={delivery} onChange={() => setDelivery(true)} /><span>Entrega</span></label>
        </fieldset>
        {delivery && <div className="form-grid">
          {([['street', 'Rua'], ['number', 'Número'], ['complement', 'Complemento (opcional)'], ['neighborhood', 'Bairro'],
            ['city', 'Cidade'], ['state', 'UF'], ['zipCode', 'CEP']] as const).map(([key, text]) => (
            <Field key={key} id={`entrega-${key}`} label={text}>
              <input id={`entrega-${key}`} className="input" value={address[key]} maxLength={key === 'state' ? 2 : 120}
                     onChange={event => setAddressField(key)(key === 'state' ? event.target.value.toUpperCase() : event.target.value)} />
            </Field>
          ))}
        </div>}
        {problems.length > 0 && <div className="notice notice-error" role="alert">{problems.map(p => <p key={p}>{p}</p>)}</div>}
        {submit.error && !(submit.error instanceof ApiError && submit.error.code === 'STOCK_INSUFFICIENT') &&
          <p className="notice notice-error" role="alert">{errorMessage(submit.error)}</p>}
        <button type="button" className="btn btn-primary btn-block" onClick={finish} disabled={submit.isPending}>
          {submit.isPending ? 'Criando pedido…' : 'Criar pedido'}</button>
      </section>
    </div>
  );
}

// ---------------- 3. Confirmacao ----------------

function ConfirmStep({ order, onOrder, onRestart }: { order: SalesOrder; onOrder: (order: SalesOrder) => void; onRestart: () => void }) {
  const confirm = useMutation({
    mutationFn: () => api.request(`/sales/orders/${order.id}/payment`, { method: 'POST', schema: ConfirmSalePaymentResponse }),
    onSuccess: onOrder,
  });
  const cancel = useMutation({
    mutationFn: () => api.request(`/orders/${order.id}/cancel`, { method: 'POST', schema: CancelOrderResponse }),
    onSuccess: onOrder,
  });
  const pending = order.status === 'PENDING_PAYMENT';
  const busy = confirm.isPending || cancel.isPending;
  return (
    <section className="card stack" aria-labelledby="confirmacao-titulo">
      <div className="page-head">
        <h2 id="confirmacao-titulo">Venda <span className="mono">#{order.id}</span></h2>
        <span className={statusPill(order.status)}>{label(ORDER_STATUS, order.status)}</span>
      </div>
      <dl className="facts">
        <div><dt>Cliente</dt><dd>{order.customer.name}</dd></div>
        <div><dt>Pagamento</dt><dd>{label(PAYMENT_TYPE, order.payment.type)}</dd></div>
        <div><dt>Total</dt><dd className="num"><strong>{money(order.total)}</strong></dd></div>
        <div><dt>Entrega</dt><dd>{order.fulfillment === 'DELIVERY' ? 'Entrega' : 'Retirada na loja'}</dd></div>
      </dl>
      {order.status === 'PAID' && <p className="notice notice-ok" role="status">Pagamento confirmado. Estoque baixado.</p>}
      {order.status === 'CANCELED' && <p className="notice notice-warn" role="status">Pedido cancelado. A reserva foi liberada.</p>}
      {pending && order.payment.type === 'BOLETO' && <p className="muted">O boleto fica aguardando pagamento. Confirme em <Link href={`/vendas/${order.id}`}>Vendas</Link> quando ele for pago.</p>}
      <ErrorNotice error={confirm.error ?? cancel.error} />
      <div className="actions">
        {pending && order.payment.type !== 'BOLETO' &&
          <button type="button" className="btn btn-primary" disabled={busy} onClick={() => confirm.mutate()}>
            {confirm.isPending ? 'Confirmando…' : 'Confirmar pagamento recebido'}</button>}
        {pending && <button type="button" className="btn btn-danger" disabled={busy} onClick={() => cancel.mutate()}>
          {cancel.isPending ? 'Cancelando…' : 'Cancelar pedido'}</button>}
        <button type="button" className="btn" onClick={onRestart} disabled={busy}>Nova venda</button>
      </div>
    </section>
  );
}
