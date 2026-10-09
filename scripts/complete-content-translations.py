from pathlib import Path
import re,json
root=Path(__file__).resolve().parents[1]
p=root/'frontend/src/app/core/i18n.ts';s=p.read_text(encoding='utf-8')
catalog=(root/'backend/src/main/java/rs/ac/uns/ftn/museum/config/RichDemoCatalog.java').read_text(encoding='utf-8')
authors=[x.strip().split('|') for x in re.search(r'AUTHORS = rows\("""(.*?)"""\)',catalog,re.S)[1].strip().splitlines()]
en=(root/'scripts/authors-en.txt').read_text(encoding='utf-8').splitlines()
assert len(authors)==len(en)
pairs={a[3]:e for a,e in zip(authors,en)}
pairs.update(dict(x.split('|',1) for x in '''Prvi korak kroz plavu tišinu.|A first step into blue silence.
Materijal i trag ruke.|Material and the trace of a hand.
Pogled ka gradu koji se budi.|A view towards the waking city.
Počnite obilazak odnosom bronzane forme i praznine.|Begin with the relationship between bronze form and empty space.
Uporedite teksturu kamena sa prethodnom bronzanom formom.|Compare the stone texture with the preceding bronze form.
Završite obilazak posmatranjem senke iz različitih uglova.|Finish by observing the shadow from different angles.
Autorski rad u muzejskoj zbirci.|An original work in the museum collection.
Kolektivna realizacija uz glavnog autora.|Collaborative execution with the principal artist.
Nalozi, zaposleni i muzejski program|Accounts, staff and museum programme
Svi muzeji — zbirni operativni izveštaji|All museums — consolidated operational reports
Odličan izbor radova i jasan tok obilaska.|Excellent selection of works and a clear visitor route.
Najviše su mi značila poređenja materijala.|The comparisons between materials were most useful to me.
Volela bih još objašnjenja uz pojedine radove.|I would appreciate more interpretation beside some works.
Miran prostor, ostao sam duže nego što sam planirao.|A peaceful space; I stayed longer than planned.
Zanimljiva tema, ali mi redosled nije svuda bio jasan.|An interesting theme, though the order was not always clear to me.
Vraćam se zbog drugačijeg čitanja istog dela.|I return to discover different readings of the same work.
Deci su posebno prijale kratke priče uz postavke.|The children especially enjoyed the short installation stories.
Nisam uspeo da se povežem sa temom ove izložbe.|I could not connect with this exhibition's theme.'''.splitlines()))
s=s.replace('for(const [index,[sr,en]] of Object.entries(COLLECTION_EN).entries())','Object.assign(COLLECTION_EN,'+json.dumps(pairs,ensure_ascii=False,indent=2)+');\nfor(const [index,[sr,en]] of Object.entries(COLLECTION_EN).entries())')
s=s.replace('// Shared with non-injectable presentation helpers','SR["content.pathHint"]="collection/grad-001.txt";EN["content.pathHint"]="collection/city-001.txt";\nSR["content.languageHint"]="sr";EN["content.languageHint"]="en";\n// Shared with non-injectable presentation helpers')
p.write_text(s,encoding='utf-8')
for p in (root/'frontend/src/app').rglob('*.ts'):
 if p.name.endswith('.spec.ts'):continue
 s=p.read_text(encoding='utf-8')
 s=s.replace('placeholder="sr"','[attr.placeholder]="\'content.languageHint\'|t"').replace('placeholder="collection/grad-001.txt"','[attr.placeholder]="\'content.pathHint\'|t"')
 if 'template:' in s:
  s=re.sub(r'{{([\w.]+)\.(type|note|comment)}}',r'{{\1.\2|t}}',s)
  s=re.sub(r'{{(name\(\)|exhibitionName\(\)|museumName\([^{}]+\))}}',r'{{\1|t}}',s)
  s=s.replace("+' '+e.name", "+' '+(e|localized:'name')")
  s=s.replace('<p><code>{{q.token}}</code></p>','')
  s=s.replace('imports:[LocalizedContentPipe,UiLabelPipe,Feedback,TranslatePipe],template:`<section class="page-heading"><p class="eyebrow">{{\'uiText.guide.workspace\'|t}}','imports:[Feedback,TranslatePipe],template:`<section class="page-heading"><p class="eyebrow">{{\'uiText.guide.workspace\'|t}}')
 p.write_text(s,encoding='utf-8')
p=root/'frontend/src/app/pages/catalog.ts';s=p.read_text(encoding='utf-8').replace('import { translate }','import { localizedContent, translate }').replace("`${e.name} ${e.description||''}`", "`${localizedContent(e,'name')} ${localizedContent(e,'description')}`");p.write_text(s,encoding='utf-8')
p=root/'frontend/src/app/pages/events.ts';s=p.read_text(encoding='utf-8').replace('import { translate }','import { localizedContent, translate }').replace("e.name.toLocaleLowerCase('sr')", "localizedContent(e,'name').toLocaleLowerCase()");p.write_text(s,encoding='utf-8')
