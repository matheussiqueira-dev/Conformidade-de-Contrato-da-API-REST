"""Gera um recorte pequeno e determinístico do Brazilian E-Commerce Public Dataset (Olist)
para o histórico de clientes da demonstração do SBI.

Fonte: https://www.kaggle.com/datasets/olistbr/brazilian-ecommerce (CC BY-NC-SA 4.0).
Uso: python montar_recorte.py  (lê raw/, grava demo/)
"""
import csv, hashlib, os
from collections import defaultdict
from datetime import datetime

AQUI = os.path.dirname(os.path.abspath(__file__))
RAW, OUT = os.path.join(AQUI, "raw"), os.path.join(AQUI, "demo")
CLIENTES_RECORRENTES, CLIENTES_UNICOS, PJ_A_CADA = 40, 20, 7
JANELA_DIAS = 365

def ler(nome):
    with open(os.path.join(RAW, nome + ".csv"), encoding="utf-8-sig") as f:
        return list(csv.DictReader(f))

def h(texto):
    return int(hashlib.sha256(texto.encode()).hexdigest(), 16)

def dv(base, pesos):
    s = sum(int(d) * p for d, p in zip(base, pesos)) % 11
    return "0" if s < 2 else str(11 - s)

def cpf(seed):
    b = f"{seed % 10**9:09d}"
    if len(set(b)) == 1: b = "1" + b[1:]
    b += dv(b, range(10, 1, -1)); b += dv(b, range(11, 1, -1))
    return b

def cnpj(seed):
    b = f"{seed % 10**8:08d}0001"
    b += dv(b, [5,4,3,2,9,8,7,6,5,4,3,2]); b += dv(b, [6,5,4,3,2,9,8,7,6,5,4,3,2])
    return b

NOMES = ["Ana","Bruno","Carla","Diego","Elisa","Fábio","Gabriela","Heitor","Isabela","João","Larissa","Marcos",
         "Natália","Otávio","Paula","Rafael","Sofia","Tiago","Vanessa","Wagner","Yasmin","Lucas","Camila","Pedro"]
SOBRENOMES = ["Albuquerque","Barbosa","Cavalcanti","Duarte","Esteves","Ferraz","Gusmão","Holanda","Lacerda",
              "Macedo","Nogueira","Pacheco","Queiroz","Rezende","Siqueira","Teixeira","Valença","Xavier"]
RAMOS = ["Comércio","Distribuidora","Papelaria","Utilidades","Materiais","Serviços"]
FORMA = {"credit_card": "CARD", "debit_card": "CARD", "boleto": "BOLETO", "voucher": "PIX"}

def main():
    os.makedirs(OUT, exist_ok=True)
    clientes = {c["customer_id"]: c for c in ler("olist_customers_dataset")}
    pedidos = [p for p in ler("olist_orders_dataset") if p["order_status"] == "delivered"]
    itens, pags = defaultdict(list), defaultdict(list)
    for i in ler("olist_order_items_dataset"): itens[i["order_id"]].append(i)
    for p in ler("olist_order_payments_dataset"): pags[p["order_id"]].append(p)
    produtos = {p["product_id"]: p for p in ler("olist_products_dataset")}
    trad = {t["product_category_name"]: t["product_category_name_english"] for t in ler("product_category_name_translation")}

    por_cliente = defaultdict(list)
    for p in pedidos:
        if itens[p["order_id"]] and pags[p["order_id"]] and all(produtos.get(i["product_id"], {}).get("product_category_name") for i in itens[p["order_id"]]):
            por_cliente[clientes[p["customer_id"]]["customer_unique_id"]].append(p)

    recorrentes = sorted((u for u, ps in por_cliente.items() if len(ps) >= 2), key=lambda u: (-len(por_cliente[u]), h(u)))[:CLIENTES_RECORRENTES]
    unicos = sorted((u for u, ps in por_cliente.items() if len(ps) == 1), key=h)[:CLIENTES_UNICOS]
    escolhidos = recorrentes + unicos

    sel = [p for u in escolhidos for p in por_cliente[u]]
    datas = [datetime.fromisoformat(p["order_purchase_timestamp"]) for p in sel]
    ini, fim = min(datas), max(datas)
    escala = JANELA_DIAS / max((fim - ini).days, 1)

    out_cli, out_ped, out_itens, prods = [], [], [], {}
    for n, u in enumerate(escolhidos, 1):
        cod = f"CLI-{n:03d}"; s = h(u); orig = clientes[por_cliente[u][0]["customer_id"]]
        pj = n % PJ_A_CADA == 0
        nome = f"{SOBRENOMES[s % len(SOBRENOMES)]} {RAMOS[s % len(RAMOS)]} Ltda" if pj else f"{NOMES[s % len(NOMES)]} {SOBRENOMES[(s // 7) % len(SOBRENOMES)]}"
        email = (f"contato.{cod.lower()}" if pj else f"{NOMES[s % len(NOMES)]}.{cod.lower()}").lower()
        email = email.translate(str.maketrans("áéíóúâêôãõç", "aeiouaeoaoc")) + "@example.com"
        out_cli.append({"codigo": cod, "tipo": "CORPORATE" if pj else "INDIVIDUAL", "nome": nome,
                        "cpf": "" if pj else cpf(s), "cnpj": cnpj(s) if pj else "", "email": email,
                        "cidade": orig["customer_city"].title(), "uf": orig["customer_state"], "cep_prefixo": orig["customer_zip_code_prefix"]})
        for p in sorted(por_cliente[u], key=lambda x: x["order_purchase_timestamp"]):
            oid = p["order_id"]; dt = datetime.fromisoformat(p["order_purchase_timestamp"])
            pg = max(pags[oid], key=lambda x: float(x["payment_value"]))
            forma = FORMA.get(pg["payment_type"], "PIX")
            ped = f"PED-{len(out_ped) + 1:04d}"
            out_ped.append({"codigo": ped, "cliente": cod, "vendedor": f"VENDEDOR_{h(oid) % 3 + 1}",
                            "dias_atras": round((fim - dt).days * escala), "hora": dt.strftime("%H:%M"),
                            "status": "PAID", "forma_pagamento": forma,
                            "parcelas": int(pg["payment_installments"] or 1) if forma == "CARD" else 1,
                            "cartao_ultimos4": f"{h(oid) % 10000:04d}" if forma == "CARD" else "",
                            "entrega": "DELIVERY" if h(oid + "e") % 3 else "PICKUP", "origem_olist": oid})
            qtd = defaultdict(lambda: [0, 0.0])
            for i in itens[oid]:
                pr = produtos[i["product_id"]]; sku = "OL-" + i["product_id"][:6].upper()
                cat = pr["product_category_name"]
                if sku not in prods:
                    prods[sku] = {"sku": sku, "nome": f"{cat.replace('_', ' ').capitalize()} {sku[3:]}",
                                  "categoria": cat.replace("_", " ").capitalize(), "categoria_en": trad.get(cat, ""),
                                  "preco": i["price"], "custo_medio_demo": f"{float(i['price']) * (0.55 + (h(sku) % 16) / 100):.2f}",
                                  "peso_g": pr["product_weight_g"], "origem_olist": i["product_id"]}
                qtd[sku][0] += 1; qtd[sku][1] = float(i["price"])
            for sku, (q, preco) in sorted(qtd.items()):
                out_itens.append({"pedido": ped, "sku": sku, "quantidade": q, "preco_unitario": f"{preco:.2f}"})

    for nome, linhas in [("clientes", out_cli), ("produtos", list(prods.values())), ("pedidos", out_ped), ("itens_pedido", out_itens)]:
        with open(os.path.join(OUT, nome + ".csv"), "w", newline="", encoding="utf-8") as f:
            w = csv.DictWriter(f, fieldnames=list(linhas[0].keys())); w.writeheader(); w.writerows(linhas)
    total = sum(int(i["quantidade"]) * float(i["preco_unitario"]) for i in out_itens)
    formas = defaultdict(int)
    for p in out_ped: formas[p["forma_pagamento"]] += 1
    print(f"clientes={len(out_cli)} (PJ={sum(c['tipo']=='CORPORATE' for c in out_cli)}) pedidos={len(out_ped)} itens={len(out_itens)} produtos={len(prods)} categorias={len({p['categoria'] for p in prods.values()})}")
    print(f"faturamento_bruto={total:.2f} formas={dict(formas)} max_pedidos_cliente={max(len(por_cliente[u]) for u in escolhidos)}")

if __name__ == "__main__":
    main()
