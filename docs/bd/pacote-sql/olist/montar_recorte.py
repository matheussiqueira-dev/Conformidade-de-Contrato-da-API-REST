"""Gera um recorte pequeno e determinístico do Brazilian E-Commerce Public Dataset (Olist)
para o histórico de clientes da demonstração do SBI, com o catálogo trocado por produtos Apple.

Fonte: https://www.kaggle.com/datasets/olistbr/brazilian-ecommerce (CC BY-NC-SA 4.0).
Uso: python montar_recorte.py [pasta_raw] [pasta_saida]  (padrão: lê raw/, grava demo/)
"""
import csv, hashlib, os, sys
from collections import defaultdict
from datetime import datetime

AQUI = os.path.dirname(os.path.abspath(__file__))
RAW = sys.argv[1] if len(sys.argv) > 1 else os.path.join(AQUI, "raw")
OUT = sys.argv[2] if len(sys.argv) > 2 else os.path.join(AQUI, "demo")
CLIENTES_RECORRENTES, CLIENTES_UNICOS, PJ_A_CADA = 40, 20, 7
JANELA_DIAS = 365

# Catálogo Apple da loja. O recorte original vendia de tudo (37 categorias, de cama e banho a pet shop); cada produto
# Olist vira um item deste catálogo: o quantil do preço Olist escolhe a faixa (FAIXAS) e o hash do produto escolhe o item
# dentro dela. O item do pedido usa o preço do catálogo: preço de mercado no varejo brasileiro pesquisado em 15/10/2026
# (à vista em revendas como Magazine Luiza, Amazon e KaBuM) ou, sem oferta encontrada, o preço da Apple Store Brasil menos
# 10% (fontes em docs/bd/massa-demonstracao.md). Smartphones, Audio, Wearables e Acessorios são as categorias do seed demo.
# (faixa, sku, nome, categoria, preço, peso em gramas)
CATALOGO = [
    (0, "OL-ADP-USBC-P2", "Adaptador USB-C para P2", "Acessorios", "89.00", 10),
    (0, "OL-PEL-IP17", "Película Belkin UltraGlass 2 para iPhone 17", "Acessorios", "359.00", 20),
    (0, "OL-CABO-USBC-1M", "Cabo USB-C de 60 W (1 m)", "Acessorios", "197.00", 30),
    (0, "OL-CABO-LTN-1M", "Cabo USB-C para Lightning (1 m)", "Acessorios", "197.00", 30),
    (0, "OL-AIRTAG-1", "AirTag (2ª geração)", "Acessorios", "332.00", 11),
    (0, "OL-PUL-ESP-46", "Pulseira esportiva para Apple Watch 46 mm", "Acessorios", "494.00", 30),
    (0, "OL-CAPA-TRP-IP17", "Capa transparente com MagSafe para iPhone 17", "Acessorios", "476.00", 40),
    (1, "OL-PUL-LOOP-42", "Pulseira loop esportiva para Apple Watch 42 mm", "Acessorios", "494.00", 20),
    (1, "OL-CAPA-SIL-IP18P", "Capa de silicone com MagSafe para iPhone 18 Pro", "Acessorios", "476.00", 40),
    (1, "OL-MAGSAFE-1M", "Carregador MagSafe (1 m)", "Acessorios", "431.00", 60),
    (1, "OL-FONTE-35W", "Adaptador de energia USB-C duplo de 35 W", "Acessorios", "503.00", 90),
    (1, "OL-FOLIO-IPAD11", "Smart Folio para iPad 11 pol.", "Acessorios", "809.00", 190),
    (1, "OL-MOUSE-USBC", "Magic Mouse (USB-C)", "Acessorios", "746.00", 100),
    (1, "OL-PENCIL-USBC", "Apple Pencil (USB-C)", "Acessorios", "809.00", 20),
    (1, "OL-AIRTAG-4", "AirTag (2ª geração, 4 unidades)", "Acessorios", "1199.00", 60),
    (1, "OL-AIRPODS-5", "AirPods 5", "Audio", "1499.00", 40),
    (1, "OL-HOMEPOD-MINI", "HomePod mini", "Casa inteligente", "999.00", 345),
    (2, "OL-PENCIL-PRO", "Apple Pencil Pro", "Acessorios", "1349.00", 20),
    (2, "OL-TECLADO-TID", "Magic Keyboard com Touch ID", "Acessorios", "1529.00", 240),
    (2, "OL-AIRPODS-5-SF", "AirPods 5 com estojo de recarga sem fio", "Audio", "1529.00", 40),
    (2, "OL-APPLETV-4K", "Apple TV 4K", "Casa inteligente", "3239.00", 210),
    (2, "OL-AIRPODS-PRO3", "AirPods Pro 3", "Audio", "1999.00", 50),
    (2, "OL-AW-SE3-40", "Apple Watch SE 3 40 mm GPS", "Wearables", "2499.00", 30),
    (3, "OL-IPAD-11-128", "iPad 11 pol. (A16) Wi-Fi 128 GB", "Tablets", "3899.00", 480),
    (3, "OL-AW-S12-42", "Apple Watch Series 12 42 mm GPS", "Wearables", "4949.00", 30),
    (3, "OL-IP17E-256", "iPhone 17e 256 GB Branco", "Smartphones", "4799.00", 167),
    (3, "OL-AIRPODS-MAX2", "AirPods Max 2", "Audio", "5699.00", 385),
    (3, "OL-IPAD-MINI-128", "iPad mini Wi-Fi 128 GB", "Tablets", "7199.00", 293),
    (3, "OL-AW-S12-46-CEL", "Apple Watch Series 12 46 mm GPS + Cellular", "Wearables", "6659.00", 40),
    (3, "OL-IP16-128", "iPhone 16 128 GB Preto", "Smartphones", "4959.00", 170),
    (4, "OL-IPAD-AIR-11", "iPad Air 11 pol. M4 Wi-Fi 128 GB", "Tablets", "7499.00", 460),
    (4, "OL-MAC-MINI-M6", "Mac mini M6 16 GB 256 GB", "Computadores", "9719.00", 670),
    (4, "OL-IP17-256", "iPhone 17 256 GB Azul", "Smartphones", "5699.00", 177),
    (4, "OL-AW-ULTRA4", "Apple Watch Ultra 4 49 mm", "Wearables", "9449.00", 61),
    (4, "OL-IPAIR-256", "iPhone Air 256 GB", "Smartphones", "7299.00", 165),
    (4, "OL-MBA13-M5", "MacBook Air 13 pol. M5 16 GB 512 GB", "Computadores", "11999.00", 1240),
    (5, "OL-IP18P-256", "iPhone 18 Pro 256 GB", "Smartphones", "10799.00", 204),
    (5, "OL-IPAD-PRO-11", "iPad Pro 11 pol. M5 Wi-Fi 256 GB", "Tablets", "15299.00", 444),
    (5, "OL-IP18PM-256", "iPhone 18 Pro Max 256 GB", "Smartphones", "11699.00", 233),
    (5, "OL-IMAC24-M4", "iMac 24 pol. M4 16 GB 256 GB", "Computadores", "16199.00", 4440),
    (5, "OL-MBA15-M5", "MacBook Air 15 pol. M5 16 GB 512 GB", "Computadores", "16469.00", 1510),
    (5, "OL-MBP14-M5", "MacBook Pro 14 pol. M5 16 GB 1 TB", "Computadores", "21599.00", 1550),
]
FAIXAS = (0.40, 0.68, 0.84, 0.94, 0.985)  # quantil máximo do preço Olist nas faixas 0 a 4; acima, faixa 5

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

    # Produto Olist -> item do catálogo Apple: faixa pelo quantil do preço Olist, item pelo hash dentro da faixa.
    preco_olist = {}
    for p in sel:
        for i in itens[p["order_id"]]:
            preco_olist[i["product_id"]] = max(preco_olist.get(i["product_id"], 0.0), float(i["price"]))
    ordem = sorted(preco_olist, key=lambda pid: (preco_olist[pid], h(pid)))
    apple = {}
    for pos, pid in enumerate(ordem):
        faixa = sum((pos + 0.5) / len(ordem) > q for q in FAIXAS)
        opcoes = [c for c in CATALOGO if c[0] == faixa]
        apple[pid] = opcoes[h(pid) % len(opcoes)]

    out_cli, out_ped, out_itens = [], [], []
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
            qtd = defaultdict(int)
            for i in itens[oid]:
                qtd[apple[i["product_id"]]] += 1
            for item, q in sorted(qtd.items(), key=lambda x: x[0][1]):
                out_itens.append({"pedido": ped, "sku": item[1], "quantidade": q, "preco_unitario": item[4]})

    # Custo médio sintético, só para margem e lucro aparecerem: 30% a 45% do preço nos acessórios, 62% a 73% nos aparelhos.
    prods = [{"sku": sku, "nome": nome, "categoria": cat, "preco": preco,
              "custo_medio_demo": f"{float(preco) * ((0.30 + h(sku) % 16 / 100) if cat == 'Acessorios' else (0.62 + h(sku) % 12 / 100)):.2f}",
              "peso_g": peso} for _, sku, nome, cat, preco, peso in CATALOGO]
    for nome, linhas in [("clientes", out_cli), ("produtos", prods), ("pedidos", out_ped), ("itens_pedido", out_itens)]:
        with open(os.path.join(OUT, nome + ".csv"), "w", newline="", encoding="utf-8") as f:
            w = csv.DictWriter(f, fieldnames=list(linhas[0].keys())); w.writeheader(); w.writerows(linhas)
    total = sum(int(i["quantidade"]) * float(i["preco_unitario"]) for i in out_itens)
    formas = defaultdict(int)
    for p in out_ped: formas[p["forma_pagamento"]] += 1
    print(f"clientes={len(out_cli)} (PJ={sum(c['tipo']=='CORPORATE' for c in out_cli)}) pedidos={len(out_ped)} itens={len(out_itens)} produtos={len(prods)} categorias={len({p['categoria'] for p in prods})}")
    print(f"faturamento_bruto={total:.2f} formas={dict(formas)} max_pedidos_cliente={max(len(por_cliente[u]) for u in escolhidos)}")

if __name__ == "__main__":
    main()
