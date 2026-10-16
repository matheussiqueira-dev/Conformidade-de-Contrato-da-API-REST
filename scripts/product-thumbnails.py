"""Gera as miniaturas de produto do frontend a partir de fotos de licenca livre do Wikimedia Commons.

Le scripts/product-thumbnails.json (arquivo do Commons, autor e licenca de cada foto), baixa a miniatura de 330 px
pela API do Commons, recorta o quadrado central em 96 px e grava frontend/public/produtos/<file>.
Escreve frontend/lib/product-photos.ts com SKUs, nomes e creditos (pagina /creditos do frontend).
Uso, com Pillow num ambiente virtual: python scripts/product-thumbnails.py <pasta vazia para os downloads>
"""
import csv, json, os, sys, time, urllib.error, urllib.parse, urllib.request
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "frontend", "public", "produtos")
LIB = os.path.join(ROOT, "frontend", "lib", "product-photos.ts")
SEED = {"IP-128-PT": "iPhone 128 GB Preto", "AP-BR": "AirPods Branco", "AW-41": "Apple Watch 41 mm",
        "CP-SIL": "Capa de silicone", "CG-20W": "Carregador 20 W", "PL-VID": "Pelicula de vidro"}
UA = {"User-Agent": "SBI-demo-thumbnails/1.0 (https://github.com/matheussiqueira-dev/Conformidade-de-Contrato-da-API-REST)"}
SIZE = 96


def get(url):
    # Pausa entre chamadas e nova tentativa no 429, como pede a politica de uso da API do Wikimedia.
    for tentativa in range(6):
        time.sleep(2)
        try:
            with urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=60) as r:
                return r.read()
        except urllib.error.HTTPError as e:
            if e.code != 429 or tentativa == 5:
                raise
            time.sleep(int(e.headers.get("Retry-After") or 30))


def main():
    tmp = sys.argv[1]
    with open(os.path.join(ROOT, "scripts", "product-thumbnails.json"), encoding="utf-8") as f:
        fotos = json.load(f)
    nomes = dict(SEED)
    with open(os.path.join(ROOT, "docs", "bd", "pacote-sql", "olist", "produtos.csv"), encoding="utf-8") as f:
        nomes.update({r["sku"]: r["nome"] for r in csv.DictReader(f)})
    os.makedirs(OUT, exist_ok=True)
    linhas = []
    for foto in fotos:
        faltam = [s for s in foto["skus"] if s not in nomes]
        if faltam:
            raise SystemExit(f"SKU sem produto: {faltam}")
        q = urllib.parse.urlencode({"action": "query", "format": "json", "titles": "File:" + foto["title"],
                                    "prop": "imageinfo", "iiprop": "url", "iiurlwidth": 330})
        pagina = next(iter(json.loads(get("https://commons.wikimedia.org/w/api.php?" + q))["query"]["pages"].values()))
        info = pagina["imageinfo"][0]
        bruto = os.path.join(tmp, foto["file"] + ".src")
        with open(bruto, "wb") as f:
            f.write(get(info["thumburl"]))
        img = Image.open(bruto).convert("RGB")
        lado = min(img.size)  # recorte quadrado central: o produto preenche a miniatura
        x, y = (img.width - lado) // 2, (img.height - lado) // 2
        img.crop((x, y, x + lado, y + lado)).resize((SIZE, SIZE), Image.LANCZOS).save(os.path.join(OUT, foto["file"]), "WEBP", quality=82, method=6)
        linhas.append({"file": foto["file"], "skus": foto["skus"], "names": [nomes[s] for s in foto["skus"]],
                       "title": foto["title"], "page": info["descriptionurl"], "author": foto["author"],
                       "license": foto["license"], "licenseUrl": foto["licenseUrl"]})
    with open(LIB, "w", encoding="utf-8", newline="\n") as f:
        f.write("// Gerado por scripts/product-thumbnails.py a partir de scripts/product-thumbnails.json; nao editar a mao.\n")
        f.write("export interface ProductPhoto { file: string; skus: string[]; names: string[]; title: string; page: string;\n")
        f.write("  author: string; license: string; licenseUrl: string }\n\n")
        f.write("export const PRODUCT_PHOTOS: ProductPhoto[] = " + json.dumps(linhas, ensure_ascii=False, indent=2) + ";\n")
    print(f"{len(linhas)} fotos, {sum(len(l['skus']) for l in linhas)} SKUs com foto")


if __name__ == "__main__":
    main()
